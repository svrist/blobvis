package dk.diku.blob.blobvis.prefuse;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Model;
import prefuse.data.Graph;

public class BlobGraphModel extends BlobGraphFuser {
	private static final String STEP_EVENT_KEY = "===STEP====";
	private Map<String,List<ActionListener>> listeners;

	public BlobGraphModel(Graph g, Model m){
		super(g,m);
		listeners = new HashMap<String,List<ActionListener>>();
	}

	@Override
	protected void execute(StepResult sr){
		super.execute(sr);
		fireEvent(sr.apbnext.opCode(),sr);
		fireEvent(STEP_EVENT_KEY,sr);
	}

	/* (non-Javadoc)
	 * @see dk.diku.blob.blobvis.prefuse.ModelActionExecutor#registerOpcodeListener(java.lang.String, java.awt.event.ActionListener)
	 */
	public void registerOpcodeListener(String opcode, ActionListener a){
		List<ActionListener> al;
		if (listeners.containsKey(opcode)){
			al = listeners.get(opcode);
		}else{
			al = new ArrayList<ActionListener>();
			listeners.put(opcode,al);
		}
		al.add(a);
	}


	private void fireEvent(String opcode,StepResult sr){
		if (listeners.containsKey(opcode)){
			List<ActionListener> al = listeners.get(opcode);
			ActionEvent ae = new ActionEvent(sr, 1, opcode);
			for (ActionListener actionListener : al) {
				actionListener.actionPerformed(ae);
			}
		}
	}

	/* (non-Javadoc)
	 * @see dk.diku.blob.blobvis.prefuse.ModelActionExecutor#registerStepListener(java.awt.event.ActionListener)
	 */
	public void registerStepListener(ActionListener a){
		registerOpcodeListener(STEP_EVENT_KEY, a);
	}
}
