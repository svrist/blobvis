package dk.diku.blob.blobvis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Blob;
import model.BondSite;
import model.Model;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.visual.VisualItem;


public class BlobGraphFuser {

	private Model m;
	private Graph g;

	private Map<Blob,Node> bton;
	private Map<Integer,Blob> ntob;

	Control clickHandler;
	private DFSBlob dfs;

	public BlobGraphFuser(Graph g, Model m, ControlAdapter clickHandler){
		this.g = g;
		this.m = m;
		bton = new HashMap<Blob, Node>();
		ntob = new HashMap<Integer,Blob>();
		dfs = new DFSBlob(m.APB());
		this.clickHandler = clickHandler;
	}

	private class DFSBlob {
		List<Blob> nodelf;
		boolean inpgr = true;
		Blob start;

		DFSBlob(Blob start){
			nodelf = new ArrayList<Blob>();
			this.start = start;
		}

		public void run(){
			dfsBlob(start);
		}
		private Node dfsBlob(Blob pb1) {
			Blob cur = pb1;

			if (!bton.containsKey(cur)) {
				Node n = BlobGraphFuser.addNode(g, cur, inpgr);
				if (!inpgr){
					BlobGraphFuser.setNodeRightClickHandler(n, clickHandler);
				}
				bton.put(cur, n);
				ntob.put(n.getRow(),cur);
				if (m.APB().equals(cur)) {
					BlobGraphFuser.setNodeApb(n);
				} else if (m.ADB().equals(cur)) {
					BlobGraphFuser.setNodeAdb(n);
				}
				for (int i = 3; i >= 0; i--) {
					BondSite bs = BondSite.create(i);
					Blob bn = cur.follow(bs);
					if (bn != null) {
						Node nn = null;
						if (!bton.containsKey(bn)) {
							if (i == 0 && inpgr) {
								inpgr = false;
							}
							nn = dfsBlob(bn);
						} else if (!nodelf.contains(bn)) {
							nn = bton.get(bn);
						}
						BlobGraphFuser.addEdge(g,n,cur,bs,nn,bn);
					}
				}
				nodelf.add(cur);
			}
			return bton.get(cur);
		}

	}

	public static void addEdge(Graph g, Node n1, Blob b1, BondSite bs1, Node n2,
			Blob b2) {
		if (n2 != null) {
			if (g.getEdge(n2, n1) == null) {
				Edge e = g.addEdge(n1, n2);
				e.set(BFConstants.EDGENUMBERSRC, bs1.ordinal());
				BondSite bs2 = b2.boundTo(b1);
				if (bs2 != null) {
					e.set(BFConstants.EDGENUMBERTAR, bs2.ordinal());
				}
			}
		}
	}

	static void setNodeRightClickHandler(Node n, Control clickHandler){
		n.set(BFConstants.ACTION, clickHandler);
	}

	public static Node addNode(Graph g, Blob b, boolean inPgr) {
		Node ret = g.addNode();
		ret.set(BFConstants.BLOBFIELD, b);

		if (inPgr) {
			ret.set(BFConstants.BLOBTYPE, 3);
			ret.setString(BFConstants.LABEL, "" + b.opCode());
		} else {
			ret.setString(BFConstants.LABEL, "" + b.getCargo());
		}
		ret.set(BFConstants.BLOBINPGR, inPgr);

		return ret;
	}

	public static void setNodeAdb(Node n) {
		n.set(BFConstants.BLOBTYPE, 2);
	}

	public static void setNodeApb(Node n) {
		n.set(BFConstants.BLOBTYPE, 1);
	}

	public void populateGraphFromModelAPB() {

		dfs.run();
	}

	public void addDataBlobToBondSite(VisualItem item, BondSite from, BondSite to, int cargo) {
		addBlobToBondSite(item,ntob.get(item.getRow()),from,to,false,cargo);
	}

	private void addBlobToBondSite(Tuple n, Blob blob, BondSite from, BondSite to,boolean inPgr,int cargo) {
		Blob bn = new Blob(cargo);
		m.addBlob(bn);
		Blob.link(blob, bn, from, to);
		Node newn = addNode(g,bn,inPgr);
		addEdge(g, g.getNode(n.getRow()), blob, from, newn,bn);
	}

	public Blob getBlob(Tuple item) {
		return ntob.get(item.getRow());
	}






}
