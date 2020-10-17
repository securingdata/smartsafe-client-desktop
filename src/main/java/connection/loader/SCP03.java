package connection.loader;

import java.security.GeneralSecurityException;
import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import connection.APDUResponse;
import util.Crypto;
import util.StringHex;

public class SCP03 extends SCP {
	private static final Logger logger = Logger.getLogger(SCP03.class);
	
	private static final StringHex SIXTEEN_BYTES_NULL = new StringHex("00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00");
	private static final byte CARD_CRYPTO_DERIVATION_CSTE = 0x00;
	private static final byte HOST_CRYPTO_DERIVATION_CSTE = 0x01;
	private static final byte SENC_DERIVATION_CSTE        = 0x04;
	private static final byte SMAC_DERIVATION_CSTE        = 0x06;
	private static final byte RMAC_DERIVATION_CSTE        = 0x07;
	
	public SCP03() {
		super();
		KEY_INFO_LEN = 3;
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
			
			computeDerivationScheme(SMAC_DERIVATION_CSTE);
			setSessionKey(SMAC_NAME, Crypto.aesCMAC(sMac, derivationData, null));
			
			if (logger.isInfoEnabled()) {
				logger.info("Session keys:");
				logger.info("Senc -> " + new StringHex(sEnc.getEncoded()));
				logger.info("Smac -> " + new StringHex(sMac.getEncoded()));
				logger.info("Kdek -> " + new StringHex(kDek.getEncoded()));
			}
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
	}
	public void computeCryptograms() throws GPException {
		StringHex computedCardCrypto;
		try {
			computeDerivationScheme(CARD_CRYPTO_DERIVATION_CSTE);
			computedCardCrypto = Crypto.aesCMAC(sMac, derivationData, null).get(0, 8);
			
			computeDerivationScheme(HOST_CRYPTO_DERIVATION_CSTE);
			hostCrypto = Crypto.aesCMAC(sMac, derivationData, null).get(0, 8);
			
			if (logger.isInfoEnabled()) {
				logger.info("Computed cryptograms:");
				logger.info("Card -> " + computedCardCrypto);
				logger.info("Host -> " + hostCrypto);
			}
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
		if (!cardCrypto.equals(computedCardCrypto)) {
			if (logger.isInfoEnabled()) {
				logger.info("Computed card cryptogram mistmatches with received one.");
			}
			throw new GPException("Computed card cryptogram mistmatches with received one.");
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
			String newLen = StringHex.byteToHex((byte) (new StringHex(data).size() + 8));
			if (macChaining == null)
				macChaining = SIXTEEN_BYTES_NULL;
			StringHex dataToMac = new StringHex(header + newLen + data);
			try {
				macChaining = Crypto.aesCMAC(sMac, dataToMac, macChaining);
			} catch (GeneralSecurityException e) {
				throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
			}
			if (logger.isInfoEnabled()) {
				logger.info("Command to mac: " + header + " " + newLen + " " + data);
				logger.info("Data to mac: " + dataToMac);
				logger.info("Computed mac: " + macChaining.get(0, 8) + "\n");
			}
		}
		
		return new StringHex(data + macChaining.get(0, 8)).toString();
	}
	public APDUResponse unwrap(APDUResponse response) throws GPException {
		return response;
	}
}
