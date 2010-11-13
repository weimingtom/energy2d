package org.concord.energy2d.system;

import java.awt.Color;

import org.concord.energy2d.model.Boundary;
import org.concord.energy2d.model.Constants;
import org.concord.energy2d.model.DirichletHeatBoundary;
import org.concord.energy2d.model.HeatBoundary;
import org.concord.energy2d.model.Model2D;
import org.concord.energy2d.model.NeumannHeatBoundary;
import org.concord.energy2d.model.Part;
import org.concord.energy2d.util.Scripter;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Charles Xie
 * 
 */
class XmlDecoder extends DefaultHandler {

	private System2D box;
	private String str;

	// model properties
	private float modelWidth = 10;
	private float modelHeight = 10;
	private float timeStep = 1;
	private int measurementInterval = 500;
	private int viewUpdateInterval = 100;
	private boolean sunny;
	private float sunAngle = (float) Math.PI * 0.5f;
	private float solarPowerDensity = 2000;
	private int solarRayCount = 24;
	private float solarRaySpeed = 0.1f;
	private int photonEmissionInterval = 20;
	private boolean convective = true;
	private float backgroundConductivity = Constants.AIR_THERMAL_CONDUCTIVITY;
	private float backgroundDensity = Constants.AIR_DENSITY;
	private float backgroundSpecificHeat = Constants.AIR_SPECIFIC_HEAT;
	private float backgroundViscosity = Constants.AIR_VISCOSITY;
	private float backgroundTemperature;
	private float thermalBuoyancy;
	private byte buoyancyApproximation = Model2D.BUOYANCY_AVERAGE_COLUMN;

	// view properties
	private boolean ruler;
	private boolean grid;
	private boolean isotherm;
	private boolean streamline;
	private boolean rainbow;
	private int rainbowX, rainbowY, rainbowW, rainbowH;
	private boolean velocity;
	private boolean clock = true;
	private boolean smooth = true;
	private float minimumTemperature;
	private float maximumTemperature = 40;

	// part properties
	private float partThermalConductivity = Float.NaN;
	private float partSpecificHeat = Float.NaN;
	private float partDensity = Float.NaN;
	private float partEmissivity = Float.NaN;
	private float partAbsorption = Float.NaN;
	private float partReflection = Float.NaN;
	private float partTransmission = Float.NaN;
	private float partTemperature = Float.NaN;
	private float partWindSpeed;
	private float partWindAngle;
	private boolean partConstantTemperature = true;
	private float partPower = Float.NaN;
	private boolean partFilled = true;
	private boolean partVisible = true;
	private boolean partDraggable = true;
	private Color partColor = Color.gray;
	private Part part;

	XmlDecoder(System2D box) {
		this.box = box;
	}

	public void startDocument() {
	}

	public void endDocument() {

		box.model.setLx(modelWidth);
		box.model.setLy(modelHeight);
		box.view.setArea(0, modelWidth, 0, modelHeight);
		box.model.setTimeStep(timeStep);
		box.model.setMeasurementInterval(measurementInterval);
		box.model.setViewUpdateInterval(viewUpdateInterval);
		box.model.setSunny(sunny);
		box.model.setSunAngle(sunAngle);
		box.model.setSolarPowerDensity(solarPowerDensity);
		box.model.setSolarRayCount(solarRayCount);
		box.model.setSolarRaySpeed(solarRaySpeed);
		box.model.setPhotonEmissionInterval(photonEmissionInterval);
		box.model.setConvective(convective);
		box.model.setBackgroundConductivity(backgroundConductivity);
		box.model.setBackgroundDensity(backgroundDensity);
		box.model.setBackgroundSpecificHeat(backgroundSpecificHeat);
		box.model.setBackgroundTemperature(backgroundTemperature);
		box.model.setBackgroundViscosity(backgroundViscosity);
		box.model.setThermalBuoyancy(thermalBuoyancy);
		box.model.setBuoyancyApproximation(buoyancyApproximation);

		box.view.setRulerOn(ruler);
		box.view.setGridOn(grid);
		box.view.setIsothermOn(isotherm);
		box.view.setStreamlineOn(streamline);
		box.view.setVelocityOn(velocity);
		box.view.setRainbowOn(rainbow);
		box.view.setRainbowRectangle(rainbowX, rainbowY, rainbowW, rainbowH);
		box.view.setMinimumTemperature(minimumTemperature);
		box.view.setMaximumTemperature(maximumTemperature);
		box.view.setClockOn(clock);
		box.view.setSmooth(smooth);

		box.model.refreshPowerArray();
		box.model.refreshTemperatureBoundaryArray();
		box.model.refreshMaterialPropertyArrays();
		box.model.setInitialTemperature();
		box.view.repaint();

	}

	public void startElement(String uri, String localName, String qName,
			Attributes attrib) {

		String attribName, attribValue;

		if (qName == "rectangle") {
			if (attrib != null) {
				float x = Float.NaN, y = Float.NaN, w = Float.NaN, h = Float.NaN;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "x") {
						x = Float.parseFloat(attribValue);
					} else if (attribName == "y") {
						y = Float.parseFloat(attribValue);
					} else if (attribName == "width") {
						w = Float.parseFloat(attribValue);
					} else if (attribName == "height") {
						h = Float.parseFloat(attribValue);
					}
				}
				if (!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(w)
						&& !Float.isNaN(h))
					part = box.model.addRectangularPart(x, y, w, h);
			}
		} else if (qName == "ellipse") {
			if (attrib != null) {
				float x = Float.NaN, y = Float.NaN, a = Float.NaN, b = Float.NaN;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "x") {
						x = Float.parseFloat(attribValue);
					} else if (attribName == "y") {
						y = Float.parseFloat(attribValue);
					} else if (attribName == "a") {
						a = Float.parseFloat(attribValue);
					} else if (attribName == "b") {
						b = Float.parseFloat(attribValue);
					}
				}
				if (!Float.isNaN(x) && !Float.isNaN(y) && !Float.isNaN(a)
						&& !Float.isNaN(b))
					part = box.model.addEllipticalPart(x, y, a, b);
			}
		} else if (qName == "polygon") {
			if (attrib != null) {
				int count = -1;
				String vertices = null;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "count") {
						count = Integer.parseInt(attribValue);
					} else if (attribName == "vertices") {
						vertices = attribValue;
					}
				}
				if (count > 0 && vertices != null) {
					float[] v = Scripter.parseArray(count * 2, vertices);
					float[] x = new float[count];
					float[] y = new float[count];
					for (int i = 0; i < count; i++) {
						x[i] = v[2 * i];
						y[i] = v[2 * i + 1];
					}
					part = box.model.addPolygonPart(x, y);
				}
			}
		} else if (qName == "temperature_at_border") {
			if (attrib != null) {
				float left = Float.NaN, right = Float.NaN, upper = Float.NaN, lower = Float.NaN;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "left") {
						left = Float.parseFloat(attribValue);
					} else if (attribName == "right") {
						right = Float.parseFloat(attribValue);
					} else if (attribName == "upper") {
						upper = Float.parseFloat(attribValue);
					} else if (attribName == "lower") {
						lower = Float.parseFloat(attribValue);
					}
				}
				if (!Float.isNaN(left) && !Float.isNaN(right)
						&& !Float.isNaN(upper) && !Float.isNaN(lower)) {
					DirichletHeatBoundary b = null;
					HeatBoundary boundary = box.model.getHeatBoundary();
					if (boundary instanceof DirichletHeatBoundary) {
						b = (DirichletHeatBoundary) boundary;
					} else {
						b = new DirichletHeatBoundary();
						box.model.setHeatBoundary(b);
					}
					b.setTemperatureAtBorder(Boundary.UPPER, upper);
					b.setTemperatureAtBorder(Boundary.RIGHT, right);
					b.setTemperatureAtBorder(Boundary.LOWER, lower);
					b.setTemperatureAtBorder(Boundary.LEFT, left);
				}
			}
		} else if (qName == "flux_at_border") {
			if (attrib != null) {
				float left = Float.NaN, right = Float.NaN, upper = Float.NaN, lower = Float.NaN;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "left") {
						left = Float.parseFloat(attribValue);
					} else if (attribName == "right") {
						right = Float.parseFloat(attribValue);
					} else if (attribName == "upper") {
						upper = Float.parseFloat(attribValue);
					} else if (attribName == "lower") {
						lower = Float.parseFloat(attribValue);
					}
				}
				if (!Float.isNaN(left) && !Float.isNaN(right)
						&& !Float.isNaN(upper) && !Float.isNaN(lower)) {
					NeumannHeatBoundary b = null;
					HeatBoundary boundary = box.model.getHeatBoundary();
					if (boundary instanceof NeumannHeatBoundary) {
						b = (NeumannHeatBoundary) boundary;
					} else {
						b = new NeumannHeatBoundary();
						box.model.setHeatBoundary(b);
					}
					b.setFluxAtBorder(Boundary.UPPER, upper);
					b.setFluxAtBorder(Boundary.RIGHT, right);
					b.setFluxAtBorder(Boundary.LOWER, lower);
					b.setFluxAtBorder(Boundary.LEFT, left);
				}
			}
		} else if (qName == "thermometer") {
			if (attrib != null) {
				float x = Float.NaN, y = Float.NaN;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "x") {
						x = Float.parseFloat(attribValue);
					} else if (attribName == "y") {
						y = Float.parseFloat(attribValue);
					}
				}
				if (!Float.isNaN(x) && !Float.isNaN(y))
					box.model.addThermometer(x, y);
			}
		} else if (qName == "text") {
			if (attrib != null) {
				float x = Float.NaN, y = Float.NaN;
				String str = null;
				for (int i = 0, n = attrib.getLength(); i < n; i++) {
					attribName = attrib.getQName(i).intern();
					attribValue = attrib.getValue(i);
					if (attribName == "x") {
						x = Float.parseFloat(attribValue);
					} else if (attribName == "y") {
						y = Float.parseFloat(attribValue);
					} else if (attribName == "string") {
						str = attribValue;
					}
				}
				if (!Float.isNaN(x) && !Float.isNaN(y))
					box.view.addText(str, x, y);
			}
		}

	}

	public void endElement(String uri, String localName, String qName) {

		if (qName == "model_width") {
			modelWidth = Float.parseFloat(str);
		} else if (qName == "model_height") {
			modelHeight = Float.parseFloat(str);
		} else if (qName == "timestep") {
			timeStep = Float.parseFloat(str);
		} else if (qName == "measurement_interval") {
			measurementInterval = Integer.parseInt(str);
		} else if (qName == "viewupdate_interval") {
			viewUpdateInterval = Integer.parseInt(str);
		} else if (qName == "sunny") {
			sunny = Boolean.parseBoolean(str);
		} else if (qName == "sun_angle") {
			sunAngle = Float.parseFloat(str);
		} else if (qName == "solar_power_density") {
			solarPowerDensity = Float.parseFloat(str);
		} else if (qName == "solar_ray_count") {
			solarRayCount = Integer.parseInt(str);
		} else if (qName == "solar_ray_speed") {
			solarRaySpeed = Float.parseFloat(str);
		} else if (qName == "photon_emission_interval") {
			photonEmissionInterval = Integer.parseInt(str);
		} else if (qName == "convective") {
			convective = Boolean.parseBoolean(str);
		} else if (qName == "background_conductivity") {
			backgroundConductivity = Float.parseFloat(str);
		} else if (qName == "background_density") {
			backgroundDensity = Float.parseFloat(str);
		} else if (qName == "background_specific_heat") {
			backgroundSpecificHeat = Float.parseFloat(str);
		} else if (qName == "background_temperature") {
			backgroundTemperature = Float.parseFloat(str);
		} else if (qName == "background_viscosity") {
			backgroundViscosity = Float.parseFloat(str);
		} else if (qName == "thermal_buoyancy") {
			thermalBuoyancy = Float.parseFloat(str);
		} else if (qName == "buoyancy_approximation") {
			buoyancyApproximation = Byte.parseByte(str);
		} else if (qName == "minimum_temperature") {
			minimumTemperature = Float.parseFloat(str);
		} else if (qName == "maximum_temperature") {
			maximumTemperature = Float.parseFloat(str);
		} else if (qName == "ruler") {
			ruler = Boolean.parseBoolean(str);
		} else if (qName == "isotherm") {
			isotherm = Boolean.parseBoolean(str);
		} else if (qName == "streamline") {
			streamline = Boolean.parseBoolean(str);
		} else if (qName == "velocity") {
			velocity = Boolean.parseBoolean(str);
		} else if (qName == "grid") {
			grid = Boolean.parseBoolean(str);
		} else if (qName == "rainbow") {
			rainbow = Boolean.parseBoolean(str);
		} else if (qName == "rainbow_x") {
			rainbowX = Integer.parseInt(str);
		} else if (qName == "rainbow_y") {
			rainbowY = Integer.parseInt(str);
		} else if (qName == "rainbow_w") {
			rainbowW = Integer.parseInt(str);
		} else if (qName == "rainbow_h") {
			rainbowH = Integer.parseInt(str);
		} else if (qName == "clock") {
			clock = Boolean.parseBoolean(str);
		} else if (qName == "smooth") {
			smooth = Boolean.parseBoolean(str);
		} else if (qName == "thermal_conductivity") {
			partThermalConductivity = Float.parseFloat(str);
		} else if (qName == "specific_heat") {
			partSpecificHeat = Float.parseFloat(str);
		} else if (qName == "density") {
			partDensity = Float.parseFloat(str);
		} else if (qName == "emissivity") {
			partEmissivity = Float.parseFloat(str);
		} else if (qName == "absorption") {
			partAbsorption = Float.parseFloat(str);
		} else if (qName == "reflection") {
			partReflection = Float.parseFloat(str);
		} else if (qName == "transmission") {
			partTransmission = Float.parseFloat(str);
		} else if (qName == "temperature") {
			partTemperature = Float.parseFloat(str);
		} else if (qName == "constant_temperature") {
			partConstantTemperature = Boolean.parseBoolean(str);
		} else if (qName == "power") {
			partPower = Float.parseFloat(str);
		} else if (qName == "wind_speed") {
			partWindSpeed = Float.parseFloat(str);
		} else if (qName == "wind_angle") {
			partWindAngle = Float.parseFloat(str);
		} else if (qName == "color") {
			partColor = new Color(Integer.parseInt(str, 16));
		} else if (qName == "filled") {
			partFilled = Boolean.parseBoolean(str);
		} else if (qName == "visible") {
			partVisible = Boolean.parseBoolean(str);
		} else if (qName == "draggable") {
			partDraggable = Boolean.parseBoolean(str);
		} else if (qName == "boundary") {
			// nothing to do at this point
		} else if (qName == "part") {
			if (part != null) {
				if (!Float.isNaN(partThermalConductivity))
					part.setThermalConductivity(partThermalConductivity);
				if (!Float.isNaN(partSpecificHeat))
					part.setSpecificHeat(partSpecificHeat);
				if (!Float.isNaN(partDensity))
					part.setDensity(partDensity);
				if (!Float.isNaN(partTemperature))
					part.setTemperature(partTemperature);
				if (!Float.isNaN(partPower))
					part.setPower(partPower);
				if (!Float.isNaN(partEmissivity))
					part.setEmissivity(partEmissivity);
				if (!Float.isNaN(partAbsorption))
					part.setAbsorption(partAbsorption);
				if (!Float.isNaN(partReflection))
					part.setReflection(partReflection);
				if (!Float.isNaN(partTransmission))
					part.setTransmission(partTransmission);
				part.setWindAngle(partWindAngle);
				part.setWindSpeed(partWindSpeed);
				part.setConstantTemperature(partConstantTemperature);
				part.setDraggable(partDraggable);
				part.setVisible(partVisible);
				part.setFilled(partFilled);
				part.setColor(partColor);
				resetPartVariables();
			}
		}

	}

	private void resetPartVariables() {
		partThermalConductivity = Float.NaN;
		partSpecificHeat = Float.NaN;
		partDensity = Float.NaN;
		partTemperature = Float.NaN;
		partConstantTemperature = true;
		partPower = Float.NaN;
		partEmissivity = Float.NaN;
		partAbsorption = Float.NaN;
		partReflection = Float.NaN;
		partTransmission = Float.NaN;
		partWindSpeed = 0;
		partWindAngle = 0;
		partFilled = true;
		partVisible = true;
		partDraggable = true;
		partColor = Color.gray;
	}

	public void characters(char[] ch, int start, int length) {
		str = new String(ch, start, length);
	}

	public void warning(SAXParseException e) {
		e.printStackTrace();
	}

	public void error(SAXParseException e) {
		e.printStackTrace();
	}

	public void fatalError(SAXParseException e) {
		e.printStackTrace();
	}

}
