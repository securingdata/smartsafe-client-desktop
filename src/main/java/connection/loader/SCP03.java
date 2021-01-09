package connection.loader;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CardTerminal;

import connection.APDUResponse;
import util.Crypto;
import util.StringHex;

public class SCP03 extends SCP {
	private static final StringHex SIXTEEN_BYTES_NULL = new StringHex("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
	private static final byte CARD_CRYPTO_DERIVATION_CSTE = 0x00;
	private static final byte HOST_CRYPTO_DERIVATION_CSTE = 0x01;
	private static final byte SENC_DERIVATION_CSTE        = 0x04;
	private static final byte SMAC_DERIVATION_CSTE        = 0x06;
	private static final byte RMAC_DERIVATION_CSTE        = 0x07;
	
	private byte[] encryptionCounter;
	
	public SCP03(CardTerminal reader) {
		super(reader);
		KEY_INFO_LEN = 3;
		encryptionCounter = new byte[16];
	}
	
	
	public String getCipherName() {
		return "AES/CBC/NoPadding";
	}
	public Key instanciateKey(byte[] keyValue) {
		return new SecretKeySpec(keyValue, "AES");
	}
	
	public void computeSessionKeys() throws GPException {
		try {
			computeDerivationScheme(SENC_DERIVATION_CSTE);
			setSessionKey(SENC_NAME, Crypto.aesCMAC(sEnc, derivationData, null));
			
			computeDerivationScheme(RMAC_DERIVATION_CSTE);
			setSessionKey(RMAC_NAME, Crypto.aesCMAC(sMac, derivationData, null));
			
			computeDerivationScheme(SMAC_DERIVATION_CSTE);
			setSessionKey(SMAC_NAME, Crypto.aesCMAC(sMac, derivationData, null));
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
	}
	public void computeCryptograms() throws GPException {
		try {
			computeDerivationScheme(CARD_CRYPTO_DERIVATION_CSTE);
			hostCrypto = Crypto.aesCMAC(sMac, derivationData, null).get(0, 8);
			
			computeDerivationScheme(HOST_CRYPTO_DERIVATION_CSTE);
			hostCrypto = Crypto.aesCMAC(sMac, derivationData, null).get(0, 8);
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
	}
	
	private void computeDerivationScheme(byte derivationCste) {
		if (derivationData == null) {
			derivationData = StringHex.concatenate(SIXTEEN_BYTES_NULL, hostChallenge, cardChallenge);
		}
		
		derivationData.set(11, derivationCste);
		
		switch(derivationCste) {
			case CARD_CRYPTO_DERIVATION_CSTE:
			case HOST_CRYPTO_DERIVATION_CSTE:
				derivationData.set(13, (byte) 0x00);
				derivationData.set(14, (byte) 0x40);
				derivationData.set(15, (byte) 0x01);
				break;
			case SENC_DERIVATION_CSTE:
			case SMAC_DERIVATION_CSTE:
			case RMAC_DERIVATION_CSTE:
				derivationData.set(13, (byte) 0x00);
				derivationData.set(14, (byte) 0x80);
				derivationData.set(15, (byte) 0x01);
				break;
		}
	}
	public String wrap(String header, String data, String le) throws GPException {
		if (secLevel == SEC_LEVEL_NO || secLevel == SEC_LEVEL_AUTH)
			return data;
		
		if ((secLevel & SEC_LEVEL_C_MAC) != 0) {
			if ((secLevel & SEC_LEVEL_C_DEC) != 0) {
				//For IV
				incrementEncryptionCounter();
				encryptionCounter[0] = 0;
				try {
					StringHex iv = Crypto.aes(sEnc, new StringHex(encryptionCounter));
					data = Crypto.aesCBC(true, sEnc, new StringHex(data), iv).toString();
				} catch (GeneralSecurityException e) {
					throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
				}
			}
			
			String newLen = StringHex.byteToHex((byte) (new StringHex(data).size() + 8));
			if (macChaining == null)
				macChaining = SIXTEEN_BYTES_NULL;
			StringHex dataToMac = new StringHex(header + newLen + data);
			try {
				macChaining = Crypto.aesCMAC(sMac, dataToMac, macChaining);
			} catch (GeneralSecurityException e) {
				throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
			}
		}
		return new StringHex(data + macChaining.get(0, 8)).toString();
	}
	public APDUResponse unwrap(APDUResponse response) throws GPException {
		if ((secLevel & SEC_LEVEL_R_MAC) != 0) {
			if (response.getData().length == 0)
				return response;
			int size = response.size();
			StringHex sw = response.get(size - 2, 2);
			StringHex dataToMac = StringHex.concatenate(response.get(0, size - 10), sw);
			try {
				StringHex computedMac = Crypto.aesCMAC(rMac, dataToMac, macChaining);
				if (!computedMac.get(0, 8).equals(response.get(size - 10, 8)))
					throw new GPException("Incorrect R-MAC");
			} catch (GeneralSecurityException e) {
				throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
			}
			
			if ((secLevel & SEC_LEVEL_R_ENC) != 0) {
				try {
					incrementEncryptionCounter();
					encryptionCounter[0] = (byte) 0x80;
					StringHex iv = Crypto.aes(sEnc, new StringHex(encryptionCounter));
					StringHex newData = StringHex.concatenate(Crypto.aesCBC(false, sEnc, response.get(0, size - 10), iv), sw);
					return new APDUResponse(newData.toString());
				} catch (GeneralSecurityException e) {
					throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
				}
			}
			return new APDUResponse(dataToMac.toString());
		}
		return response;
	}
	
	private void incrementEncryptionCounter() {
		for (short s = (short) (encryptionCounter.length - 1); s >= 0; s--) {
			encryptionCounter[s]++;
			if(encryptionCounter[s] != 0)
				return;
		}
	}
}
