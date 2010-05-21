package dk.diku.blob.blobvis.prefuse;

import model.Blob;
import prefuse.data.Node;

public interface BlobFuser {
	void addEdge(Blob b1, Blob b2);

	/**
	 * Add a blob to the prefuse graph.
	 * Used when populating the prefuse graph from the blob simulator structures
	 * @param b The blob to insert
	 * @param inPgr indicates if this is a program blob or data blob.
	 * @return
	 */
	Node addBlobAsNode(Blob b, boolean inPgr);

}