package dk.diku.blob.blobvis.prefuse;

import model.Blob;
import prefuse.controls.Control;
import prefuse.data.Schema;

public class BFConstants {
	public static final String BLOBFIELD = "blob.field";
	public static final Schema BLOB_SCHEMA = new Schema();
	public static final Schema EDGE_SCHEMA = new Schema();
	public static final String BLOBINPGR = "blob.inpgr";
	public static final String EDGENUMBERSRC = "edge.number.src";
	public static final String EDGENUMBERTAR = "edge.number.tar";
	public static final String BLOBTYPE = "blob.type";
	public static final String BLOBBS = "blob.bs";
	public static final String ACTION = "blob.action";
	/** Label data field included in generated Graphs */
	public static final String LABEL = "label";
	/** Node table schema used for generated Graphs */
	public static final Schema LABEL_SCHEMA = new Schema();
	
	public static final int BLOB_TYPE_DATA = 0;
	public static final int BLOB_TYPE_APB = 1;
	public static final int BLOB_TYPE_ADB = 2;
	public static final int BLOB_TYPE_INPGR = 3;


	static {
		BLOB_SCHEMA.addColumn(BLOBFIELD, Blob.class, null);
		BLOB_SCHEMA.addColumn(BLOBINPGR, Boolean.class, true);
		BLOB_SCHEMA.addColumn(BLOBTYPE, Integer.class, 0);
		/*BLOB_SCHEMA.addColumn(BLOBBS+0, Node.class);
		BLOB_SCHEMA.addColumn(BLOBBS+1, Node.class);
		BLOB_SCHEMA.addColumn(BLOBBS+2, Node.class);
		BLOB_SCHEMA.addColumn(BLOBBS+3, Node.class);*/
		BLOB_SCHEMA.addColumn(ACTION, Control.class);

		EDGE_SCHEMA.addColumn(EDGENUMBERSRC, Integer.class,-1);
		EDGE_SCHEMA.addColumn(EDGENUMBERTAR, Integer.class,-1);

		LABEL_SCHEMA.addColumn(LABEL, String.class, "");
	}




}
