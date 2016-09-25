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

			for (int i = 0; i < assetsFiles.length; i++) {
				File assets = assetsFiles[i];
				AssetsFile assetsF = new AssetsFile(assets);
				if (i % 10 == 0) {
					System.out.println("converting " + i + "/"
							+ assetsFiles.length + ":" + assets.getName());
				}
				List<File> extractedFiles;
				try {

					extractedFiles = assetsF.extractTo(scratchLocation);
				} catch (FailedExtractionException e) {
					System.err.println("Skipping: "
							+ assetsF.getFile().getName());
					continue;
				}
				for (File ddsFile : extractedFiles) {
					ConvertDDS(ddsFile, outputLocation);
				}
			}
			for (File f : scratchLocation.listFiles()) {
				f.deleteOnExit();
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			System.err.println("Failed to extract "+sourceFile.getName());
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.err.println("Failed to extract "+sourceFile.getName());
			e.printStackTrace();
		}

	}

	private static BufferedReader getOutput(Process p) {
		return new BufferedReader(new InputStreamReader(p.getInputStream()));
	}

	private static BufferedReader getError(Process p) {
		return new BufferedReader(new InputStreamReader(p.getErrorStream()));
	}

	private static void ConvertDDS(File dds, File targetDir)
			throws InterruptedException, IOException {
		if (targetDir.exists() && !targetDir.isDirectory()) {
			throw new IllegalArgumentException("Target is not a directory!");
		}
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		String ddsName = dds.getName();
		int extIndex = ddsName.indexOf(".dds");
		String targetName = ddsName + ".png";
		if (extIndex != -1) {
			targetName = ddsName.substring(0, extIndex) + ".png";
		}
		File targetFile = new File(targetDir, targetName);
		ProcessBuilder pb2 = new ProcessBuilder(ImageExtractor.MAGICK_COMMAND, "convert",
				"-flip", "\"" + dds.getAbsolutePath() + "\"", "\""
						+ targetFile.getAbsolutePath() + "\"");
		Process proc2 = pb2.start();
		proc2.waitFor();

	}

}
