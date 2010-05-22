/**
 * 
 */
package dk.diku.blob.blobvis.gui;

import java.awt.Component;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.KeyStroke;

import prefuse.util.io.SimpleFileFilter;

public abstract class AbstractOpenGraphAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	public AbstractOpenGraphAction() {
		super();
		putValue(AbstractAction.NAME, "Open File...");
		putValue(AbstractAction.ACCELERATOR_KEY, KeyStroke
				.getKeyStroke("ctrl O"));
		putValue(AbstractAction.MNEMONIC_KEY, Integer.valueOf('O'));
	}


	public static String getBlobConfigFilename(Component c,Preferences prefs) {
		String lastUsedDir = prefs.get("LAST_USED_PATH", "");
		JFileChooser jfc = new JFileChooser(lastUsedDir);
		jfc.setDialogType(JFileChooser.OPEN_DIALOG);
		jfc.setDialogTitle("Open Graph or Tree File");
		jfc.setAcceptAllFileFilterUsed(false);
		SimpleFileFilter ff;
		ff = new SimpleFileFilter("cfg", "Blob Configuration (*.cfg)");
		ff.addExtension("cfg");
		jfc.setFileFilter(ff);

		int retval = jfc.showOpenDialog(c);
		if (retval != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		File f = jfc.getSelectedFile();
		if (f != null) {
			prefs.put("LAST_USED_PATH", f.getParent());
			return f.getAbsolutePath();
		}else{
			return null;
		}


	}
}