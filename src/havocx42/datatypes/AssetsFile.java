package havocx42.datatypes;

import havocx42.BinaryUtil;
import havocx42.ByteArrayInputStreamWI;
import havocx42.FailedExtractionException;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class AssetsFile {
	private File file;
	private byte[] data;

	public AssetsFile(File file) {
		super();
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public List<File> extractTo(File target) throws IOException,
			FailedExtractionException {
		if (target.exists() && !target.isDirectory()) {
			throw new IllegalArgumentException("Target is not a directory!");
		}
		if (!target.exists()) {
			target.mkdirs();
		}
		RandomAccessFile sourceF = new RandomAccessFile(file, "r");
		data = new byte[(int) sourceF.length()];
		sourceF.readFully(data);
		sourceF.close();

		ArrayList<File> extractedFiles = new ArrayList<File>();
		// discovery
		String fname = file.getName().substring(0,
				file.getName().indexOf("_CAB"));
		List<String> texFileNames = getTexFileNames(fname);
		for (String texName : texFileNames) {

			String name = texName.substring(0, 1)
					+ texName.substring(1).toUpperCase();
			byte[] nameBytes = name.getBytes();
			byte[] pattern = new byte[nameBytes.length + 4];
			pattern[0] = (byte) name.length();
			for (int x = 0; x < nameBytes.length; x++) {
				pattern[4 + x] = nameBytes[x];
			}
			long offset = BinaryUtil.getOffset(data, pattern, 4);
			DataInputStream di = new DataInputStream(new ByteArrayInputStream(
					data));
			long bytesToSkip = offset + pattern.length;
			if (bytesToSkip % 4 != 0) {
				bytesToSkip = bytesToSkip + (4 - (bytesToSkip % 4));
			}
			di.skip(bytesToSkip);
			int width = BinaryUtil.readIntLE(di);
			int height = BinaryUtil.readIntLE(di);
			int imageSize = BinaryUtil.readIntLE(di);
			DXTHeader.TextureType format = null;
			try {
				format = DXTHeader.TextureType.convertFromUnity(BinaryUtil
						.readIntLE(di));
			} catch (Exception e) {
				throw new FailedExtractionException(
						"Unable to extract the format type", e);
			}
			// read image data
			di.skip(0x2c);
			byte[] imageData = new byte[imageSize];
			di.read(imageData, 0, imageSize);
			di.close();
			// write image data
			File out = new File(target, name + ".dds");
			DXTHeader dxtheader = new DXTHeader(width, height, imageSize,
					format);
			FileOutputStream fw = new FileOutputStream(out);
			fw.write(dxtheader.toByteArray());
			fw.write(imageData);
			fw.close();
			extractedFiles.add(out);
		}
		return extractedFiles;
	}

	private List<String> getTexFileNames(String name) throws IOException {
		ByteArrayInputStreamWI bis = new ByteArrayInputStreamWI(data);
		ArrayList<String> texFileNames = new ArrayList<String>();
		String discPattern = "/" + name + "/";
		long discOffset = 0;
		discOffset = BinaryUtil.findOffset(bis, discPattern.getBytes());
		while (discOffset != -1) {
			long extOffset = BinaryUtil.findOffset(bis, ".".getBytes());
			// sanity check
			long nameOffset = discOffset + discPattern.length();
			int nameLength = (int) (extOffset - nameOffset);
			if ((nameLength) > 300 || (nameLength) < 1) {
				throw new RuntimeException("wtf!");
			}
			// confirm the extension
			byte[] ext = new byte[4];
			bis.read(ext, 0, 4);
			if (new String(ext).equals(".png")) {
				byte[] fileNameBArray = new byte[nameLength];
				bis.reset();
				bis.skip(nameOffset);
				bis.read(fileNameBArray, 0, nameLength);
				texFileNames.add(new String(fileNameBArray));
			}
			discOffset = BinaryUtil.findOffset(bis, discPattern.getBytes());
		}
		bis.close();
		return texFileNames;

	}

}
