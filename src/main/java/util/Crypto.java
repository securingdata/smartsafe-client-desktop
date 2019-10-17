package util;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class Crypto {
	public static final int AES_BLOCK_SIZE = 16;
	
	public static StringHex aesCMAC1(Key key, StringHex msg, StringHex iv) throws GeneralSecurityException {
		IvParameterSpec ips;
		if (iv == null)
			ips = new IvParameterSpec(new byte[16]);
		else
			ips = new IvParameterSpec(iv.toBytes());
		Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, key, ips);
		byte[] k1 = cipher.doFinal(new byte[16]);
		k1 = new StringHex(k1).shiftLeft(1).toBytes();
		System.out.println("- " + new StringHex(k1));
		if ((k1[0] & 0x80) != 0) {
			System.out.println(1);
			k1 = new StringHex(k1).xor(new StringHex("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 87")).toBytes();
		}
		
		byte[] rawData = msg.toBytes();
		int nbBlocks = rawData.length / AES_BLOCK_SIZE;
		for (int i = 0; i < nbBlocks; i++) {
			if (i == nbBlocks - 1) {
				ips = new IvParameterSpec(new StringHex(ips.getIV()).xor(new StringHex(k1)).toBytes());
			}
			cipher.init(Cipher.ENCRYPT_MODE, key, ips);
			ips = new IvParameterSpec(cipher.doFinal(rawData, i * AES_BLOCK_SIZE, AES_BLOCK_SIZE));
		}
		return new StringHex(ips.getIV());
	}
	
	public static StringHex aesCMAC(Key key, StringHex msg, StringHex iv) throws GeneralSecurityException {
		if (iv != null)
			msg = StringHex.concatenate(iv, msg);
		CMac cmac = new CMac(new AESEngine());
		cmac.init(new KeyParameter(key.getEncoded()));
		cmac.update(msg.toBytes(), 0, msg.size());
		byte[] res = new byte[16];
		cmac.doFinal(res, 0);
		return new StringHex(res);
	}
}
