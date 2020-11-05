package util;

import java.security.GeneralSecurityException;
import java.security.Key;

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
}
