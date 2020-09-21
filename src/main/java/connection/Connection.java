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

import javafx.beans.property.StringProperty;
import util.StringHex;

public class Connection {
	final static Logger logger = Logger.getLogger(Connection.class);
	private static StringProperty logListener;
	
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
	
	public static void setLogListener(StringProperty sp) {
		logListener = sp;
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
				if (logListener != null) {
					logListener.set(logListener.get() + "Connected to " + ct.getName() + "\n");
					logListener.set(logListener.get() + "ATR: " + new StringHex(atr.getBytes()) + "\n\n");
				}
				return atr;
			}
		} catch (CardException e) {
			if (logger.isInfoEnabled()) {
				logger.info("Issue with reader " + ct.getName() + ": " + e.getMessage());
			}
			if (logListener != null) {
				logListener.set(logListener.get() + "Issue with reader " + ct.getName() + ": " + e.getMessage() + "\n");
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
				if (logger.isInfoEnabled())
					logger.info("Trying with reader:" + ct.getName() + "\n");
				if (logListener != null)
					logListener.set(logListener.get() + "Trying with reader:" + ct.getName() + "\n\n");

			}
		} catch (CardException e) {
			if (logger.isInfoEnabled())
				logger.info("No reader found.");
			if (logListener != null)
				logListener.set(logListener.get() + "No reader found.\n");
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
		if (logListener != null) {
			logListener.set(logListener.get() + "Cold Reset\n");
			logListener.set(logListener.get() + "ATR: " + new StringHex(atr.getBytes()) + "\n\n");
		}
		return atr;
	}
	private void logBlock(String title, StringHex block) {
		String tmp;
		for (int i = 0; i < block.size(); i += 16) {
			if (i == 0 && title.equals("Send: ")) {
				tmp = title + block.get(0, Math.min(5, block.size()));
				if (logger.isInfoEnabled())
					logger.info(tmp);
				if (logListener != null)
					logListener.set(logListener.get() + tmp + "\n");
				i = i - 16 + 5;
			}
			else {
				tmp = (i == 0 ? title : "      ") + block.get(i, Math.min(16, block.size() - i));
				if (logger.isInfoEnabled())
					logger.info(tmp);
				if (logListener != null)
					logListener.set(logListener.get() + tmp + "\n");
			}
		}
	}
	private APDUResponse send(CommandAPDU command) throws CardException {
		logBlock("Send: ", new StringHex(command.getBytes()));
		APDUResponse response =  new APDUResponse(channel.transmit(command).getBytes());
		logBlock("\nResp: ", new StringHex(response.toBytes()));
		if (logger.isInfoEnabled())
			logger.info("");
		if (logListener != null)
			logListener.set(logListener.get() + "\n");
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
