package dk.diku.blob.blobvis.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

import model.Blob;
import model.BondSite;
import net.miginfocom.swing.MigLayout;
import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;

public class AddGridData {
	public int countNodes =0;

	public boolean randomData = true;
	public boolean randomBs = true;

	public BondSite out;
	public BondSite in;

	private static Random randGen = new Random();

	public static BondSite randomBs(boolean inclNorth){
		List<BondSite> ret = new ArrayList<BondSite>();
		if (!inclNorth){
			for (BondSite bs : BondSite.asList()) {
				if (bs != BondSite.North) {
					ret.add(bs);
				}
			}
		}else{
			ret = BondSite.asList();
		}
		return ret.get(randGen.nextInt(ret.size()));
	}

	public static int randomCargo(){
		return randGen.nextInt(128);
	}

	public static void showAddBlobGridDialog(Component c, BlobGraphFuser bgf, VisualItem item) {
		Blob b = bgf.getBlob(item);
		AddGridData agd = getAddGridData(c);
		List<Blob> work = new ArrayList<Blob>();
		work.add(b);

		if (agd.countNodes > 0) {
			int count = agd.countNodes;
			while (count > 0 && !work.isEmpty()) {
				Blob cur = work.remove(0);

				List<BondSite> freeBs = Blob.emptyBondSites(cur);
				freeBs.remove(BondSite.North);
				if (freeBs.size()>0){
					int card = randGen.nextInt(freeBs.size())+1;

					Collections.shuffle(freeBs);
					List<BondSite> newsites = new ArrayList<BondSite>(freeBs
							.subList(0, card));
					for (BondSite bs : newsites) {
						Blob newb = bgf.addDataBlobToBondSite(cur, bs,
								randomBs(false), randomCargo());
						work.add(newb);
						count--;
					}
				}


			}

		}
	}

	private static AddGridData getAddGridData(Component c) {

		final AddGridData agd = new AddGridData();

		// -- build the dialog -----
		// we need to get the enclosing frame first
		while (c != null && !(c instanceof JFrame)) {
			c = c.getParent();
		}
		final JDialog dialog = new JDialog((JFrame) c, "Add new random Blobs", true);

		// create the ok/cancel buttons
		final JButton ok = new JButton("OK");
		ok.setDefaultCapable(true);


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
		ActionListener okAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				agd.countNodes = (Integer) numberinput.getValue();
				dialog.setVisible(false);
			}
		};

		numberinput.addActionListener(okAction);
		numberinput.setColumns(5);
		numberinput.setValue(10);
		ok.addActionListener(okAction);
		numberinput.requestFocusInWindow();


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


}
