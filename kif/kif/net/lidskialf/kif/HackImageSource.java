package net.lidskialf.kif;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * Nasty hack in order to load an image from.. wait for it... an inputstream! What a /revolutionary/ concept.
 */
public class HackImageSource extends sun.awt.image.FileImageSource {

	private InputStream is;

	public HackImageSource(InputStream is) {
		super("");
		this.is = is;
	}

	protected sun.awt.image.ImageDecoder getDecoder() {
		return getDecoder(new BufferedInputStream(is));
	}
}
