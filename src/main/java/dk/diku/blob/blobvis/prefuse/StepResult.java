/**
 * 
 */
package dk.diku.blob.blobvis.prefuse;

import model.Blob;
import model.BondSite;

public class StepResult {
	public Blob adbnext;
	public Blob apbnext;
	public Blob adbcur;
	public Blob apbcur;
	public boolean reread_cargo = false;

	public StepResult(Blob apbcur, Blob adbcur) {
		this.adbcur = adbcur;
		this.apbcur = apbcur;

		// Keep adb
		adbnext = adbcur;
		// Follow 2 per default.
		apbnext = apbcur.follow(BondSite.South);
	}

	// ================ BEGIN BUILDER ===============
	public StepResult adbNext(Blob adb) {
		adbnext = adb;
		return this;
	}

	public StepResult apbNext(Blob apb) {
		apbnext = apb;
		return this;
	}

	public StepResult reread(boolean flag) {
		reread_cargo = flag;
		return this;
	}

	public StepResult apbNext(BondSite bs) {
		apbnext = apbcur.follow(bs);
		return this;
	}

	public StepResult testValid() {
		if (adbnext == null
				|| apbnext == null
				|| adbcur == null
				|| apbcur == null) {
			throw new RuntimeException("Object not valid: " + "apbnext:"
					+ apbnext + ", " + "adbnext:" + adbnext + ", "
					+ "apbcur:" + apbcur + ", " + "adbcur:" + adbcur + ", ");
		}
		return this;
	}
	public StepResult adbNext(BondSite b) {
		adbnext = adbcur.follow(b);
		return this;
	}
	// =================END BUILDER ===============



}