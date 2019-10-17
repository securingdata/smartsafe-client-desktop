package connection;

import util.StringHex;

public class APDUResponse extends StringHex {
	public APDUResponse(byte[] data) {
		super(data);
	}
	
	public short getStatusWord() {
		return (short) (((data.get(data.size() - 2) << 8) & 0xFF00) | (data.get(data.size() - 1) & 0xFF));
	}
	public byte[] getData() {
		byte[] bArray = new byte[data.size() - 2];
		for (int i = 0; i < bArray.length; i++)
			bArray[i] = data.get(i);
		return bArray;
	}
}
