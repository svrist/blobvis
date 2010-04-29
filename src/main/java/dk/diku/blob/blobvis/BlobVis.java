package dk.diku.blob.blobvis;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

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
import prefuse.action.assignment.FontAction;
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
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.force.ForceSimulator;
import prefuse.util.ui.JForcePanel;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.TreeDepthItemSorter;

@SuppressWarnings("serial")
public class BlobVis extends JPanel {
	public class SingleForceAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			m_vis.cancel("force");
			m_vis.cancel("singleforce");

			System.out.println("Single force run");
			m_vis.run("singleforce");

		}

	}

	boolean paused = false;

	public class PauseForceAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			m_vis.cancel("force");
			m_vis.cancel("base");
			if (paused) {
				System.out.println("Restarting after pause");
				m_vis.run("force");
				paused = false;
			} else {
				System.out.println("Pausing");
				paused = true;
			}
			m_vis.run("base");

		}

	}

	public class StepModelAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent arg0) {
			hops += 1;
			filter.setDistance(hops);
			m_vis.run("init");
			System.out.println("Hops: " + hops);

			/*
			 * getVisualization().cancel("force");
			 * getVisualization().run("force");
			 */

		}

	}

	public class SwitchAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			Node r = g.getNode(0);

			System.out.println(r.get(BlobFuse.BLOBFIELD));

			m_vis.cancel("init");
			m_vis.run("init");
		}

	}

	
	Node selected = null;
	private static final String GRAPH = "graph";
	private static final String EDGES = "graph.edges";
	private static final String NODES = "graph.nodes";
	private static final String AGGR = "aggregates";
	Layout lay;
	private boolean tree = false;
	int hops = 10;
	final GraphDistanceFilter filter;
	private Visualization m_vis;

	public BlobVis(String filename) {
		super(new BorderLayout());
		m_vis = new Visualization();

		LabelRenderer tr = new LabelRenderer();
		tr.setRoundedCorner(8, 8);

		/*
		 * Renderer polyR = new
		 * AggregatePolygonRenderer(Constants.POLY_TYPE_CURVE);
		 * ((PolygonRenderer) polyR).setCurveSlack(0.15f);
		 */
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
		base.add(genColors()); // base.add(new AggregateLayout(AGGR));

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
		m_vis.putAction("base", base);
		// set up the display
		Display display = new Display(m_vis);
		display.setSize(800, 800);
		display.pan(150, 150);
		display.setHighQuality(true);
		display.setItemSorter(new TreeDepthItemSorter());

		display.addControlListener(new BlobDragControl());
		display.addControlListener(new FocusControl(1));
		display.addControlListener(new PanControl());
		display.addControlListener(new ZoomControl());
		display.addControlListener(new WheelZoomControl());
		display.addControlListener(new ZoomToFitControl());
		// create a panel for editing force values
        ForceSimulator fsim = ((ForceDirectedLayout)force.get(1)).getForceSimulator();
        JForcePanel fpanel = new JForcePanel(fsim);
		
		 JSplitPane split = new JSplitPane();
	        split.setLeftComponent(display);
	        split.setRightComponent(fpanel);
	        split.setOneTouchExpandable(true);
	        split.setContinuousLayout(false);
	        split.setDividerLocation(-1);
	        
	        // now we run our action list
	        m_vis.run("draw");
	        
	     add(split);
		

		// set things running
		m_vis.run("init");
		m_vis.run("base");

		if (!paused) {
			m_vis.run("force");
		}

		/*
		registerKeyboardAction(new SwitchAction(), "switchit", KeyStroke
				.getKeyStroke("ctrl 1"), WHEN_FOCUSED);

		registerKeyboardAction(new StepModelAction(), "switchit5", KeyStroke
				.getKeyStroke("ctrl 5"), WHEN_FOCUSED);

		registerKeyboardAction(new PauseForceAction(), "pauseforce", KeyStroke
				.getKeyStroke("P"), WHEN_FOCUSED);

		registerKeyboardAction(new SingleForceAction(), "singleforce",
				KeyStroke.getKeyStroke("S"), WHEN_FOCUSED);
		*/

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

			ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR,
					ColorLib.gray(255));

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

			ActionList colors = new ActionList();
			
			if (use_aggregate) {
				int[] palette = new int[] { ColorLib.rgba(255, 200, 200, 150),
						ColorLib.rgba(200, 255, 200, 150),
						ColorLib.rgba(200, 200, 255, 150) };
				ColorAction aFill = new DataColorAction(AGGR, "id",
						Constants.NOMINAL, VisualItem.FILLCOLOR, palette);
				colors.add(aFill);
			}

			// bundle the color actions
			
			colors.add(nStroke);
			colors.add(nFill);
			colors.add(nEdges);
			colors.add(nEdgesText);
			colors.add(new FontAction(EDGES, FontLib.getFont("SansSerif",
					Font.PLAIN, 8)));
			colors.add(nText);
			colors.add(aStroke);

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
		VisualItem f = (VisualItem) vg.getNode(0);// .getChild(1);
		m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
		f.setFixed(false);

		return vg;

	}

	Graph g;
	private Model m;
	private boolean use_aggregate = false;

	public void readProgramAndDataAsGraph(String filename) {
		g = new Graph();
		g.getNodeTable().addColumns(LABEL_SCHEMA);
		g.addColumns(BlobFuse.BLOB_SCHEMA);
		m = new Model();
		m.readConfiguration(filename);
		nodel = new HashMap<Blob, Node>();
		nodelf = new ArrayList<Blob>();
		dfsBlob(m.APB());
		VisualGraph vg = setGraph(g);

		if (use_aggregate) {
			fillAggregates(vg);
		}
	}

	@SuppressWarnings("unchecked")
	private void fillAggregates(VisualGraph vg) {
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

	private String genGraph(String type) {
		if ("hest".equals(type)) {
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
		} else if ("ko".equals(type)) {
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
		} else if ("blob".equals(type)) {
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

		}
		return type;
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
						Edge e = g.addEdge(n, nn);
						e.set(BlobFuse.EDGENUMBERSRC, i);
						BondSite otherend = bn.boundTo(cur);
						if (otherend != null)
							e.set(BlobFuse.EDGENUMBERTAR, otherend.ordinal());
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
