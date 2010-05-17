/**
 * 
 */
package dk.diku.blob.blobvis.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;

public class DataBlobPopupMenu extends JPopupMenu {
	private static final long serialVersionUID = 1L;
	JMenuItem anItem;

	@SuppressWarnings("serial")
	public DataBlobPopupMenu(final Component c, final BlobGraphFuser bgf,final VisualItem vi) {

		anItem = new JMenuItem("Add Blob here...");
		anItem.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AddBlobDialog.showDialogAndPerformAdd(c,bgf,vi);
			}
		});
		add(anItem);
		anItem = new JMenuItem("Add Grid here...");
		anItem.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				AddGridData.showAddBlobGridDialog(c,bgf,vi);
			}

		});
		add(anItem);
		anItem = new JMenuItem("Delete this blob");
		anItem.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DeleteBlob.showConfirmDialogAndPerformDelete(c,bgf,vi);
			}

		});
		add(anItem);
	}

}