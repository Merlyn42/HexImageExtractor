package havocx42.datatypes;

public class FileMetaData {
	String name;
	public int offset;
	public int length;

	FileMetaData(String name, int offset, int length) {
		this.name = name;
		this.offset = offset;
		this.length = length;
	}

}
