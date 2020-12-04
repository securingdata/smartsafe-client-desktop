package smartsafe.controller;

import javafx.application.Platform;
import smartsafe.Prefs;

public class ConnectionTimer {
	private static class Timer extends Thread {
		public boolean stop;
		private int remainingTime;
		
		public Timer(int time) {
			this.remainingTime = time;
		}
		
		public void run() {
			stop = false;
			while(!stop && remainingTime > 0) {
				try {
					remainingTime -= 10;
					Thread.sleep(10000);
				} catch (InterruptedException e) {}
			}
			if (!stop) {
				//Timer expired
				Platform.runLater(() -> Controls.ACTION_CONNECT.run());
			}
		}
	}
	
	private static Timer timer;
	public static void start() {
		if (timer != null)
			stop();
		timer = new Timer(Integer.parseInt(Prefs.myPrefs.get(Prefs.KEY_TIMER, Prefs.DEFAULT_TIMER)));
		timer.start();
	}
	public static void stop() {
		if (timer != null) {
			timer.stop = true;
		}
	}
}
