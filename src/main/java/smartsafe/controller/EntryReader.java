package smartsafe.controller;

import java.util.LinkedList;

import smartsafe.model.Entry;

public class EntryReader {
	private static class Reader extends Thread {
		private volatile LinkedList<LinkedList<Entry>> queues;
		private volatile LinkedList<Entry> insertionQueue;
		private volatile boolean interruptRequest, interrupted;
		
		public Reader() {
			queues = new LinkedList<>();
			this.newQueue();
		}
		
		public int getRemaining() {
			int remaining = 1;
			for (LinkedList<Entry> queue : queues)
				remaining += queue.size();
			return remaining;
		}
		public synchronized void addEntry(Entry e) {
			insertionQueue.add(e);
			if (!queues.contains(insertionQueue))
				queues.add(insertionQueue);
		}
		public synchronized void removeEntry(Entry e) {
			for (LinkedList<Entry> queue : queues) {
				while(queue.remove(e));
			}
		}
		public synchronized void newQueue() {
			insertionQueue = new LinkedList<>();
		}
		
		public void requestInterrupt() {
			if (interrupted)
				return;
			interruptRequest = true;
		}
		public boolean isReadingInterrupted() {
			return interrupted;
		}
		public void restart() {
			interrupted = false;
		}
		
		public void run() {
			Entry entry;
			try {
				while (!queues.isEmpty()) {
					while((entry = queues.getLast().poll()) != null) {
						if (!entry.isInCache()) {
							if (!interruptRequest)
								Controls.getAppli().selectGroup(entry.group);
							if (!interruptRequest)
								Controls.getAppli().selectEntry(entry);
							if (!interruptRequest) {
								Controls.getAppli().getData(Entry.INDEX_PASSWORD);
								entry.maskPassword();
							}
							if (!interruptRequest)
								Controls.getAppli().getData(Entry.INDEX_URL);
							if (!interruptRequest)
								Controls.getAppli().getData(Entry.INDEX_lAST_UPDATE);
							if (!interruptRequest)
								Controls.getAppli().getData(Entry.INDEX_EXP_DATE);
							if (!interruptRequest)
								Controls.getAppli().getData(Entry.INDEX_NOTES);
							if (!interruptRequest)
								entry.validate();
						}
						if (interruptRequest) {
							queues.getLast().add(0, entry);
							interruptRequest = false;
							interrupted = true;
						}
						while (interrupted) {
							sleep(500);
						}
					}
					queues.removeLast();
				}
				
			} catch (Exception e) {/*Happens when deconnecting the token during data loading*/}
			reader = null;
		}
	}
	private static Reader reader;
	private static boolean allowReaderCreation = false;
	
	public static void setAllowReaderCreation(boolean allow) {
		allowReaderCreation = allow;
	}
	public static int getRemaining() {
		if (reader != null)
			return reader.getRemaining();
		return 0;
	}
	public static void readEntry(Entry e) {
		if (reader == null) {
			if (!allowReaderCreation)
				return;
			reader = new Reader();
			reader.addEntry(e);
			reader.start();
		}
		else {
			reader.addEntry(e);
		}
	}
	public static void removeEntry(Entry e) {
		if (reader != null)
			reader.removeEntry(e);
	}
	public static void newQueue() {
		if (reader != null)
			reader.newQueue();
	}
	public static void suspendReader() {
		if (reader != null) {
			reader.requestInterrupt();
			while(!reader.isReadingInterrupted()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
			}
		}
	}
	public static void restartReader() {
		if (reader != null)
			reader.restart();
	}
}
