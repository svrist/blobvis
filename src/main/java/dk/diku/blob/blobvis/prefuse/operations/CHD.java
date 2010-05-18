package dk.diku.blob.blobvis.prefuse.operations;

import model.BondSite;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;
import dk.diku.blob.blobvis.prefuse.StepResult;

public class CHD extends Operation {

	protected CHD(BlobGraphFuser state) {
		super(state);
	}

	@Override
	public StepResult prepare(Blob apb,Blob adb) {
		StepResult result;
		BondSite b = BondSite.create((2 + 1) & apb.getCargo());
		result = new StepResult(apb, adb).adbNext(b);
		return result;
	}

}
