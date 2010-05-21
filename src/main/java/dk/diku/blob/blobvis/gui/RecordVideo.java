package dk.diku.blob.blobvis.gui;

import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;

import javax.media.Manager;
import javax.media.Player;

public class RecordVideo extends Frame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static void main (String[] args) {
		try {
			Frame f = new RecordVideo();
			f.pack();
			f.setVisible (true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public RecordVideo()throws java.io.IOException,
	java.net.MalformedURLException,
	javax.media.MediaException {
		FileDialog fd = new FileDialog
		(this, "TrivialJMFPlayer", FileDialog.LOAD);
		fd.setVisible(true);
		File f = new File (fd.getDirectory(), fd.getFile());
		Player p = Manager.createRealizedPlayer
		(f.toURI().toURL());
		Component c = p.getVisualComponent();
		add (c);
		p.start();
	}
}

