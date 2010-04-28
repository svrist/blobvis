package dk.diku.blob.blobvis;

import model.Blob;
import prefuse.data.Schema;

public class BlobFuse {
	public static final String BLOBFIELD = "blob.field";
	
	public static final Schema BLOB_SCHEMA = new Schema();

	public static final String BLOBINPGR = "blob.inpgr";
	static {
		BLOB_SCHEMA.addColumn(BLOBFIELD, Blob.class, null);
		BLOB_SCHEMA.addColumn(BLOBINPGR, Boolean.class, true);
	}
	
}
