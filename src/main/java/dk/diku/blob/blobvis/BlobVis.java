package dk.diku.blob.blobvis;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.UIManager;
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
import prefuse.util.ui.JForcePanel;
import prefuse.util.ui.JValueSlider;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.gui.AbstractOpenGraphAction;
import dk.diku.blob.blobvis.gui.DataBlobPopupMenu;
import dk.diku.blob.blobvis.prefuse.BFConstants;
import dk.diku.blob.blobvis.prefuse.BlobDragControl;
import dk.diku.blob.blobvis.prefuse.BlobEdgeRenderer;
import dk.diku.blob.blobvis.prefuse.BlobGraphModel;
import dk.diku.blob.blobvis.prefuse.StepResult;

@SuppressWarnings("serial")
public class BlobVis extends JPanel {
	private static final String PREFS_LAST_HOPS = "LAST_HOPS";
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
	private static final int DEFAULT_HOPS = 10;

	// Prefuse layers
	private Visualization mvis;
	private Display display;
	// private Graph g;
	private VisualGraph vg;

	// defaults
	private int hops;

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
	private ForceDirectedLayout forceLayout;

	private final class ProgressListener implements
	PropertyChangeListener {
		private final ReadBlobConfigTask task;
		private final String filename;
		private boolean canceled = false;

		private ProgressListener(ReadBlobConfigTask task, String filename) {
			this.task = task;
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
			if (progressMonitor.isCanceled() || task.isDone()) {
				if (progressMonitor.isCanceled()) {
					task.cancel(true);
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
			mvis.cancel(ACTION_BASEPAUSED);
			toggleForce();
		}
	}

	public class StepModelAction implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			// Make sure that all steps are finished before a new one is
			// executed.

			synchronized (mvis) {
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
		return !mvis.getAction(ACTION_FORCE).isRunning();
	}

	private void tmpRunForce1s() {
		if (isForcePaused()) {
			mvis.cancel(ACTION_1SFORCE);
			mvis.run(ACTION_1SFORCE);
		}
	}

	public BlobVis() {
		super(new BorderLayout());
		bgf = new BlobGraphModel();

		mvis = new Visualization();

		hops = prefs.getInt(PREFS_LAST_HOPS, DEFAULT_HOPS);

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

		Box boxpanel = setupBoxpanel(); //NOPMD
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

				mvis.run(ACTION_1SFORCE);
			}
		});
		bgf.registerStepListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StepResult sr = (StepResult) e.getSource();
				VisualItem vnn = (VisualItem) vg.getNode(bgf
						.getNode(sr.apbnext).getRow());
				mvis.getGroup(Visualization.FOCUS_ITEMS).setTuple(vnn);
			}
		});
	}

	private void readData(String filename) {
		String f = filename;
		if (filename == null || "".equals(filename)) {
			f = AbstractOpenGraphAction.getBlobConfigFilename(this, prefs);
		}
		if (f != null) {
			readProgramAndDataAsGraph(f);
		}
	}

	class ZoomActionListener extends AbstractZoomControl implements
	ActionListener {
		private float direction = 1;

		ZoomActionListener(float direction) {
			super();
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
			private long duration = 2000;
			private int margin = 50;
			@Override
			public void actionPerformed(ActionEvent e) {
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
		Color c = UIManager.getLookAndFeel().getDefaults().getColor("Panel.background");
		slider.setBackground(c);

		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				int sliderHops = slider.getValue().intValue();
				filter.setDistance(sliderHops);
				prefs.putInt(PREFS_LAST_HOPS,sliderHops);

				if (!mvis.getAction(ACTION_FORCE).isRunning()){
					mvis.cancel(ACTION_1SFORCE);
					mvis.run(ACTION_1SFORCE);
				}
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
				if (!ended && !mvis.getAction(ACTION_PLAY).isRunning()) {
					mvis.cancel(ACTION_PLAY);
					mvis.run(ACTION_PLAY);
					play.setText("Stop program");
				} else {
					play.setText("Play program");
					mvis.cancel(ACTION_PLAY);
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
		display = new Display(mvis);
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
		mvis.setRendererFactory(dfr);
	}

	private void setupActions() {
		// now create the main layout routine
		ActionList init = new ActionList();

		forceLayout = new ForceDirectedLayout(GRAPH);

		init.add(filter);
		colorsetup = getColorSetup(grayScale);
		init.add(colorsetup);
		init.add(new RadialTreeLayout(GRAPH));
		// init.add(new RandomLayout());
		init.add(new RepaintAction());

		ActionList singleforce = new ActionList(1000);
		singleforce.add(filter);
		singleforce.add(colorsetup);
		singleforce.add(forceLayout);
		singleforce.add(new RepaintAction());
		mvis.putAction(ACTION_1SFORCE, singleforce);

		ActionList basePaused = new ActionList(1000);
		basePaused.add(filter);
		basePaused.add(colorsetup); // base.add(new AggregateLayout(AGGR));
		basePaused.add(new RepaintAction());
		mvis.putAction(ACTION_BASEPAUSED, basePaused);

		// ActionList pausedActions = new ActionList(500);
		// pausedActions.add(new VisibilityAnimator());

		// m_vis.putAction("pausedactions", pausedActions);
		// m_vis.alwaysRunAfter("pausedactions", "basepaused");

		ActionList force = new ActionList(Action.INFINITY, 32);
		force.add(filter);
		force.add(colorsetup);
		force.add(forceLayout);
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
		mvis.putAction(ACTION_PLAY, playac);

		mvis.putAction(ACTION_FORCE, force);
		mvis.putAction("init", init);
	}

	/**
	 * Start actions if the graph is ready.
	 */
	protected void runEverything() {
		if (bgf.getGraph() != null) {
			mvis.run("init");
			tmpRestartForce();
		}
	}

	/**
	 * Stop force simulation actions.
	 * 
	 * Helper for force simulation action controls.
	 */
	private void stopForce() {
		mvis.cancel(ACTION_FORCE);
		pause.setText("Start force simulation");
		paused = true;
	}

	/**
	 * Start force simulation actions.
	 * 
	 * Helper for force simulation action controls.
	 */
	private void startForce() {
		mvis.run(ACTION_FORCE);
		pause.setText("Pause force simulation");
		paused = false;
	}

	/**
	 * Stop force simulation, temporarily. Keep current state
	 */
	private void tmpStopForce() {
		mvis.cancel(ACTION_FORCE);
		pause.setText("Start force simulation");
	}

	/**
	 * Start force simulation if needed;
	 */
	private void tmpRestartForce() {
		if (paused) {
			pause.setText("Start force simulation");
			mvis.cancel(ACTION_FORCE);
		} else {
			mvis.run(ACTION_FORCE);
			pause.setText("Pause force simulation");

		}
	}

	/**
	 * Toggle the force simulation
	 * 
	 * @return true if new state is "running"
	 */
	private boolean toggleForce() {
		if (mvis.getAction(ACTION_FORCE).isRunning()) {
			stopForce();
			return false; //NOPMD
		} else {
			startForce();
			return true;
		}
	}

	private void toggleGrayscale() {
		grayScale ^= true;
		colorsetup = getColorSetup(grayScale);


		String[] actionlist = { ACTION_FORCE, ACTION_1SFORCE, "init" };
		for (String s : actionlist) {
			boolean isRunning = mvis.getAction(s).isRunning();
			ActionList al = (ActionList) mvis.removeAction(s);
			al.remove(1);
			al.add(1, colorsetup);
			mvis.putAction(s, al);
			if (isRunning) {
				mvis.run(s);
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
					ColorLib.rgba(100, 255, 0, 210), // apb 1
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
		mvis.removeGroup(GRAPH);
		vg = mvis.addGraph(GRAPH, g);
		bgf.setVisualGraph(vg);
		mvis.setValue(EDGES, null, VisualItem.INTERACTIVE, Boolean.FALSE);
		VisualItem f = (VisualItem) vg.getNode(0);
		mvis.getGroup(Visualization.FOCUS_ITEMS).setTuple(f);
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
		//		menubar.add(setupDataMenu());
		menubar.add(setupSettingsMenu(ad));

		frame.setJMenuBar(menubar);

		frame.setExtendedState(Frame.MAXIMIZED_HORIZ);

		frame.getContentPane().add(ad);

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		ad.readData(filename);
		return frame;
	}

	private static JMenu setupSettingsMenu(final BlobVis ad) {
		JMenu setMenu = new JMenu("Settings");
		JMenuItem forceSet = new JMenuItem("Force-simulation settings...");
		forceSet.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JForcePanel.showForcePanel(ad.forceLayout.getForceSimulator());
			}
		});
		setMenu.add(forceSet);
		JCheckBoxMenuItem cbGrayItem;
		cbGrayItem = new JCheckBoxMenuItem("GrayScale colors");
		cbGrayItem.setMnemonic('g');
		cbGrayItem.setAccelerator(KeyStroke.getKeyStroke("ctrl G"));
		cbGrayItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Toggle Gray");
				ad.toggleGrayscale();
			}
		});
		setMenu.add(cbGrayItem);
		return setMenu;
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
		fileMenu.add(new AbstractOpenGraphAction(
		){
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
					debug = null;//NOPMD
				}
				ad.display.repaint();
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
