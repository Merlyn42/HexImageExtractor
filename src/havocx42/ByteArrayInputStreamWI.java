package havocx42;

import java.io.ByteArrayInputStream;

public class ByteArrayInputStreamWI extends ByteArrayInputStream {

	public ByteArrayInputStreamWI(byte[] buf) {
		super(buf);
		// TODO Auto-generated constructor stub
	}

	public ByteArrayInputStreamWI(byte[] buf, int offset, int length) {
		super(buf, offset, length);
		// TODO Auto-generated constructor stub
	}

	public int getIndex() {
		return pos;

	}

}
