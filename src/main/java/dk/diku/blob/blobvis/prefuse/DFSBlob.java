/**
 * 
 */
package dk.diku.blob.blobvis.prefuse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import model.Blob;
import model.BondSite;
import dk.diku.blob.blobvis.gui.Progressable;
import dk.diku.blob.blobvis.util.Pair;

class DFSBlob {

	/**
	 * 
	 */
	private final BlobFuser target;
	//boolean inpgr = true;
	private Blob start;

	private double mcount = -1;
	private int count = 0;
	Progressable p = new Progressable() {
		@Override
		public void progress(int progress) {/* ignore */
		}
	};

	/**
	 * Setup a new Depth First Search based on a given Blob and add nodes and
	 * edges to the given Fuser
	 * @param target
	 * @param start
	 * @param totalCount
	 */
	public DFSBlob(BlobFuser target, Blob start, double totalCount) {
		this.target = target;
		this.start = start;
		mcount = totalCount;
	}

	private void stackEdge(Map<Blob, List<Blob>> edges, Blob b,
			Blob neighbor) {
		List<Blob> neighboredges;
		if (edges.containsKey(neighbor)) {
			neighboredges = edges.get(neighbor);
		} else {
			neighboredges = new ArrayList<Blob>();
			edges.put(neighbor, neighboredges);
		}
		neighboredges.add(b);
	}

	public void run() throws InterruptedException {
		it_dfsBlob(start);


	}

	private void it_dfsBlob(Blob start) throws InterruptedException {
		Stack<Pair<Blob,Boolean>> nextStack = new Stack<Pair<Blob,Boolean>>();
		Stack<Blob> traversed = new Stack<Blob>();
		Map<Blob, List<Blob>> edges = new HashMap<Blob, List<Blob>>();

		// Enqueue root
		nextStack.add(new Pair<Blob,Boolean>(start,true));


		while (!nextStack.isEmpty()) {
			if (count % 10 == 0) {
				p.progress((int) ((traversed.size() / mcount) * 100));
			}
			count++;
			// Dequeue next node for comparison
			// And add it 2 list of traversed nodes
			Pair<Blob,Boolean> pb = nextStack.pop();
			Blob b = pb.one;
			traversed.push(b);
			target.addBlobAsNode(b,pb.two);
			// empty any waiting edges. This node is now ready
			popEdges(edges, b);
			// Enqueue new neighbors
			for (BondSite bs : BondSite.asList()) {
				Boolean inp = pb.two;
				Blob neighbor = b.follow(bs);
				if (neighbor == null) {
					continue;
				}
				Pair<Blob,Boolean> newbse = new Pair<Blob,Boolean>(neighbor,inp);
				if (!traversed.contains(neighbor)
						&& !nextStack.contains(newbse)) {
					if (bs.equals(BondSite.North)){
						newbse.two = !inp;
					}
					nextStack.push(newbse);
				}
				if (nextStack.contains(newbse)) {
					// Save edges for all childs in a list.
					stackEdge(edges, b, neighbor);
				}
			}
			if (Thread.interrupted()){
				throw new InterruptedException();
			}
		}
	}

	private boolean popEdges(Map<Blob, List<Blob>> edges, Blob b) {
		// Empty edge list for this node.
		boolean viaNorth=false;
		if (edges.containsKey(b)) {
			for (Blob prev : edges.get(b)) {
				if (prev.boundTo(b).equals(BondSite.North)){
					viaNorth = true;
				}
				target.addEdge(prev, b);
			}
		}
		return viaNorth;
	}
}