package dk.diku.blob.blobvis.prefuse.operations;

import java.util.HashMap;
import java.util.Map;

import model.Blob;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;
import dk.diku.blob.blobvis.prefuse.StepResult;

public abstract class Operation {
	protected BlobGraphFuser state;
	private Operation(){}
	protected Blob apb;
	protected Blob adb;
	private static Map<String,Operation> map = new HashMap<String,Operation>();
	protected Operation(BlobGraphFuser state){
		this.state=state;
		apb = state.APB();
		adb = state.ADB();
	}
	public static Operation decodeOpcode(BlobGraphFuser bgf){


		if (bgf.APB().opCode().startsWith("JB")) {
			result = doJB(apb, adb);
		} else if (bgf.APB().opCode().startsWith("DBS")
				|| bgf.APB().opCode().startsWith("SCG")) {
			result = new StepResult(apb, adb).reread(true);
		} else if (bgf.APB().opCode().startsWith("CHD")) {
			result = doCHD(apb, adb);
		} else if (bgf.APB().opCode().startsWith("JCG")) {
			result = doJCG(apb, adb);
		} else if (bgf.APB().opCode().startsWith("SBS")) {
			result = doSBS(apb, adb);

		} else if (bgf.APB().opCode().startsWith("JN")) {
			result = doJN(apb, adb);
		} else if (bgf.APB().opCode().startsWith("SWL")) {
			result = doSWL(apb, adb);
		} else {
			result = new StepResult(apb, adb); // Default action
		}
		if (bgf.APB().opCode().startsWith("CHD")){
			return new CHD(bgf);
		}else{
			return null;
		}

	}
	public abstract StepResult prepare(Blob apb, Blob adb);
}
