package connection;

import java.util.List;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;

import org.apache.log4j.Logger;

import util.StringHex;

public class Connection {
	final static Logger logger = Logger.getLogger(Connection.class);
	
	public static void main(String[] args) throws CardException {
		Connection connection = Connection.getConnection();
		connection.connectAuto();
		connection.send("00 A4 04 00", "a000000151000000", "00");
		connection.coldReset();
		connection.send("00 A4 04 00", "a000000151000000", "00");
		
		/*StringHex resp = connection.send("00 A4 04 00", "A0 00 00 00 03 00 00 00", "00");
		resp = connection.send("00 CA 00 66", "", "00");*/
		//System.out.println(resp.toString());
		
	}
	
	private static Connection connection;
	
	private TerminalFactory terminalFactory;
	private CardTerminal terminal;
	private Card card;
	private CardChannel channel;
	
	private Connection() {
		terminalFactory = TerminalFactory.getDefault();
	}
	
	
	public static Connection getConnection() {
		if (connection == null) {
			connection = new Connection();
		}
		return connection;
	}
	
	public static List<CardTerminal> getTerminals() {
		try {
			return TerminalFactory.getDefault().terminals().list();
		} catch (CardException e) {
			return null;
		}
	}
	public ATR contectAutoToReader(CardTerminal ct) {
		try {
			if (ct.isCardPresent()) {
				terminal = ct;
				card = terminal.connect("*");
				channel = card.getBasicChannel();
				ATR atr = card.getATR();
				if (logger.isInfoEnabled()) {
					logger.info("Connected to " + ct.getName());
					logger.info("ATR: " + new StringHex(atr.getBytes()) + "\n");
				}
				return atr;
			}
		} catch (CardException e) {
			if (logger.isInfoEnabled()) {
				logger.info("Issue with reader " + ct.getName() + ": " + e.getMessage());
			}
		}
		return null;
	}
	public ATR connectAuto() {
		try {
			for (CardTerminal ct : terminalFactory.terminals().list()) {
				ATR atr = contectAutoToReader(ct);
				if (atr != null)
					return atr;
				logger.info("Trying with reader:" + ct.getName() + "\n");
			}
		} catch (CardException e) {
			if (logger.isInfoEnabled()) {
				logger.info("No reader found.");
			}
		}
		return null;
	}
	public void disconnect() throws CardException {
		channel = null;
		card.disconnect(true);
	}
	public ATR coldReset() throws CardException {
		channel = null;
		card.disconnect(true);
		card = terminal.connect("*");
		channel = card.getBasicChannel();
		ATR atr = card.getATR();
		if (logger.isInfoEnabled()) {
			logger.info("Cold Reset");
			logger.info("ATR: " + new StringHex(atr.getBytes()) + "\n");
		}
		return atr;
	}
	private void logBlock(String title, StringHex block) {
		for (int i = 0; i < block.size(); i += 16) {
			if (i == 0 && title.equals("Send: ")) {
				logger.info(title + block.get(0, Math.min(5, block.size())));
				i = i - 16 + 5;
			}
			else
				logger.info((i == 0 ? title : "      ") + block.get(i, Math.min(16, block.size() - i)));
		}
	}
	private APDUResponse send(CommandAPDU command) throws CardException {
		if (logger.isInfoEnabled()) {
			logBlock("Send: ", new StringHex(command.getBytes()));
		}
		APDUResponse response =  new APDUResponse(channel.transmit(command).getBytes());
		if (logger.isInfoEnabled()) {
			logBlock("\nResp: ", new StringHex(response.toBytes()));
			logger.info("");
		}
		return response;
	}
	public APDUResponse send(StringHex header, StringHex data, StringHex le) throws CardException {
		byte[] bHeader = header.toBytes();
		assert(bHeader.length == 4);
		assert(le.size() == 1);
		if (data == null)
			return send(new CommandAPDU(bHeader[0], bHeader[1], bHeader[2], bHeader[3], le.toBytes()[0]));
		else
			return send(new CommandAPDU(bHeader[0], bHeader[1], bHeader[2], bHeader[3], data.toBytes(), le.toBytes()[0]));
	}
	public APDUResponse send(String header, String data, String le) throws CardException {
		if (le.isEmpty())
			le = "00";
		return send(new StringHex(header), data.isEmpty() ? null : new StringHex(data), new StringHex(le));
	}
	public boolean isConnected() {
		return channel != null;
	}
}
