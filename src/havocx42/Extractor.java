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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Extractor implements Runnable {
	private File sourceFile;
	private File scratchRoot;
	private File outputRoot;
	private File outputLocation;
	private File scratchLocation;
	private String workingName;
	private HarcFile harcF;

	public Extractor(File sf, File sr, File or) {
		sourceFile = sf;
		scratchRoot = sr;
		outputRoot = or;
		harcF = new HarcFile(sourceFile);
		workingName = harcF.getFile().getName().substring(0, harcF.getFile().getName().length() - 5);
		scratchLocation = new File(scratchRoot, workingName);
		scratchLocation.deleteOnExit();
		outputLocation = new File(outputRoot, workingName);
	}

	@Override
	public void run() {
		System.out.println("STARTING EXTRACTION WORKER FOR " + sourceFile.getName());
		try {

			System.out.println("Extracting " + harcF.getFile().getAbsolutePath() + " to " + scratchLocation.getAbsolutePath());
			List<File> bundleFiles;

			bundleFiles = harcF.expandTo(scratchLocation);
			File batchFile = writeBatch(bundleFiles);
			decompress(batchFile);
			File[] assetsFiles = getDecompressedFiles();

			runConverters(assetsFiles);

			for (File f : scratchLocation.listFiles()) {
				//f.deleteOnExit();
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

	/**
	 * @param assetsFiles
	 * @throws InterruptedException
	 */
	private void runConverters(File[] assetsFiles) throws InterruptedException {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
		for (File asset : assetsFiles) {
			Runnable worker = new Converter(asset, outputLocation, scratchLocation);
			executor.execute(worker);
		}
		
		executor.shutdown();
		while (!executor.isTerminated()) {
			Thread.sleep(3000);
			System.out
					.println(sourceFile.getName() + " converting " + executor.getCompletedTaskCount() + "/" + executor.getTaskCount());
		}
	}

	/**
	 * @return
	 */
	private File[] getDecompressedFiles() {
		File[] assetsFiles = scratchLocation.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".assets") && !name.contains(".resource");
			}
		});
		return assetsFiles;
	}

	/**
	 * @param batchFile
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private void decompress(File batchFile) throws IOException, InterruptedException {
		System.out.println("Calling UABE to decompress ab files");
		File UABEF = new File(ImageExtractor.UABE_PATH+ImageExtractor.UABE_COMMAND);
		System.out.println(UABEF.getAbsolutePath()+" "+ "batchexport " + batchFile.getAbsolutePath());
		ProcessBuilder pb = new ProcessBuilder(UABEF.getAbsolutePath(), "batchexport",batchFile.getAbsolutePath());
		System.out.println(new File(ImageExtractor.UABE_PATH).getAbsolutePath());
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
	}

	/**
	 * @param bundleFiles
	 * @return
	 * @throws IOException
	 */
	private File writeBatch(List<File> bundleFiles) throws IOException {
		File batchFile = new File(scratchLocation, "batch");
		FileWriter batchWriter = new FileWriter(batchFile);
		for (File bundle : bundleFiles) {
			batchWriter.write("+FILE " + bundle.getAbsolutePath() + "\r\n");
		}
		batchWriter.flush();
		batchWriter.close();
		return batchFile;
	}

	private static BufferedReader getOutput(Process p) {
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}

	private static BufferedReader getError(Process p) {
		return new BufferedReader(new InputStreamReader(p.getErrorStream()));
	}

}
