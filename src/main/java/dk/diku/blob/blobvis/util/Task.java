/**
 * 
 */
package dk.diku.blob.blobvis.util;

import javax.swing.SwingWorker;

import dk.diku.blob.blobvis.gui.Progressable;

public abstract class Task extends SwingWorker<Void, Void> implements Progressable {
	/*
	 * Main task. Executed in background thread.
	 */
	@Override
	public Void doInBackground() {
		// Initialize progress property.
		setProgress(0);
		work();
		return null;
	}

	public abstract void work();

	/*
	 * Executed in event dispatching thread
	 */
	@Override
	public abstract void done();
}