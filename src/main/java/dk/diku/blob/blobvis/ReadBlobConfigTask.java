/**
 * 
 */
package dk.diku.blob.blobvis;

import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;
import dk.diku.blob.blobvis.util.Task;

final class ReadBlobConfigTask extends Task {
	private final String filename;

	private BlobGraphFuser bgf;

	public ReadBlobConfigTask(String filename, BlobGraphFuser bgf) {
		this.filename = filename;
		this.bgf = bgf;
	}

	@Override
	public void work() {
		progress(0);
		bgf.readBlobModelConfiguration(filename);
		try {
			bgf.populateGraphFromModelAPB(this);
		} catch (InterruptedException e) {
			bgf.getGraph().clear();
			bgf.getModel().reset();
		}

	}



	@Override
	public void progress(int progress) {
		setProgress(progress);
	}

	@Override
	public void done() {
		setProgress(100);
	}
}