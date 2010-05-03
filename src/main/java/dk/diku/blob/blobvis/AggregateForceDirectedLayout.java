package dk.diku.blob.blobvis;

import java.util.Iterator;

import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.util.force.ForceItem;
import prefuse.util.force.ForceSimulator;
import prefuse.visual.AggregateItem;
import prefuse.visual.VisualItem;

public class AggregateForceDirectedLayout extends ForceDirectedLayout {

	public AggregateForceDirectedLayout(String graph) {
		super(graph);
	}

	public AggregateForceDirectedLayout(String group, boolean enforceBounds,
			boolean runonce) {
		super(group, enforceBounds, runonce);

	}

	@SuppressWarnings("unchecked")
	@Override
	protected void initSimulator(ForceSimulator fsim) {
		super.initSimulator(fsim);

		Iterator<VisualItem> iter = m_vis.visibleItems(m_nodeGroup);
		ForceItem last = null;
		while ( iter.hasNext() ) {
			VisualItem cur = iter.next();
			if (last != null){
				ForceItem fcur = (ForceItem)cur.get(FORCEITEM);
				fsim.addSpring(last,fcur);
				last= fcur;
			}
		}

	}

	public AggregateForceDirectedLayout(String group, boolean enforceBounds) {
		super(group, enforceBounds);

	}

	public AggregateForceDirectedLayout(String group, ForceSimulator fsim,
			boolean enforceBounds, boolean runonce) {
		super(group, fsim, enforceBounds, runonce);

	}

	public AggregateForceDirectedLayout(String group, ForceSimulator fsim,
			boolean enforceBounds) {
		super(group, fsim, enforceBounds);

	}

	@Override
	protected float getMassValue(VisualItem n) {
		// TODO Auto-generated method stub
		if (n instanceof AggregateItem) {
			AggregateItem agr = (AggregateItem) n;
			return (float) agr.getSize();
		} else {
			return super.getMassValue(n);
		}
	}

	double tmp_dx = 0;
	@Override
	public void setX(VisualItem item, VisualItem referrer, double x) {
		if (item instanceof AggregateItem) {
			AggregateItem aggr = (AggregateItem) item;
			tmp_dx = x-item.getStartX();
			movess(aggr, tmp_dx,0);
			//System.out.println("Aggr set x. saving and waiting" +x+" "+tmp_dx);
		}
		if (tmp_dx == -12345)
			System.out.println("Undef dx ?");
		super.setX(item, referrer, x);

	}

	@Override
	public void setY(VisualItem item, VisualItem referrer, double y) {
		if (item instanceof AggregateItem) {
			AggregateItem aggr = (AggregateItem) item;
			movess(aggr, 0, y-item.getStartY());
		}
		super.setY(item, referrer, y);

	}
	protected void movess(VisualItem item, double dx, double dy) {
		if (dx == 0 && dy ==0)
			return;
		if ( item instanceof AggregateItem ) {
			AggregateItem aggr = (AggregateItem) item;
			Iterator<VisualItem> items = aggr.items();
			while ( items.hasNext() ) {
				movess(items.next(), dx, dy);
			}
		}else{
			double x = item.getX();
			double y = item.getY();

			item.setStartX(x);  item.setStartY(y);
			item.setX(x+dx);    item.setY(y+dy);
			item.setEndX(x+dx); item.setEndY(y+dy);
		}

	}
}
