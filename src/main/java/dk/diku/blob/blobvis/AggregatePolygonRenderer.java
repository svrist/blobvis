package dk.diku.blob.blobvis;

import java.awt.Shape;

import prefuse.render.PolygonRenderer;
import prefuse.visual.VisualItem;

public class AggregatePolygonRenderer extends PolygonRenderer {

	public AggregatePolygonRenderer() {
		super();
	}

	public AggregatePolygonRenderer(int polyType) {
		super(polyType);
	}

	@Override
	protected Shape getRawShape(VisualItem item) {
		double x = item.getX(), y = item.getY();
		item.setX(0); item.setY(0); // Make sure that movement can be done with this.
		// the fhull needs to be based from 0,0, so I reset the X values before getting
		// raw shape
		Shape ret = super.getRawShape(item);
		item.setX(x); item.setY(y);
		return ret;
	}
}
