package connection.loader;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;

import connection.APDUResponse;
import connection.Application;
import util.Bits;
import util.StringHex;

public abstract class SCP extends Application implements Bits {
	private static final Logger logger = Logger.getLogger(SCP.class);
	
	public enum StaticDerivation {
		NO_DERIVATION, EMVCPS1_1, VISA, VISA2;
	}
	
	public static final byte SEC_LEVEL_NO       = ZERO;
	public static final byte SEC_LEVEL_C_MAC    = BIT1;
	public static final byte SEC_LEVEL_C_DEC    = BIT2;
	public static final byte SEC_LEVEL_R_MAC    = BIT5;
	public static final byte SEC_LEVEL_R_ENC    = BIT6;
	public static final byte SEC_LEVEL_ANY_AUTH = BIT7;
	public static final byte SEC_LEVEL_AUTH     = BIT8;
	
	protected static final byte THREE_SCP_KEYS           = BIT1;
	protected static final byte CMAC_ON_UNMODIFIED_APDU  = BIT2;
	protected static final byte INITIATION_MODE_EXPLICIT = BIT3;
	protected static final byte ICV_SET_TO_MAC_OVER_AID  = BIT4;
	protected static final byte ICV_ENCRYPT_FOR_CMAC     = BIT5;
	protected static final byte RMAC_SUPPORT             = BIT6;
	protected static final byte WELL_KNOWN_PSEUDO_RANDOM = BIT7;
	
	protected static final int KEY_DIV_DATA_LEN = 10;
	protected              int KEY_INFO_LEN;
	protected static final int CHALLENGE_LEN    = 8;
	protected static final int CRYPTOGRAM_LEN   = 8;
	
	protected static final String SENC_NAME = "sEnc";
	protected static final String SMAC_NAME = "sMac";
	protected static final String KDEK_NAME = "kDek";
	
	protected HashMap<Short, Key> keySet;
	protected StaticDerivation staticDerivation;
	protected byte implementation;
	
	protected short currentKeys;
	protected byte secLevel;
	protected Key sEnc;
	protected Key sMac;
	protected Key kDek;
	protected StringHex macChaining;
	protected StringHex hostChallenge;
	protected StringHex cardChallenge;
	protected StringHex cardCrypto;
	protected StringHex hostCrypto;
	protected StringHex keyDivData;
	protected StringHex keyInfo;
	protected StringHex derivationData;
	
	protected SCP() {
		super(null);
		keySet = new HashMap<>();
		implementation = (byte) 0x15;
		staticDerivation = StaticDerivation.NO_DERIVATION;
		KEY_INFO_LEN = 2;
		secLevel = SEC_LEVEL_NO;
		hostChallenge = new StringHex("1122334455667788");
	}
	
	public void setImplementationOption(byte implementation) {
		this.implementation = implementation;
	}
	public void setStaticDerivation(StaticDerivation staticDerivation) {
		this.staticDerivation = staticDerivation;
	}
	public void addKey(short kvnAndKid, Key key) {
		keySet.put(kvnAndKid, key);
	}
	
	public boolean isThreeSCPKeys() {
		return (implementation & THREE_SCP_KEYS) != 0;
	}
	public boolean isCMACOnUnmodifiedAPDU() {
		return (implementation & CMAC_ON_UNMODIFIED_APDU) != 0;
	}
	public boolean isInitiationModeExplicit() {
		return (implementation & INITIATION_MODE_EXPLICIT) != 0;
	}
	public boolean isIcvSetToMACOverAID() {
		return (implementation & ICV_SET_TO_MAC_OVER_AID) != 0;
	}
	public boolean isIcvEncryptForCMAC() {
		return (implementation & ICV_ENCRYPT_FOR_CMAC) != 0;
	}
	public boolean isRMACSupported() {
		return (implementation & RMAC_SUPPORT) != 0;
	}
	public boolean isWellKnownPseudoRandom() {
		return (implementation & WELL_KNOWN_PSEUDO_RANDOM) != 0;
	}
	
	public abstract String getCipherName();
	public Cipher getCipher() throws GPException {
		try {
			return Cipher.getInstance(getCipherName());
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			throw new GPException("Crypto exception.", e.getCause());
		}
	}
	
	private void setCurrentKeys(byte kvn, byte kid) {
		currentKeys = (short) (((kvn << 8) & 0xFF00) | (kid & 0xFF));
	}
	protected Key getKey(short kvnAndKid) {
		return keySet.get(new Short(kvnAndKid));
	}
	public abstract Key instanciateKey(byte[] keyValue);
	protected Key convertInTDES(StringHex des2) {
		return new SecretKeySpec(StringHex.concatenate(des2, des2.get(0, 8)).toBytes(), "DESede");
	}
	public void setSessionKey(String keyName, StringHex keyValue) throws GPException {
		setSessionKey(keyName, keyValue.toBytes());
	}
	public void setSessionKey(String keyName, byte[] keyValue) throws GPException {
		switch (keyName) {
			case SENC_NAME:
				sEnc = instanciateKey(keyValue);
				return;
			case SMAC_NAME:
				sMac = instanciateKey(keyValue);
				return;
			case KDEK_NAME:
				kDek = instanciateKey(keyValue);
				return;
			default:
				throw new GPException("Unknown key name.");
		}
	}
	public void deriveStaticKeys() throws GPException {
		if (logger.isInfoEnabled()) {
			logger.info("Static master keys:");
			logger.info("Kenc -> " + new StringHex(getKey(currentKeys).getEncoded()));
			logger.info("Kmac -> " + new StringHex(getKey((short) (currentKeys + 1)).getEncoded()));
			logger.info("Kdek -> " + new StringHex(getKey((short) (currentKeys + 2)).getEncoded()));
		}
		StringHex kdd = null;
		Cipher cipher = getCipher();
		
		try {
			switch(staticDerivation) {
				case NO_DERIVATION:
					setSessionKey(SENC_NAME, getKey(currentKeys).getEncoded());
					setSessionKey(SMAC_NAME, getKey((short) (currentKeys + 1)).getEncoded());
					setSessionKey(KDEK_NAME, getKey((short) (currentKeys + 2)).getEncoded());
					if (logger.isInfoEnabled()) {
						logger.info("No static diversification performed.");
					}
					return;
				case EMVCPS1_1://EMVCPS
					kdd = StringHex.concatenate(keyDivData.get(4, 6), new StringHex("F0 01"), 
												keyDivData.get(4, 6), new StringHex("0F 01"));
					cipher = Cipher.getInstance("DESede/ECB/NoPadding");
					
					cipher.init(Cipher.ENCRYPT_MODE, convertInTDES(new StringHex(getKey(currentKeys).getEncoded())));
					setSessionKey(SENC_NAME, cipher.doFinal(kdd.toBytes()));

					cipher.init(Cipher.ENCRYPT_MODE, convertInTDES(new StringHex(getKey((short) (currentKeys + 1)).getEncoded())));
					kdd.set(7, (byte) 0x02);
					kdd.set(15, (byte) 0x02);
					setSessionKey(SMAC_NAME, cipher.doFinal(kdd.toBytes()));
					
					cipher.init(Cipher.ENCRYPT_MODE, convertInTDES(new StringHex(getKey((short) (currentKeys + 2)).getEncoded())));
					kdd.set(7, (byte) 0x03);
					kdd.set(15, (byte) 0x03);
					setSessionKey(KDEK_NAME, cipher.doFinal(kdd.toBytes()));
					break;
				case VISA:
					break;
				case VISA2:
					break;
			}
			if (logger.isInfoEnabled()) {
				logger.info("Diversified master keys:");
				logger.info("Kenc -> " + new StringHex(sEnc.getEncoded()));
				logger.info("Kmac -> " + new StringHex(sMac.getEncoded()));
				logger.info("Kdek -> " + new StringHex(kDek.getEncoded()));
			}
		} catch (GeneralSecurityException e) {
			throw new GPException("Crypto exception. " + e.getMessage(), e.getCause());
		}
	}
	public abstract void computeSessionKeys() throws GPException;
	public abstract void computeCryptograms() throws GPException;
	public abstract String wrap(String header, String data, String le) throws GPException;
	public abstract APDUResponse unwrap(APDUResponse response) throws GPException;
	
	public APDUResponse initUpdate(byte kvn, byte kid) throws GPException {
		assert(hostChallenge.size() == 8);
		APDUResponse response = send("Initialize Update", "8050" + StringHex.byteToHex(kvn) + StringHex.byteToHex(kid), hostChallenge.toString(), "00");
		secLevel = SEC_LEVEL_NO;
		
		if ((response.getStatusWord() & 0xFFFF) == 0x9000) {
			setCurrentKeys(kvn, kid);
			keyDivData = response.get(0, KEY_DIV_DATA_LEN);
			keyInfo = response.get(KEY_DIV_DATA_LEN, KEY_INFO_LEN);
			cardChallenge = response.get(KEY_DIV_DATA_LEN + KEY_INFO_LEN, CHALLENGE_LEN);
			cardCrypto = response.get(KEY_DIV_DATA_LEN + KEY_INFO_LEN + CHALLENGE_LEN, CRYPTOGRAM_LEN);
			if (logger.isInfoEnabled()) {
				logger.info("Parsing Init Update response...");
				logger.info("Key diversification data: " + keyDivData.toString());
				logger.info("Key info: " + keyInfo.toString());
				logger.info("Card challenge: " + cardChallenge.toString());
				logger.info("Card cryptogram: " + cardCrypto.toString());
				logger.info("Parsing Done.");
			}
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("Initialize Update command failed!");
			}
			throw new GPException(Integer.toHexString(response.getStatusWord() & 0xFFFF) + " obtained instead of 9000 in Initialize Update command");
		}
		
		deriveStaticKeys();
		computeSessionKeys();
		computeCryptograms();
		
		if (logger.isInfoEnabled()) {
			logger.info("");
		}
		
		return response;
	}
	public APDUResponse externalAuth(byte secLevel) throws GPException {
		this.secLevel = (byte) (SEC_LEVEL_AUTH | SEC_LEVEL_C_MAC);//Hack to force a correct wrapping
		APDUResponse response = send("External Authenticate", "84 82 " + StringHex.byteToHex(secLevel) + " 00", hostCrypto.toString(), "00");
		if ((response.getStatusWord() & 0xFFFF) == 0x9000) {
			this.secLevel = (byte) (SEC_LEVEL_AUTH | secLevel);
			if (logger.isInfoEnabled()) {
				logger.info("Secure channel successfully opened!");
			}
		}
		else {
			this.secLevel = SEC_LEVEL_NO;
			if (logger.isInfoEnabled()) {
				logger.info("External Authenticate command failed!");
			}
			throw new GPException(Integer.toHexString(response.getStatusWord()) + " obtained instead of 9000 in External Authenticate command");
		}
		
		if (logger.isInfoEnabled()) {
			logger.info("");
		}
		
		return response;
	}
}
