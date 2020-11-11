package util;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.params.KeyParameter;

public class Crypto {
	public static final int AES_BLOCK_SIZE = 16;
	public static final byte[] IV = "initvectorsmarts".getBytes();
	
	public static final StringHex BACKUP_HEADER = new StringHex("536d61727453616665");
	
	public static StringHex keyFromPassword(String password) {
		SHA512Digest sha = new SHA512Digest();
		byte[] res = new byte[64];
		sha.update(password.getBytes(), 0, password.getBytes().length);
		sha.doFinal(res, 0);
		return new StringHex(res);
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
	public static StringHex signatureISO9797_1_M2_ALG3(Key key, StringHex msg, StringHex iv) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
		
		//Padding
		msg = StringHex.concatenate(msg, new StringHex("80"));
		while (msg.size() % 8 != 0)
			msg = StringHex.concatenate(msg, new StringHex("00"));
		
		//Single DES on blocks except the last one
		for (int i = 0; i < msg.size() - 8; i += 8) {
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key.getEncoded(), 0, 8, "DES"), new IvParameterSpec(iv.toBytes()));
			iv = new StringHex(cipher.doFinal(msg.get(i, 8).toBytes()));
		}
		
		//Final TDES
		cipher = Cipher.getInstance("DESede/CBC/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv.toBytes()));
		return new StringHex(cipher.doFinal(msg.get(msg.size() - 8, 8).toBytes()));
	}
}
