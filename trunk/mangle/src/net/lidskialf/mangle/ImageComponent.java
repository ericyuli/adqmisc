package net.lidskialf.mangle;

import java.awt.Graphics;
import java.awt.Image;

import com.amazon.kindle.kindlet.ui.KComponent;

public class ImageComponent extends KComponent {

	private static final long serialVersionUID = -1025892776027810043L;
	
	private ImageLoader scaledImg;
	private Manglet manglet;
	
	public ImageComponent(Manglet manglet) {
		this.manglet = manglet;
	}
	
	public void setImage(ImageLoader newImg) {
		synchronized (this) {
			this.scaledImg = newImg;
			this.scaledImg.setImageComponent(this);
	
			if (this.scaledImg.getDisplayImage() != null) {
				repaint();
			} else {
				manglet.setBusyIndicator(true);				
			}
		}
	}
	
	public void imageReady() {
		manglet.setBusyIndicator(false);
		repaint();
	}

	public void paint(Graphics g) {		
		synchronized (this) {
			Image img = scaledImg.getDisplayImage();
			if (img == null)
				return;
			
			int xoff = (getWidth() - img.getWidth(null)) / 2;
			int yoff = (getHeight() - img.getHeight(null)) / 2;
			g.drawImage(img, xoff, yoff, null);
		}
	}
}
