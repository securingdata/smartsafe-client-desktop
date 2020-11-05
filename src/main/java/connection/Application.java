package connection;

import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;

import connection.loader.GPException;

public class Application {
	private CardTerminal selectedReader;
	protected Connection connection;
	
	public Application(CardTerminal reader) {
		selectedReader = reader;
	}
	public void coldReset() throws GPException {
		try {
			if (connection == null) {
				connection = Connection.getConnection();
				if (selectedReader != null)
					connection.contectAutoToReader(selectedReader);
				else
					connection.connectAuto();
			}
			connection.coldReset();
		} catch (CardException e) {
			throw new GPException("Card exception. " + e.getMessage(), e.getCause());
		}
	}
	public void disconnect() {
		if (connection != null) {
			try {
				connection.disconnect();
			} catch (CardException e) {
				connection = null;
			}
		}
	}
	public APDUResponse select(String aid) throws GPException {
		return send("Select", "00 A4 04 00", aid, "00");
	}
	public APDUResponse send(String header, String data, String le) throws GPException {
		try {
			return unwrap(connection.send(header, wrap(header, data, le), le));
		} catch (CardException e) {
			throw new GPException("Card exception. " + e.getMessage(), e.getCause());
		}
	}
	public APDUResponse send(String cmdName, String header, String data, String le) throws GPException {
		return send(header, data, le);
	}
	public String wrap(String header, String data, String le) throws GPException {
		return data;
	}
	public APDUResponse unwrap(APDUResponse response) throws GPException {
		return response;
	}
}
