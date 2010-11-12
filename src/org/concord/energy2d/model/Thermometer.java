/*
 *   Copyright (C) 2009  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 */

package org.concord.energy2d.model;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.concord.energy2d.event.MeasurementEvent;
import org.concord.energy2d.event.MeasurementListener;

/**
 * @author Charles Xie
 * 
 */
public class Thermometer extends Manipulable {

	private final static int MAX = 500;
	private List<TimedData> data;
	private List<MeasurementListener> listeners;

	public Thermometer(float x, float y) {
		super(new Rectangle2D.Float());
		data = Collections.synchronizedList(new ArrayList<TimedData>());
		listeners = new ArrayList<MeasurementListener>();
		setLocation(x, y);
	}

	public Thermometer duplicate(float x, float y) {
		return new Thermometer(x, y);
	}

	public void setLocation(float x, float y) {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		r.x = x - r.width * 0.5f;
		r.y = y - r.height * 0.5f;
	}

	public float getX() {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		return r.x + 0.5f * r.width;
	}

	public float getY() {
		Rectangle2D.Float r = (Rectangle2D.Float) getShape();
		return r.y + 0.5f * r.height;
	}

	public void addMeasurementListener(MeasurementListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}

	public void removeMeasurementListener(MeasurementListener l) {
		listeners.remove(l);
	}

	private void notifyMeasurementListeners() {
		if (listeners.isEmpty())
			return;
		MeasurementEvent e = new MeasurementEvent(this);
		for (MeasurementListener x : listeners)
			x.measurementTaken(e);
	}

	public void clear() {
		data.clear();
		notifyMeasurementListeners();
	}

	public List<TimedData> getData() {
		return data;
	}

	public float getCurrentData() {
		if (data.isEmpty())
			return Float.NaN;
		return data.get(data.size() - 1).getValue();
	}

	public void addData(float time, float temperature) {
		data.add(new TimedData(time, temperature));
		notifyMeasurementListeners();
		if (data.size() > MAX)
			data.remove(0);
	}

	public String toXml() {
		String xml = "<thermometer ";
		xml += " x=\"" + getX() + "\"";
		xml += " y=\"" + getY() + "\"/>";
		return xml;
	}

}
