package connection.loader;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardTerminal;

import connection.APDUResponse;
import util.Crypto;
import util.StringHex;

public class SCP02 extends SCP {
	private static final StringHex EIGHT_BYTES_NULL = new StringHex("00 00 00 00 00 00 00 00");
	
	protected static final byte THREE_SCP_KEYS           = BIT1;
	protected static final byte CMAC_ON_UNMODIFIED_APDU  = BIT2;
	protected static final byte INITIATION_MODE_EXPLICIT = BIT3;
	protected static final byte ICV_SET_TO_MAC_OVER_AID  = BIT4;
	protected static final byte ICV_ENCRYPT_FOR_CMAC     = BIT5;
	protected static final byte RMAC_SUPPORT             = BIT6;
	protected static final byte WELL_KNOWN_PSEUDO_RANDOM = BIT7;
	
	public SCP02(CardTerminal reader) {
		super(reader);
	}
	
	public boolean isCMACOnUnmodifiedAPDU() {
		return (implementation & CMAC_ON_UNMODIFIED_APDU) != 0;
	}
	public boolean isIcvEncryptForCMAC() {
		return (implementation & ICV_ENCRYPT_FOR_CMAC) != 0;
	}
	
	public String getCipherName() {
		return "DESede/ECB/NoPadding";
	}
	public Key instanciateKey(byte[] keyValue) {
		return convertInTDES(new StringHex(keyValue));
	}
	@Override
	public void computeSessionKeys() throws GPException {
		IvParameterSpec iv = new IvParameterSpec(EIGHT_BYTES_NULL.toBytes());
		derivationData = StringHex.concatenate(cardChallenge.get(0, 2), new StringHex("00 00 00 00 00 00 00 00 00 00 00 00"));
		
		try {
			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, sEnc, iv);
			setSessionKey(SENC_NAME, new StringHex(cipher.doFinal(StringHex.concatenate(new StringHex("0182"), derivationData).toBytes())));
			
			cipher.init(Cipher.ENCRYPT_MODE, sMac, iv);
			setSessionKey(SMAC_NAME, new StringHex(cipher.doFinal(StringHex.concatenate(new StringHex("0101"), derivationData).toBytes())));
			
			cipher.init(Cipher.ENCRYPT_MODE, kDek, iv);
			setSessionKey(KDEK_NAME, new StringHex(cipher.doFinal(StringHex.concatenate(new StringHex("0181"), derivationData).toBytes())));
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
	}
	@Override
	public void computeCryptograms() throws GPException {
		IvParameterSpec iv = new IvParameterSpec(EIGHT_BYTES_NULL.toBytes());
		StringHex padding = new StringHex("80 00 00 00 00 00 00 00");
		StringHex computedCardCrypto;
		
		try {
			Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, sEnc, iv);
			computedCardCrypto = new StringHex(cipher.doFinal(StringHex.concatenate(hostChallenge, cardChallenge, padding).toBytes())).get(16, 8);
			
			cipher.init(Cipher.ENCRYPT_MODE, sEnc, iv);
			hostCrypto = new StringHex(cipher.doFinal(StringHex.concatenate(cardChallenge, hostChallenge, padding).toBytes())).get(16, 8);
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
		if (!cardCrypto.equals(computedCardCrypto)) {
			System.out.println(cardCrypto);
			System.out.println(computedCardCrypto);
			throw new GPException("Computed card cryptogram mistmatches with received one.");
		}
	}
	@Override
	public String wrap(String header, String data, String le) throws GPException {
		if (secLevel == SEC_LEVEL_NO || secLevel == SEC_LEVEL_AUTH)
			return data;
		if ((secLevel & SEC_LEVEL_C_MAC) != 0) {
			String newLen;
			if (isCMACOnUnmodifiedAPDU())
				newLen = StringHex.byteToHex((byte) (new StringHex(data).size()));
			else
				newLen = StringHex.byteToHex((byte) (new StringHex(data).size() + 8));
			
			StringHex dataToMac = new StringHex(header + newLen + data);
			try {
				if (macChaining == null)
					macChaining = EIGHT_BYTES_NULL;
				else if (isIcvEncryptForCMAC()) {
					Cipher cipher = Cipher.getInstance("DES/CBC/NoPadding");
					cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sMac.getEncoded(), 0, 8, "DES"), new IvParameterSpec(EIGHT_BYTES_NULL.toBytes()));
					macChaining = new StringHex(cipher.doFinal(macChaining.toBytes()));
				}
				
				macChaining = Crypto.signatureISO9797_1_M2_ALG3(sMac, dataToMac, macChaining);
			} catch (GeneralSecurityException e) {
				throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
			}
		}
		
		return new StringHex(data + macChaining.get(0, 8)).toString();
	}
	@Override
	public APDUResponse unwrap(APDUResponse response) throws GPException {
		return response;
	}
}
