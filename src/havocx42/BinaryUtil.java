package havocx42;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BinaryUtil {

	public static String getUntil(File f, byte[] pattern) throws IOException {
		FileInputStream fr = new FileInputStream(f);
		StringBuilder sb = new StringBuilder();
		byte[] buffer = new byte[1024];
		int bytesRead = fr.read(buffer, 0, 1024);
		while (bytesRead != -1) {
			sb.append(new String(buffer));
			int end = sb.indexOf(new String(pattern));
			if (end != -1) {
				fr.close();
				return sb.substring(0, end);
			}
			bytesRead = fr.read(buffer, 0, 1024);
		}
		fr.close();
		return null;
	}

	public static long getOffset(byte[] data, byte[] pattern, int alignment) {

		for (int i = 0; i < data.length - pattern.length; i += alignment) {
			for (int j = 0; j <= pattern.length; ++j) {
				if (j == pattern.length) {
					return i;
				}
				if (data[i + j] != pattern[j]) {
					break;
				}
			}
		}
		return -1;
	}

	public static long findOffset(ByteArrayInputStreamWI data, byte[] pattern)
			throws IOException {
		long curPos = 0;
		while (true) {
			int byteRead;
			byteRead = data.read();
			curPos = data.getIndex();
			if (byteRead == -1) {
				return -1;
			}
			if (byteRead == pattern[0]) {
				for (int j = 1; j <= pattern.length; ++j) {
					byteRead = data.read();
					if (j == pattern.length) {
						data.reset();
						data.skip(curPos - 1);
						return curPos - 1;
					}
					if (byteRead != pattern[j]) {
						data.reset();
						data.skip(curPos);
						break;
					}

				}
			}
		}
	}

	public static boolean byteArrCompare(byte[] first, byte[] second) {
		if (first.length != second.length) {
			return false;
		}
		for (int i = 0; i < first.length; i++) {
			if (first[i] != second[i]) {
				return false;
			}
		}
		return true;
	}

	public static int readIntLE(InputStream is) throws IOException {
		return is.read() + is.read() * 0x100 + is.read() * 0x100 * 0x100
				+ is.read() * 0x100 * 0x100 * 0x100;

	}

	public static void writeIntLE(OutputStream os, int value)
			throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dout = new DataOutputStream(bos);
		dout.writeInt(value);
		byte[] arr = bos.toByteArray();
		os.write(arr[3]);
		os.write(arr[2]);
		os.write(arr[1]);
		os.write(arr[0]);
	}

}
