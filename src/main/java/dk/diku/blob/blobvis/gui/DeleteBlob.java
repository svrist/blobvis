package dk.diku.blob.blobvis.gui;

import java.awt.Component;

import javax.swing.JOptionPane;

import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;

public final class DeleteBlob {
	public static void showConfirmDialogAndPerformDelete(Component component,BlobGraphFuser bgf,VisualItem visualItem) {
		int result = JOptionPane.showConfirmDialog(component,
				"Are you sure you want to delete this Blob?",
				"Confirm blob deletion", JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE);
		if (result == JOptionPane.YES_OPTION) {
			bgf.removeBlob(visualItem);
		}

	}
	private DeleteBlob(){	}
}
