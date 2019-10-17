package connection.loader;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import connection.APDUResponse;

public class SCP01 extends SCP {
	public String getCipherName() {
		return "DESede/ECB/NoPadding";
	}
	public Key instanciateKey(byte[] keyValue) {
		return new SecretKeySpec(keyValue, "DESede");
	}
	@Override
	public void computeSessionKeys() throws GPException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void computeCryptograms() throws GPException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String wrap(String header, String data, String le) throws GPException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public APDUResponse unwrap(APDUResponse response) throws GPException {
		// TODO Auto-generated method stub
		return null;
	}
}
