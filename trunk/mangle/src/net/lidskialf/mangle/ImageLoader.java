package net.lidskialf.mangle;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;

public class ImageLoader implements ImageObserver {
	
	private Image sourceImage;
	private Image displayImage;
	private ImageComponent ic;
	private int destWidth;
	private int destHeight;
	private boolean displayImageComplete = false;
	private boolean aborted = false;

	public ImageLoader(Image sourceImage, int destWidth, int destHeight) {
		this.sourceImage = sourceImage;
		this.destWidth = destWidth;
		this.destHeight = destHeight;
		
		Toolkit.getDefaultToolkit().prepareImage(sourceImage, -1, -1, this);
	}

	public Image getDisplayImage() {
		if (!displayImageComplete)
			return null;
		return displayImage;
	}
	
	public void setImageComponent(ImageComponent ic) {
		this.ic = ic;
	}
	
	public void abort() {
		aborted = true;
		
		if (sourceImage != null)
			sourceImage.flush();

		if (displayImage != null)
			displayImage.flush();
	}
	
	public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
		if (aborted)
			return false;

		if (sourceImage != null) {
			if ((infoflags & ImageObserver.ALLBITS) != 0) {
				int imgWidth = img.getWidth(null);
				int imgHeight = img.getHeight(null);
				
				if (imgHeight > imgWidth)
					displayImage = sourceImage.getScaledInstance(-1, destHeight, Image.SCALE_AREA_AVERAGING);
				else
					displayImage = sourceImage.getScaledInstance(destWidth, -1, Image.SCALE_AREA_AVERAGING);

				Toolkit.getDefaultToolkit().prepareImage(displayImage, -1, -1, this);
				sourceImage = null;
				return false;
			}
		}

		if (displayImage != null) {
			if ((infoflags & ImageObserver.ALLBITS) != 0) {
				displayImageComplete = true;
				ic.imageReady();
				return false;
			}
		}
		
		return true;
	}
}
