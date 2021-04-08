package fr.securingdata.smartsafe.controller;

import fr.securingdata.smartsafe.Prefs;
import javafx.application.Platform;

public class ConnectionTimer {
	private static class Timer extends Thread {
		public boolean stop;
		private volatile int remainingTime;
		
		public Timer(int time) {
			setTime(time);
		}
		
		public synchronized void setTime(int time) {
			remainingTime = time;
		}
		
		public void run() {
			stop = false;
			while(!stop && remainingTime > 0) {
				try {
					Thread.sleep(Math.min(5000, remainingTime * 1000));
					setTime(remainingTime - 5);
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
			timer.setTime(Integer.parseInt(Prefs.get(Prefs.KEY_TIMER)));
		}
	}
	public static void stop() {
		if (timer != null) {
			timer.stop = true;
			timer = null;
		}
	}
}
