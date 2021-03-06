/*
 *   Copyright (C) 2009  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 */

package org.concord.energy2d.event;

import java.util.EventObject;

/**
 * @author Charles Xie
 * 
 */
public class ManipulationEvent extends EventObject {

	public static final byte DELETE = 0;
	public static final byte TRANSLATE = 1;
	public static final byte RESIZE = 2;
	public static final byte PROPERTY_CHANGE = 3;
	public static final byte SUN_SHINE = 4;
	public static final byte SUN_ANGLE_INCREASE = 5;
	public static final byte SUN_ANGLE_DECREASE = 6;
	public static final byte OBJECT_ADDED = 7;
	public static final byte RUN = 11;
	public static final byte STOP = 12;
	public static final byte RESET = 13;
	public static final byte RELOAD = 14;
	public static final byte GRID = 15;
	public static final byte GRAPH = 16;
	public static final byte AUTO_STOP = 17;

	private Object target;
	private byte type = -1;

	public ManipulationEvent(Object source, byte type) {
		super(source);
		this.type = type;
	}

	public ManipulationEvent(Object source, Object target, byte type) {
		super(source);
		this.target = target;
		this.type = type;
	}

	public byte getType() {
		return type;
	}

	public Object getTarget() {
		return target;
	}

}
