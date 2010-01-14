/*
 *   Copyright (C) 2009  The Concord Consortium, Inc.,
 *   25 Love Lane, Concord, MA 01742
 */

package org.concord.energy2d.view;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

/**
 * @author Charles Xie
 * 
 */
class ScalarDistributionRenderer {

	private BufferedImage image;
	private int[] pixels;
	private int w, h;
	private float scale = 40f / 255f;
	private short[][] rgbScale;

	ScalarDistributionRenderer(short[][] rgbScale) {
		this.rgbScale = rgbScale;
	}

	void setIntensityScale(float scale) {
		this.scale = scale;
	}

	float getIntensityScale() {
		return scale;
	}

	int getColor(float value) {
		float v = value * scale;
		if (v > rgbScale.length - 2)
			v = rgbScale.length - 2;
		else if (v < 0)
			v = 0;
		int iv = (int) v;
		v -= iv;
		int rc = (int) (rgbScale[iv][0] * (1 - v) + rgbScale[iv + 1][0] * v);
		int gc = (int) (rgbScale[iv][1] * (1 - v) + rgbScale[iv + 1][1] * v);
		int bc = (int) (rgbScale[iv][2] * (1 - v) + rgbScale[iv + 1][2] * v);
		return (255 << 24) | (rc << 16) | (gc << 8) | bc;
	}

	void render(float[][] distribution, JComponent c, Graphics2D g) {

		if (!c.isVisible())
			return;

		w = c.getWidth();
		h = c.getHeight();
		createImage(w, h, c);

		int m = distribution.length;
		int n = distribution[0].length;

		float dx = (float) m / (float) w;
		float dy = (float) n / (float) h;

		int rc = 0, gc = 0, bc = 0;
		int ix, iy, iv;
		float v;
		for (int i = 0; i < w; i++) {
			ix = (int) (i * dx);
			for (int j = 0; j < h; j++) {
				iy = (int) (j * dy);
				v = distribution[ix][iy] * scale;
				if (v > rgbScale.length - 2)
					v = rgbScale.length - 2;
				else if (v < 0)
					v = 0;
				iv = (int) v;
				v -= iv;
				rc = (int) (rgbScale[iv][0] * (1 - v) + rgbScale[iv + 1][0] * v);
				gc = (int) (rgbScale[iv][1] * (1 - v) + rgbScale[iv + 1][1] * v);
				bc = (int) (rgbScale[iv][2] * (1 - v) + rgbScale[iv + 1][2] * v);
				pixels[i + j * w] = (255 << 24) | (rc << 16) | (gc << 8) | bc;
			}
		}
		image.setRGB(0, 0, w, h, pixels, 0, w);
		g.drawImage(image, 0, 0, c);

	}

	private void createImage(int w, int h, JComponent c) {
		if (image != null) {
			if (w != image.getWidth(c) || h != image.getHeight(c)) {
				image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
				pixels = new int[w * h];
			}
		} else {
			image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			pixels = new int[w * h];
		}
	}

}