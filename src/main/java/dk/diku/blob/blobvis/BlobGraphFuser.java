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
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

public class BlobGraphFuser {

	private Model m;
	private Graph g;
	private VisualGraph vg;

	private Map<Blob, Node> bton;
	private Map<Integer, Blob> ntob;
	private List<Blob> nodelf;

	Control clickHandler;
	private DFSBlob dfs;

	public BlobGraphFuser(Graph g, Model m, ControlAdapter clickHandler) {
		this.g = g;
		this.m = m;
		bton = new HashMap<Blob, Node>();
		ntob = new HashMap<Integer, Blob>();
		dfs = new DFSBlob(m.APB());
		this.clickHandler = clickHandler;
	}

	public void setVisualGraph(VisualGraph vg) {
		this.vg = vg;
	}

	private class DFSBlob implements Runnable{

		boolean inpgr = true;
		Blob start;
		Progressable p = new Progressable() {
			@Override
			public void progress(int progress) {/* ignore */
			}
		};

		DFSBlob(Blob start) {
			nodelf = new ArrayList<Blob>();
			this.start = start;
		}

		public void run() {
			dfsBlob(start);
			saveRoot(bton.get(start));
		}

		int count = 0;

		private Node dfsBlob(Blob pb1) {
			/*
			 * if (count%10==0){ p.progress((int)
			 * ((nodelf.size()/(double)m.count())*100)); } count++;
			 */
			Blob cur = pb1;

			if (!bton.containsKey(cur)) {
				Node n = BlobGraphFuser.addNode(g, cur, inpgr);
				if (!inpgr) {
					BlobGraphFuser.setNodeRightClickHandler(n, clickHandler);
				}
				bton.put(cur, n);
				ntob.put(n.getRow(), cur);
				if (m.APB().equals(cur)) {
					BlobGraphFuser.setNodeApb(n);
				} else if (m.ADB().equals(cur)) {
					BlobGraphFuser.setNodeAdb(n);
				}
				/*if (cur.opCode().equals("EXT") || cur.opCode().equals("JB 1")) {
					// System.out.println("addEdge:"+n+"->"+nn+"(bn:"+bn+")");
					System.out.println(cur.follow(BondSite.North) + ","
							+ cur.follow(BondSite.East)+","
							+ cur.follow(BondSite.South)+","
							+ cur.follow(BondSite.West));
				}*/
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
						}else{
							/*System.out.println("bn: "+bn+":"+bn.opCode()+"="+bton.containsKey(bn)+"-"+nodelf.contains(bn));
							System.out.println(bn.follow(BondSite.North) + ","
									+ bn.follow(BondSite.East)+","
									+ bn.follow(BondSite.South)+","
									+ bn.follow(BondSite.West));*/
						}
						/*	if (cur.opCode().equals("EXT") || cur.opCode().equals("JB 1")) {
							System.out.println("addEdge:" + n + "->" + nn
									+ "(bn:" + bn + ")");
						}*/
						BlobGraphFuser.addEdge(g, n, cur, bs, nn, bn);
					}
				}
				nodelf.add(cur);
			}
			return bton.get(cur);
		}

	}

	public static void addEdge(Graph g, Node n1, Blob b1, BondSite bs1,
			Node n2, Blob b2) {
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

	static void setNodeRightClickHandler(Node n, Control clickHandler) {
		n.set(BFConstants.ACTION, clickHandler);
	}

	Node root;

	public void saveRoot() {
		root = g.getSpanningTree().getRoot();
	}

	public void saveRoot(Node r) {
		root = r;
	}


	public void resetRoot() {
		g.getSpanningTree(root);
	}

	public static Node addNode(Graph g, Blob b, boolean inPgr) {

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

	public static void setNodeAdb(Node n) {
		n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_ADB);
	}

	public static void setNodeApb(Node n) {
		n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_APB);
	}

	public void populateGraphFromModelAPB(Progressable p) {
		dfs.p = p;
		populateGraphFromModelAPB();
	}

	public void populateGraphFromModelAPB() {
		dfs.run();
	}

	public void addDataBlobToBondSite(VisualItem item, BondSite from,
			BondSite to, int cargo) {
		Node n = addBlobToBondSite(item, ntob.get(item.getRow()), from, to,
				false, cargo);
		BlobGraphFuser.setNodeRightClickHandler(n, clickHandler);
	}

	private Node addBlobToBondSite(Tuple n, Blob blob, BondSite from,
			BondSite to, boolean inPgr, int cargo) {
		Blob bn = new Blob(cargo);
		m.addBlob(bn);
		Blob.link(blob, bn, from, to);
		Node newn = addNode(g, bn, inPgr);
		bton.put(bn, newn);
		ntob.put(newn.getRow(), bn);

		VisualItem cur = (VisualItem) vg.getNode(n.getRow());
		VisualItem item = (VisualItem) vg.getNode(newn.getRow());
		item.setX(cur.getX() + 1);
		item.setY(cur.getY() + 1);
		item.setEndY(cur.getY() + 1);
		item.setEndX(cur.getX() + 1);
		System.out.println(item.getX() + " " + item.getY());
		addEdge(g, g.getNode(n.getRow()), blob, from, newn, bn);
		return newn;
	}

	public Blob getBlob(Tuple item) {
		return ntob.get(item.getRow());
	}

	public void removeBlob(VisualItem vi) {
		Blob b = ntob.get(vi.getRow());
		for (int i = 0; i < 4; i++) {
			BondSite bs = BondSite.create(i);
			Blob otherend = b.follow(bs);
			if (otherend != null) {
				Blob.unlink(b, otherend, bs, otherend.boundTo(b));
			}
		}
		g.removeNode(vi.getRow());
	}

	public Node getRoot() {
		return root;
	}

}
