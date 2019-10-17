package connection.loader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;

import connection.APDUResponse;
import util.StringHex;
import util.TLV;

public class GPCommands {
	private static final Logger logger = Logger.getLogger(GPCommands.class);
	
	public static final String SECURE_CLA = "84";
	
	public static final String INS_DELETE  = "E4";
	public static final String INS_INSTALL = "E6";
	public static final String INS_LOAD    = "E8";
	
	public static final String P1_INSTALL_FOR_LOAD            = "02";
	public static final String P1_INSTALL_FOR_INSTALL         = "04";
	public static final String P1_INSTALL_FOR_MAKE_SELECTABLE = "08";
	
	public static final int BLOCK_LEN = 0x80;
	
	
	private SCP scp;
	
	public GPCommands(SCP scp) {
		this.scp = scp;
	}
	
	public static StringHex getRawCap(String path) {
		byte[] order = {1, 2, 4, 3, 6, 7, 8, 10, 5, 9, 11};
		byte[][] components = new byte[12][];
		
		try (ZipFile zip = new ZipFile(path)) {
			Enumeration<? extends ZipEntry> entries = zip.entries();
			
			
			while(entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().endsWith(".cap")) {
					byte[] componentData;
					try (BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(entry))) {
						componentData = new byte[(int) entry.getSize()];
						bis.read(componentData);
					}
					components[componentData[0]] = componentData;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		StringHex raw = new StringHex("");
		for (byte o : order) {
			if (components[o] != null)
				raw = StringHex.concatenate(raw, new StringHex(components[o]));
		}
		
		return raw;
	}
	public void loadCAP(byte[] rawCap) throws GPException {
		StringHex loadFileDataBlock = TLV.createTLV(new StringHex("C4"), new StringHex(rawCap));
		
		byte blockNumber = 0;
		for (int i = 0, remaining = loadFileDataBlock.size(); remaining > 0; i += BLOCK_LEN, remaining -= BLOCK_LEN, blockNumber++) {
			load(BLOCK_LEN >= remaining, blockNumber, loadFileDataBlock.get(i, Math.min(BLOCK_LEN, remaining)).toString());
		}
	}
	
	public APDUResponse select(String aid) throws GPException {
		APDUResponse resp =  scp.select(aid);
		if (logger.isInfoEnabled()) {
			logger.info("Analysing select response: TODO");//TODO
		}
		return resp;
	}
	public APDUResponse initUpdate(byte kvn, byte kid) throws GPException {
		return scp.initUpdate(kvn, kid);
	}
	public APDUResponse externalAuth(byte secLevel) throws GPException {
		return scp.externalAuth(secLevel);
	}

	public APDUResponse delete(String aid, boolean related) throws GPException {
		return scp.send("Delete", SECURE_CLA + INS_DELETE + "00" + (related ? "80" : "00"), TLV.createTLV(new StringHex("4F"), new StringHex(aid)).toString(), "00");
	}
	
	public APDUResponse installForLoad(String pckgAID, String sdAid) throws GPException {
		String hash = "";
		String loadParam = "";
		String token = "";
		
		StringHex shPckgAID = new StringHex(pckgAID);
		StringHex shSdAid = new StringHex(sdAid);
		StringHex shHash = new StringHex(hash);
		StringHex shLoadParam = new StringHex(loadParam);
		StringHex shToken = new StringHex(token);
		
		return scp.send("Install For Load", SECURE_CLA + INS_INSTALL + P1_INSTALL_FOR_LOAD + "00", 
				StringHex.byteToHex((byte)shPckgAID.size()) + shPckgAID.toString() +
				StringHex.byteToHex((byte)shSdAid.size()) + shSdAid.toString() + 
				StringHex.byteToHex((byte)shHash.size()) + shHash.toString() + 
				StringHex.byteToHex((byte)shLoadParam.size()) + shLoadParam.toString() + 
				StringHex.byteToHex((byte)shToken.size()) + shToken.toString(), "00");
	}
	
	public APDUResponse installForInstallAndMakeSelectable(String loadFileAID, String moduleAID, String appAID, String privileges, String parameters) throws GPException {
		StringHex shLoadFileAID = new StringHex(loadFileAID);
		StringHex shModuleAID = new StringHex(moduleAID);
		StringHex shAppAID = new StringHex(appAID);
		StringHex shPrivileges = new StringHex((privileges == null || privileges.isEmpty()) ? "00" : privileges);
		StringHex shParameters = new StringHex((parameters == null || parameters.isEmpty()) ? "C9 01 00" : parameters);
		
		String p1 = new StringHex(P1_INSTALL_FOR_INSTALL).xor(new StringHex(P1_INSTALL_FOR_MAKE_SELECTABLE)).toString();
		
		return scp.send("Install For Install And Make Selectable", SECURE_CLA + INS_INSTALL + p1 + "00", 
				StringHex.byteToHex((byte)shLoadFileAID.size()) + shLoadFileAID.toString() +
				StringHex.byteToHex((byte)shModuleAID.size()) + shModuleAID.toString() + 
				StringHex.byteToHex((byte)shAppAID.size()) + shAppAID.toString() + 
				StringHex.byteToHex((byte)shPrivileges.size()) + shPrivileges.toString() + 
				StringHex.byteToHex((byte)shParameters.size()) + shParameters.toString(), "00");
	}
	
	public APDUResponse load(boolean lastBlock, byte blockNumber, String block) throws GPException {
		return scp.send("Load", SECURE_CLA + INS_LOAD + (lastBlock ? "80" : "00") + StringHex.byteToHex(blockNumber), block, "00");
	}
}
