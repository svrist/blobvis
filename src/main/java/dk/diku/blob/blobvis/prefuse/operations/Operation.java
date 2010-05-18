package dk.diku.blob.blobvis.prefuse.operations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Blob;
import model.BondSite;
import dk.diku.blob.blobvis.prefuse.BlobGraphFuser;
import dk.diku.blob.blobvis.prefuse.StepResult;

public abstract class Operation {
	
	public enum OP {
		CHD,
		SBS,
		JB	
	}
	private static HashMap<String, OP> ops;
	static {
		ops = new HashMap<String,Operation.OP>();
		ops.put("CHD", OP.CHD);
		ops.put("SBS", OP.SBS);
		ops.put("JB", OP.JB);
		ops.put("JB", OP.JB);
	}
	protected Blob apb;
	protected Blob adb;
	
	public Operation args(List<String> args){
		this.args = args;
		return this;
	}
	
	public void setFocusBlobs(BlobGraphFuser bgf){
		apb = bgf.ADB();
		adb = bgf.APB();
	}
	
	public String op;
	public List<String> args;
	protected Operation(String op){
		this.op = op;
	}

	public static OP parse(String opCode) {
		String[] raw = opCode.split(" ");
		ops.get(raw[0]);
		return null;
	}
		
}
