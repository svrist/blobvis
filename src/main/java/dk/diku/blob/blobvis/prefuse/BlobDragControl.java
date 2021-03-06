package dk.diku.blob.blobvis.prefuse;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.visual.VisualItem;
import dk.diku.blob.blobvis.BlobVis;

/**
 * Interactive drag control that is "aggregate-aware"
 */
public class BlobDragControl extends ControlAdapter {

	private VisualItem activeItem;
	protected Point2D down = new Point2D.Double();
	protected Point2D temp = new Point2D.Double();
	protected boolean dragged;
	private boolean paused = false;

	/**
	 * Creates a new drag control that issues repaint requests as an item
	 * is dragged.
	 */
	public BlobDragControl() {
	}

	/**
	 * @see prefuse.controls.Control#itemEntered(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	@Override
	public void itemEntered(VisualItem item, MouseEvent e) {
		Display d = (Display)e.getSource();
		d.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		activeItem = item;
		/*if ( !(item instanceof AggregateItem) )*/
		setFixed(item, true);
	}

	/**
	 * @see prefuse.controls.Control#itemExited(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	@Override
	public void itemExited(VisualItem item, MouseEvent e) {
		if ( activeItem == item ) {
			activeItem = null;
			setFixed(item, false);
		}
		Display d = (Display)e.getSource();
		d.setCursor(Cursor.getDefaultCursor());
	}

	/**
	 * @see prefuse.controls.Control#itemPressed(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	@Override
	public void itemPressed(VisualItem item, MouseEvent e) {
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}
		dragged = false;
		Display d = (Display)e.getComponent();
		d.getAbsoluteCoordinate(e.getPoint(), down);
		/*if ( item instanceof AggregateItem )
			setFixed(item, true);*/
		paused=!item.getVisualization().getAction(BlobVis.ACTION_FORCE).isRunning();
		item.getVisualization().cancel(BlobVis.ACTION_FORCE);
		item.getVisualization().run(BlobVis.ACTION_FORCE);

	}

	/**
	 * @see prefuse.controls.Control#itemReleased(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	@Override
	public void itemReleased(VisualItem item, MouseEvent e) {
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}
		if ( dragged ) {
			activeItem = null;
			setFixed(item, false);
			dragged = false;
		}
		if (paused) {
			item.getVisualization().cancel(BlobVis.ACTION_FORCE);
		}


	}

	/**
	 * @see prefuse.controls.Control#itemDragged(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	@Override
	public void itemDragged(VisualItem item, MouseEvent e) {
		if (!SwingUtilities.isLeftMouseButton(e)) {
			return;
		}
		dragged = true;
		Display d = (Display)e.getComponent();
		d.getAbsoluteCoordinate(e.getPoint(), temp);
		double dx = temp.getX()-down.getX();
		double dy = temp.getY()-down.getY();

		move(item, dx, dy);

		down.setLocation(temp);
	}


	protected static void setFixed(VisualItem item, boolean fixed) {
		/*if ( item instanceof AggregateItem ) {
			Iterator<VisualItem> items = ((AggregateItem)item).items();
			while ( items.hasNext() ) {
				setFixed(items.next(), fixed);
			}
		} else {*/
		item.setFixed(fixed);
		/*}*/
	}


	protected static void move(VisualItem item, double dx, double dy) {
		/*if ( item instanceof AggregateItem ) {
			Iterator<VisualItem> items = ((AggregateItem)item).items();
			while ( items.hasNext() ) {
				move(items.next(), dx, dy);
			}
		}else{*/
		double x = item.getX();
		double y = item.getY();
		item.setStartX(x);  item.setStartY(y);
		item.setX(x+dx);    item.setY(y+dy);
		item.setEndX(x+dx); item.setEndY(y+dy);
		/*}*/

	}

} // end of class AggregateDragControl
