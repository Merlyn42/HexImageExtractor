package havocx42;

import havocx42.datatypes.AssetsFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Converter implements Runnable {

	private File assets;
	private File scratchLocation;
	private File outputLocation;

	public Converter(File assets, File outputLocation, File scratchLocation) {
		this.assets = assets;
		this.outputLocation = outputLocation;
		this.scratchLocation = scratchLocation;

	}

	@Override
	public void run() {
		AssetsFile assetsF = new AssetsFile(assets);
		List<File> extractedFiles;
		try {
			extractedFiles = assetsF.extractTo(scratchLocation);
			for (File ddsFile : extractedFiles) {
				ConvertDDS(ddsFile, outputLocation);
			}
		} catch (FailedExtractionException e) {
			System.err.println("Skipping: " + assetsF.getFile().getName());
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void ConvertDDS(File dds, File targetDir) throws InterruptedException, IOException {
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
		ProcessBuilder pb2 = new ProcessBuilder(ImageExtractor.MAGICK_COMMAND, "convert", "-flip", "\"" + dds.getAbsolutePath() + "\"",
				"\"" + targetFile.getAbsolutePath() + "\"");
		Process proc2 = pb2.start();
		proc2.waitFor();

	}

}
