/*
 *   Copyright (C) 2009  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 */

package org.concord.energy2d.model;

import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.concord.energy2d.event.VisualizationEvent;
import org.concord.energy2d.event.VisualizationListener;
import org.concord.energy2d.math.Polygon2D;

/**
 * Units:
 * 
 * Temperature: centigrade; Length: meter; Time: second; Thermal diffusivity:
 * m^2/s; Power: centigrade/second
 * 
 * @author Charles Xie
 * 
 */
public class Model2D {

	private int indexOfStep;

	// Air's thermal conductivity = 0.025 W/(m*K)
	private float backgroundConductivity = 0.025f;

	// Air's specific heat capacity = 1012 J/(kg*K)
	private float backgroundCapacity = 1012;

	// Air's density = 1.204 kg/m^3 at 25 C
	private float backgroundDensity = 1.204f;

	private float backgroundTemperature;

	// temperature array
	private float[][] t;

	// velocity x-component array (m/s)
	private float[][] u;

	// velocity y-component array (m/s)
	private float[][] v;

	// internal temperature boundary array
	private float[][] tb;

	// internal heat generation array
	private float[][] q;

	// wind speed
	private float[][] uWind, vWind;

	// conductivity array
	private float[][] conductivity;

	// specific heat capacity array
	private float[][] capacity;

	// density array
	private float[][] density;

	// fluid cell array
	private boolean[][] fluidity;

	private List<Thermometer> thermometers;

	private List<Part> parts;
	private List<Photon> photons;

	private RaySolver2D raySolver;
	private FluidSolver2D fluidSolver;
	private HeatSolver2D heatSolver;

	private boolean sunny;
	private int photonEmissionInterval = 20;

	private int nx = 100;
	private int ny = 100;

	// length in x direction (unit: meter)
	private float lx = 10;

	// length in y direction (unit: meter)
	private float ly = 10;

	private float deltaX = lx / nx;
	private float deltaY = ly / ny;

	private boolean running;
	private boolean notifyReset;
	private int viewUpdateInterval = 100;

	// optimization flags
	private boolean hasPartPower;
	private boolean radiative;
	private boolean convective = true;

	private List<VisualizationListener> visualizationListeners;
	private List<PropertyChangeListener> propertyChangeListeners;

	public Model2D() {

		t = new float[nx][ny];
		u = new float[nx][ny];
		v = new float[nx][ny];
		q = new float[nx][ny];
		tb = new float[nx][ny];
		uWind = new float[nx][ny];
		vWind = new float[nx][ny];
		conductivity = new float[nx][ny];
		capacity = new float[nx][ny];
		density = new float[nx][ny];
		fluidity = new boolean[nx][ny];

		init();

		heatSolver = new HeatSolver2DImpl(nx, ny);
		heatSolver.setCapacity(capacity);
		heatSolver.setConductivity(conductivity);
		heatSolver.setDensity(density);
		heatSolver.setPower(q);
		heatSolver.setVelocity(u, v);
		heatSolver.setTemperatureBoundary(tb);
		heatSolver.setFluidity(fluidity);

		fluidSolver = new FluidSolver2DImpl(nx, ny);
		fluidSolver.setFluidity(fluidity);
		fluidSolver.setTemperature(t);
		fluidSolver.setWindSpeed(uWind, vWind);

		raySolver = new RaySolver2D(lx, ly);
		raySolver.setPower(q);

		setGridCellSize();

		parts = Collections.synchronizedList(new ArrayList<Part>());
		thermometers = Collections
				.synchronizedList(new ArrayList<Thermometer>());
		photons = Collections.synchronizedList(new ArrayList<Photon>());

		visualizationListeners = new ArrayList<VisualizationListener>();
		propertyChangeListeners = new ArrayList<PropertyChangeListener>();

	}

	public void setConvective(boolean convective) {
		this.convective = convective;
	}

	public boolean isConvective() {
		return convective;
	}

	public void setThermalBuoyancy(float thermalBuoyancy) {
		fluidSolver.setThermalBuoyancy(thermalBuoyancy);
	}

	public float getThermalBuoyancy() {
		return fluidSolver.getThermalBuoyancy();
	}

	public void setViscosity(float viscosity) {
		fluidSolver.setViscosity(viscosity);
	}

	public float getViscosity() {
		return fluidSolver.getViscosity();
	}

	public void setSunny(boolean sunny) {
		this.sunny = sunny;
		if (sunny) {
			radiative = true;
		} else {
			photons.clear();
		}
	}

	public boolean isSunny() {
		return sunny;
	}

	public void setSunAngle(float sunAngle) {
		if (Math.abs(sunAngle - raySolver.getSunAngle()) < 0.001f)
			return;
		photons.clear();
		raySolver.setSunAngle(sunAngle);
	}

	public float getSunAngle() {
		return raySolver.getSunAngle();
	}

	public void setSolarPowerDensity(float solarPowerDensity) {
		raySolver.setSolarPowerDensity(solarPowerDensity);
	}

	public float getSolarPowerDensity() {
		return raySolver.getSolarPowerDensity();
	}

	public void setSolarRayCount(int solarRayCount) {
		if (solarRayCount == raySolver.getSolarRayCount())
			return;
		photons.clear();
		raySolver.setSolarRayCount(solarRayCount);
	}

	public int getSolarRayCount() {
		return raySolver.getSolarRayCount();
	}

	public void setRaySpeed(float raySpeed) {
		raySolver.setRaySpeed(raySpeed);
	}

	public float getRaySpeed() {
		return raySolver.getRaySpeed();
	}

	public void setPhotonEmissionInterval(int photonEmissionInterval) {
		this.photonEmissionInterval = photonEmissionInterval;
	}

	public int getPhotonEmissionInterval() {
		return photonEmissionInterval;
	}

	public void addPhoton(Photon p) {
		if (p != null)
			photons.add(p);
	}

	public void removePhoton(Photon p) {
		photons.remove(p);
	}

	public List<Photon> getPhotons() {
		return photons;
	}

	private void setGridCellSize() {
		heatSolver.setGridCellSize(deltaX, deltaY);
		fluidSolver.setGridCellSize(deltaX, deltaY);
		raySolver.setGridCellSize(deltaX, deltaY);
	}

	public void setLx(float lx) {
		this.lx = lx;
		deltaX = lx / nx;
		setGridCellSize();
	}

	public float getLx() {
		return lx;
	}

	public void setLy(float ly) {
		this.ly = ly;
		deltaY = ly / ny;
		setGridCellSize();
	}

	public float getLy() {
		return ly;
	}

	public HeatBoundary getHeatBoundary() {
		return heatSolver.getBoundary();
	}

	public void setHeatBoundary(HeatBoundary b) {
		heatSolver.setBoundary(b);
	}

	public void setBackgroundTemperature(float backgroundTemperature) {
		this.backgroundTemperature = backgroundTemperature;
	}

	public float getBackgroundTemperature() {
		return backgroundTemperature;
	}

	public void setBackgroundConductivity(float backgroundConductivity) {
		this.backgroundConductivity = backgroundConductivity;
	}

	public float getBackgroundConductivity() {
		return backgroundConductivity;
	}

	public void setBackgroundCapacity(float backgroundCapacity) {
		this.backgroundCapacity = backgroundCapacity;
	}

	public float getBackgroundCapacity() {
		return backgroundCapacity;
	}

	public void setBackgroundDensity(float backgroundDensity) {
		this.backgroundDensity = backgroundDensity;
	}

	public float getBackgroundDensity() {
		return backgroundDensity;
	}

	public void addThermometer(float x, float y) {
		Thermometer t = new Thermometer(x, y);
		Rectangle2D.Float r = (Rectangle2D.Float) t.getShape();
		r.width = 0.025f * lx;
		r.height = 0.05f * ly;
		t.setLocation(x, y);
		thermometers.add(t);
	}

	public List<Thermometer> getThermometers() {
		return thermometers;
	}

	public void addRectangularPart(float x, float y, float w, float h) {
		Part p = new Part(new Rectangle2D.Float(x, y, w, h));
		addPart(p);
	}

	public void addEllipticalPart(float x, float y, float a, float b) {
		Part p = new Part(new Ellipse2D.Float(x - 0.5f * a, y - 0.5f * b, a, b));
		addPart(p);
	}

	public void addRingPart(float x, float y, float inner, float outer) {
		Area area = new Area(new Ellipse2D.Float(x - 0.5f * outer, y - 0.5f
				* outer, outer, outer));
		area.subtract(new Area(new Ellipse2D.Float(x - 0.5f * inner, y - 0.5f
				* inner, inner, inner)));
		Part p = new Part(area);
		addPart(p);
	}

	public void addPolygonPart(float[] x, float[] y) {
		Part p = new Part(new Polygon2D(x, y));
		addPart(p);
	}

	public List<Part> getParts() {
		return parts;
	}

	public Part getPart(int i) {
		if (i < 0 || i >= parts.size())
			return null;
		return parts.get(i);
	}

	public int getPartCount() {
		return parts.size();
	}

	public void addPart(Part p) {
		if (!parts.contains(p)) {
			parts.add(p);
			if (p.getPower() != 0)
				hasPartPower = true;
			if (p.getEmissivity() > 0)
				radiative = true;
		}
	}

	public void removePart(Part p) {
		parts.remove(p);
		checkPartPower();
		checkPartRadiation();
	}

	public void refreshMaterialPropertyArrays() {
		float x, y, windSpeed;
		boolean initial = indexOfStep == 0;
		for (int i = 0; i < nx; i++) {
			x = i * deltaX;
			for (int j = 0; j < ny; j++) {
				y = j * deltaY;
				conductivity[i][j] = backgroundConductivity;
				capacity[i][j] = backgroundCapacity;
				density[i][j] = backgroundDensity;
				fluidity[i][j] = true;
				uWind[i][j] = vWind[i][j] = 0;
				synchronized (parts) {
					for (Part p : parts) {
						if (p.getShape().contains(x, y)) {
							// no overlap of parts will be allowed
							conductivity[i][j] = p.getConductivity();
							capacity[i][j] = p.getCapacity();
							density[i][j] = p.getDensity();
							if (!initial && p.getConstantTemperature()
									&& !Float.isNaN(p.getTemperature()))
								t[i][j] = p.getTemperature();
							fluidity[i][j] = false;
							if ((windSpeed = p.getWindSpeed()) != 0) {
								uWind[i][j] = (float) (windSpeed * Math.cos(p
										.getWindAngle()));
								vWind[i][j] = (float) (windSpeed * Math.sin(p
										.getWindAngle()));
							}
							break;
						}
					}
				}
			}
		}
		if (initial) {
			setInitialTemperature();
			setInitialVelocity();
		}
	}

	public void refreshPowerArray() {
		float x, y;
		for (int i = 0; i < nx; i++) {
			x = i * deltaX;
			for (int j = 0; j < ny; j++) {
				y = j * deltaY;
				q[i][j] = 0;
				if (hasPartPower) {
					synchronized (parts) {
						for (Part p : parts) {
							if (p.getPower() != 0
									&& p.getShape().contains(x, y)) {
								// no overlap of parts will be allowed
								q[i][j] = p.getPower();
								break;
							}
						}
					}
				}
			}
		}
	}

	public void refreshTemperatureBoundaryArray() {
		float x, y;
		for (int i = 0; i < nx; i++) {
			x = i * deltaX;
			for (int j = 0; j < ny; j++) {
				y = j * deltaY;
				tb[i][j] = Float.NaN;
				synchronized (parts) {
					for (Part p : parts) {
						if (Float.isNaN(p.getTemperature()))
							continue;
						if (p.getConstantTemperature()
								&& p.getShape().contains(x, y)) {
							tb[i][j] = p.getTemperature();
							break;
						}
					}
				}
			}
		}
	}

	private void init() {
		for (int i = 0; i < nx; i++) {
			Arrays.fill(conductivity[i], backgroundConductivity);
			Arrays.fill(capacity[i], backgroundCapacity);
			Arrays.fill(density[i], backgroundDensity);
		}
		setInitialTemperature();
	}

	public void clear() {
		parts.clear();
		photons.clear();
		thermometers.clear();
	}

	private void setInitialVelocity() {
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				if (fluidity[i][j]) {
					u[i][j] = v[i][j] = 0;
				} else {
					u[i][j] = uWind[i][j];
					v[i][j] = vWind[i][j];
				}
			}
		}
	}

	public void setInitialTemperature() {
		if (parts == null) {
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					t[i][j] = backgroundTemperature;
				}
			}
		} else {
			float x, y;
			boolean found = false;
			for (int i = 0; i < nx; i++) {
				x = i * deltaX;
				for (int j = 0; j < ny; j++) {
					y = j * deltaY;
					found = false;
					synchronized (parts) {
						for (Part p : parts) {
							if (p.getShape().contains(x, y)) {
								// no overlap of parts will be allowed
								if (!Float.isNaN(p.getTemperature())) {
									t[i][j] = p.getTemperature();
									found = true;
								}
								break;
							}
						}
					}
					if (!found)
						t[i][j] = backgroundTemperature;
				}
			}
		}
		if (thermometers != null && !thermometers.isEmpty()) {
			synchronized (thermometers) {
				for (Thermometer t : thermometers) {
					t.clear();
				}
			}
		}
	}

	public void run() {
		checkPartPower();
		checkPartRadiation();
		refreshPowerArray();
		if (!running) {
			running = true;
			while (running)
				nextStep();
			if (notifyReset) {
				indexOfStep = 0;
				reallyReset();
				notifyVisualizationListeners();
				notifyReset = false;
			}
		}
	}

	public void stop() {
		running = false;
	}

	public void reset() {
		if (running) {
			stop();
			notifyReset = true;
		} else {
			reallyReset();
		}
		running = false;
		indexOfStep = 0;
	}

	private void reallyReset() {
		setInitialTemperature();
		setInitialVelocity();
		photons.clear();
		heatSolver.reset();
		fluidSolver.reset();
	}

	private void checkPartPower() {
		hasPartPower = false;
		synchronized (parts) {
			for (Part p : parts) {
				if (p.getPower() != 0) {
					hasPartPower = true;
					break;
				}
			}
		}
	}

	private void checkPartRadiation() {
		radiative = sunny;
		if (!radiative) {
			synchronized (parts) {
				for (Part p : parts) {
					if (p.getEmissivity() > 0) {
						radiative = true;
						break;
					}
				}
			}
		}
	}

	private void nextStep() {
		if (indexOfStep % viewUpdateInterval == 0) {
			notifyVisualizationListeners();
			takeMeasurement();
		}
		if (radiative) {
			if (indexOfStep % photonEmissionInterval == 0) {
				refreshPowerArray();
				if (sunny)
					raySolver.sunShine(photons, parts);
				raySolver.radiate(this);
			}
			raySolver.solve(this);
		}
		if (convective) {
			fluidSolver.solve(u, v);
		}
		heatSolver.solve(convective, t);
		indexOfStep++;
	}

	public void setViewUpdateInterval(int viewUpdateInterval) {
		this.viewUpdateInterval = viewUpdateInterval;
	}

	public int getViewUpdateInterval() {
		return viewUpdateInterval;
	}

	public float getTime() {
		return indexOfStep * heatSolver.getTimeStep();
	}

	public void setTimeStep(float timeStep) {
		if (getTimeStep() != timeStep)
			notifyPropertyChangeListeners("Time step", getTimeStep(), timeStep);
		heatSolver.setTimeStep(timeStep);
		fluidSolver.setTimeStep(timeStep);
	}

	public float getTimeStep() {
		return heatSolver.getTimeStep();
	}

	public void setTemperature(float[][] t) {
		this.t = t;
	}

	public float getTemperatureAt(float x, float y) {
		int i = Math.min(t.length - 1, Math.round(x / deltaX));
		int j = Math.min(t[0].length - 1, Math.round(y / deltaY));
		return t[i][j];
	}

	public void setTemperatureAt(float x, float y, float temperature) {
		int i = Math.min(t.length - 1, Math.round(x / deltaX));
		int j = Math.min(t[0].length - 1, Math.round(y / deltaY));
		t[i][j] = temperature;
	}

	float getAverageTemperatureAt(float x, float y) {
		float temp = 0;
		int i0 = Math.round(x / deltaX);
		int j0 = Math.round(y / deltaY);
		int i = Math.min(t.length - 1, i0);
		int j = Math.min(t[0].length - 1, j0);
		temp += t[i][j];
		i = Math.min(t.length - 1, i0 + 1);
		j = Math.min(t[0].length - 1, j0);
		temp += t[i][j];
		i = Math.min(t.length - 1, i0 - 1);
		j = Math.min(t[0].length - 1, j0);
		temp += t[i][j];
		i = Math.min(t.length - 1, i0);
		j = Math.min(t[0].length - 1, j0 + 1);
		temp += t[i][j];
		i = Math.min(t.length - 1, i0);
		j = Math.min(t[0].length - 1, j0 - 1);
		temp += t[i][j];
		return temp * 0.2f;
	}

	void changeAverageTemperatureAt(float x, float y, float increment) {
		increment *= 0.2f;
		int i0 = Math.round(x / deltaX);
		int j0 = Math.round(y / deltaY);
		int i = Math.min(t.length - 1, i0);
		int j = Math.min(t[0].length - 1, j0);
		t[i][j] += increment;
		i = Math.min(t.length - 1, i0 + 1);
		j = Math.min(t[0].length - 1, j0);
		t[i][j] += increment;
		i = Math.min(t.length - 1, i0 - 1);
		j = Math.min(t[0].length - 1, j0);
		t[i][j] += increment;
		i = Math.min(t.length - 1, i0);
		j = Math.min(t[0].length - 1, j0 + 1);
		t[i][j] += increment;
		i = Math.min(t.length - 1, i0);
		j = Math.min(t[0].length - 1, j0 - 1);
		t[i][j] += increment;
	}

	public float[][] getTemperature() {
		return t;
	}

	public float[][] getXVelocity() {
		return u;
	}

	public float[][] getYVelocity() {
		return v;
	}

	private void takeMeasurement() {
		if (!thermometers.isEmpty()) {
			int ix, iy;
			synchronized (thermometers) {
				for (Thermometer m : thermometers) {
					ix = Math.round(m.getX() / deltaX);
					iy = Math.round(m.getY() / deltaY);
					if (ix >= 0 && ix < nx && iy >= 0 && iy < ny)
						m.addData(getTime(), t[ix][iy]);
				}
			}
		}
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (!propertyChangeListeners.contains(listener))
			propertyChangeListeners.add(listener);
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null)
			propertyChangeListeners.remove(listener);
	}

	private void notifyPropertyChangeListeners(String propertyName,
			Object oldValue, Object newValue) {
		if (propertyChangeListeners.isEmpty())
			return;
		PropertyChangeEvent e = new PropertyChangeEvent(this, propertyName,
				oldValue, newValue);
		for (PropertyChangeListener x : propertyChangeListeners)
			x.propertyChange(e);
	}

	public void addVisualizationListener(VisualizationListener listener) {
		if (!visualizationListeners.contains(listener))
			visualizationListeners.add(listener);
	}

	public void removeVisualizationListener(VisualizationListener listener) {
		if (listener != null)
			visualizationListeners.remove(listener);
	}

	private void notifyVisualizationListeners() {
		if (visualizationListeners.isEmpty())
			return;
		VisualizationEvent e = new VisualizationEvent(this);
		for (VisualizationListener x : visualizationListeners)
			x.visualizationRequested(e);
	}

}