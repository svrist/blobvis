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
import dk.diku.blob.blobvis.prefuse.operations.Operation;
import dk.diku.blob.blobvis.util.Progressable;

public class BlobGraphFuser implements BlobFuser {

	// State
	private Model m;
	private Graph g;
	private VisualGraph vg;
	private Node root;

	// Fusion between nodes and blobs
	private Map<Blob, Node> bton;
	private Map<Integer, Blob> ntob;

	public BlobGraphFuser() {
		internalReset();
		bton = new HashMap<Blob, Node>();
		ntob = new HashMap<Integer, Blob>();
	}

	public void readBlobModelConfiguration(String filename){
		getModel().readConfiguration(filename);
	}


	public void reset(){
		internalReset();
	}
	private void internalReset(){
		if (g!= null){ g.clear(); }
		if (m != null){ m.reset(); }
		setGraph(new Graph());
		setModel(new Model());
		getGraph().getNodeTable().addColumns(BFConstants.LABEL_SCHEMA);
		getGraph().getNodeTable().addColumns(BFConstants.BLOB_SCHEMA);
		getGraph().getEdgeTable().addColumns(BFConstants.EDGE_SCHEMA);
	}

	/* TODO: shouldnt be neccessary */
	public void setVisualGraph(VisualGraph vg) {
		this.vg = vg;
	}

	/* (non-Javadoc)
	 * @see dk.diku.blob.blobvis.prefuse.BlobFuser#addEdge(model.Blob, model.Blob)
	 */
	public void addEdge(Blob b1,BondSite bs1, Blob b2,BondSite bs2){
		Blob.link(b1, b2, bs1, bs2);
		addEdge(b1,b2);
	}

	/* (non-Javadoc)
	 * @see dk.diku.blob.blobvis.prefuse.BlobFuser#addEdge(model.Blob, model.Blob)
	 */
	public void addEdge(Blob b1,Blob b2){
		FuseUtil.addEdge(getGraph(),getNode(b1),b1,getNode(b2),b2);
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
		assert(getNode(b) == null);
		if (getNode(b) != null){
			throw new IllegalArgumentException("Tried to add node that was there already: "+b);
		}
		Node n = FuseUtil.addNode(getGraph(), b, inPgr);
		// Internal bookkeeping of relations
		setRelation(b, n);

		// Update APB/ADB markers if needed.
		if (getModel().APB().equals(b)) {
			FuseUtil.setNodeApb(n);
		} else if (getModel().ADB().equals(b)) {
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
		Edge e1 = getGraph().getEdge(srcn, bbn1);
		if (e1 != null) {
			e1.set(BFConstants.EDGENUMBERSRC, newvalue.ordinal());
		} else {
			e1 = getGraph().getEdge(bbn1, srcn);
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
		DFSBlob dfs = new DFSBlob(this, getModel().APB(),getModel().count());
		dfs.setProgressable(p);
		runDFS(dfs);
	}
	public void populateGraphFromModelAPB() throws InterruptedException {
		runDFS(new DFSBlob(this, getModel().APB(),getModel().count()));
	}

	private void runDFS(DFSBlob dfs) throws InterruptedException {
		dfs.run();
		saveRoot(getNode(getModel().APB()));
	}

	public Blob addDataBlobToBondSite(Blob b, BondSite from,
			BondSite to, int cargo) {
		return addDataBlobToBondSite(getNode(b),from,to,cargo);
	}

	/**
	 * Insert a new Blob at item with the given bondsites as connection points
	 * and with the given cargo
	 * @param item Base to add new blob to
	 * @param from the Bondsite on the existing blob
	 * @param to Bondsite on the new node
	 * @param cargo The cargo to set.
	 */
	public Blob addDataBlobToBondSite(Tuple item, BondSite from,
			BondSite to, int cargo) {
		return addBlobToBondSite(item, ntob.get(item.getRow()), from, to,
				false, cargo);

	}

	private Blob addBlobToBondSite(Tuple n, Blob blob, BondSite from,
			BondSite to, boolean inPgr, int cargo) {

		// Add to simulator
		Blob bn = new Blob(cargo);
		getModel().addBlob(bn);
		Blob.link(blob, bn, from, to);

		// add to graph
		Node newn = FuseUtil.addNode(getGraph(), bn, inPgr);
		FuseUtil.addEdge(getGraph(), getGraph().getNode(n.getRow()), blob, newn, bn);

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

		return bn;
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
		getGraph().removeNode(vi.getRow());
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

	private void removeEdge(Blob b1, Blob b2) {
		removeEdge(getNode(b1),getNode(b2));

	}
	private void removeEdge(Node n1, Node n2) {
		FuseUtil.removeEdge(g,n1,n2,false);
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
				FuseUtil.removeEdge(g,edge.getSourceNode(),edge.getTargetNode(),true);
			}
		}
		Edge ne = getGraph().addEdge(n1, n2);

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
		Edge thebug = getGraph().addEdge(nn, adbnnext);
		removeEdge(r, adbncur);
		adbncur.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_DATA);
		FuseUtil.setNodeInPgr(adbncur,false);
		FuseUtil.setNodeAdb(adbnnext);
		/* System.out.println(nn + " -> " + adbnnext); */
		thebug.set(BFConstants.EDGENUMBERSRC, 0);
		thebug.set(BFConstants.EDGENUMBERTAR, 0);
	}


	private void stepModel(){
		getModel().step();
		Node n = getNode(apb());
		saveRoot(n);
		FuseUtil.setNodeApb(n);
	}

	private Blob apb() {
		return getModel().APB();
	}
	private Blob adb() {
		return getModel().ADB();
	}

	public void step(){
		StepResult result;
		Operation o = Operation.parse(apb().opCode());
		switch (o.type) {
		case CHD:
			result = doCHD(apb(), adb());
			break;
		case SCG:
		case DBS:
			result = new StepResult(apb(), adb()).reread(true);
			break;
		case JB:
			result = doJB(apb(), adb());
			break;
		case SBS:
			result = doSBS(apb(), adb());
			break;
		case JN:
			result = doJN(apb(), adb());
			break;
		case SWL:
			result = doSWL(apb(), adb());
			break;
		case JCG:
			result = doJCG(apb(), adb());
			break;
		case INS:
			result = doINS(apb(),adb(),o);
			break;
		case FIN:
			// do default action.
		default:
			result = new StepResult(apb(), adb()); // Default action
			break;
		}
		execute(result);
	}

	private StepResult doINS(Blob apb, Blob adb,Operation o) {
		StepResult result =  new StepResult(apb, adb).nostep(true);
		BondSite b = BondSite.create(Integer.parseInt(o.args.get(0)));
		Blob oldBlob = adb.follow(b);


		// as result has nostep=true, we need to do the step
		// our selves.
		stepModel();
		// this corrosponds to some kind of advice in aspect orient.
		Blob newBlob = adb.follow(b);
		addBlobAsNode(newBlob, false);
		if( oldBlob != null )
		{
			addEdge( oldBlob, newBlob);
			removeEdge(adb,oldBlob);
		}
		addEdge( adb, newBlob);
		return result;
	}

	protected void execute(StepResult sr){
		sr.testValid();
		FuseUtil.setNodeInPgr(getRoot());
		Node n = getNode(sr.apbcur);
		Node nn = getNode(sr.apbnext);
		updateTheBug(n, nn, getNode(sr.adbcur),
				getNode(sr.adbnext));
		if (!sr.nostep){
			stepModel();
		}
		if (sr.rereadCargo){
			rereadCargo(sr.adbnext);
		}
	}


	private StepResult doSWL(Blob apb, Blob adb) {
		StepResult result = new StepResult(apb, adb);
		BondSite b1 = BondSite
		.create(((8 + 4) & apb.getCargo()) / 4);
		BondSite b2 = BondSite.create((2 + 1) & apb.getCargo());

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
		result.apbNext(apb.follow(getModel().ADB().follow(b) == null ? BondSite.West
				: BondSite.South));
		return result;
	}

	public void setModel(Model m) {
		this.m = m;
	}

	public Model getModel() {
		return m;
	}

	public void setGraph(Graph g) {
		this.g = g;
	}

	public Graph getGraph() {
		return g;
	}

}
