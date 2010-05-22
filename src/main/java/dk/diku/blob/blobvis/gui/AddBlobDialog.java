/**
 * 
 */
package dk.diku.blob.blobvis.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

import model.Blob;
import model.BondSite;
import net.miginfocom.swing.MigLayout;
import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;

public class AddBlobDialog {
	public static class BsListItem {
		public BsListItem(BondSite bondsite) {
			this.bondsite = bondsite;
		}

		private BondSite bondsite;

		@Override
		public String toString() {
			return bondsite.ordinal() + " : " + bondsite.toString();
		}

		public BondSite getBondsite() {
			return bondsite;
		}
	}

	private BondSite fromBs;
	private BondSite toBs;
	private int cargo;
	private boolean ok = true;

	@Override
	public String toString() {
		return "AddBlobData [cargo=" + cargo + ", fromBs=" + fromBs
		+ ", ok=" + ok + ", toBs=" + toBs + "]";
	}

	private static AddBlobDialog getAddBlobData(Component comp,Blob blob) {
		final AddBlobDialog abd = new AddBlobDialog();
		abd.ok = false;
		// -- build the dialog -----
		// we need to get the enclosing frame first
		Component component = getJFrame(comp);
		final JDialog dialog = new JDialog((JFrame) component, "Add new Blob", true);

		// create the ok/cancel buttons
		final JButton ok = new JButton("OK");

		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				abd.ok = false;
				dialog.setVisible(false);
			}
		});

		// build the selection list
		final List<BsListItem> possibleFrom = getPossibleFromBondSites(blob);
		if (possibleFrom.isEmpty()) {
			JOptionPane.showMessageDialog(component,
					"This data blob has no free BondSites", "Error",
					JOptionPane.ERROR_MESSAGE);
			abd.ok = false;

			return abd;	//NOPMD
		}
		final List<BsListItem> possibleTo = getPossibleToBondSites();

		final JComboBox fromlist = new JComboBox(possibleFrom.toArray());
		fromlist.setSelectedIndex(0);
		abd.fromBs = ((BsListItem) fromlist.getSelectedItem()).getBondsite();
		final JComboBox tolist = new JComboBox(possibleTo.toArray());
		tolist.setSelectedIndex(1);
		abd.toBs = ((BsListItem) tolist.getSelectedItem()).getBondsite();
		fromlist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int sel = fromlist.getSelectedIndex();
				if (sel >= 0) {
					ok.setEnabled(true);
					abd.fromBs = ((BsListItem) fromlist.getSelectedItem()).getBondsite();
				} else {
					ok.setEnabled(false);
					abd.fromBs = null;//NOPMD
				}
			}
		});
		tolist.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int sel = tolist.getSelectedIndex();
				if (sel >= 0) {
					ok.setEnabled(true);
					BsListItem bsi = (BsListItem) tolist.getSelectedItem();
					abd.toBs = bsi.getBondsite();
				} else {
					ok.setEnabled(false);
					//NOPMD
					abd.toBs = null;
				}
			}
		});

		// layout the buttons
		Box bbox = getButtons(ok, cancel);

		final JFormattedTextField cargoinput = getNumberTextField(blob.getCargo(),0,127);

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
		JPanel panel = getMainLayout(fromlist, tolist, bbox, cargoinput);

		// show the dialog
		dialog.setContentPane(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(component);
		dialog.setVisible(true);
		dialog.dispose();



		return abd;
	}

	static Component getJFrame(Component comp) {
		Component c = comp;
		while (c != null && !(c instanceof JFrame)) {
			c = c.getParent();
		}
		return c;
	}

	private static JPanel getMainLayout(final JComboBox fromlist,
			final JComboBox tolist, Box bbox,
			final JFormattedTextField cargoinput) {
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
		return panel;
	}

	private static JFormattedTextField getNumberTextField(int defaultValue,int min, int max) {
		NumberFormatter numformat = new NumberFormatter(NumberFormat
				.getIntegerInstance());
		numformat.setMinimum(min);
		numformat.setMaximum(max);
		final JFormattedTextField cargoinput = new JFormattedTextField(
				numformat);
		cargoinput.setValue(defaultValue);
		return cargoinput;
	}

	private static Box getButtons(final JButton ok, JButton cancel) {
		Box bbox = new Box(BoxLayout.X_AXIS);
		bbox.add(Box.createHorizontalStrut(5));
		bbox.add(Box.createHorizontalGlue());
		bbox.add(ok);
		bbox.add(Box.createHorizontalStrut(5));
		bbox.add(cancel);
		bbox.add(Box.createHorizontalStrut(5));
		return bbox;
	}

	private static List<BsListItem> getPossibleToBondSites() {
		final List<BsListItem> possibleTo = new ArrayList<BsListItem>();
		for (BondSite bs : BondSite.asList()) {
			BsListItem bsi = new BsListItem(bs);
			possibleTo.add(bsi);
		}
		return possibleTo;
	}

	private static List<BsListItem> getPossibleFromBondSites(Blob b) {
		final List<BsListItem> possibleFrom = new ArrayList<BsListItem>();
		for (BondSite bs : Blob.emptyBondSites(b)) {
			BsListItem bsi = new BsListItem(bs);
			if (bs != BondSite.North) {
				possibleFrom.add(bsi);
			}
		}
		return possibleFrom;
	}

	public static void showDialogAndPerformAdd(Component c, BlobGraphFuser bgf,VisualItem item) {
		// Show a dialog for a new  blob
		AddBlobDialog abd = AddBlobDialog.getAddBlobData(c,bgf.getBlob(item));
		if (abd.ok) {
			bgf.addDataBlobToBondSite(item, abd.fromBs, abd.toBs, abd.cargo);
		}
	}

	public BondSite getFromBs() {
		return fromBs;
	}

	public BondSite getToBs() {
		return toBs;
	}
	public int getCargo() {
		return cargo;
	}
	public boolean isOk() {
		return ok;
	}

}