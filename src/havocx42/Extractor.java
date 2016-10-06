package havocx42;

import havocx42.datatypes.AssetsFile;
import havocx42.datatypes.HarcFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Extractor implements Runnable {
	private File sourceFile;
	private File scratchRoot;
	private File outputRoot;

	public Extractor(File sf, File sr, File or) {
		sourceFile = sf;
		scratchRoot = sr;
		outputRoot = or;
	}

	@Override
	public void run() {
		System.out.println("STARTING EXTRACTION WORKER FOR "+sourceFile.getName());
		try {
			HarcFile harcF = new HarcFile(sourceFile);
			File scratchLocation = new File(scratchRoot, harcF.getFile()
					.getName()
					.substring(0, harcF.getFile().getName().length() - 5));
			scratchLocation.deleteOnExit();
			File outputLocation = new File(outputRoot, harcF.getFile()
					.getName()
					.substring(0, harcF.getFile().getName().length() - 5));
			System.out.println("Extracting "
					+ harcF.getFile().getAbsolutePath() + " to "
					+ scratchLocation.getAbsolutePath());
			List<File> bundleFiles;

			bundleFiles = harcF.extractTo(scratchLocation);

			File batchFile = new File(scratchLocation, "batch");
			FileWriter batchWriter = new FileWriter(batchFile);
			for (File bundle : bundleFiles) {
				batchWriter.write("+FILE " + bundle.getAbsolutePath() + "\n");
			}
			batchWriter.close();
			System.out.println("Calling UABE to decompress ab files");
			ProcessBuilder pb = new ProcessBuilder(ImageExtractor.UABE_PATH
					+ ImageExtractor.UABE_COMMAND, "batchexport", "\""
					+ batchFile.getAbsolutePath() + "\"");
			pb.directory(new File(ImageExtractor.UABE_PATH));
			Process proc = pb.start();
			if (true) {
				BufferedReader output = getOutput(proc);
				BufferedReader error = getError(proc);
				String ligne = "";
				while ((ligne = output.readLine()) != null) {
					System.out.println(ligne);
				}
				while ((ligne = error.readLine()) != null) {
					System.out.println(ligne);
				}
			}
			proc.waitFor();
			File[] assetsFiles = scratchLocation
					.listFiles(new FilenameFilter() {
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".assets")
									&& !name.contains(".resource");
						}
					});
			ExecutorService executor = Executors.newFixedThreadPool(4);
			for (File asset : assetsFiles) {

				Runnable worker = new Converter(asset, outputLocation,
						scratchLocation);
				executor.execute(worker);

			}
			executor.shutdown();
			while (!executor.isTerminated()) {

			}
			
			for (File f : scratchLocation.listFiles()) {
				f.deleteOnExit();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			System.err.println("Failed to extract " + sourceFile.getName());
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.err.println("Failed to extract " + sourceFile.getName());
			e.printStackTrace();
		}

	}

	private static BufferedReader getOutput(Process p) {
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}

	private static BufferedReader getError(Process p) {
		return new BufferedReader(new InputStreamReader(p.getErrorStream()));
	}

}
