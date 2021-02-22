package util;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

public class Crypto {
	public static final int AES_BLOCK_SIZE = 16;
	
	public static StringHex keyFromPassword(String password) {
		SHA512Digest sha = new SHA512Digest();
		byte[] res = new byte[64];
		sha.update(password.getBytes(), 0, password.getBytes().length);
		sha.doFinal(res, 0);
		return new StringHex(res);
	}
	
	public static StringHex aes(Key key, StringHex msg) throws GeneralSecurityException {
		byte[] res = new byte[msg.size()];
		AESEngine cipher = new AESEngine();
		cipher.init(true, new KeyParameter(key.getEncoded()));
		cipher.processBlock(msg.toBytes(), 0, res, 0);
		return new StringHex(res);
	}
	public static StringHex aesCBC(boolean encrypting, Key key, StringHex msg, StringHex iv) throws GeneralSecurityException {
		if (encrypting) {
			//Padding
			msg = StringHex.concatenate(msg, new StringHex("80"));
			while (msg.size() % 16 != 0)
				msg = StringHex.concatenate(msg, new StringHex("00"));
		}
		
		byte[] res = new byte[msg.size()];
		BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
		cipher.init(encrypting, new ParametersWithIV(new KeyParameter(key.getEncoded()), iv.toBytes()));
		cipher.processBytes(msg.toBytes(), 0, msg.size(), res, 0);
		
		StringHex result = new StringHex(res);
		
		if (!encrypting) {
			//Remove padding
			int i;
			for (i = result.size() - 1; i > 0 && result.get(i) == 0; i--);
			if (result.get(i) != (byte) 0x80)
				throw new GeneralSecurityException("Incorrect padding");
			result = result.get(0, i);
		}
		
		return result;
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
