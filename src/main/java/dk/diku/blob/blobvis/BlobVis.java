package dk.diku.blob.blobvis;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import model.Model;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.animate.VisibilityAnimator;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.data.Graph;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.display.DebugStatsPainter;
import prefuse.util.display.ExportDisplayAction;
import prefuse.util.display.PaintListener;
import prefuse.util.ui.JValueSlider;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.gui.DataBlobPopupMenu;
import dk.diku.blob.blobvis.gui.OpenGraphAction;
import dk.diku.blob.blobvis.prefuse.BFConstants;
import dk.diku.blob.blobvis.prefuse.BlobDragControl;
import dk.diku.blob.blobvis.prefuse.BlobEdgeRenderer;
import dk.diku.blob.blobvis.prefuse.BlobGraphModel;
import dk.diku.blob.blobvis.prefuse.StepResult;

@SuppressWarnings("serial")
public class BlobVis extends JPanel {
	private static Preferences prefs;

	// The file currently open. For restarting.
	private String currentFile = null;

	// Constants for prefuse.
	private static final String GRAPH = "graph";
	private static final String EDGES = "graph.edges";
	private static final String NODES = "graph.nodes";
	private static final String AGGR = "aggregates";

	// Prefuse layers
	private Visualization m_vis;
	private Display display;
	private Graph g;
	private VisualGraph vg;

	// defaults
	int hops = 15;

	// Shared filter.
	final GraphDistanceFilter filter;

	// Blob simulator model.
	private Model m;

	// Bonding between Prefuse graph and blob simulator model.
	private BlobGraphModel bgf;

	// Swing components
	private ProgressMonitor progressMonitor;
	private final JButton pause;
	final JButton step;
	final JButton restart;
	final JValueSlider slider;

	// State flags
	private boolean ended = true;
	boolean paused = false;

	private final class PopupListener extends ControlAdapter {
		@Override
		public void itemReleased(final VisualItem item, MouseEvent e) {
			if (e.isPopupTrigger()) {
				item.setFixed(true);
				tmpStopForce();
				boolean inpgr = (Boolean) item.get(BFConstants.BLOBINPGR);
				if (!inpgr) { // only show menu for data blobs
					DataBlobPopupMenu menu = new DataBlobPopupMenu(
							BlobVis.this, bgf, item);
					menu.addFocusListener(new FocusListener() {
						@Override
						public void focusLost(FocusEvent e) {
							tmpRestartForce();
						}

						@Override
						public void focusGained(FocusEvent e) {
							tmpStopForce();
						}
					});
					menu.show(e.getComponent(), e.getX(), e.getY());
					menu.requestFocus();
				}
			}
		}
	}

	public class PauseForceAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			m_vis.cancel("pausedactions");
			m_vis.cancel("basepaused");
			toggleForce();
		}
	}

	public class StepModelAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			tmpRunForce1s();


			bgf.registerOpcodeListener("EXT", new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					ended = true;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					step.setEnabled(false);
					tmpStopForce();
				}
			});
			bgf.registerStepListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					StepResult sr = (StepResult) e.getSource();
					VisualItem vnn = (VisualItem) vg.getNode(bgf.getNode(sr.apbnext).getRow());
					m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(vnn);
				}
			});



			synchronized (m_vis) {
				if (ended) {
					System.out.println("At EXT. Done");
					return;
				}
				bgf.step();
				return;
				/*Node r = bgf.getRoot();
				Blob apb = (Blob) r.get(BFConstants.BLOBFIELD);
				Blob adb = apb.follow(BondSite.North);
				StepResult result;


				if (apb.opCode().startsWith("JB")) {
					result = doJB(apb, adb);
				} else if (apb.opCode().startsWith("DBS")
						|| apb.opCode().startsWith("SCG")) {
					result = new StepResult(apb, adb).reread(true);
				} else if (apb.opCode().startsWith("CHD")) {
					result = doCHD(apb, adb);
				} else if (apb.opCode().startsWith("JCG")) {
					result = doJCG(apb, adb);
				} else if (apb.opCode().startsWith("SBS")) {
					result = doSBS(apb, adb);

				} else if (apb.opCode().startsWith("JN")) {
					result = doJN(apb, adb);
				} else if (apb.opCode().startsWith("SWL")) {
					result = doSWL(apb, adb);
				} else {
					result = new StepResult(apb, adb); // Default action
				}
				bgm.execute(result);

				/*				Blob next = apb.follow(apbBsNext);
				Node nn = bgf.getNode(next);
				bgf.updateTheBug(r, nn, adbn, adbnnext);
				stepModel(r, nn);
				if (reread) {
					bgf.rereadCargo(m.ADB());
				}

				if (next.opCode().equals("EXT")) {
					ended = true;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					step.setEnabled(false);
					tmpStopForce();
				}*/

			}
		}


		private void tmpRunForce1s() {
			if (paused) {
				m_vis.cancel("1sforce");
				m_vis.run("1sforce");
			}
		}



	}


	public BlobVis() {
		super(new BorderLayout());

		m_vis = new Visualization();
		filter = new GraphDistanceFilter(GRAPH, Visualization.FOCUS_ITEMS, hops);
		// init base components.
		setupRenderer();
		setupActions();
		setupDisplay();

		JPanel panel = new JPanel();
		step = new JButton("Step model");
		restart = new JButton("Restart model");
		pause = new JButton("Pause force movements");
		slider = new JValueSlider("Distance", 0, 35, hops);

		Box boxpanel = setupBoxpanel();
		panel.add(boxpanel);
		JSplitPane split = setupSplitPane(display, panel);
		add(split);
		ended = true;
		disableButtons();
		restart.setEnabled(false);
	}

	private void readData(String filename) {
		if (filename == null || "".equals(filename)) {
			filename = OpenGraphAction.getBlobConfigFilename(this, prefs);
		}
		if (filename != null) {
			readProgramAndDataAsGraph(filename);
		}
	}

	private Box setupBoxpanel() {
		Box buttons = setupStepButton();
		Box forcebuttons = setupForceControlButton();
		Box cf = setupSlider();

		Box boxpanel = new Box(BoxLayout.Y_AXIS);
		boxpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		boxpanel.add(buttons);
		boxpanel.add(Box.createVerticalStrut(20));
		boxpanel.add(forcebuttons);
		boxpanel.add(Box.createVerticalStrut(20));
		boxpanel.add(cf);
		return boxpanel;
	}

	private static JSplitPane setupSplitPane(Component left, Component right) {
		JSplitPane split = new JSplitPane();
		split.setLeftComponent(left);
		split.setRightComponent(right);
		split.setOneTouchExpandable(true);
		split.setContinuousLayout(false);
		split.setDividerLocation(-1);
		return split;
	}

	private Box setupSlider() {
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				filter.setDistance(slider.getValue().intValue());
				m_vis.run("basepaused");
			}
		});

		Box cf = new Box(BoxLayout.Y_AXIS);
		cf.setAlignmentX(Component.LEFT_ALIGNMENT);
		cf.add(slider);
		cf.setBorder(BorderFactory.createTitledBorder("Connectivity Filter"));
		return cf;
	}

	private Box setupForceControlButton() {
		Box forcebuttons = new Box(BoxLayout.Y_AXIS);
		forcebuttons.setAlignmentX(Component.LEFT_ALIGNMENT);
		pause.addActionListener(new PauseForceAction());
		pause.setEnabled(true);
		forcebuttons.add(pause);
		forcebuttons.setBorder(BorderFactory
				.createTitledBorder("Force simulation ctrl"));
		forcebuttons.setMaximumSize(new Dimension(Short.MAX_VALUE,
				Short.MAX_VALUE));
		return forcebuttons;
	}

	private Box setupStepButton() {
		Box buttons = new Box(BoxLayout.X_AXIS);
		buttons.setAlignmentX(LEFT_ALIGNMENT);
		step.addActionListener(new StepModelAction());
		step.setEnabled(true);
		buttons.setBorder(BorderFactory.createTitledBorder("Blob Model"));
		buttons.add(step);
		restart.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tmpStopForce();
				readProgramAndDataAsGraph(currentFile);

			}
		});
		restart.setEnabled(true);
		buttons.add(restart);
		buttons.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		return buttons;
	}

	private void setupDisplay() {
		// set up the display
		display = new Display(m_vis);
		display.setSize(800, 800);
		display.pan(150, 150);
		display.setHighQuality(true);
		// display.setItemSorter(new TreeDepthItemSorter());

		display.addControlListener(new BlobDragControl());
		display.addControlListener(new PanControl());
		display.addControlListener(new WheelZoomControl());
		display.addControlListener(new PopupListener());
	}

	private void setupRenderer() {
		LabelRenderer tr = new LabelRenderer();
		tr.setRoundedCorner(8, 8);
		DefaultRendererFactory dfr = new DefaultRendererFactory(tr,
				new BlobEdgeRenderer());
		m_vis.setRendererFactory(dfr);
	}

	private void setupActions() {
		// now create the main layout routine
		ActionList init = new ActionList();

		init.add(filter);
		init.add(getColorSetup());
		init.add(new RadialTreeLayout(GRAPH));
		// init.add(new RandomLayout());
		init.add(new RepaintAction());

		ActionList singleforce = new ActionList(1000);
		singleforce.add(filter);
		singleforce.add(getColorSetup());
		singleforce.add(new ForceDirectedLayout(GRAPH));
		singleforce.add(new RepaintAction());
		m_vis.putAction("1sforce", singleforce);

		ActionList basePaused = new ActionList(1000);
		basePaused.add(filter);
		basePaused.add(getColorSetup()); // base.add(new AggregateLayout(AGGR));
		basePaused.add(new RepaintAction());
		m_vis.putAction("basepaused", basePaused);

		ActionList pausedActions = new ActionList(500);
		pausedActions.add(new VisibilityAnimator());

		m_vis.putAction("pausedactions", pausedActions);
		m_vis.alwaysRunAfter("pausedactions", "basepaused");

		ActionList force = new ActionList(Action.INFINITY, 32);
		force.add(filter);
		force.add(getColorSetup());
		force.add(new ForceDirectedLayout(GRAPH));
		// force.add(AggrForce());
		force.add(new RepaintAction());
		m_vis.putAction("force", force);
		m_vis.putAction("init", init);
	}

	protected void runEverything() {
		if (g != null) {
			m_vis.run("init");

			tmpRestartForce();
			if (!paused) {
				m_vis.cancel("pausedactions");
			}
		}
	}

	private void stopForce() {
		m_vis.cancel("force");
		pause.setText("Start force simulation");
		paused = true;
	}

	private void startForce() {
		m_vis.run("force");
		pause.setText("Pause force simulation");
		paused = false;
	}

	/**
	 * Stop force simulation, temporarily. Keep current state
	 */
	private void tmpStopForce() {
		m_vis.cancel("force");
	}

	/**
	 * Start force simulation if needed;
	 */
	private void tmpRestartForce() {
		if (!paused) {
			m_vis.run("force");
		} else {
			m_vis.cancel("force");
		}
	}

	/**
	 * Toggle the force simulation
	 * 
	 * @return true if new state is "running"
	 */
	private boolean toggleForce() {
		if (paused) {
			startForce();
			return true;
		} else {
			stopForce();
			return false;
		}
	}

	private ActionList getColorSetup() {

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
		ColorAction nEdgesText = new ColorAction(EDGES, VisualItem.TEXTCOLOR,
				ColorLib.gray(0));

		ColorAction nText = new ColorAction(NODES, VisualItem.TEXTCOLOR,
				ColorLib.rgb(0, 0, 0));

		ColorAction aStroke = new ColorAction(AGGR, VisualItem.STROKECOLOR);
		aStroke.setDefaultColor(ColorLib.gray(200));
		aStroke.add("_hover", ColorLib.rgb(255, 100, 100));

		ActionList colors = new ActionList();

		int[] palette = new int[] { ColorLib.rgba(255, 200, 200, 210), // adb
				// 0
				ColorLib.rgba(100, 255, 000, 210), // apb 1
				ColorLib.rgba(255, 110, 110, 210), // data 2
				ColorLib.rgba(200, 255, 200, 210), // inpgr 3
		};
		ColorAction nFillAdb = new DataColorAction(NODES, BFConstants.BLOBTYPE,
				Constants.NOMINAL, VisualItem.FILLCOLOR, palette);

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
		return colors;
	}

	public VisualGraph setGraph(Graph g) {
		// update graph
		m_vis.removeGroup(GRAPH);
		VisualGraph vg = m_vis.addGraph(GRAPH, g);
		bgf.setVisualGraph(vg);
		m_vis.setValue(EDGES, null, VisualItem.INTERACTIVE, Boolean.FALSE);
		VisualItem f = (VisualItem) vg.getNode(0);
		// .getChild(1);
		m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
		f.setFixed(false);
		return vg;
	}

	/**
	 * Start loading the data from the filename in a background task When done
	 * the progress monitor will close and buttons will be activated.
	 * 
	 * @param filename
	 *            Blob configuration filename
	 */
	public void readProgramAndDataAsGraph(final String filename) {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		disableButtons();
		ended = true;
		Component c = this;
		while (c != null && !(c instanceof JFrame)) {
			c = c.getParent();
		}
		progressMonitor = new ProgressMonitor(c,
				"Loading Blob configuration from " + filename + "...", "", 0,
				100);
		progressMonitor.setProgress(0);

		// Create empy models and graphs.
		g = new Graph();
		m = new Model();
		final ReadBlobConfigTask t = new ReadBlobConfigTask(g, m, filename);

		t.addPropertyChangeListener(new PropertyChangeListener() {
			boolean canceled = false;

			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress" == evt.getPropertyName()) {
					int progress = (Integer) evt.getNewValue();
					progressMonitor.setProgress(progress);
					String message = String.format("Completed %d%%.\n",
							progress);
					progressMonitor.setNote(message);
				}
				if (progressMonitor.isCanceled() || t.isDone()) {
					if (progressMonitor.isCanceled()) {
						System.out.println("Canceled. Exiting");
						// System.exit(1);
						t.cancel(true);
						progressMonitor.close();
						canceled = true;
					} else if (canceled == false && ended != false) {
						ended = false;
						bgf = t.bgf;
						vg = setGraph(g);
						runEverything();
						currentFile = filename;
						restart.setEnabled(true);
						enableButtons();
					}
					setCursor(null); // turn off the wait cursor
				}
			}
		});
		m_vis.removeGroup(GRAPH);
		t.execute();

	}

	private void disableButtons() {
		System.out.println("Disable buttons");
		pause.setEnabled(false);
		step.setEnabled(false);
		slider.setEnabled(false);
	}

	protected void enableButtons() {
		System.out.println("Enable buttons");
		pause.setEnabled(true);
		step.setEnabled(true);
		slider.setEnabled(true);
	}

	public static void main(String[] argv) {

		// provide a final pointer to the filename while still being able to
		// change the value dynamically.
		final String[] filename = new String[1];

		// Load preferences.
		prefs = Preferences.userNodeForPackage(BlobVis.class);
		if (argv.length > 0) {
			filename[0] = argv[0];
		}

		// filename is a final pointer to an array with one value.
		// When including in nested classes it must be final.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				start(filename[0]);
			}
		});

	}

	public static JFrame start(String filename) {
		UILib.setPlatformLookAndFeel();
		JFrame frame = new JFrame("dikuBlob - B l o b V i s");
		final BlobVis ad = new BlobVis();
		JMenuBar menubar = new JMenuBar();
		menubar.add(setupFileMenu(ad));
		menubar.add(setupDataMenu());
		frame.setJMenuBar(menubar);

		frame.setExtendedState(Frame.MAXIMIZED_HORIZ);

		frame.getContentPane().add(ad);

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		ad.readData(filename);
		return frame;
	}

	private static JMenu setupDataMenu() {
		JMenu dataMenu = new JMenu("Data");
		dataMenu.add("Save current data to .bld file...");
		dataMenu.add("Replace current data from file...");
		return dataMenu;
	}

	private static JMenu setupFileMenu(final BlobVis ad) {
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new OpenGraphAction(ad, prefs));

		ExportDisplayAction eda = new ExportDisplayAction(ad.m_vis
				.getDisplay(0));
		eda.putValue(AbstractAction.NAME, "Export Screenshot...");
		eda.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke
				.getKeyStroke("ctrl E"));
		eda.putValue(AbstractAction.MNEMONIC_KEY, new Integer('E'));
		fileMenu.add(eda);

		// a group of check box menu items
		fileMenu.addSeparator();
		JCheckBoxMenuItem cbMenuItem;
		cbMenuItem = new JCheckBoxMenuItem("Display Debug Information");
		cbMenuItem.setMnemonic('d');
		cbMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
		cbMenuItem.addActionListener(new ActionListener() {
			private PaintListener m_debug = null;

			public void actionPerformed(ActionEvent e) {
				if (m_debug == null) {
					m_debug = new DebugStatsPainter();
					ad.m_vis.getDisplay(0).addPaintListener(m_debug);
				} else {
					ad.m_vis.getDisplay(0).removePaintListener(m_debug);
					m_debug = null;
				}
				ad.m_vis.getDisplay(0).repaint();
			}
		});
		fileMenu.add(cbMenuItem);
		fileMenu.addSeparator();
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.setMnemonic('x');
		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		fileMenu.add(exitItem);
		fileMenu.setMnemonic('F');
		return fileMenu;
	}
}
