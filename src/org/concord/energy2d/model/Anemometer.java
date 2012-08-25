package org.concord.energy2d.model;

import java.awt.geom.Rectangle2D;

/**
 * @author Charles Xie
 * 
 */
public class Anemometer extends Sensor {

	public final static float RELATIVE_WIDTH = 0.05f;
	public final static float RELATIVE_HEIGHT = 0.05f;

	public Anemometer(float x, float y) {
		super(new Rectangle2D.Float());
		setCenter(x, y);
	}

	public Anemometer(float x, float y, String label) {
		this(x, y);
		setLabel(label);
	}

	public Anemometer duplicate(float x, float y) {
		return new Anemometer(x, y);
	}

	public void setCenter(float x, float y) {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		r.x = x - 0.5f * r.width;
		r.y = y - 0.5f * r.height;
	}

	public void translateBy(float dx, float dy) {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		r.x += dx;
		r.y += dy;
	}

	public void setX(float x) {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		r.x = x - 0.5f * r.width;
	}

	public void setY(float y) {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		r.y = y - 0.5f * r.height;
	}

	/** returns the x coordinate of the center */
	public float getX() {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		return r.x + 0.5f * r.width;
	}

	/** returns the y coordinate of the center */
	public float getY() {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		return r.y + 0.5f * r.height;
	}

	@Override
	public String toXml() {
		String xml = "<anemometer";
		if (stencil != ONE_POINT)
			xml += " stencil=\"" + stencil + "\"";
		String uid = getUid();
		if (uid != null && !uid.trim().equals(""))
			xml += " uid=\"" + uid + "\"";
		String label = getLabel();
		if (label != null && !label.trim().equals(""))
			xml += " label=\"" + label + "\"";
		xml += " x=\"" + getX() + "\"";
		xml += " y=\"" + getY() + "\"/>";
		return xml;
	}

}
