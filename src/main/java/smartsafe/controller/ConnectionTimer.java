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
					Thread.sleep(Math.min(5000, remainingTime * 1000));
					remainingTime -= 5;
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
		int time = Integer.parseInt(Prefs.get(Prefs.KEY_TIMER));
		if (time != 0) {
			timer = new Timer(time);
			timer.start();
		}
	}
	public static void restart() {
		if (timer != null) {
			stop();
			start();
		}
	}
	public static void stop() {
		if (timer != null) {
			timer.stop = true;
			timer = null;
		}
	}
}
