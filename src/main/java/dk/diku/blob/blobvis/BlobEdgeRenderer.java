package dk.diku.blob.blobvis;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import prefuse.render.EdgeRenderer;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

public class BlobEdgeRenderer extends EdgeRenderer {

	public BlobEdgeRenderer() {
		super();
	}

	public BlobEdgeRenderer(int edgeType, int arrowType) {
		super(edgeType, arrowType);
		// TODO Auto-generated constructor stub
	}

	public BlobEdgeRenderer(int edgeType) {
		super(edgeType);
	}
	
	public BlobEdgeRenderer(float zoom_cutoff){
		this.zoom_cutoff = zoom_cutoff; 
	}

	float zoom_cutoff = 0.6f;



	/**
	 * @see prefuse.render.Renderer#render(java.awt.Graphics2D, prefuse.visual.VisualItem)
	 */
	@Override
	public void render(Graphics2D g, VisualItem item) {
		// render the edge line
		super.render(g, item);
		EdgeItem   edge = (EdgeItem)item;
		/*VisualItem item1 = edge.getSourceItem();
		VisualItem item2 = edge.getTargetItem();
		if (!item1.canGet(BlobFuse.BLOBFIELD, Blob.class)
				|| !item2.canGet(BlobFuse.BLOBFIELD, Blob.class)) {
			return;
		}*/
		if (!edge.canGet(BFConstants.EDGENUMBERSRC, Integer.class)){
			return;
		}
		
		boolean useInt = zoom_cutoff > Math.max(g.getTransform().getScaleX(),
				g.getTransform().getScaleY());
		if (useInt){
			return;
		}


		// Draw bond site text:
		int textColor = item.getTextColor();
/*
		Blob from = (Blob) item1.get(BlobFuse.BLOBFIELD);
		Blob to = (Blob) item2.get(BlobFuse.BLOBFIELD);*/
		int bsf = (Integer)edge.get(BFConstants.EDGENUMBERSRC);
		int bst = (Integer)edge.get(BFConstants.EDGENUMBERTAR);
		/*if (to.boundTo(from) != null){
			bst = to.boundTo(from).ordinal();
		}*/
		drawBondsite(g, item.getFont(), edge.getSourceItem().getBounds(), textColor, ""+bsf);
		drawBondsite(g, item.getFont(), edge.getTargetItem().getBounds(), textColor, ""+bst);

	}

	private void drawBondsite(Graphics2D g, Font m_font, Rectangle2D itembounds,
			int textColor, String text) {
		boolean useInt = 1.5 > Math.max(g.getTransform().getScaleX(),
				g.getTransform().getScaleY());
		double ypadding=1, xpadding=1;


		Point2D start = null, end = null;
		start = m_tmpPoints[1];
		end   = m_tmpPoints[0];
		FontMetrics fm = DEFAULT_GRAPHICS.getFontMetrics(m_font);
		Dimension m_textDim = new Dimension();
		m_textDim.width = fm.stringWidth(text);
		m_textDim.height = fm.getHeight();

		Rectangle2D bounds = (Rectangle2D) itembounds.clone();

		// Increase size of box to counter that text is defined from the bottom left corner
		bounds.setRect(bounds.getX()-m_textDim.width, bounds.getY()-ypadding,
				bounds.getWidth()+m_textDim.width+xpadding, bounds.getHeight()+m_textDim.height/2+ypadding);



		// compute the intersection with the target bounding box

		int i = GraphicsLib.intersectLineRectangle(start, end,
				bounds, m_isctPoints);
		if ( i > 0 ){
			// found an intersection
			end = m_isctPoints[0];
		}
		double y = end.getY();
		double x = end.getX();




		if ( text != null && ColorLib.alpha(textColor) > 0 ) {
			g.setPaint(ColorLib.getColor(textColor));
			g.setFont(m_font);

			
			//double th = m_textDim.height;
			// compute starting y-coordinate
			/*y += fm.getAscent();
			y += th - m_textDim.height;*/
			drawString(g, fm, text, useInt, x, y, m_textDim.width);

		}
	}

	private final void drawString(Graphics2D g, FontMetrics fm, String text,
			boolean useInt, double x, double y, double w)
	{
		// compute the x-coordinate
		double tx = x; //+ (w - fm.stringWidth(text)) / 2;

		// use integer precision unless zoomed-in
		// results in more stable drawing
		if ( useInt ) {
			g.drawString(text, (int)tx, (int)y);
		} else {
			g.drawString(text, (float)tx, (float)y);
		}
	}

}
