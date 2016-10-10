package havocx42.datatypes;

import havocx42.BinaryUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HarcFile {
	static final byte[] MWORD = { 'H', 'A', 'R', 'C', '\r', '\n' };
	static final byte[] END_HEADER = { '\r', '\n', '\r', '\n' };

	private class FileMetaData {
		String name;
		public int offset;
		public int length;

		FileMetaData(String name, int offset, int length) {
			this.name = name;
			this.offset = offset;
			this.length = length;
		}

	}

	private File file;
	private List<FileMetaData> headerList = null;
	private int headerLength;

	private List<FileMetaData> getHeader() throws IOException {
		if (headerList == null) {
			populateHeaderList();
		}
		return headerList;
	}

	private void populateHeaderList() throws IOException {
		String headerS = BinaryUtil.getUntil(file, END_HEADER);
		headerLength = headerS.length();
		headerS = headerS.substring(6);
		// store offset
		headerList = new ArrayList<FileMetaData>();
		String[] header = headerS.split("\r\n");
		for (String meta : header) {
			String[] split = meta.split(",");
			headerList.add(new FileMetaData(split[2].replaceFirst("\\./", ""),
					Integer.parseInt(split[0]), Integer.parseInt(split[1])));
		}

	}

	public HarcFile(File file) {
		if (!confirmFileType(file)) {
			throw new IllegalArgumentException(file.getAbsolutePath()
					+ " is not a HARC file");
		}
		this.file = file;

	}

	public File getFile() {
		return file;

	}

	public List<File> expandTo(File target) throws IOException {
		if (target.exists() && !target.isDirectory()) {
			throw new IllegalArgumentException("Target is not a directory!");
		}
		if (!target.exists()) {
			target.mkdirs();
		}

		populateHeaderList();
		FileInputStream fr = new FileInputStream(file);
		fr.skip(headerLength + END_HEADER.length);
		ArrayList<File> bundleFiles = new ArrayList<File>();
		for (FileMetaData meta : getHeader()) {
			byte[] writeBuffer = new byte[meta.length];
			int bytesRead = fr.read(writeBuffer, 0, meta.length);
			fr.skip(5);
			if (bytesRead != meta.length) {
				throw new IOException("Premature end of HARC file!");
			}
			// write file
			File out = new File(target, meta.name);
			FileOutputStream fw = new FileOutputStream(out);
			fw.write(writeBuffer);
			fw.close();
			bundleFiles.add(out);
		}
		fr.close();
		return bundleFiles;
	}

	public List<String> getFileListing() throws IOException {
		ArrayList<String> listing = new ArrayList<String>();
		for (FileMetaData meta : getHeader()) {
			listing.add(meta.name);
		}
		return listing;
	}

	private boolean confirmFileType(File f) {
		boolean result = false;
		FileInputStream fr = null;
		try {
			fr = new FileInputStream(f);
			byte[] head = new byte[6];

			fr.read(head, 0, 6);
			if (BinaryUtil.byteArrCompare(head, MWORD)) {
				result = true;
			}
			fr.close();
		} catch (IOException e) {
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e1) {
				}
			}
		}
		return result;
	}

}
