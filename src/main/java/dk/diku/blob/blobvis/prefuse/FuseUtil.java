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
		}else{
			throw new NullPointerException("N2 argument is null (n1:"+n1+")");
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
			ret.setString(BFConstants.LABEL, "" + b.opCode());
		} else {
			ret.setString(BFConstants.LABEL, "" + b.getCargo());
		}
		setNodeInPgr(ret, inPgr);

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
	static void setNodeInPgr(Node n, boolean inPgr) {
		if (inPgr){
			n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_INPGR);
		}else{
			n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_DATA);
		}
		n.set(BFConstants.BLOBINPGR,inPgr);


	}

	static void removeEdge(Graph g, Node n1, Node n2,boolean keepSuperFlouous) {

		Edge e1 = g.getEdge(n1, n2);

		if (e1 != null) {
			/*
			 * System.out.println("Removing " + e1 + " " +
			 * e1.getSourceNode() + "->" + e1.getTargetNode());
			 */
			g.removeEdge(e1);
		}
		Edge e2 = g.getEdge(n2, n1);
		if (e2 != null) {
			/*
			 * System.out.println("Removing " + e2 + " " +
			 * e2.getSourceNode() + "->" + e2.getTargetNode());
			 */
			g.removeEdge(e2);
		}
		if (!keepSuperFlouous){
			if (g.getDegree(n1) == 0){
				System.out.println(n1+" went superflous. Removing");
				g.removeNode(n1);
			}
			if (g.getDegree(n2) == 0){
				System.out.println(n2+" went superflous. Removing");
				g.removeNode(n2);
			}
		}
	}

	public static void setNodeInPgr(Node n) {
		setNodeInPgr(n,true);

	}

}
