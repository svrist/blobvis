/**
 * 
 */
package dk.diku.blob.blobvis;

import model.Model;
import prefuse.data.Graph;
import dk.diku.blob.blobvis.prefuse.BFConstants;
import dk.diku.blob.blobvis.prefuse.BlobGraphModel;
import dk.diku.blob.blobvis.util.Task;

final class ReadBlobConfigTask extends Task {
	private final String filename;
	Graph g;
	Model m;
	BlobGraphModel bgf;

	ReadBlobConfigTask(Graph g, Model m, String filename) {
		this.filename = filename;
		this.g = g;
		this.m = m;
	}

	@Override
	public void work() {
		progress(0);
		setupGraph();
		m.readConfiguration(filename);
		bgf = new BlobGraphModel(g, m);
		try {
			bgf.populateGraphFromModelAPB(this);
		} catch (InterruptedException e) {
			g.clear();
			m.reset();
		}

	}

	private void setupGraph() {

		g.getNodeTable().addColumns(BFConstants.LABEL_SCHEMA);
		g.getNodeTable().addColumns(BFConstants.BLOB_SCHEMA);
		g.getEdgeTable().addColumns(BFConstants.EDGE_SCHEMA);
	}

	@Override
	public void progress(int progress) {
		setProgress(progress);
	}

	@Override
	public void done() {
		System.out.println("Done");

		setProgress(100);
	}
}