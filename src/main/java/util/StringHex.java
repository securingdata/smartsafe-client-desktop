package util;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class StringHex {
	public List<Byte> data;
	
	public StringHex(String string) {
		string = string.replaceAll(" ", "");
		if (string.length() % 2 != 0)
			string = "0" + string;
		this.data = new ArrayList<>(string.length() / 2);
		for (int i = 0; i < string.length(); i += 2) {
			data.add(Short.valueOf(string.substring(i, i + 2), 16).byteValue());
		}
	}
	public StringHex(byte[] data) {
		this(data, 0, data.length);
	}
	public StringHex(byte[] data, int offset, int length) {
		this.data = new ArrayList<>(length);
		for (int i = offset; i < offset + length; i++)
			this.data.add(data[i]);
	}
	public StringHex(byte data) {
		this(new byte[] {data});
	}
	public StringHex(short data) {
		this(new byte[] {(byte) ((data >> 8) & 0xFF), (byte) (data & 0xFF)});
	}
	
	public StringHex get(int from, int length) {
		return new StringHex(toBytes(), from, length);
	}
	public byte get(int index) {
		return data.get(index);
	}
	public void set(int index, byte value) {
		data.set(index, value);
	}
	
	public StringHex shiftLeft(int n) {
		StringHex ret = new StringHex(new BigInteger(toBytes()).shiftLeft(n).toByteArray());
		while (ret.size() > size()) {
			ret.data.remove(0);
		}
		return ret;
	}
	public StringHex xor(StringHex sh) {
		return new StringHex(new BigInteger(toBytes()).xor(new BigInteger(sh.toBytes())).toByteArray());
	}
	public StringHex and(StringHex sh) {
		return new StringHex(new BigInteger(toBytes()).and(new BigInteger(sh.toBytes())).toByteArray());
	}
	
	public int size() {
		return data.size();
	}
	public byte[] toBytes() {
		byte[] bArray = new byte[data.size()];
		for (int i = 0; i < bArray.length; i++)
			bArray[i] = data.get(i);
		return bArray;
	}
	public ByteBuffer toByteBuffer() {
		ByteBuffer bb = ByteBuffer.allocate(data.size());
		for (byte b : data)
			bb.put(b);
		bb.position(0);
		return bb;
	}
	public static StringHex concatenate(StringHex...stringHexs) {
		StringHex sh = new StringHex("");
		int length = 0;
		for (StringHex s : stringHexs)
			length += s.data.size();
		sh.data = new ArrayList<>(length);
		for (StringHex s : stringHexs)
			sh.data.addAll(s.data);
		return sh;
	}
	
	public static String byteToHex(byte b) {
		String h = Integer.toHexString(b & 0xFF).toUpperCase();
		if (h.length() == 1)
			h = "0" + h;
		return h;
	}
	public static String shortToHex(short s) {
		return byteToHex((byte) ((s >> 8) & 0xFF)) + byteToHex((byte) (s & 0xFF));
	}
	
	public String toString() {
		String h = "";
		if (size() == 0)
			return h;
		for (byte b : data)
			h += byteToHex(b) + " ";
		return h.substring(0, h.length() - 1);
	}
	public boolean equals(Object o) {
		if (o instanceof StringHex) {
			return toString().equals(o.toString());
		}
		return false;
	}
}
