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

			Node r = g.getSpanningTree().getRoot();
			Blob apb = (Blob) r.get(BlobFuse.BLOBFIELD);
			Blob adb = apb.follow(BondSite.North);
			BondSite apbBsNext = BondSite.South;
			System.out.println("Adb: " + adb + " b:" + apbBsNext + " apb:"
					+ apb + " m(" + m.APB() + "," + m.ADB() + ")");

			Node adbn = findNode(r, adb);
			Node adbnnext = findNode(r, adb);

			r.set(BlobFuse.BLOBTYPE, 3);
			if (apb.opCode().startsWith("JB")) {
				BondSite b = BondSite.create((2 + 1) & apb.getCargo());
				if (m.ADB().follow(b) != null) {
					apbBsNext = BondSite.South;
					System.out.println("Going south");
				} else {
					System.out.println("Nothing on " + m.ADB() + "." + b
							+ " going west(" + m.ADB().follow(BondSite.East)
							+ m.ADB().follow(BondSite.South)
							+ m.ADB().follow(BondSite.West) + ") - " + adb);
					apbBsNext = BondSite.West;
				}
			} else if (apb.opCode().startsWith("CHD")) {
				BondSite b = BondSite.create((2 + 1) & apb.getCargo());
				adb = m.ADB().follow(b);
				// g.removeEdge(g.getEdge)
				adbnnext = findNode(adbn, adb);
			} else if (apb.opCode().startsWith("SBS")) {
				BondSite b1 = BondSite
						.create(((8 + 4) & m.APB().getCargo()) / 4);
				BondSite b2 = BondSite.create((2 + 1) & m.APB().getCargo());
				Blob bb1 = adb.follow(b1);
				Blob bb2 = adb.follow(b2);
				if (bb1 != null) {
					setSrcBondSite(adbn, bb1, b2);
				}
				if (bb2 != null) {
					setSrcBondSite(adbn, bb2, b1);
				}

			} else if (apb.opCode().startsWith("JN")) {
				BondSite b1 = BondSite
						.create(((8 + 4) & m.APB().getCargo()) / 4);
				BondSite b2 = BondSite.create((2 + 1) & m.APB().getCargo());

				Blob dest1 = adb.follow(b1);
				Node dest1n = findNode(adbn, dest1);
				Blob dest = dest1.follow(b2);
				Node destn = findNode(dest1n, dest);
				if (dest != null) {
					BondSite ds = dest.boundTo(dest1);
					linkNodes(adbn, b1, destn, ds);
				} else {
					Node n = findNode(adbn, dest1);
					Edge ne = g.getEdge(adbn, n);
					if (ne != null) {
						g.removeEdge(ne);
						ne = g.getEdge(n, adbn);
						if (ne != null) {
							g.removeEdge(ne);
						}
					}
				}
			} else if (apb.opCode().startsWith("SWL")) {
				BondSite b1 = BondSite
						.create(((8 + 4) & m.APB().getCargo()) / 4);
				BondSite b2 = BondSite.create((2 + 1) & m.APB().getCargo());

				Blob adb_b1 = m.ADB().follow(b1);
				Node adb_b1n = null;
				BondSite ts2 = null;
				if (adb_b1 != null) {
					ts2 = adb_b1.boundTo(m.ADB());
					adb_b1n = findNode(adbn, adb_b1);
				}
				Blob adb_b2 = m.ADB().follow(b2);
				Blob adb_b2_b1 = null;
				if (adb_b2 != null) {
					adb_b2_b1 = adb_b2.follow(b1);
					Node adb_b2n = findNode(adbn, adb_b2);
					if (adb_b2_b1 != null) {
						BondSite ts1 = adb_b2_b1.boundTo(adb_b2);
						// TODO: Blob.link( ADB, adb_b2_b1, b1, ts1 );
						Node adb_b2_b1n = findNode(adb_b2n, adb_b2_b1);
						linkNodes(adbn, b1, adb_b2_b1n, ts1);

						if (adb_b1 != null) {
							// TODO: Blob.link( adb_b2, adb_b1, b1, ts2 );
							linkNodes(adb_b2n, b1, adb_b1n, ts2);
							// case 4: something(x) on b1 and something(y) on
							// b2.b1 -> b1=y, b2.b1=x
						} else {
							System.out.println("Case 2");
							// Case 2: Nothing on b1 and something(x) on b2.b1
							// -> b2.b1=null,b1=x
							// TODO: adb_b2.unlink( b1 );
							removeEdge(adb_b2n, adb_b2_b1n);
						}
					} else if (adb_b1 != null) {
						// case 3: Something(x) on b1 and nothing on b2.b1 ->
						// b2.b1=x,b1=nothing
						// TODO:Blob.link( adb_b2,adb_b1 , b1, ts2 );
						linkNodes(adb_b2n, b1, adb_b1n, ts2);
						// TODO: ADB.unlink( b1 );
						removeEdge(adbn, adb_b1n);
					} else {
						System.out.println("Case 6");
						// case 6: Nothing on b1 and nothing on b2.b1 -> nothing
						// happens
					}
				} else if (adb_b1 != null) {
					// case 5: something(x) on b1 and nothing on b2 -> b1=null,
					// x disappers
					// TODO: Blob.unlink( ADB, adb_b1, b1, ts2 );
					removeEdge(adbn, adb_b1n);
				} else {
					// Case 1: Nothing on b1 and nothing on b2 -> nothing
					// happens
				}
			} else {
				apbBsNext = BondSite.South;
			}

			Blob next = apb.follow(apbBsNext);
			System.out.println(next.opCode());
			System.out.println(r);
			Node nn = findNode(r, next);
			updateTheBug(r, nn, adbn, adbnnext);
			stepModel(r, nn);

		}

		private void linkNodes(Node n1, BondSite b1, Node n2, BondSite b2) {
			// loop over adb/dest edges.
			// Remove all with src/tar of b1/ds
			// link adb with dest. Set src=b1, tar=ds
			System.out.println("Adb: " + n1);
			List<Edge> rems = gatherRemoveList(b1, n1);
			System.out.println("dstn: " + n2);
			rems.addAll(gatherRemoveList(b2, n2));
			for (Edge element : rems) {
				Edge edge = element;
				g.removeEdge(edge);
			}
			Edge ne = g.addEdge(n1, n2);
			ne.set(BlobFuse.EDGENUMBERSRC, b1.ordinal());
			ne.set(BlobFuse.EDGENUMBERTAR, b2.ordinal());
			System.out.println("Added ne: " + ne);
		}

		@SuppressWarnings("unchecked")
		private List<Edge> gatherRemoveList(BondSite needle, Node n) {
			List<Edge> rems = new ArrayList<Edge>();
			Iterator removeIterator = n.edges();
			while (removeIterator.hasNext()) {
				Edge ce = (Edge) removeIterator.next();
				String field = ce.getSourceNode() == n ? BlobFuse.EDGENUMBERSRC
						: BlobFuse.EDGENUMBERTAR;
				String field2 = ce.getSourceNode() != n ? BlobFuse.EDGENUMBERSRC
						: BlobFuse.EDGENUMBERTAR;
				int bsi = (Integer) ce.get(field);
				int tar = (Integer) ce.get(field2);
				System.out.println("edge: " + bsi + "->" + tar + " == "
						+ needle + "." + needle.ordinal());
				if (bsi == needle.ordinal()) {
					rems.add(ce);
				}
			}
			return rems;
		}

		private void setSrcBondSite(Node srcn, Blob target, BondSite newvalue) {
			Node bbn1 = findNode(srcn, target);
			Edge e1 = g.getEdge(srcn, bbn1);
			if (e1 != null) {
				e1.set(BlobFuse.EDGENUMBERSRC, newvalue.ordinal());
			} else {
				e1 = g.getEdge(bbn1, srcn);
				e1.set(BlobFuse.EDGENUMBERTAR, newvalue.ordinal());
			}

		}

		private void updateTheBug(Node r, Node nn, Node adbncur, Node adbnnext) {

			Edge thebug = g.addEdge(nn, adbnnext);
			removeEdge(r, adbncur);
			adbncur.set(BlobFuse.BLOBTYPE, 4);
			adbnnext.set(BlobFuse.BLOBTYPE, 2);
			System.out.println(nn + " -> " + adbnnext);

			thebug.set(BlobFuse.EDGENUMBERSRC, 0);
			thebug.set(BlobFuse.EDGENUMBERTAR, 0);
		}

		private void removeEdge(Node n1, Node n2) {
			System.out.println("n1: " + n1 + " n2:" + n2);
			Edge e1 = g.getEdge(n1, n2);
			Edge e2 = g.getEdge(n2, n1);
			if (e1 != null) {
				System.out.println("Removing " + e1 + " " + e1.getSourceNode()
						+ "->" + e1.getTargetNode());
				g.removeEdge(e1);
			}
			if (e2 != null) {
				System.out.println("Removing " + e2 + " " + e2.getSourceNode()
						+ "->" + e2.getTargetNode());
				g.removeEdge(e2);
			}
		}

		private void stepModel(Node r, Node nn) {

			if (nn != null) {
				VisualItem vnn = (VisualItem) vg.getNode(nn.getRow());
				nn.set(BlobFuse.BLOBTYPE, 1);
				// vg.getSpanningTree((Node)vnn);
				g.getSpanningTree(nn);
				m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(vnn);
				System.out.println(m_vis.getGroup(Visualization.FOCUS_ITEMS)
						.getTupleCount());

				m.step();
			} else {
				throw new RuntimeException("Failed to find the child successor");
			}
		}

		private Node findNode(Node r, Blob next) {
			Node nn = null;

			Iterator ed = r.edges();
			while (ed.hasNext()) {
				Edge e = (Edge) ed.next();
				if (e.getSourceNode() == r) {
					nn = e.getTargetNode();
				} else {
					nn = e.getSourceNode();
				}
				Blob tmp = (Blob) nn.get(BlobFuse.BLOBFIELD);
				if (tmp == next)
					break;
				else
					nn = null;
			}
			if (nn == null) {
				StringBuffer sb = new StringBuffer();
				ed = r.edges();
				while (ed.hasNext()) {
					Edge e = (Edge) ed.next();
					if (e.getSourceNode() == r) {
						nn = e.getTargetNode();
					} else {
						nn = e.getSourceNode();
					}
					sb.append(nn);
					sb.append(",");
				}
				Blob rb = (Blob) r.get(BlobFuse.BLOBFIELD);
				throw new RuntimeException("Failed to find " + next + "("
						+ next.opCode() + ") as a child from " + r + "(" + rb
						+ "," + rb.opCode() + ")\nChildCount:"
						+ r.getChildCount() + ": " + sb.toString());
			}
			return nn;
		}

	}

	Node selected = null;
	private static final String GRAPH = "graph";
	private static final String EDGES = "graph.edges";
	private static final String NODES = "graph.nodes";
	private static final String AGGR = "aggregates";
	Layout lay;
	private boolean tree = false;
	int hops = 5;
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
		base.add(filter);
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
		ForceSimulator fsim = ((ForceDirectedLayout) force.get(1))
				.getForceSimulator();
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

		display.registerKeyboardAction(new SwitchAction(), "switchit",
				KeyStroke.getKeyStroke("ctrl 1"), WHEN_FOCUSED);
		/*
		 * registerKeyboardAction(new StepModelAction(), "switchit5", KeyStroke
		 * .getKeyStroke("ctrl 5"), WHEN_FOCUSED);
		 * 
		 * registerKeyboardAction(new PauseForceAction(), "pauseforce",
		 * KeyStroke .getKeyStroke("P"), WHEN_FOCUSED);
		 * 
		 * registerKeyboardAction(new SingleForceAction(), "singleforce",
		 * KeyStroke.getKeyStroke("S"), WHEN_FOCUSED);
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

			int[] palette = new int[] { ColorLib.rgba(255, 100, 100, 200), // apb
					ColorLib.rgba(100, 255, 100, 200), // adb
					ColorLib.rgba(255, 200, 200, 170), // inpgr
					ColorLib.rgba(200, 255, 200, 170), // data
			};
			ColorAction nFillAdb = new DataColorAction(NODES,
					BlobFuse.BLOBTYPE, Constants.NOMINAL, VisualItem.FILLCOLOR,
					palette);

			// bundle the color actions

			colors.add(nStroke);
			colors.add(nFill);
			colors.add(nFillAdb);
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
		VisualItem f = (VisualItem) vg.getNode(0);
		// .getChild(1);
		m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
		f.setFixed(false);

		return vg;

	}

	Graph g;
	VisualGraph vg;
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

		vg = m_vis.addGraph(GRAPH, g);
		dfsBlob(m.APB(), m);
		vg = setGraph(g);

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
			dfsBlob(pb1, null);

		}
		return type;
	}

	Map<Blob, Node> nodel;
	List<Blob> nodelf;

	boolean inpgr = true;

	private Node dfsBlob(Blob pb1, Model m) {
		Blob cur = pb1;

		if (!nodel.containsKey(cur)) {
			Node n = g.addNode();
			nodel.put(cur, n);

			if (m.APB().equals(cur)) {
				System.out.println("Found apb");
				n.set(BlobFuse.BLOBTYPE, 1);
			} else if (m.ADB().equals(cur)) {
				n.set(BlobFuse.BLOBTYPE, 2);
			} else if (inpgr) {
				n.set(BlobFuse.BLOBTYPE, 3);
			} else {
				n.set(BlobFuse.BLOBTYPE, 4);
			}

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
						nn = dfsBlob(bn, m);
					} else if (!nodelf.contains(bn)) {
						nn = nodel.get(bn);
					}
					if (nn != null) {
						if (g.getEdge(nn, n) == null) {
							Edge e = g.addEdge(n, nn);
							e.set(BlobFuse.EDGENUMBERSRC, i);
							BondSite otherend = bn.boundTo(cur);
							if (otherend != null) {
								e.set(BlobFuse.EDGENUMBERTAR, otherend
										.ordinal());

							}
						}
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
