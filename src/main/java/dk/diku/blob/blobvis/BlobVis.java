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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;

import model.Blob;
import model.BondSite;
import model.Model;
import net.miginfocom.swing.MigLayout;
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
import prefuse.action.layout.Layout;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.action.layout.graph.RadialTreeLayout;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.util.io.SimpleFileFilter;
import prefuse.util.ui.JValueSlider;
import prefuse.util.ui.UILib;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualGraph;
import prefuse.visual.VisualItem;

@SuppressWarnings("serial")
public class BlobVis extends JPanel {
	private static Preferences prefs;

	private static String lastUsedDir;

	private boolean ended = true;

	boolean paused = false;

	private final class PopupListener extends ControlAdapter {
		@Override
		public void itemReleased(final VisualItem item, MouseEvent e) {
			if (e.isPopupTrigger()) {
				item.setFixed(true);
				tmpStopForce();
				Control al = (Control) item.get(BFConstants.ACTION);
				if (al != null) {
					PopUpDemo menu = new PopUpDemo(item);
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

			m_vis.cancel("base");
			m_vis.cancel("pausedactions");
			m_vis.cancel("basepaused");

			toggleForce();


		}

	}

	private static class AddBlobData {
		public BondSite fromBs;
		public BondSite toBs;
		public int cargo;
		public boolean ok = true;

		@Override
		public String toString() {
			return "AddBlobData [cargo=" + cargo + ", fromBs=" + fromBs
			+ ", ok=" + ok + ", toBs=" + toBs + "]";
		}

	}

	private static class BsListItem {
		public BsListItem(BondSite bs) {
			this.bs = bs;
		}

		public BondSite bs;

		@Override
		public String toString() {
			return bs.ordinal() + " : " + bs.toString();
		}
	}

	protected AddBlobData getAddBlobData(Blob b) {
		Component c = this;
		final AddBlobData abd = new AddBlobData();
		abd.ok = false;
		final List<BsListItem> possibleFrom = new ArrayList<BsListItem>();
		final List<BsListItem> possibleTo = new ArrayList<BsListItem>();

		// -- build the dialog -----
		// we need to get the enclosing frame first
		while (c != null && !(c instanceof JFrame)) {
			c = c.getParent();
		}
		final JDialog dialog = new JDialog((JFrame) c, "Add new Blob", true);

		// create the ok/cancel buttons
		final JButton ok = new JButton("OK");

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abd.ok = false;
				dialog.setVisible(false);
				System.out.println("Adbok " + abd.ok);
			}
		});

		// build the selection list
		int selindx = 0; // Default north
		int foundc = 0;
		for (int i = 0; i < 4; i++) {
			BsListItem bsi = new BsListItem(BondSite.create(i));
			if (b.follow(bsi.bs) == null) {
				possibleFrom.add(bsi);
				if (i > 0 && selindx == 0) {
					selindx = foundc;
				}
				foundc++;
			}
		}
		if (possibleFrom.size() == 0) {
			JOptionPane.showMessageDialog(c,
					"This data blob has no free BondSites", "Error",
					JOptionPane.ERROR_MESSAGE);
			abd.ok = false;
			return abd;
		}
		for (int i = 0; i < 4; i++) {
			BsListItem bsi = new BsListItem(BondSite.create(i));
			possibleTo.add(bsi);
		}

		final JComboBox fromlist = new JComboBox(possibleFrom.toArray());
		fromlist.setSelectedIndex(selindx);
		abd.fromBs = ((BsListItem) fromlist.getSelectedItem()).bs;
		final JComboBox tolist = new JComboBox(possibleTo.toArray());
		tolist.setSelectedIndex(1);
		abd.toBs = ((BsListItem) tolist.getSelectedItem()).bs;
		fromlist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int sel = fromlist.getSelectedIndex();
				if (sel >= 0) {
					ok.setEnabled(true);
					abd.fromBs = ((BsListItem) fromlist.getSelectedItem()).bs;
				} else {
					ok.setEnabled(false);
					abd.fromBs = null;
				}
			}
		});
		tolist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int sel = tolist.getSelectedIndex();
				if (sel >= 0) {
					ok.setEnabled(true);
					BsListItem bsi = (BsListItem) tolist.getSelectedItem();
					// System.out.println(bsi+" : "+abd);
					abd.toBs = bsi.bs;
				} else {
					ok.setEnabled(false);
					abd.toBs = null;
				}
			}
		});

		// layout the buttons
		Box bbox = new Box(BoxLayout.X_AXIS);
		bbox.add(Box.createHorizontalStrut(5));
		bbox.add(Box.createHorizontalGlue());
		bbox.add(ok);
		bbox.add(Box.createHorizontalStrut(5));
		bbox.add(cancel);
		bbox.add(Box.createHorizontalStrut(5));

		NumberFormatter numformat = new NumberFormatter(NumberFormat
				.getIntegerInstance());
		numformat.setMinimum(0);
		numformat.setMaximum(127);
		final JFormattedTextField cargoinput = new JFormattedTextField(
				numformat);
		cargoinput.setValue(b.getCargo());

		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abd.ok = true;
				dialog.setVisible(false);
				abd.cargo = (Integer) cargoinput.getValue();
			}
		});

		Box bbox2 = new Box(BoxLayout.X_AXIS);
		bbox2.setBorder(BorderFactory.createTitledBorder("Blob Model"));
		bbox2.add(fromlist);
		bbox2.add(tolist);
		bbox2.add(cargoinput);

		// put everything into a panel
		JPanel panel = new JPanel(new MigLayout());
		panel.add(new JLabel("From Bondsite:"));

		panel.add(new JLabel("To Bondsite:"), "skip 1");
		panel.add(new JLabel("Cargo:"), "wrap");

		panel.add(fromlist);
		panel.add(new JLabel("->"));
		panel.add(tolist);
		panel.add(cargoinput, "wrap");
		panel.add(bbox, "span, gapleft push,wrap");
		panel.setBorder(BorderFactory.createTitledBorder("Blob data"));

		// show the dialog
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(c);
		dialog.setVisible(true);
		dialog.dispose();

		return abd;
	}

	protected void addVisualBlob(VisualItem item) {
		AddBlobData abd = getAddBlobData(bgf.getBlob(item)); // Show a dialog
		// for a new
		// blob
		if (abd.ok) {
			bgf.saveRoot();// turns out that adding a node resets the spanning
			// grah.
			// this saveRoot + resetRoot ensures that the root is preserved
			// after addition
			// of a new data blob
			bgf.addDataBlobToBondSite(item, abd.fromBs, abd.toBs, abd.cargo);
			bgf.resetRoot();
		}
	}

	private static Random randGen = new Random();

	private void showAddBlobGridDialog(VisualItem item) {
		Blob b = bgf.getBlob(item);
		AddGridData agd = getAddGridData(b);
		List<Blob> work = new ArrayList<Blob>();
		work.add(b);

		if (agd.countNodes > 0) {
			int count = agd.countNodes;
			while (count > 0 && work.size() > 0) {
				Blob cur = work.remove(0);
				List<BondSite> freeBs = Blob.emptyBondSites(cur);

				int card = randGen.nextInt(freeBs.size());
				Collections.shuffle(freeBs);
				List<BondSite> newsites = new ArrayList<BondSite>(freeBs
						.subList(0, card));
				for (BondSite bs : newsites) {
					// bgf.addDataBlobToBondSite(item, from, to, cargo)

				}

			}

		}
	}

	private AddGridData getAddGridData(Blob blob) {
		Component c = this;
		final AddGridData agd = new AddGridData();

		// -- build the dialog -----
		// we need to get the enclosing frame first
		while (c != null && !(c instanceof JFrame)) {
			c = c.getParent();
		}
		final JDialog dialog = new JDialog((JFrame) c, "Add new Blob", true);

		// create the ok/cancel buttons
		final JButton ok = new JButton("OK");

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				agd.countNodes = 0;
				dialog.setVisible(false);
			}
		});

		Box bbox = new Box(BoxLayout.X_AXIS);
		bbox.add(Box.createHorizontalStrut(5));
		bbox.add(Box.createHorizontalGlue());
		bbox.add(ok);
		bbox.add(Box.createHorizontalStrut(5));
		bbox.add(cancel);
		bbox.add(Box.createHorizontalStrut(5));
		// put everything into a panel
		JPanel panel = new JPanel(new MigLayout());
		NumberFormatter numformat = new NumberFormatter(NumberFormat
				.getIntegerInstance());
		numformat.setMinimum(1);
		numformat.setMaximum(100);
		final JFormattedTextField numberinput = new JFormattedTextField(
				numformat);
		numberinput.setValue(10);
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				agd.countNodes = (Integer) numberinput.getValue();
				dialog.setVisible(false);
			}
		});

		panel.add(new JLabel("Blobs: "));
		panel.add(numberinput, "wrap");
		panel.add(bbox, "span, gapleft push");

		// show the dialog
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(c);
		dialog.setVisible(true);
		dialog.dispose();

		return agd;
	}

	class PopUpDemo extends JPopupMenu {
		JMenuItem anItem;

		public PopUpDemo(final VisualItem vi) {
			anItem = new JMenuItem("Add Blob here...");
			anItem.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					addVisualBlob(vi);
				}
			});
			add(anItem);
			anItem = new JMenuItem("Add Grid here...");
			anItem.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					showAddBlobGridDialog(vi);
				}

			});
			add(anItem);
			anItem = new JMenuItem("Delete this blob");
			anItem.addActionListener(new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					confirmDeleteBlob(vi);
				}

			});
			add(anItem);
		}

	}

	private void confirmDeleteBlob(VisualItem vi) {

		int result = JOptionPane.showConfirmDialog(this,
				"Are you sure you want to delete this Blob?",
				"Confirm blob deletion", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (result == JOptionPane.YES_OPTION) {
			bgf.removeBlob(vi);
		}

	}

	class PopClickListener extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {

			if (e.isPopupTrigger())
				doPop(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger())
				doPop(e);
		}

		private void doPop(MouseEvent e) {

		}
	}

	public class SwitchAction implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {

			tmpRunForce1s();
			synchronized (m_vis) {
				if (ended) {
					System.out.println("At EXT. Done");
					return;
				}

				Node r = bgf.getRoot();
				Blob apb = (Blob) r.get(BFConstants.BLOBFIELD);
				Blob adb = apb.follow(BondSite.North);
				BondSite apbBsNext = BondSite.South;

				Node adbn = findNode(r, adb);
				Node adbnnext = findNode(r, adb);

				r.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_INPGR);
				if (apb.opCode().startsWith("JB")) {
					BondSite b = BondSite.create((2 + 1) & apb.getCargo());
					if (m.ADB().follow(b) != null) {
						apbBsNext = BondSite.South;
						/* System.out.println("Going south"); */
					} else {
						/*
						 * System.out.println("Nothing on " + m.ADB() + "." + b
						 * + " going west(" + m.ADB().follow(BondSite.East) +
						 * m.ADB().follow(BondSite.South) +
						 * m.ADB().follow(BondSite.West) + ") - " + adb);
						 */
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
						System.out.println("Removing "+adbn+"->"+n);
						removeEdge(adbn,n);

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
							removeEdge(adbn, adb_b1n);
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
						removeEdge(adbn, adb_b1n);
					} else {
						// Case 1: Nothing on b1 and nothing on b2 -> nothing
						// happens
					}
				} else {
					apbBsNext = BondSite.South;
				}

				Blob next = apb.follow(apbBsNext);
				Node nn = findNode(r, next);
				updateTheBug(r, nn, adbn, adbnnext);
				stepModel(r, nn);

				if (next.opCode().equals("EXT")) {
					ended = true;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					stopForce();
				}


			}
		}

		private void tmpRunForce1s() {
			if (paused){
				m_vis.cancel("1sforce");
				m_vis.run("1sforce");
			}
		}

		private void linkNodes(Node n1, BondSite b1, Node n2, BondSite b2) {
			// loop over adb/dest edges.
			// Remove all with src/tar of b1/ds
			// link adb with dest. Set src=b1, tar=ds
			// System.out.println("Adb: " + n1);
			List<Edge> rems = gatherRemoveList(b1, n1);
			// System.out.println("dstn: " + n2);
			rems.addAll(gatherRemoveList(b2, n2));
			for (Edge element : rems) {
				Edge edge = element;
				removeEdge(edge.getSourceNode(),edge.getTargetNode());
			}
			Edge ne = g.addEdge(n1, n2);
			ne.set(BFConstants.EDGENUMBERSRC, b1.ordinal());
			ne.set(BFConstants.EDGENUMBERTAR, b2.ordinal());
			// System.out.println("Added ne: " + ne);
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

		private void setSrcBondSite(Node srcn, Blob target, BondSite newvalue) {
			Node bbn1 = findNode(srcn, target);
			Edge e1 = g.getEdge(srcn, bbn1);
			if (e1 != null) {
				e1.set(BFConstants.EDGENUMBERSRC, newvalue.ordinal());
			} else {
				e1 = g.getEdge(bbn1, srcn);
				e1.set(BFConstants.EDGENUMBERTAR, newvalue.ordinal());
			}

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

		private void removeEdge(Node n1, Node n2) {
			// System.out.println("n1: " + n1 + " n2:" + n2);
			Edge e1 = g.getEdge(n1, n2);
			Edge e2 = g.getEdge(n2, n1);
			if (e1 != null) {
				/*
				 * System.out.println("Removing " + e1 + " " +
				 * e1.getSourceNode() + "->" + e1.getTargetNode());
				 */
				g.removeEdge(e1);
			}
			if (e2 != null) {
				/*
				 * System.out.println("Removing " + e2 + " " +
				 * e2.getSourceNode() + "->" + e2.getTargetNode());
				 */
				g.removeEdge(e2);
			}
			if (g.getDegree(n1) == 0){
				System.out.println(n1+" went superflous. Removing");
				g.removeNode(n1);
			}
			if (g.getDegree(n2) == 0){
				System.out.println(n2+" went superflous. Removing");
				g.removeNode(n2);
			}


		}

		private void stepModel(Node r, Node nn) {

			if (nn != null) {
				VisualItem vnn = (VisualItem) vg.getNode(nn.getRow());
				nn.set(BFConstants.BLOBTYPE, BFConstants.BLOB_TYPE_APB);
				bgf.saveRoot(nn);
				m_vis.getGroup(Visualization.FOCUS_ITEMS).setTuple(vnn);
				m.step();
			} else {
				throw new RuntimeException("Failed to find the child successor");
			}
		}

		@SuppressWarnings("unchecked")
		private Node findNode(Node r, Blob next) {
			Node nn = null;

			Iterator<Edge> ed = r.edges();
			while (ed.hasNext()) {
				Edge e = ed.next();
				if (e.getSourceNode() == r) {
					nn = e.getTargetNode();
				} else {
					nn = e.getSourceNode();
				}
				Blob tmp = (Blob) nn.get(BFConstants.BLOBFIELD);
				if (tmp == next)
					break;
				else
					nn = null;
			}
			/*if (nn == null) {
				StringBuffer sb = new StringBuffer();
				ed = r.edges();
				while (ed.hasNext()) {
					Edge e = ed.next();
					if (e.getSourceNode() == r) {
						nn = e.getTargetNode();
					} else {
						nn = e.getSourceNode();
					}
					sb.append(nn);
					sb.append(",");
				}
				Blob rb = (Blob) r.get(BFConstants.BLOBFIELD);
				String noc = next != null ? next.opCode() : "nextnullish";
				String roc = rb != null ? rb.opCode() : "rbnullish";
				throw new RuntimeException("Failed to find " + next + "(" + noc
						+ ") as a child from " + r + "(" + rb + "," + roc
						+ ")\nChildCount:" + r.getChildCount() + ": "
						+ sb.toString());
			}*/
			return nn;
		}

	}

	Node selected = null;
	private static final String GRAPH = "graph";
	private static final String EDGES = "graph.edges";
	private static final String NODES = "graph.nodes";
	private static final String AGGR = "aggregates";
	Layout lay;

	int hops = 15;
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

		// now create the main layout routine
		ActionList init = new ActionList();
		filter = new GraphDistanceFilter(GRAPH, hops);
		init.add(filter);
		init.add(genColors());
		init.add(new RadialTreeLayout(GRAPH));
		//init.add(new RandomLayout());
		init.add(new RepaintAction());

		ActionList singleforce = new ActionList(1000);
		singleforce.add(genColors());
		singleforce.add(new ForceDirectedLayout(GRAPH));
		singleforce.add(new RepaintAction());
		m_vis.putAction("1sforce", singleforce);

		ActionList base = new ActionList(Action.INFINITY, 100);
		base.add(filter);
		base.add(genColors()); // base.add(new AggregateLayout(AGGR));
		base.add(new RepaintAction());

		ActionList basePaused = new ActionList(1000);
		basePaused.add(filter);
		basePaused.add(genColors()); // base.add(new AggregateLayout(AGGR));
		basePaused.add(new RepaintAction());
		m_vis.putAction("basepaused", basePaused);


		ActionList pausedActions = new ActionList(500);
		pausedActions.add(new VisibilityAnimator());

		m_vis.putAction("pausedactions", pausedActions);
		m_vis.alwaysRunAfter("pausedactions", "basepaused");

		ActionList force = new ActionList(Action.INFINITY, 32);
		force.add(genColors());
		force.add(new ForceDirectedLayout(GRAPH));
		// force.add(AggrForce());
		force.add(new RepaintAction());
		m_vis.putAction("force", force);
		m_vis.putAction("init", init);
		m_vis.putAction("base", base);
		// set up the display
		Display display = new Display(m_vis);
		display.setSize(800, 800);
		display.pan(150, 150);
		display.setHighQuality(true);
		//display.setItemSorter(new TreeDepthItemSorter());

		display.addControlListener(new BlobDragControl());
		// display.addControlListener(new FocusControl(1));
		display.addControlListener(new PanControl());
		// display.addControlListener(new ZoomControl());
		display.addControlListener(new WheelZoomControl());

		display.addControlListener(new PopupListener());
		// display.addControlListener(new ZoomToFitControl());

		JPanel panel = new JPanel();
		display.addMouseListener(new PopClickListener());

		Box buttons = new Box(BoxLayout.Y_AXIS);
		step = new JButton("Step model");
		step.addActionListener(new SwitchAction());
		step.setEnabled(true);
		buttons.setBorder(BorderFactory.createTitledBorder("Blob Model"));
		buttons.add(step);
		buttons.setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));

		Box forcebuttons = new Box(BoxLayout.Y_AXIS);
		pause = new JButton("Pause force movements");
		pause.addActionListener(new PauseForceAction());
		pause.setEnabled(true);
		forcebuttons.add(pause);
		forcebuttons.setBorder(BorderFactory
				.createTitledBorder("Force simulation ctrl"));
		forcebuttons.setMaximumSize(new Dimension(Short.MAX_VALUE,
				Short.MAX_VALUE));

		slider = new JValueSlider("Distance", 0, 35, hops);
		slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				filter.setDistance(slider.getValue().intValue());
				m_vis.run("basepaused");
			}
		});

		/*
		 * slider.setPreferredSize(new Dimension(300,30));
		 * slider.setMaximumSize(new Dimension(300,30));
		 */

		Box cf = new Box(BoxLayout.Y_AXIS);
		cf.setAlignmentX(Component.LEFT_ALIGNMENT);
		cf.add(slider);
		cf.setBorder(BorderFactory.createTitledBorder("Connectivity Filter"));

		Box boxpanel = new Box(BoxLayout.Y_AXIS);
		boxpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		boxpanel.add(buttons);
		boxpanel.add(Box.createVerticalStrut(20));
		boxpanel.add(forcebuttons);
		boxpanel.add(Box.createVerticalStrut(20));
		boxpanel.add(cf);

		panel.add(boxpanel);
		// panel.add(Box.createVerticalGlue());

		JSplitPane split = new JSplitPane();
		split.setLeftComponent(display);
		split.setRightComponent(panel);
		split.setOneTouchExpandable(true);
		split.setContinuousLayout(false);
		split.setDividerLocation(-1);
		add(split);
		// now we run our action list
		// set things running
		if (filename != null) {
			readProgramAndDataAsGraph(filename);
		} else {
			ended = true;
		}

	}

	protected void runEverything() {
		if (g != null) {
			m_vis.run("init");
			//m_vis.run("base");

			if (!paused) {
				startForce();
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
		}else{
			m_vis.cancel("force");
		}
	}

	/**
	 * Start force simulation if needed;
	 */
	private void tmpStartForce() {
		m_vis.run("force");
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

			int[] palette = new int[] { ColorLib.rgba(255, 200, 200, 210), // adb
					// 0
					ColorLib.rgba(100, 255, 000, 210), // apb 1
					ColorLib.rgba(255, 110, 110, 210), // data 2
					ColorLib.rgba(200, 255, 200, 210), // inpgr 3
			};
			ColorAction nFillAdb = new DataColorAction(NODES,
					BFConstants.BLOBTYPE, Constants.NOMINAL,
					VisualItem.FILLCOLOR, palette);

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
		bgf.setVisualGraph(vg);
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

	abstract class Task extends SwingWorker<Void, Void> implements Progressable {
		/*
		 * Main task. Executed in background thread.
		 */
		@Override
		public Void doInBackground() {
			// Initialize progress property.
			setProgress(0);
			work();
			return null;
		}

		public abstract void work();

		/*
		 * Executed in event dispatching thread
		 */
		@Override
		public void done() {
			System.out.println("Done");
			setCursor(null); // turn off the wait cursor
			setProgress(100);
		}
	}

	private ProgressMonitor progressMonitor;

	public void readProgramAndDataAsGraph(final String filename) {

		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		disableButtons();
		Component c = this;
		while (c != null && !(c instanceof JFrame)) {
			c = c.getParent();
		}
		progressMonitor = new ProgressMonitor(c,
				"Loading Blob configuration...", "", 0, 100);
		progressMonitor.setProgress(0);

		final Task t = new Task() {
			@Override
			public void work() {
				progress(0);
				g = new Graph();
				g.getNodeTable().addColumns(BFConstants.LABEL_SCHEMA);
				g.getNodeTable().addColumns(BFConstants.BLOB_SCHEMA);
				g.getEdgeTable().addColumns(BFConstants.EDGE_SCHEMA);
				m = new Model();
				m.readConfiguration(filename);
				bgf = new BlobGraphFuser(g, m, clickHandler);
				m_vis.removeGroup(GRAPH);
				//vg = m_vis.addGraph(GRAPH, g);
				try {
					bgf.populateGraphFromModelAPB(this);
					vg = setGraph(g);
					if (use_aggregate) {
						fillAggregates(vg);
					}
				} catch (InterruptedException e) {
					g.clear();
					m.reset();
				}

			}

			@Override
			public void progress(int progress) {
				setProgress(progress);
			}
		};
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

				} else if ("state".equals(evt.getPropertyName())) {
					System.out.println("State: " + evt.getNewValue());
				}
				if (progressMonitor.isCanceled() || t.isDone()) {
					if (progressMonitor.isCanceled()) {
						System.out.println("Canceled. Exiting");
						// System.exit(1);
						t.cancel(true);
						progressMonitor.close();
						canceled = true;
					} else if (canceled == false) {

						ended = false;

						runEverything();
						enableButtons();
					}
				}
			}
		});
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
			if (type.canGet(BFConstants.BLOBINPGR, Boolean.class)) {
				boolean b = (Boolean) type.get(BFConstants.BLOBINPGR);
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

	private ControlAdapter clickHandler = new ControlAdapter() {
		@Override
		public void itemClicked(VisualItem item, MouseEvent e) {
			PopUpDemo menu = new PopUpDemo(item);
			menu.show(m_vis.getDisplay(0), e.getX(), e.getY());
		}
	};

	public static void main(String[] argv) {
		final String[] filename = new String[1];
		prefs = Preferences.userNodeForPackage(BlobVis.class);
		lastUsedDir = prefs.get("LAST_USED_PATH", "");
		if (argv.length > 0) {
			filename[0] = argv[0];
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				demo(filename[0]);
			}
		});

	}

	public static JFrame demo(String filename) {
		UILib.setPlatformLookAndFeel();
		JFrame frame = new JFrame("dikuBlob - B l o b V i s");
		if (filename == null || "".equals(filename)) {
			filename = OpenGraphAction.getBlobConfigFilename(frame);
		}

		BlobVis ad = new BlobVis(filename);

		JMenuBar menubar = new JMenuBar();

		JMenu dataMenu = new JMenu("Data");
		dataMenu.add(new OpenGraphAction(ad));
		menubar.add(dataMenu);
		frame.setJMenuBar(menubar);
		frame.setExtendedState(Frame.MAXIMIZED_HORIZ);
		frame.getContentPane().add(ad);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		return frame;
	}

	private final JButton pause;
	final JButton step;
	final JValueSlider slider;

	private BlobGraphFuser bgf;

	public static class OpenGraphAction extends AbstractAction {
		private BlobVis m_view;

		public OpenGraphAction(BlobVis view) {
			m_view = view;
			putValue(AbstractAction.NAME, "Open File...");
			putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke
					.getKeyStroke("ctrl O"));
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			String filename = getBlobConfigFilename(m_view);
			if (filename == null)
				return;

			m_view.readProgramAndDataAsGraph(filename);

		}

		public static String getBlobConfigFilename(Component c) {
			JFileChooser jfc = new JFileChooser(lastUsedDir);
			jfc.setDialogType(JFileChooser.OPEN_DIALOG);
			jfc.setDialogTitle("Open Graph or Tree File");
			jfc.setAcceptAllFileFilterUsed(false);
			SimpleFileFilter ff;
			ff = new SimpleFileFilter("cfg", "Blob Configuration (*.cfg)");
			ff.addExtension("cfg");
			jfc.setFileFilter(ff);

			int retval = jfc.showOpenDialog(c);
			if (retval != JFileChooser.APPROVE_OPTION)
				return null;

			File f = jfc.getSelectedFile();
			if (f != null) {
				prefs.put("LAST_USED_PATH", f.getParent());
			}
			return f.getAbsolutePath();

		}
	}
}
