package dk.diku.blob.blobvis.prefuse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import dk.diku.blob.blobvis.prefuse.operations.Operation;

public class BlobGraphFuser implements BlobFuser {

	// State
	private Model m;
	private Graph g;
	private VisualGraph vg;
	private Node root;

	// Fusion between nodes and blobs
	private Map<Blob, Node> bton;
	private Map<Integer, Blob> ntob;

	public BlobGraphFuser(Graph g, Model m) {
		this.g = g;
		this.m = m;
		bton = new HashMap<Blob, Node>();
		ntob = new HashMap<Integer, Blob>();
	}

	public void setVisualGraph(VisualGraph vg) {
		this.vg = vg;
	}

	/* (non-Javadoc)
	 * @see dk.diku.blob.blobvis.prefuse.BlobFuser#addEdge(model.Blob, model.Blob)
	 */
	public void addEdge(Blob b1,Blob b2){
		FuseUtil.addEdge(g,getNode(b1),b1,getNode(b2),b2);
	}

	/**
	 * Save the given Node as the focus point of the blob simulation.
	 * 
	 * The APB of "The Bug".
	 * @param r Node to set as the current focus point.
	 */
	private void saveRoot(Node r) {
		root = r;
	}


	/* (non-Javadoc)
	 * @see dk.diku.blob.blobvis.prefuse.BlobFuser#addBlobAsNode(model.Blob, boolean)
	 */
	public Node addBlobAsNode(Blob b,boolean inPgr) {
		Node n = FuseUtil.addNode(g, b, inPgr);
		// Internal bookkeeping of relations
		setRelation(b, n);

		// Update APB/ADB markers if needed.
		if (m.APB().equals(b)) {
			FuseUtil.setNodeApb(n);
		} else if (m.ADB().equals(b)) {
			FuseUtil.setNodeAdb(n);
		}
		return n;
	}

	private void setRelation(Blob b, Node n) {
		bton.put(b, n);
		ntob.put(n.getRow(), b);
	}

	private void setSrcBondSite(Node srcn, Blob target, BondSite newvalue) {
		Node bbn1 = getNode(target);
		Edge e1 = g.getEdge(srcn, bbn1);
		if (e1 != null) {
			e1.set(BFConstants.EDGENUMBERSRC, newvalue.ordinal());
		} else {
			e1 = g.getEdge(bbn1, srcn);
			e1.set(BFConstants.EDGENUMBERTAR, newvalue.ordinal());
		}
	}

	/**
	 * Initialize a prefuse Graph from the contents of a Blob simulator
	 * datastructure.
	 * 
	 * This method takes a Progressable which will get progress % of the
	 * operation.
	 * @param p Progressable which will recieve progress information (0->100%)
	 * @throws InterruptedException if interupted in some way.
	 */
	public void populateGraphFromModelAPB(Progressable p) throws InterruptedException {
		DFSBlob dfs = new DFSBlob(this, m.APB(),m.count());
		dfs.p = p;
		runDFS(dfs);
	}
	public void populateGraphFromModelAPB() throws InterruptedException {
		runDFS(new DFSBlob(this, m.APB(),m.count()));
	}

	private void runDFS(DFSBlob dfs) throws InterruptedException {
		dfs.run();
		saveRoot(getNode(m.APB()));
	}

	/**
	 * Insert a new Blob at item with the given bondsites as connection points
	 * and with the given cargo
	 * @param item Base to add new blob to
	 * @param from the Bondsite on the existing blob
	 * @param to Bondsite on the new node
	 * @param cargo The cargo to set.
	 */
	public void addDataBlobToBondSite(VisualItem item, BondSite from,
			BondSite to, int cargo) {
		addBlobToBondSite(item, ntob.get(item.getRow()), from, to,
				false, cargo);

	}

	private Node addBlobToBondSite(Tuple n, Blob blob, BondSite from,
			BondSite to, boolean inPgr, int cargo) {

		// Add to simulator
		Blob bn = new Blob(cargo);
		m.addBlob(bn);
		Blob.link(blob, bn, from, to);

		// add to graph
		Node newn = FuseUtil.addNode(g, bn, inPgr);
		FuseUtil.addEdge(g, g.getNode(n.getRow()), blob, newn, bn);

		// Fix positioning.
		// TODO: should be positioned somewhere else. Maybe as a delegate from
		// the visualization code
		VisualItem cur = (VisualItem) vg.getNode(n.getRow());
		VisualItem item = (VisualItem) vg.getNode(newn.getRow());
		item.setX(cur.getX() + 1);
		item.setY(cur.getY() + 1);
		item.setEndY(cur.getY() + 1);
		item.setEndX(cur.getX() + 1);

		// BlobGraphFuser invariant bookkeeping.
		setRelation(bn, newn);

		return newn;
	}

	public Blob getBlob(Tuple item) {
		Blob ret = ntob.get(item.getRow());
		if (ret == null){
			throw new NullPointerException("Failed to find "+item+" in the blobGraphFuser libraries");
		}
		return ret;
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

	private Node getRoot() {
		return root;
	}

	private void rereadCargo(Blob b) {
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

	private void removeEdge(Node n1, Node n2) {
		removeEdge(n1,n2,false);
	}

	private void removeEdge(Node n1, Node n2,boolean keepSuperFlouous) {

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

	private void linkNodes(Node n1, BondSite b1, Node n2, BondSite b2) {
		// loop over adb/dest edges.
		// Remove all with src/tar of b1/ds
		// link adb with dest. Set src=b1, tar=ds
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

	private void updateTheBug(Node r, Node nn, Node adbncur, Node adbnnext) {
		Edge thebug = g.addEdge(nn, adbnnext);
		removeEdge(r, adbncur);
		adbncur.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_DATA);
		adbnnext.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_ADB);
		/* System.out.println(nn + " -> " + adbnnext); */
		thebug.set(BFConstants.EDGENUMBERSRC, 0);
		thebug.set(BFConstants.EDGENUMBERTAR, 0);
	}

	private void stepModel(Node node, Node nn) {
		if (nn != null) {
			nn.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_APB);
			saveRoot(nn);
			m.step();
		} else {
			throw new RuntimeException("Failed to find the child successor");
		}
	}

	private Blob APB() {
		return m.APB();
	}
	private Blob ADB() {
		return m.ADB();
	}

	public void step(){
		StepResult result;
		Operation o = Operation.parse(APB().opCode());
		switch (o.type) {
		case CHD:
			result = doCHD(APB(), ADB());
			break;
		case SCG:
		case DBS:
			result = new StepResult(APB(), ADB()).reread(true);
			break;
		case JB:
			result = doJB(APB(), ADB());
			break;
		case SBS:
			result = doSBS(APB(), ADB());
			break;
		case JN:
			result = doJN(APB(), ADB());
			break;
		case SWL:
			result = doSWL(APB(), ADB());
			break;
		case JCG:
			result = doJCG(APB(), ADB());
			break;
		default:
			result = new StepResult(APB(), ADB()); // Default action
			break;
		}
		execute(result);
	}

	protected void execute(StepResult sr){
		sr.testValid();
		FuseUtil.setNodeInPgr(getRoot());
		Node nn = getNode(sr.apbnext);
		updateTheBug(getNode(sr.apbcur), nn, getNode(sr.adbcur),
				getNode(sr.adbnext));
		stepModel(getNode(sr.apbcur), nn);
		if (sr.reread_cargo){
			rereadCargo(sr.adbnext);
		}
	}


	private StepResult doSWL(Blob apb, Blob adb) {
		StepResult result;
		BondSite b1 = BondSite
		.create(((8 + 4) & apb.getCargo()) / 4);
		BondSite b2 = BondSite.create((2 + 1) & apb.getCargo());
		result = new StepResult(apb, adb);

		Blob adb_b1 = adb.follow(b1);
		Node adb_b1n = null;
		BondSite ts2 = null;
		if (adb_b1 != null) {
			ts2 = adb_b1.boundTo(adb);
			adb_b1n = getNode(adb_b1);
		}
		Blob adb_b2 = adb.follow(b2);
		Blob adb_b2_b1 = null;
		if (adb_b2 != null) {
			adb_b2_b1 = adb_b2.follow(b1);
			Node adb_b2n = getNode(adb_b2);
			if (adb_b2_b1 != null) {
				BondSite ts1 = adb_b2_b1.boundTo(adb_b2);
				// TODO: Blob.link( ADB, adb_b2_b1, b1, ts1 );
				Node adb_b2_b1n = getNode(adb_b2_b1);
				linkNodes(getNode(adb), b1, adb_b2_b1n, ts1);
				if (adb_b1 != null) {
					// TODO: Blob.link( adb_b2, adb_b1, b1, ts2 );
					linkNodes(adb_b2n, b1, adb_b1n, ts2);
					// case 4: something(x) on b1 and something(y)
					// on
					// b2.b1 -> b1=y, b2.b1=x
				} else {

					// Case 2: Nothing on b1 and something(x) on
					// b2.b1
					// -> b2.b1=null,b1=x
					// TODO: adb_b2.unlink( b1 );
					removeEdge(adb_b2n, adb_b2_b1n);
				}
			} else if (adb_b1 != null) {
				// case 3: Something(x) on b1 and nothing on b2.b1
				// ->
				// b2.b1=x,b1=nothing
				// TODO:Blob.link( adb_b2,adb_b1 , b1, ts2 );
				linkNodes(adb_b2n, b1, adb_b1n, ts2);
				// TODO: ADB.unlink( b1 );
				removeEdge(getNode(adb), adb_b1n);
			} else {
				/* System.out.println("Case 6"); */
				// case 6: Nothing on b1 and nothing on b2.b1 ->
				// nothing
				// happens
			}
		} else if (adb_b1 != null) {
			// case 5: something(x) on b1 and nothing on b2 ->
			// b1=null,
			// x disappers
			// TODO: Blob.unlink( ADB, adb_b1, b1, ts2 );
			removeEdge(getNode(adb), adb_b1n);
		} else {
			// Case 1: Nothing on b1 and nothing on b2 -> nothing
			// happens
		}
		return result;
	}

	private StepResult doJN(Blob apb, Blob adb) {
		StepResult result;
		BondSite b1 = BondSite
		.create(((8 + 4) & apb.getCargo()) / 4);
		BondSite b2 = BondSite.create((2 + 1) & apb.getCargo());

		Blob dest1 = adb.follow(b1);
		Blob dest = dest1.follow(b2);
		Node destn = getNode(dest);
		if (dest != null) {
			BondSite ds = dest.boundTo(dest1);
			linkNodes(getNode(adb), b1, destn, ds);
		} else {
			Node n = getNode(dest1);
			removeEdge(getNode(adb), n);
		}
		result = new StepResult(apb, adb);
		return result;
	}

	private StepResult doSBS(Blob apb, Blob adb) {
		StepResult result;
		BondSite b1 = BondSite
		.create(((8 + 4) & apb.getCargo()) / 4);
		BondSite b2 = BondSite.create((2 + 1) & apb.getCargo());
		Blob bb1 = adb.follow(b1);
		Blob bb2 = adb.follow(b2);
		if (bb1 != null) {
			setSrcBondSite(getNode(adb), bb1, b2);
		}
		if (bb2 != null) {
			setSrcBondSite(getNode(adb), bb2, b1);
		}
		result = new StepResult(apb, adb);
		return result;
	}

	private StepResult doJCG(Blob apb, Blob adb) {
		StepResult result;
		int c = (4 + 2 + 1) & apb.getCargo();
		result = new StepResult(apb, adb);
		if (!adb.getCargo(c)) {
			result.apbNext(BondSite.West);
		}
		return result;
	}

	private StepResult doCHD(Blob apb, Blob adb) {
		StepResult result;
		BondSite b = BondSite.create((2 + 1) & apb.getCargo());
		result = new StepResult(apb, adb).adbNext(b);
		return result;
	}

	private StepResult doJB(Blob apb, Blob adb) {
		StepResult result;
		result = new StepResult(apb, adb).reread(false);
		BondSite b = BondSite.create((2 + 1) & apb.getCargo());
		result.apbNext(apb.follow(m.ADB().follow(b) == null ? BondSite.West
				: BondSite.South));
		return result;
	}

}
