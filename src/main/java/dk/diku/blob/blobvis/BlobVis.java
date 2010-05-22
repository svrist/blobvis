package dk.diku.blob.blobvis;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
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

import net.miginfocom.swing.MigLayout;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.DataColorAction;
import prefuse.action.assignment.FontAction;
import prefuse.action.assignment.StrokeAction;
import prefuse.action.filter.GraphDistanceFilter;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.controls.AbstractZoomControl;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.data.Graph;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DebugStatsPainter;
import prefuse.util.display.DisplayLib;
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
	private static final String ACTION_BASEPAUSED = "basepaused";

	private static final String ACTION_PLAY = "play";

	public static final String ACTION_1SFORCE = "1sforce";

	public static final String ACTION_FORCE = "force";

	private static Preferences prefs;

	// The file currently open. For restarting.
	private String currentFile = null;

	// Constants for prefuse.
	private static final String GRAPH = "graph";
	private static final String EDGES = "graph.edges";
	private static final String NODES = "graph.nodes";

	// Prefuse layers
	private Visualization m_vis;
	private Display display;
	// private Graph g;
	private VisualGraph vg;

	// defaults
	private int hops = 5;

	// Shared filter.
	private final GraphDistanceFilter filter;

	// Bonding between Prefuse graph and blob simulator model.
	private BlobGraphModel bgf;

	// Swing components
	private ProgressMonitor progressMonitor;
	private final JButton pause;
	private final JButton step;
	private final JButton play;
	private final JButton restart;
	private final JValueSlider slider;

	// State flags
	private boolean ended = true;
	private boolean paused = false;

	private boolean grayScale;

	private ActionList colorsetup;

	private StepModelAction stepModelAction;

	private final class ProgressListener implements
	PropertyChangeListener {
		private final ReadBlobConfigTask t;
		private final String filename;
		private boolean canceled = false;

		private ProgressListener(ReadBlobConfigTask t, String filename) {
			this.t = t;
			this.filename = filename;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress".equals(evt.getPropertyName())) {
				int progress = (Integer) evt.getNewValue();
				progressMonitor.setProgress(progress);
				String message = String.format("Completed %d%%.\n",
						progress);
				progressMonitor.setNote(message);
			}
			if (progressMonitor.isCanceled() || t.isDone()) {
				if (progressMonitor.isCanceled()) {
					t.cancel(true);
					progressMonitor.close();
					canceled = true;
				} else if (!canceled && ended ) {
					ended = false;
					setGraph(bgf.getGraph());
					runEverything();
					currentFile = filename;
					restart.setEnabled(true);
					enableButtons();
				}
				setCursor(null); // turn off the wait cursor
			}
		}
	}

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
			m_vis.cancel(ACTION_BASEPAUSED);
			toggleForce();
		}
	}

	public class StepModelAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// Make sure that all steps are finished before a new one is
			// executed.

			synchronized (m_vis) {
				// ensure that a force simulation is running during this
				// operation.
				tmpRunForce1s();
				if (ended) { // Dont do anything at ext nodes.
					return;
				}
				bgf.step();
			}
		}

	}

	public boolean isForcePaused() {
		return !m_vis.getAction(ACTION_FORCE).isRunning();
	}

	private void tmpRunForce1s() {
		if (isForcePaused()) {
			m_vis.cancel(ACTION_1SFORCE);
			m_vis.run(ACTION_1SFORCE);
		}
	}

	public BlobVis() {
		super(new BorderLayout());
		bgf = new BlobGraphModel();

		m_vis = new Visualization();
		filter = new GraphDistanceFilter(GRAPH, Visualization.FOCUS_ITEMS, hops);
		// init base components.
		setupRenderer();
		setupActions();
		setupDisplay();
		setupModelStepListeners();

		JPanel panel = new JPanel();
		step = new JButton("Step program");
		restart = new JButton("Restart program");
		play = new JButton("Play program");
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

	private void setupModelStepListeners() {
		bgf.registerOpcodeListener("EXT", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ended = true;
				step.setEnabled(false);
				tmpStopForce();

				m_vis.run(ACTION_1SFORCE);
			}
		});
		bgf.registerStepListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StepResult sr = (StepResult) e.getSource();
				VisualItem vnn = (VisualItem) vg.getNode(bgf
						.getNode(sr.apbnext).getRow());
				m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(vnn);
			}
		});
	}

	private void readData(String filename) {
		if (filename == null || "".equals(filename)) {
			filename = OpenGraphAction.getBlobConfigFilename(this, prefs);
		}
		if (filename != null) {
			readProgramAndDataAsGraph(filename);
		}
	}

	class ZoomActionListener extends AbstractZoomControl implements
	ActionListener {
		private float direction = 1;

		ZoomActionListener(float direction) {
			this.direction = direction;
		}

		private Point point = new Point();

		@Override
		public void actionPerformed(ActionEvent e) {
			// Display display = m_vis.getDisplay(0);
			point.x = display.getWidth() / 2;
			point.y = display.getHeight() / 2;
			zoom(display, point, 1 + 0.1f * direction, false);
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
		// boxpanel.add(Box.createVerticalStrut(100));
		boxpanel.add(Box.createVerticalGlue());

		Box zoomButtons = new Box(BoxLayout.X_AXIS);
		zoomButtons.setAlignmentX(LEFT_ALIGNMENT);
		zoomButtons.setAlignmentY(BOTTOM_ALIGNMENT);
		zoomButtons.setBorder(BorderFactory.createTitledBorder("Zoom"));
		JButton zoomPlus = new JButton("+");
		zoomButtons.add(zoomPlus);
		zoomPlus.addActionListener(new ZoomActionListener(1.2f));

		JButton zoomMinus = new JButton("-");
		zoomMinus.addActionListener(new ZoomActionListener(-1.2f));
		zoomButtons.add(zoomMinus);

		JButton zoomFit = new JButton("[]");
		zoomFit.setToolTipText("Zoom Fit to screen");
		zoomFit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				long duration = 2000;
				int margin = 50;

				if (!display.isTranformInProgress()) {
					Visualization vis = display.getVisualization();
					Rectangle2D bounds = vis.getBounds(NODES);
					GraphicsLib.expand(bounds, margin
							+ (int) (1 / display.getScale()));
					DisplayLib.fitViewToBounds(display, bounds, duration);
				}
			}
		});
		zoomButtons.add(zoomFit);
		boxpanel.add(zoomButtons);
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
				m_vis.run(ACTION_BASEPAUSED);
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

	private final Box setupStepButton() {
		Box buttons = new Box(BoxLayout.X_AXIS);
		JPanel mig = new JPanel(new MigLayout());
		buttons.setBorder(BorderFactory.createTitledBorder("Blob Model"));
		buttons.setAlignmentX(LEFT_ALIGNMENT);

		stepModelAction = new StepModelAction();

		step.addActionListener(stepModelAction);
		step.setEnabled(true);
		mig.add(step);

		play.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!ended && !m_vis.getAction(ACTION_PLAY).isRunning()) {
					m_vis.cancel(ACTION_PLAY);
					m_vis.run(ACTION_PLAY);
					play.setText("Stop program");
				} else {
					play.setText("Play program");
					m_vis.cancel(ACTION_PLAY);
				}
			}
		});

		mig.add(play);
		play.setEnabled(true);
		restart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				tmpStopForce();
				readProgramAndDataAsGraph(currentFile);

			}
		});
		restart.setEnabled(true);
		mig.add(restart);

		buttons.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		buttons.add(mig);
		return buttons;
	}

	private void setupDisplay() {
		// set up the display
		display = new Display(m_vis);
		display.setSize(800, 800);
		display.pan(400, 400);
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
		colorsetup = getColorSetup(grayScale);
		init.add(colorsetup);
		init.add(new RadialTreeLayout(GRAPH));
		// init.add(new RandomLayout());
		init.add(new RepaintAction());

		ActionList singleforce = new ActionList(1000);
		singleforce.add(filter);
		singleforce.add(colorsetup);
		singleforce.add(new ForceDirectedLayout(GRAPH));
		singleforce.add(new RepaintAction());
		m_vis.putAction(ACTION_1SFORCE, singleforce);

		ActionList basePaused = new ActionList(1000);
		basePaused.add(filter);
		basePaused.add(colorsetup); // base.add(new AggregateLayout(AGGR));
		basePaused.add(new RepaintAction());
		m_vis.putAction(ACTION_BASEPAUSED, basePaused);

		// ActionList pausedActions = new ActionList(500);
		// pausedActions.add(new VisibilityAnimator());

		// m_vis.putAction("pausedactions", pausedActions);
		// m_vis.alwaysRunAfter("pausedactions", "basepaused");

		ActionList force = new ActionList(Action.INFINITY, 32);
		force.add(filter);
		force.add(colorsetup);
		force.add(new ForceDirectedLayout(GRAPH));
		// force.add(AggrForce());
		force.add(new RepaintAction());

		final ActionList playac = new ActionList(Action.INFINITY, 500);
		playac.add(new Action() {
			@Override
			public void run(double frac) {
				stepModelAction.actionPerformed(null);
				if (ended) {
					play.setText("Play program");
					play.setEnabled(false);
					playac.cancel();
				}
			}
		});
		m_vis.putAction(ACTION_PLAY, playac);

		m_vis.putAction(ACTION_FORCE, force);
		m_vis.putAction("init", init);
	}

	/**
	 * Start actions if the graph is ready.
	 */
	protected void runEverything() {
		if (bgf.getGraph() != null) {
			m_vis.run("init");
			tmpRestartForce();
		}
	}

	/**
	 * Stop force simulation actions.
	 * 
	 * Helper for force simulation action controls.
	 */
	private void stopForce() {
		m_vis.cancel(ACTION_FORCE);
		pause.setText("Start force simulation");
		paused = true;
	}

	/**
	 * Start force simulation actions.
	 * 
	 * Helper for force simulation action controls.
	 */
	private void startForce() {
		m_vis.run(ACTION_FORCE);
		pause.setText("Pause force simulation");
		paused = false;
	}

	/**
	 * Stop force simulation, temporarily. Keep current state
	 */
	private void tmpStopForce() {
		m_vis.cancel(ACTION_FORCE);
		pause.setText("Start force simulation");
	}

	/**
	 * Start force simulation if needed;
	 */
	private void tmpRestartForce() {
		if (!paused) {
			m_vis.run(ACTION_FORCE);
			pause.setText("Pause force simulation");
		} else {
			pause.setText("Start force simulation");
			m_vis.cancel(ACTION_FORCE);
		}
	}

	/**
	 * Toggle the force simulation
	 * 
	 * @return true if new state is "running"
	 */
	private boolean toggleForce() {
		if (m_vis.getAction(ACTION_FORCE).isRunning()) {
			stopForce();
			return false;
		} else {
			startForce();
			return true;
		}
	}

	private void toggleGrayscale() {
		grayScale = !grayScale;
		colorsetup = getColorSetup(grayScale);


		String[] actionlist = { ACTION_FORCE, ACTION_1SFORCE, "init" };
		for (String s : actionlist) {
			boolean isRunning = m_vis.getAction(s).isRunning();
			ActionList al = (ActionList) m_vis.removeAction(s);
			al.remove(1);
			al.add(1, colorsetup);
			m_vis.putAction(s, al);
			if (isRunning) {
				m_vis.run(s);
			}
		}
		tmpRunForce1s();

	}

	/**
	 * Setup colors for nodes, edges, text and the different types of nodes.
	 * 
	 * @param grayScale
	 *            use grayscale colors or not.
	 * @return an ActionList with the complete setup.
	 */
	private static ActionList getColorSetup(boolean grayScale) {
		// set up the visual operators
		// first set up all the color actions
		ColorAction nStroke = new ColorAction(NODES, VisualItem.STROKECOLOR);
		nStroke.setDefaultColor(ColorLib.gray(100));
		// nStroke.add("_hover", ColorLib.gray(50));

		ColorAction nFill = new ColorAction(NODES, VisualItem.FILLCOLOR,
				ColorLib.gray(255));

		// nFill.add("_hover", ColorLib.gray(100));
		// nFill.add(VisualItem.FIXED, ColorLib.rgb(255, 100, 100));
		// nFill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255, 200, 125));

		ColorAction nEdges = new ColorAction(EDGES, VisualItem.STROKECOLOR);
		nEdges.setDefaultColor(ColorLib.gray(100));
		ColorAction nEdgesText = new ColorAction(EDGES, VisualItem.TEXTCOLOR,
				ColorLib.gray(0));

		StrokeAction sa = new StrokeAction(NODES);

		sa.add(
				"[" + BFConstants.BLOBTYPE + "] <= "
				+ BFConstants.BLOB_TYPE_APB, new BasicStroke(2.5f));

		StrokeAction sa2 = new StrokeAction(EDGES);
		sa2.add("[" + BFConstants.EDGENUMBERSRC + "] = 0",
				new BasicStroke(2.5f));

		ColorAction nText = new ColorAction(NODES, VisualItem.TEXTCOLOR,
				ColorLib.gray(0));

		ActionList colors = new ActionList();

		int[] palette;

		if (grayScale) {
			palette = new int[] { ColorLib.gray(255), ColorLib.gray(255),
					ColorLib.gray(160), ColorLib.gray(200), };
		} else {
			palette = new int[] { ColorLib.rgba(255, 110, 110, 210), // adb 0
					ColorLib.rgba(100, 255, 000, 210), // apb 1
					ColorLib.rgba(255, 200, 200, 210), // data 2
					ColorLib.rgba(200, 255, 200, 210), // inpgr 3
			};
		}
		DataColorAction nFillAdb = new DataColorAction(NODES,
				BFConstants.BLOBTYPE, Constants.NUMERICAL,
				VisualItem.FILLCOLOR, palette);
		nFillAdb.setBinCount(4);
		nFillAdb.setScale(Constants.LINEAR_SCALE);

		// bundle the color actions
		colors.add(sa);
		colors.add(sa2);
		colors.add(nStroke);
		colors.add(nFill);
		colors.add(nFillAdb);
		colors.add(nEdges);
		colors.add(nEdgesText);
		colors.add(new FontAction(EDGES, FontLib.getFont("SansSerif",
				Font.PLAIN, 8)));
		colors.add(nText);

		return colors;
	}

	/**
	 * When a graph is ready make it a visual graph, and make the first node the
	 * focus of the visualization.
	 * 
	 * @param g
	 *            The graph with all the nodes.
	 * @return A visual graph.
	 */
	private VisualGraph setGraph(Graph g) {
		// update graph
		m_vis.removeGroup(GRAPH);
		vg = m_vis.addGraph(GRAPH, g);
		bgf.setVisualGraph(vg);
		m_vis.setValue(EDGES, null, VisualItem.INTERACTIVE, Boolean.FALSE);
		VisualItem f = (VisualItem) vg.getNode(0);
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
		bgf.reset();
		// Setup task
		final ReadBlobConfigTask t = new ReadBlobConfigTask(filename, bgf);

		// add progress and done listeners.
		t.addPropertyChangeListener(new ProgressListener(t, filename));

		t.execute();

	}

	/**
	 * Disable all irrellevant buttons
	 * 
	 * For example when loading a big graph
	 */
	private void disableButtons() {
		pause.setEnabled(false);
		play.setEnabled(false);
		step.setEnabled(false);
		slider.setEnabled(false);
	}

	/**
	 * Enable the buttons
	 * 
	 * For example when the model and graph is ready.
	 */
	protected void enableButtons() {
		pause.setEnabled(true);
		play.setEnabled(true);
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

	/**
	 * Setup and display the main frame.
	 * 
	 * @param filename
	 *            Filename to load (if any)
	 * @return JFrame
	 */
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

	/**
	 * Setup the menubar item "data"
	 * 
	 * @return The menubar item "data"
	 */
	private static JMenu setupDataMenu() {
		JMenu dataMenu = new JMenu("Data");
		dataMenu.add("Save current data to .bld file...");
		dataMenu.add("Replace current data from file...");
		return dataMenu;
	}

	/**
	 * Setup the menubar item "file"
	 * 
	 * @param ad
	 *            The blobvis being visualized
	 * @return The menubar item "file"
	 */
	private static JMenu setupFileMenu(final BlobVis ad) {
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(new OpenGraphAction(prefs){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ad.readData(null);
			}
		});

		// Export screenshot action, copied from prefuse to be included in my
		// menu instead of only as keyboard shortcut.
		ExportDisplayAction eda = new ExportDisplayAction(ad.display);
		eda.putValue(AbstractAction.NAME, "Export Screenshot...");
		eda.putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke
				.getKeyStroke("ctrl E"));
		eda.putValue(AbstractAction.MNEMONIC_KEY, Integer.valueOf('E'));
		fileMenu.add(eda);

		// a group of check box menu items
		fileMenu.addSeparator();

		fileMenu.add("Preferences");

		// Show debug information, copied from prefuse to be included in my
		// menu instead of only as keyboard shortcut.
		JCheckBoxMenuItem cbMenuItem;
		cbMenuItem = new JCheckBoxMenuItem("Display Debug Information");
		cbMenuItem.setMnemonic('d');
		cbMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
		cbMenuItem.addActionListener(new ActionListener() {
			private PaintListener debug = null;

			public void actionPerformed(ActionEvent e) {
				if (debug == null) {
					debug = new DebugStatsPainter();
					ad.display.addPaintListener(debug);
				} else {
					ad.display.removePaintListener(debug);
					debug = null;
				}
				ad.display.repaint();
			}
		});
		fileMenu.add(cbMenuItem);



		JCheckBoxMenuItem cbGrayItem;
		cbGrayItem = new JCheckBoxMenuItem("GrayScale");
		cbGrayItem.setMnemonic('g');
		cbGrayItem.setAccelerator(KeyStroke.getKeyStroke("ctrl G"));
		cbGrayItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				ad.toggleGrayscale();
			}
		});
		fileMenu.add(cbGrayItem);
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
