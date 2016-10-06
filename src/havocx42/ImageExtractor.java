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

public class ImageExtractor implements Runnable {

	/**
	 * @param args
	 * @throws FileNotFoundException
	 */

	public static final String UABE_COMMAND = "AssetBundleExtractor.exe";
	public static final String UABE_PATH = "./tool/UABE/";
	public static final String MAGICK_COMMAND = "./tool/magick/magick.exe";
	public static final File SCRATCH = new File("scratch");
	public static final File DEFAULT_OUTPUT = new File("images");

	public static void main(String[] args)throws IOException,
			InterruptedException {

		// safety checks
		File scratchRoot = SCRATCH;
		if (!scratchRoot.exists()) {
			scratchRoot.mkdir();
		}
		File outputRoot = DEFAULT_OUTPUT;
		if (!outputRoot.exists()) {
			outputRoot.mkdir();
		}

		if (args.length < 1) {
			System.out.println("Provide Source Location");
			return;
		}
		
		File sourceLocation = new File(args[0]);
		File[] harcFiles = sourceLocation.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".harc");
			}
		});
		ExecutorService executor = Executors.newFixedThreadPool(4);
		for (File sourceFile : harcFiles) {			
			Runnable worker = new Extractor(sourceFile,scratchRoot,outputRoot);
			executor.execute(worker);			
		}
		executor.shutdown();
		while(!executor.isTerminated()){
		Thread.sleep(3000);
		}
		System.out.println("Complete");
	}



	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
