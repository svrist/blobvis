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
		public BsListItem(BondSite bs) {
			this.bs = bs;
		}

		public BondSite bs;

		@Override
		public String toString() {
			return bs.ordinal() + " : " + bs.toString();
		}
	}

	public BondSite fromBs;
	public BondSite toBs;
	public int cargo;
	public boolean ok = true;

	@Override
	public String toString() {
		return "AddBlobData [cargo=" + cargo + ", fromBs=" + fromBs
		+ ", ok=" + ok + ", toBs=" + toBs + "]";
	}

	private static AddBlobDialog getAddBlobData(Component c,Blob b) {
		final AddBlobDialog abd = new AddBlobDialog();
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

	public static void showDialogAndPerformAdd(Component c, BlobGraphFuser bgf,VisualItem item) {
		// Show a dialog for a new  blob
		AddBlobDialog abd = AddBlobDialog.getAddBlobData(c,bgf.getBlob(item));
		if (abd.ok) {
			bgf.addDataBlobToBondSite(item, abd.fromBs, abd.toBs, abd.cargo);
		}
	}

}