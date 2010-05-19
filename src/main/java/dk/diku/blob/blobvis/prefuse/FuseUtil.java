package dk.diku.blob.blobvis.prefuse;

import model.Blob;
import model.BondSite;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;

public class FuseUtil {

	/*private static void addEdge(Graph g, Node n1, Blob b1, BondSite bs1,
			Node n2, Blob b2) {
		if (n2 != null) {
			BondSite bs2 = b2.boundTo(b1);
			FuseUtil.addEdge(g, n1, b1, bs1, n2, b2, bs2);
		}
	}*/

	static void addEdge(Graph g, Node n1, Blob b1, BondSite bs1,
			Node n2, Blob b2, BondSite bs2) {
		if (n2 != null) {
			if (g.getEdge(n2, n1) == null) {
				Edge e = g.addEdge(n1, n2);
				e.set(BFConstants.EDGENUMBERSRC, bs1.ordinal());
				if (bs2 != null) {
					e.set(BFConstants.EDGENUMBERTAR, bs2.ordinal());
				}
			}
		}
	}

	static void addEdge(Graph g, Node n1, Blob b1, Node n2, Blob b2) {
		if (n2 != null) {
			BondSite bs1 = b1.boundTo(b2);
			BondSite bs2 = b2.boundTo(b1);
			addEdge(g, n1, b1, bs1, n2, b2, bs2);
		}
	}

	/**
	 * Add a node the graph.
	 * @param g
	 * @param b
	 * @param inPgr
	 * @return
	 */
	static Node addNode(Graph g, Blob b, boolean inPgr) {
		Node ret = g.addNode();
		ret.set(BFConstants.BLOBFIELD, b);
		if (inPgr) {
			ret.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_INPGR);
			ret.setString(BFConstants.LABEL, "" + b.opCode());
		} else {
			ret.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_DATA);
			ret.setString(BFConstants.LABEL, "" + b.getCargo());
		}
		ret.set(BFConstants.BLOBINPGR, inPgr);
		return ret;
	}

	/**
	 * Mark this node as the Active Data Blob, ADB
	 * @param n the node
	 */
	static void setNodeAdb(Node n) {
		n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_ADB);
	}

	/**
	 * Mark this node as the Active Program Blob, APB
	 * @param n The node
	 */
	static void setNodeApb(Node n) {
		n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_APB);
	}

	/**
	 * Set the InPgr field of the node
	 * @param n Node to mark as normal InPgr node.
	 */
	static void setNodeInPgr(Node n) {
		n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_INPGR);
	}

}
