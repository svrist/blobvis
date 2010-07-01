package dk.diku.blob.blobvis.prefuse.operations;

import java.util.Arrays;
import java.util.List;


public class Operation {

	public enum OP {
		CHD,
		SBS,
		JB, DBS, SCG, JN, SWL, JCG,FIN,INS,SWP1,SWP3
	}
	public Operation args(List<String> args){
		this.args = args;
		return this;
	}

	public String op;
	public List<String> args;
	public OP type;
	protected Operation(String op){
		this.op = op;
	}

	public static Operation parse(String opCode) {
		String[] raw = opCode.split(" ");
		Operation o = new Operation(raw[0]).args(
				Arrays.asList(
						Arrays.copyOfRange(raw, 1, raw.length))
		);
		o.type = OP.valueOf(raw[0].toUpperCase());
		return o;
	}

}
