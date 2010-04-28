package dk.diku.blob.blobvis;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.KeyStroke;

import model.Blob;
import model.BondSite;
import model.Model;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.controls.FocusControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.render.PolygonRenderer;
import prefuse.render.Renderer;
import prefuse.util.ColorLib;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.TreeDepthItemSorter;

@SuppressWarnings("serial")
public class BlobVis extends Display {
	public class SingleForceAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			getVisualization().cancel("force");
			getVisualization().cancel("singleforce");

			System.out.println("Single force run");
			getVisualization().run("singleforce");

		}

	}

	boolean paused = false;

	public class PauseForceAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			getVisualization().cancel("force");
			getVisualization().cancel("base");
			if (paused) {
				System.out.println("Restarting after pause");
				getVisualization().run("force");
				paused = false;
			} else {
				System.out.println("Pausing");
				paused = true;
			}
			getVisualization().run("base");

		}

	}

	public class StepModelAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			hops += 1;
			filter.setDistance(hops);
			getVisualization().run("init");
			System.out.println("Hops: " + hops);

			/*
			 * getVisualization().cancel("force");
			 * getVisualization().run("force");
			 */

		}

	}

	public class ToggleGraphAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			if ("hest".equals(curg)) {
				readProgramAndDataAsGraph("ko");
				curg = "ko";
			} else if ("ko".equals(curg)) {
				readProgramAndDataAsGraph("blob");
				curg = "blob";
			} else {
				readProgramAndDataAsGraph("hest");
				curg = "hest";
			}
			getVisualization().cancel("base");
			getVisualization().cancel("init");
			getVisualization().run("init");
			getVisualization().run("base");

		}

	}

	public class BlobTree extends Graph {

		public void swap(Node r, int ci1, int ci2) {

			/*
			 * int c1r = r.getChild(ci1).getRow(); int c2r =
			 * r.getChild(ci2).getRow(); System.out.println(c1r+" "+c2r); int
			 * tmp = m_links.getInt(c1r,CHILDINDEX); m_links.setInt(c1r,
			 * CHILDINDEX, m_links.getInt(c2r, CHILDINDEX) );
			 * m_links.setInt(c2r, CHILDINDEX, tmp );
			 */

			Node c1 = r.getChild(ci1);
			Edge e1 = g.getEdge(r, r.getChild(ci1));
			g.removeEdge(e1);
			g.addEdge(r, c1);

			if (c1.canGet(BlobFuse.BLOBFIELD, Blob.class)) {
				Blob b = (Blob) r.get(BlobFuse.BLOBFIELD);
				b.swap(BondSite.North, BondSite.South);
			}

			// Edge e2 = g.getEdge(r, r.getChild(ci2));

		}

	}

	public class SwitchAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Node r = g.getNode(0);

			((BlobTree) g).swap(r, 0, 1);
			getVisualization().cancel("init");
			getVisualization().run("init");
		}

	}

	String curg = "hest";
	Node selected = null;
	private static final String GRAPH = "graph";
	private static final String EDGES = "graph.edges";
	private static final String NODES = "graph.nodes";
	private static final String AGGR = "aggregates";
	Layout lay;
	private boolean tree = false;
	int hops = 10;
	final GraphDistanceFilter filter;

	public BlobVis(String filename) {
		super(new Visualization());

		LabelRenderer tr = new LabelRenderer();
		tr.setRoundedCorner(8, 8);

		/*Renderer polyR = new AggregatePolygonRenderer(Constants.POLY_TYPE_CURVE);
		((PolygonRenderer) polyR).setCurveSlack(0.15f);*/
		DefaultRendererFactory dfr = new DefaultRendererFactory(tr,
				new BlobEdgeRenderer());
		// dfr.add("ingroup('aggregates')", polyR);
		m_vis.setRendererFactory(dfr);

		if (filename == null)
			filename = "hest";
		readProgramAndDataAsGraph(filename);
		// now create the main layout routine

		// ActionList init = setupInit();

		ActionList init = new ActionList();

		filter = new GraphDistanceFilter(GRAPH, hops);

		init.add(filter);
		init.add(genColors());
		init.add(new RadialTreeLayout(GRAPH));
		init.add(new RepaintAction());

		ActionList singleforce = new ActionList();
		singleforce.add(new ForceDirectedLayout(GRAPH, false, true));
		singleforce.add(genColors());
		singleforce.add(new RepaintAction());
		m_vis.putAction("singleforce", singleforce);

		ActionList base = new ActionList(Action.INFINITY, 100);
		/*
		 * base.add(genColors()); //base.add(new AggregateLayout(AGGR));
		 * base.add(new RepaintAction());
		 */

		ActionList force = new ActionList(Action.INFINITY, 32);
		force.add(genColors());
		setLayout();
		force.add(lay);
		// force.add(AggrForce());
		force.add(new RepaintAction());

		m_vis.putAction("force", force);

		m_vis.putAction("init", init);
		// m_vis.alwaysRunAfter("force", "base");

		// m_vis.putAction("init", init);
		// m_vis.putAction("base", base);
		// set up the display
		setSize(800, 800);

		pan(150, 150);
		setHighQuality(true);
		setItemSorter(new TreeDepthItemSorter());

		//addControlListener(new AggregateDragControl());
		addControlListener(new FocusControl(1));
		addControlListener(new PanControl());
		addControlListener(new ZoomControl());
		addControlListener(new WheelZoomControl());
		addControlListener(new ZoomToFitControl());

		// set things running
		m_vis.run("init");
		/* m_vis.run("base"); */

		if (!paused) {
			m_vis.run("force");
		}

		registerKeyboardAction(new SwitchAction(), "switchit", KeyStroke
				.getKeyStroke("ctrl 1"), WHEN_FOCUSED);

		registerKeyboardAction(new StepModelAction(), "switchit5", KeyStroke
				.getKeyStroke("ctrl 5"), WHEN_FOCUSED);

		registerKeyboardAction(new PauseForceAction(), "pauseforce", KeyStroke
				.getKeyStroke("P"), WHEN_FOCUSED);

		registerKeyboardAction(new SingleForceAction(), "singleforce",
				KeyStroke.getKeyStroke("S"), WHEN_FOCUSED);

		System.out.println("NodeCount: " + g.getNodeCount());

	}

	/*
	 * private ActionList setupInit() { ActionList init = new ActionList(100);
	 * init.add(genColors()); /* init.setPacingFunction(new
	 * SlowInSlowOutPacer()); init.add(new QualityControlAnimator());
	 * 
	 * setLayout(); //init.add(lay); //init.add(AggrForce()); init.add((new
	 * AggregateLayout(AGGR)));
	 * 
	 * /* init.add(new LocationAnimator(NODES));
	 * 
	 * init.add(new RepaintAction());
	 * 
	 * return init; }
	 */

	/*
	 * private Layout AggrForce() { ForceSimulator fsim = new ForceSimulator();
	 * ForceDirectedLayout ret = null; fsim.addForce(new SpringForce());
	 * fsim.addForce(new
	 * NBodyForce());//5,NBodyForce.DEFAULT_MIN_DISTANCE,NBodyForce
	 * .DEFAULT_THETA)); fsim.addForce(new DragForce()); ret = new
	 * AggregateForceDirectedLayout(GRAPH, fsim, true); ret.setDataGroups(AGGR,
	 * null); return ret; }
	 */

	private void setLayout() {
		if (tree) {
			lay = new NodeLinkTreeLayout(GRAPH);
			((NodeLinkTreeLayout) lay)
					.setOrientation(Constants.ORIENT_TOP_BOTTOM);
		} else {
			lay = new ForceDirectedLayout(GRAPH);
		}
	}

	private ActionList col_cache = null;

	private ActionList genColors() {
		if (col_cache == null) {
			// set up the visual operators
			// first set up all the color actions
			ColorAction nStroke = new ColorAction(NODES, VisualItem.STROKECOLOR);
			nStroke.setDefaultColor(ColorLib.gray(100));
			nStroke.add("_hover", ColorLib.gray(50));

			ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR);
			nFill.setDefaultColor(ColorLib.gray(255));
			nFill.add("_hover", ColorLib.gray(100));
			nFill.add(VisualItem.FIXED, ColorLib.rgb(255, 100, 100));
			nFill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 200, 125));

			ColorAction nEdges = new ColorAction(EDGES, VisualItem.STROKECOLOR);
			nEdges.setDefaultColor(ColorLib.gray(100));
			ColorAction nEdgesText = new ColorAction(EDGES,
					VisualItem.TEXTCOLOR, ColorLib.gray(0));

			ColorAction nText = new ColorAction(NODES, VisualItem.TEXTCOLOR,
					ColorLib.rgb(0, 0, 0));

			ColorAction aStroke = new ColorAction(AGGR, VisualItem.STROKECOLOR);
			aStroke.setDefaultColor(ColorLib.gray(200));
			aStroke.add("_hover", ColorLib.rgb(255, 100, 100));

			int[] palette = new int[] { ColorLib.rgba(255, 200, 200, 150),
					ColorLib.rgba(200, 255, 200, 150),
					ColorLib.rgba(200, 200, 255, 150) };
			ColorAction aFill = new DataColorAction(AGGR, "id",
					Constants.NOMINAL, VisualItem.FILLCOLOR, palette);

			// bundle the color actions
			ActionList colors = new ActionList();
			colors.add(nStroke);
			colors.add(nFill);
			colors.add(nEdges);
			colors.add(nEdgesText);
			colors.add(nText);
			colors.add(aStroke);
			colors.add(aFill);
			col_cache = colors;
			return colors;
		} else {
			return col_cache;
		}

	}

	public VisualGraph setGraph(Graph g) {

		// update graph
		m_vis.removeGroup(GRAPH);
		VisualGraph vg = m_vis.addGraph(GRAPH, g);
		m_vis.setValue(EDGES, null, VisualItem.INTERACTIVE, Boolean.FALSE);
		VisualItem f = (VisualItem) vg.getNode(0);
		m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
		f.setFixed(false);

		return vg;

	}

	Graph g;
	private Model m;

	@SuppressWarnings("unchecked")
	public void readProgramAndDataAsGraph(String filename) {
		g = new BlobTree();
		g.getNodeTable().addColumns(LABEL_SCHEMA);

		if ("hest".equals(filename)) {
			Node n = g.addNode();
			n.setString(LABEL, "Node1");

			Node n1 = g.addNode();
			g.addEdge(n, n1);
			n1.setString(LABEL, "Node1.1");

			Node n2 = g.addNode();
			g.addEdge(n, n2);
			n2.setString(LABEL, "Node1.2");

			Node n3 = g.addNode();
			g.addEdge(n1, n3);
			n3.setString(LABEL, "Node1.1.1");
		} else if ("ko".equals(filename)) {
			Node n = g.addNode();
			n.setString(LABEL, "KoNode1");

			Node n1 = g.addNode();
			g.addEdge(n, n1);
			n1.setString(LABEL, "KoNode1.1");

			Node n2 = g.addNode();
			g.addEdge(n, n2);
			g.addEdge(n2, n1);
			n2.setString(LABEL, "KoNode1.2");

			Node n3 = g.addNode();
			g.addEdge(n1, n3);

			n3.setString(LABEL, "KoNode1.1.1");
		} else if ("blob".equals(filename)) {
			g.addColumns(BlobFuse.BLOB_SCHEMA);
			Blob pb1 = new Blob(1);
			Blob pb2 = new Blob(2);
			Blob pb3 = new Blob(3);
			Blob pb4 = new Blob(4);
			Blob.link(pb1, pb2, BondSite.South, BondSite.East);
			Blob.link(pb2, pb3, BondSite.South, BondSite.East);
			Blob.link(pb2, pb4, BondSite.West, BondSite.East);

			Blob db1 = new Blob(10);
			Blob db2 = new Blob(20);
			Blob db22 = new Blob(202);
			Blob db3 = new Blob(30);
			Blob db4 = new Blob(40);

			Blob.link(db1, db2, BondSite.South, BondSite.East);
			Blob.link(db1, db22, BondSite.West, BondSite.East);

			Blob.link(db2, db3, BondSite.South, BondSite.East);
			Blob.link(db3, db4, BondSite.South, BondSite.East);
			Blob.link(db4, db22, BondSite.South, BondSite.West);
			// The bug
			Blob.link(pb1, db1, BondSite.North, BondSite.North);

			nodel = new HashMap<Blob, Node>();
			nodelf = new ArrayList<Blob>();
			dfsBlob(pb1);

		} else {
			g.addColumns(BlobFuse.BLOB_SCHEMA);
			m = new Model();
			m.readConfiguration(filename);
			nodel = new HashMap<Blob, Node>();
			nodelf = new ArrayList<Blob>();
			dfsBlob(m.APB());
			filename = "blob";
		}

		VisualGraph vg = setGraph(g);
		if ("blob".equals(filename)) {
			m_vis.removeGroup(AGGR);
			AggregateTable at = m_vis.addAggregates(AGGR);
			at.addColumn(VisualItem.POLYGON, float[].class);
			at.addColumn("id", int.class);

			AggregateItem pgr = (AggregateItem) at.addItem();
			pgr.setInt("id", 0);
			AggregateItem data = (AggregateItem) at.addItem();
			data.setInt("id", 1);

			for (Iterator<VisualItem> iterator = vg.nodes(); iterator.hasNext();) {
				VisualItem type = iterator.next();

				if (type.canGet(BlobFuse.BLOBINPGR, Boolean.class)) {
					boolean b = (Boolean) type.get(BlobFuse.BLOBINPGR);
					if (b) {
						pgr.addItem(type);
					} else {
						data.addItem(type);
					}
				} else {
					System.out.println("No can get: " + type);
				}
			}
		}

	}

	Map<Blob, Node> nodel;
	List<Blob> nodelf;

	boolean inpgr = true;

	private Node dfsBlob(Blob pb1) {
		Blob cur = pb1;
		if (!nodel.containsKey(cur)) {
			Node n = g.addNode();
			nodel.put(cur, n);

			n.set(BlobFuse.BLOBFIELD, cur);
			if (inpgr) {
				n.setString(LABEL, "" + cur.opCode());
			} else {
				n.setString(LABEL, "" + cur.getCargo());
			}
			n.set(BlobFuse.BLOBINPGR, inpgr);
			for (int i = 3; i >= 0; i--) {
				Blob bn = cur.follow(BondSite.create(i));
				if (bn != null) {
					Node nn = null;
					if (!nodel.containsKey(bn)) {
						if (i == 0 && inpgr) {
							inpgr = false;
						}
						nn = dfsBlob(bn);
					} else if (!nodelf.contains(bn)) {
						nn = nodel.get(bn);
					}
					if (nn != null) {
						g.addEdge(n, nn);
					}
				}
			}
			nodelf.add(cur);

		}
		return nodel.get(cur);
	}

	public static void main(String[] argv) {
		String filename = null;
		if (argv.length > 0) {
			filename = argv[0];
		} else {
			filename = "primAppend.cfg";
		}
		JFrame frame = demo(filename);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setVisible(true);
	}

	public static JFrame demo(String filename) {
		BlobVis ad = new BlobVis(filename);
		JFrame frame = new JFrame("dikuBlob - B l o b V i s");
		frame.setExtendedState(Frame.MAXIMIZED_HORIZ);
		frame.getContentPane().add(ad);
		frame.pack();
		return frame;
	}

	/** Label data field included in generated Graphs */
	public static final String LABEL = "label";
	/** Node table schema used for generated Graphs */
	public static final Schema LABEL_SCHEMA = new Schema();
	static {
		LABEL_SCHEMA.addColumn(LABEL, String.class, "");
	}
}
