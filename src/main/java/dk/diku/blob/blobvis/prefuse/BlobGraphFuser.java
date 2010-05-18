package dk.diku.blob.blobvis.prefuse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import model.Blob;
import model.BondSite;
import model.Model;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.gui.Progressable;
import dk.diku.blob.blobvis.util.Pair;

public class BlobGraphFuser {

	private Model m;
	private Graph g;
	private VisualGraph vg;

	private Map<Blob, Node> bton;
	private Map<Integer, Blob> ntob;
	private List<Blob> nodelf;

	private DFSBlob dfs;

	public BlobGraphFuser(Graph g, Model m) {
		this.g = g;
		this.m = m;
		bton = new HashMap<Blob, Node>();
		ntob = new HashMap<Integer, Blob>();
		dfs = new DFSBlob(m.APB());
	}

	public void setVisualGraph(VisualGraph vg) {
		this.vg = vg;
	}

	private class DFSBlob {

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

		public void run() throws InterruptedException {
			it_dfsBlob(start);
			saveRoot(bton.get(start));
		}

		int count = 0;





		private void it_dfsBlob(Blob start) throws InterruptedException {
			Stack<Pair<Blob,Boolean>> nextStack = new Stack<Pair<Blob,Boolean>>();
			Stack<Blob> traversed = new Stack<Blob>();
			Map<Blob, List<Blob>> edges = new HashMap<Blob, List<Blob>>();

			// Enqueue root
			nextStack.add(new Pair<Blob,Boolean>(start,true));
			double mcount = m.count();

			while (!nextStack.isEmpty()) {
				if (count % 10 == 0) {
					p.progress((int) ((traversed.size() / mcount) * 100));
				}
				count++;
				// Dequeue next node for comparison
				// And add it 2 list of traversed nodes
				Pair<Blob,Boolean> pb = nextStack.pop();
				Blob b = pb.one;
				traversed.push(b);
				addBlobAsNode(b,pb.two);
				// empty any waiting edges. This node is now ready
				popEdges(edges, b);
				// Enqueue new neighbors
				for (BondSite bs : BondSite.asList()) {
					Boolean inp = pb.two;
					Blob neighbor = b.follow(bs);
					if (neighbor == null)
						continue;
					Pair<Blob,Boolean> newbse = new Pair<Blob,Boolean>(neighbor,inp);
					if (!traversed.contains(neighbor)
							&& !nextStack.contains(newbse)) {
						if (bs.equals(BondSite.North)){
							newbse.two = !inp;
						}
						nextStack.push(newbse);
					}
					if (nextStack.contains(newbse)) {
						// Save edges for all childs in a list.
						stackEdge(edges, b, neighbor);
					}
				}
				if (Thread.interrupted()){
					throw new InterruptedException();
				}

			}

		}

		private boolean popEdges(Map<Blob, List<Blob>> edges, Blob b) {
			// Empty edge list for this node.
			boolean viaNorth=false;
			if (edges.containsKey(b)) {
				for (Blob prev : edges.get(b)) {
					Node n = bton.get(prev);
					Node nn = bton.get(b);
					if (prev.boundTo(b).equals(BondSite.North)){
						viaNorth = true;
					}
					BlobGraphFuser.addEdge(g, n, prev, nn, b);
				}
			}
			return viaNorth;
		}

		private Node dfsBlob(Blob pb1) {

			if (count % 10 == 0) {
				p.progress((int) ((nodelf.size() / (double) m.count()) * 100));
			}
			count++;

			Blob cur = pb1;

			if (!bton.containsKey(cur)) {
				Node n = addBlobAsNode(cur);
				/*
				 * if (cur.opCode().equals("EXT") ||
				 * cur.opCode().equals("JB 1")) { //
				 * System.out.println("addEdge:"+n+"->"+nn+"(bn:"+bn+")");
				 * System.out.println(cur.follow(BondSite.North) + "," +
				 * cur.follow(BondSite.East)+"," +
				 * cur.follow(BondSite.South)+"," + cur.follow(BondSite.West));
				 * }
				 */
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
						} else {
							/*
							 * System.out.println("bn: "+bn+":"+bn.opCode()+"="+bton
							 * .containsKey(bn)+"-"+nodelf.contains(bn));
							 * System.out.println(bn.follow(BondSite.North) +
							 * "," + bn.follow(BondSite.East)+"," +
							 * bn.follow(BondSite.South)+"," +
							 * bn.follow(BondSite.West));
							 */
						}
						/*
						 * if (cur.opCode().equals("EXT") ||
						 * cur.opCode().equals("JB 1")) {
						 * System.out.println("addEdge:" + n + "->" + nn +
						 * "(bn:" + bn + ")"); }
						 */
						BlobGraphFuser.addEdge(g, n, cur, bs, nn, bn);
					}
				}
				nodelf.add(cur);
			}
			return bton.get(cur);
		}
		private Node addBlobAsNode(Blob cur) {
			return addBlobAsNode(cur,inpgr);
		}

		private Node addBlobAsNode(Blob cur,boolean inPgr) {
			Node n = BlobGraphFuser.addNode(g, cur, inPgr);

			bton.put(cur, n);
			ntob.put(n.getRow(), cur);
			if (m.APB().equals(cur)) {
				BlobGraphFuser.setNodeApb(n);
			} else if (m.ADB().equals(cur)) {
				BlobGraphFuser.setNodeAdb(n);
			}
			return n;
		}

	}

	public static void addEdge(Graph g, Node n1, Blob b1, Node n2, Blob b2) {
		if (n2 != null) {
			BondSite bs1 = b1.boundTo(b2);
			BondSite bs2 = b2.boundTo(b1);
			addEdge(g, n1, b1, bs1, n2, b2, bs2);
		}
	}

	public static void addEdge(Graph g, Node n1, Blob b1, BondSite bs1,
			Node n2, Blob b2) {
		if (n2 != null) {
			BondSite bs2 = b2.boundTo(b1);
			addEdge(g, n1, b1, bs1, n2, b2, bs2);
		}

	}

	public static void addEdge(Graph g, Node n1, Blob b1, BondSite bs1,
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



	private static void stackEdge(Map<Blob, List<Blob>> edges, Blob b,
			Blob neighbor) {
		List<Blob> neighboredges;
		if (edges.containsKey(neighbor)) {
			neighboredges = edges.get(neighbor);
		} else {
			neighboredges = new ArrayList<Blob>();
			edges.put(neighbor, neighboredges);
		}
		neighboredges.add(b);
	}

	Node root;

	public void saveRoot() {
		/*System.out.println("Ouch, spanning tree");
		root = g.getSpanningTree().getRoot();*/
	}

	public void saveRoot(Node r) {
		root = r;
	}

	public void resetRoot() {
		/*System.out.println("Ouch, spanningTree");
		g.getSpanningTree(root);*/
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

	public static void setNodeInPgr(Node n) {
		n.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_INPGR);
	}

	public void setSrcBondSite(Node srcn, Blob target, BondSite newvalue) {
		Node bbn1 = getNode(target);
		Edge e1 = g.getEdge(srcn, bbn1);
		if (e1 != null) {
			e1.set(BFConstants.EDGENUMBERSRC, newvalue.ordinal());
		} else {
			e1 = g.getEdge(bbn1, srcn);
			e1.set(BFConstants.EDGENUMBERTAR, newvalue.ordinal());
		}
	}

	public void populateGraphFromModelAPB(Progressable p) throws InterruptedException {
		dfs.p = p;
		populateGraphFromModelAPB();
	}

	public void populateGraphFromModelAPB() throws InterruptedException {
		dfs.run();
	}

	public void addDataBlobToBondSite(VisualItem item, BondSite from,
			BondSite to, int cargo) {
		addBlobToBondSite(item, ntob.get(item.getRow()), from, to,
				false, cargo);

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
		// System.out.println(item.getX() + " " + item.getY());
		addEdge(g, g.getNode(n.getRow()), blob, newn, bn);
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

	public void rereadCargo(Blob b) {
		Node n = bton.get(b);
		boolean inp = (Boolean)n.get(BFConstants.BLOBINPGR);
		if (inp){
			n.setString(BFConstants.LABEL,b.opCode());
		}else{
			n.setString(BFConstants.LABEL,b.getCargo()+"");
		}
	}

	public Node getNode(Blob b) {
		return bton.get(b);
	}

	public void removeEdge(Node n1, Node n2) {
		removeEdge(n1,n2,false);
	}

	public void removeEdge(Node n1, Node n2,boolean keepSuperFlouous) {

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

	public void linkNodes(Node n1, BondSite b1, Node n2, BondSite b2) {
		// loop over adb/dest edges.
		// Remove all with src/tar of b1/ds
		// link adb with dest. Set src=b1, tar=ds
		// System.out.println("Adb: " + n1);
		List<Edge> rems = gatherRemoveList(b1, n1);
		// System.out.println("dstn: " + n2);
		rems.addAll(gatherRemoveList(b2, n2));
		for (Edge element : rems) {
			Edge edge = element;
			if (edge.isValid()){
				removeEdge(edge.getSourceNode(),edge.getTargetNode(),true);
			}
		}
		Edge ne = g.addEdge(n1, n2);

		ne.set(BFConstants.EDGENUMBERSRC, b1.ordinal());
		ne.set(BFConstants.EDGENUMBERTAR, b2.ordinal());
	}

	@SuppressWarnings("unchecked")
	private List<Edge> gatherRemoveList(BondSite needle, Node n) {
		List<Edge> rems = new ArrayList<Edge>();
		Iterator removeIterator = n.edges();
		while (removeIterator.hasNext()) {
			Edge ce = (Edge) removeIterator.next();
			String field = ce.getSourceNode() == n ? BFConstants.EDGENUMBERSRC
					: BFConstants.EDGENUMBERTAR;
			int bsi = (Integer) ce.get(field);
			if (bsi == needle.ordinal()) {
				rems.add(ce);
			}
		}
		return rems;
	}

	public void updateTheBug(Node r, Node nn, Node adbncur, Node adbnnext) {
		Edge thebug = g.addEdge(nn, adbnnext);
		removeEdge(r, adbncur);
		adbncur.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_DATA);
		adbnnext.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_ADB);
		/* System.out.println(nn + " -> " + adbnnext); */
		thebug.set(BFConstants.EDGENUMBERSRC, 0);
		thebug.set(BFConstants.EDGENUMBERTAR, 0);
	}

	public void stepModel(Node node, Node nn) {
		if (nn != null) {
			nn.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_APB);
			saveRoot(nn);
			m.step();
		} else {
			throw new RuntimeException("Failed to find the child successor");
		}

	}

	public Blob APB() {
		return m.APB();
	}
	public Blob ADB() {
		return m.ADB();
	}

	public void execute(StepResult sr){
		sr.testValid();
		setNodeInPgr(getRoot());
		Node nn = getNode(sr.apbnext);
		updateTheBug(getNode(sr.apbcur), nn, getNode(sr.adbcur),
				getNode(sr.adbnext));
		stepModel(getNode(sr.apbcur), nn);
		if (sr.reread_cargo){
			rereadCargo(sr.adbnext);
		}
	}

}
