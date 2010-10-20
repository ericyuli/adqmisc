package net.lidskialf.mangle;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;

import com.amazon.kindle.kindlet.ui.KComponent;

public class ImageComponent extends KComponent implements ImageObserver {

	private static final long serialVersionUID = -1025892776027810043L;
	
	private Image img;
	private Manglet manglet;
	private boolean imgComplete = false;
	
	public ImageComponent(Manglet manglet) {
		this.manglet = manglet;
	}
	
	public void setImage(Image newImg) {
		synchronized (this) {
			this.img = newImg;
			this.imgComplete = false;
	
			if ((Toolkit.getDefaultToolkit().checkImage(img, -1, -1, this) & ImageObserver.ALLBITS) != 0) {
				this.imgComplete = true;
				repaint();
			} else {
				manglet.setBusyIndicator(true);
			}
		}
	}

	public void paint(Graphics g) {
		if (!imgComplete)
			return;
		
		synchronized (this) {
			int xoff = (getWidth() - img.getWidth(null)) / 2;
			int yoff = (getHeight() - img.getHeight(null)) / 2;
			g.drawImage(img, xoff, yoff, null);
		}
	}
	
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
		if (img != this.img)
			return false;

		if ((infoflags & ImageObserver.ALLBITS) != 0) {
			manglet.getLogger().error("ALLBITS");
			imgComplete = true;
			manglet.setBusyIndicator(false);
			repaint();
			return false;
		}

		manglet.getLogger().error("XXX");
		manglet.setBusyIndicator(true);
		return true;
	}
}
