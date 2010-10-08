package net.lidskialf.kif;

import java.awt.image.BufferedImage;

import org.zmpp.blorb.NativeImage;

public class AwtImage implements NativeImage {		
	private BufferedImage image;
	
	public AwtImage(BufferedImage image) { 
		this.image = image; 
	}
	
	public BufferedImage getImage() { 
		return image; 
	}
	
	public int getWidth() { 
		return image.getWidth(); 
	}
	
	public int getHeight() { 
		return image.getHeight(); 
	}
}
