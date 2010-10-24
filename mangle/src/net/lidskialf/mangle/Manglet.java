package net.lidskialf.mangle;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.amazon.kindle.kindlet.Kindlet;
import com.amazon.kindle.kindlet.KindletContext;
import com.amazon.kindle.kindlet.event.KindleKeyCodes;
import com.amazon.kindle.kindlet.ui.KLabelMultiline;
import com.amazon.kindle.kindlet.ui.KMenu;
import com.amazon.kindle.kindlet.ui.KMenuItem;
import com.amazon.kindle.kindlet.ui.KTextOptionOrientationMenu;
import com.amazon.kindle.kindlet.ui.KTextOptionPane;
import com.amazon.kindle.kindlet.util.Timer;
import com.amazon.kindle.kindlet.util.TimerTask;

public class Manglet implements Kindlet, ComponentListener {
	private Container root;
	
	private Component aboutComponent;
	private ImageComponent mainComponent;
	private static Logger logger;

	private String curSubTitle = "";

	private KindletContext ctx;
	
	private File curSeriesDir = null;
	private String curSeriesName = null;
	
	private String[] cbzs = null;	
	private int curCbzImageCount = 0;
	
	private Image prevImage = null;
	private int curMangaPos = 0;
	private Image curImage = null;
	private Image nextImage = null;
	
	private boolean isFirstImage = true;


	public synchronized Logger getLogger() {
		if (logger != null)
			return logger;

		logger = Logger.getLogger("mangle");
		try {
			logger.addAppender(new FileAppender(new PatternLayout("%m%n"), new File(ctx.getHomeDirectory(), "log.txt").getAbsolutePath()));		
		} catch (Throwable t) {
		}
		return logger;
	}

	
	public void create(KindletContext context) {
		this.ctx = context;
		this.root = ctx.getRootContainer();
		this.aboutComponent = createAboutComponent();
		this.mainComponent = new ImageComponent(this);
		this.root.addComponentListener(this);
		
		initTextOptions();
		installGlobalKeyHandler();
		ctx.setMenu(createMenu());

		loadGlobalState();
		updateCbzs();
		loadCurMangaState();
		if (curImage != null)
			displayImage(curImage);
		showMainComponent();
	}

	public void destroy() {
		if (curSeriesName == null)
			return;
		
		saveGlobalState();
		saveCurMangaState();
	}

	public void start() {
	}

	public void stop() {
	}
	
	private Component createAboutComponent() {
		KLabelMultiline label = new KLabelMultiline("Mangle - a better manga reader for the Kindle!\n" +
													"(c) 2010 Andrew de Quincey\n\n" +
													"Includes KWT, (c) 2010 Dan Fabulich (Apache license).\n\n" +
													"Press Menu to open a manga.\n" +
													"Use the page navigation keys to change page.\n" +
													"Use SHIFT+page navigation keys to change chapter.\n");
		return label;
	}
	
	private KMenu createMenu() {
		KMenu menu = new KMenu();

		KMenuItem menuItem = new KMenuItem("Open manga");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showMangaSelector();
			}			
		});
		menu.add(menuItem);

		menuItem = new KMenuItem("About");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showPanel(aboutComponent, "About mangle");
			}			
		});
		menu.add(menuItem);

		return menu;
	}

	private void initTextOptions() {
		KTextOptionPane textOptions = new KTextOptionPane();
		textOptions.addOrientationMenu(new KTextOptionOrientationMenu());
		ctx.setTextOptionPane(textOptions);
	}

	private void showPanel(Component panel, String title) {
		root.removeAll();
		root.add(panel, BorderLayout.CENTER);
		if (panel instanceof Container) {
			Container container = (Container) panel;
			if (container.getComponentCount() > 0)
				container.getComponent(0).requestFocus();
		}
		ctx.setSubTitle(title);
	}

	private void showMainComponent() {
		root.removeAll();

		if (curImage == null) {
			root.add(aboutComponent);
		} else {
			root.add(mainComponent);
			root.repaint();
		}

		ctx.setSubTitle(curSubTitle);
	}


	private void installGlobalKeyHandler() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			public boolean dispatchKeyEvent(KeyEvent e) {
				if (root.getComponentCount() == 0)
					return false;

				Component displayed = root.getComponent(0);
				
				if (e.isConsumed())
					return false;
				
				switch(e.getKeyCode()) {
				case KindleKeyCodes.VK_BACK:
					// if we're not on the main screen when we get a "BACK", trap it and return to the main game screen
					if ((displayed != mainComponent) || ((curImage == null) && (displayed != aboutComponent))) {
						showMainComponent();
						e.consume();
						return true;
					}
					break;
				
				case KindleKeyCodes.VK_LEFT_HAND_SIDE_TURN_PAGE:
				case KindleKeyCodes.VK_RIGHT_HAND_SIDE_TURN_PAGE: {
					if (e.getID() == KeyEvent.KEY_RELEASED) {
						
						int tmp = -1;
						if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
							tmp = nextImagePos(curMangaPos, true);
							if (tmp != -1) {
								curMangaPos = tmp;
								prevImage = curImage;
								curImage = nextImage;
								nextImage = null;
								
								displayImage(curImage);
								loadMissingImages();
							}
						} else {
							tmp = nextMangaPos(curMangaPos, true);
							if (tmp != -1) {
								curMangaPos = tmp;
								prevImage = null;
								curImage = null;
								nextImage = null;
								
								loadMissingImages();
								displayImage(curImage);
							}
						}
						
						e.consume();
						return true;
					}
					break;
				}

				case KindleKeyCodes.VK_TURN_PAGE_BACK: {
					if (e.getID() == KeyEvent.KEY_RELEASED) {
						
						int tmp = -1;
						if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) == 0) {
							tmp = prevImagePos(curMangaPos, true);
							if (tmp != -1) {
								curMangaPos = tmp;
								nextImage = curImage;
								curImage = prevImage;
								prevImage = null;
								
								displayImage(curImage);
								loadMissingImages();
							}
						} else {
							tmp = prevMangaPos(curMangaPos, true);
							if (tmp != -1) {
								curMangaPos = tmp;
								prevImage = null;
								curImage = null;
								nextImage = null;
								
								loadMissingImages();
								displayImage(curImage);
							}
						}

						e.consume();
						return true;
					}
					break;
				}
				}

				return false;
			}
		});
	}

	private void showMangaSelector() {
		showPanel(new LoadMangaPanel(this, ctx.getHomeDirectory(), new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				File cur = new File(dir, filename);
				if (!cur.isDirectory())
					return false;
				
				return true;
			}
		}), "Please select a manga");
	}

	public void fileSelected(File selectedFile)
	{
		saveCurMangaState();

		curSeriesName = selectedFile.getName();
		curSeriesDir = new File(ctx.getHomeDirectory(), curSeriesName);
		updateCbzs();
		loadCurMangaState();
		displayImage(curImage);
	}
	
	public void setBusyIndicator(boolean busy) {
		ctx.getProgressIndicator().setIndeterminate(busy);
	}
	
	
	private void displayImage(final Image img)
	{
		if (img == null) {
			return;
		}
		
		mainComponent.setImage(img);
		showMainComponent();
		updateSubTitle();
		
		// yuk, this nasty hack is to get round a platform bug on the K3 where it zaps the contents of the title bar
		// shortly AFTER the kindlet is initialised. If you've already set it, you lose your changes!
		if (isFirstImage) {
			Timer t = new Timer();
			t.schedule(new TimerTask() {				
				public void run() {
					setBusyIndicator(false);
					mainComponent.setImage(img);
					updateSubTitle();
				}
			}, 600);
			isFirstImage = false;
		}
	}
	
	private void updateSubTitle() 
	{
		int cbz = (curMangaPos >> 16) & 0xffff;
		int img = curMangaPos & 0xffff;
		
		StringBuffer sb = new StringBuffer();
		sb.append(img + 1);
		sb.append("/");
		sb.append(curCbzImageCount);
		sb.append(" [");
		sb.append(cbz + 1);
		sb.append("/");
		sb.append(cbzs.length);
		sb.append("] ");
		sb.append(curSeriesName);
		
		ctx.setSubTitle(sb.toString());
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	private void updateCbzs()
	{
		if (curSeriesName == null) {
			cbzs = new String[0];
			return;
		}
		
		// get the list of files in that directory
		File[] cbzFilesRaw = curSeriesDir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				File cur = new File(dir, filename);
				if (cur.isDirectory())
					return false;
				if ((!filename.toLowerCase().endsWith(".zip")) && (!filename.toLowerCase().endsWith(".cbz"))) 
					return false;
				
				return true;
			}
		});
		
		// now, sort 'em
		TreeSet cbzFilesSet = new TreeSet();		
		for(int i=0; i<cbzFilesRaw.length; i++)
			cbzFilesSet.add(cbzFilesRaw[i].getName());
		cbzs = (String[]) cbzFilesSet.toArray(new String[cbzFilesSet.size()]);
	}
	
	private int getCbzImageCount(int cbz)
	{
		int count = 0;
		
		ZipFile zipFile = null;
		try {
			File zipFilename = new File(curSeriesDir, cbzs[cbz]);

			zipFile = new ZipFile(zipFilename);
			Enumeration entries = zipFile.entries();
			while(entries.hasMoreElements()) {
				entries.nextElement();
				count++;
			}
		} catch (Throwable t) {
			getLogger().error("Error accessing " + cbz, t);
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (Throwable t) {}
		}
		
		return count;
	}
	
	private int findCbzIdx(String cbzFilename) {
		if (cbzFilename == null)
			return 0;
		
		for(int i =0; i < cbzs.length; i++) {
			if (cbzs[i].equals(cbzFilename))
				return i;
		}
		
		if (cbzs.length == 0)
			return -1;

		return 0;
	}

	private int findCbzFileIdx(int cbz, String cbzFileFilename) {
		if (cbzFileFilename == null)
			return 0;
		
		ZipFile zipFile = null;
		try {
			File zipFilename = new File(curSeriesDir, cbzs[cbz]);
			zipFile = new ZipFile(zipFilename);
			
			int pos = 0;
			Enumeration en = zipFile.entries();
			while(en.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) en.nextElement();
				if (cbzFileFilename.equals(entry.getName()))
					return pos;
				pos++;
			}
		} catch (Throwable t) {
			getLogger().error("Error accessing " + cbz + " " + cbzFileFilename, t);
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (Throwable t) {}
		}
		
		return 0;
	}
	
	
	private String getImageFilename(int mangaPos)
	{
		if (curSeriesName == null)
			return null;

		int cbz = (curMangaPos >> 16) & 0xffff;
		int img = curMangaPos & 0xffff;

		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(new File(curSeriesDir, cbzs[cbz]));
			
			int pos = 0;
			Enumeration en = zipFile.entries();
			while(en.hasMoreElements() && (pos < img)) {
				en.nextElement();
				pos++;
			}
			ZipEntry entry = (ZipEntry) en.nextElement();
			if (entry == null)
				return null;
			return entry.getName();
		} catch  (Throwable t) {
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (Throwable t2) {}
		}
		
		return null;
	}

	
	private int nextImagePos(int mangaPos, boolean updateImageCount) {
		int cbz = (mangaPos >> 16) & 0xffff;
		int img = mangaPos & 0xffff;
		
		if (img < (curCbzImageCount - 1)) {
			img++;
		} else if (cbz < (cbzs.length - 1)) {
			img = 0;
			cbz++;
			if (updateImageCount)
				curCbzImageCount= getCbzImageCount(cbz);
		} else {
			return -1;
		}
		
		return (cbz << 16) | img;
	}	
	
	private int prevImagePos(int mangaPos, boolean updateImageCount) {
		int cbz = (mangaPos >> 16) & 0xffff;
		int img = mangaPos & 0xffff;
		
		if (img > 0) {
			img--;
		} else if (cbz > 0) {
			cbz--;
			int imageCount = getCbzImageCount(cbz);
			if (updateImageCount)
				curCbzImageCount = imageCount;
			
			if (imageCount > 0)
				img = imageCount - 1;
			else
				img = 0;
		} else {
			return -1;
		}

		return (cbz << 16) | img;
	}	
	
	private int nextMangaPos(int mangaPos, boolean updateImageCount) {		
		int cbz = (mangaPos >> 16) & 0xffff;
		int img = mangaPos & 0xffff;
		
		if (cbz < (cbzs.length - 1)) {
			cbz++;
			img = 0;

			if (updateImageCount)
				curCbzImageCount = getCbzImageCount(cbz);
		} else {
			return -1;
		}
		
		return (cbz << 16) | img;
	}
	
	private int prevMangaPos(int mangaPos, boolean updateImageCount) {
		int cbz = (mangaPos >> 16) & 0xffff;
		int img = mangaPos & 0xffff;

		if (img > 0) {
			img = 0;
		} else if (cbz > 0) {
			img = 0;
			cbz--;

			if (updateImageCount)
				curCbzImageCount = getCbzImageCount(cbz);
		} else {
			return -1;
		}

		return (cbz << 16) | img;
	}


	private void saveGlobalState()
	{
		if (curSeriesName == null)
			return;

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(ctx.getHomeDirectory(), "mangle.state")));
			writer.write(curSeriesName);
			writer.newLine();
			writer.flush();
		} catch  (Throwable t) {
			if (writer != null)
				try {
					writer.close();
				} catch (Throwable t2) {}
		}
	}

	private void loadGlobalState()
	{
		curMangaPos = 0;
		prevImage = null;
		curImage = null;
		nextImage = null;
		curSeriesName = null;
		curSeriesDir = null;
		
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(new File(ctx.getHomeDirectory(), "mangle.state")));
			curSeriesName = reader.readLine();

			curSeriesDir = new File(ctx.getHomeDirectory(), curSeriesName);
			if (!curSeriesDir.isDirectory()) {
				curSeriesName = null;
				curSeriesDir = null;				
			}
		} catch  (Throwable t) {
			curSeriesName = null;
			curSeriesDir = null;

			if (reader != null)
				try {
					reader.close();
				} catch (Throwable t2) {}
		}
	}
	
	private void saveCurMangaState()
	{
		if (curSeriesName == null)
			return;

		int cbz = (curMangaPos >> 16) & 0xffff;
		
		String imageFilename = getImageFilename(curMangaPos);
		if (imageFilename == null)
			return;

		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(new File(ctx.getHomeDirectory(), curSeriesName + ".state")));
			writer.write(cbzs[cbz]);
			writer.newLine();
			writer.write(imageFilename);
			writer.newLine();
			writer.flush();
		} catch  (Throwable t) {
			if (writer != null)
				try {
					writer.close();
				} catch (Throwable t2) {}
		}
	}

	private void loadCurMangaState() 
	{
		if (curSeriesName == null)
			return;
		
		prevImage = null;
		curMangaPos = 0;
		curImage = null;
		nextImage = null;

		// open the progress file if its there
		File progress = new File(ctx.getHomeDirectory(), curSeriesName + ".state");
		if (progress.isFile()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(progress));
				int cbz = findCbzIdx(reader.readLine().trim());				
				if (cbz >= 0) {
					curCbzImageCount = getCbzImageCount(cbz);
					int img = findCbzFileIdx(cbz, reader.readLine().trim());
					if (img >= 0) {
						curMangaPos = (cbz << 16) | img;						
						loadMissingImages();
						return;
					}
				}
			} catch (Throwable t) {
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (Throwable t) {}
				}
			}
		}

		// just use the first image
		curMangaPos = 0;
		curCbzImageCount = getCbzImageCount(0);
		loadMissingImages();
	}
	
	private void loadMissingImages() {
		if (curImage == null)
			curImage = loadImage(curMangaPos);
		if (prevImage == null)
			prevImage = loadImage(prevImagePos(curMangaPos, false));
		if (nextImage == null)
			nextImage = loadImage(nextImagePos(curMangaPos, false));
	}

	private Image loadImage(int mangaPos) {
		if (mangaPos == -1)
			return null;

		int cbz = (mangaPos >> 16) & 0xffff;
		int img = mangaPos & 0xffff;
		
		ZipFile zipFile = null;
		InputStream is = null;
		try {
			File zipFilename = new File(curSeriesDir, cbzs[cbz]);
			zipFile = new ZipFile(zipFilename);
			
			int pos = 0;
			Enumeration en = zipFile.entries();
			while(en.hasMoreElements() && (pos < img)) {
				en.nextElement();
				pos++;
			}
			ZipEntry entry = (ZipEntry) en.nextElement();

			byte[] tmp = null;
			is = zipFile.getInputStream(entry);
			tmp = readFileBytes(is);
			if (tmp == null)
				return null;

			Image newImg = Toolkit.getDefaultToolkit().createImage(tmp).getScaledInstance(-1, root.getHeight(), Image.SCALE_AREA_AVERAGING);
			Toolkit.getDefaultToolkit().prepareImage(newImg, -1, -1, null);
			return newImg;
		} catch (Throwable t) {
			getLogger().error("Error accessing " + cbz + " " + img, t);
			return null;
		} finally {
			if (is !=  null)
				try {
					is.close();
				} catch (Throwable t) {}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (Throwable t) {}
		}
	}
	
	private byte[] readFileBytes(final InputStream inputstream) {
		if (inputstream == null) return null;

		// Start with a buffer size between 1K and 1M based on available memory.
		final int minBufferSize = (int)
		Math.max(1024,
				Math.min(Runtime.getRuntime().freeMemory()/10, 1024 * 1024));

		List buffers = new ArrayList();
		int totalBytesRead = 0;

		// Fill buffer lists
		try {
			byte[] curBuf = new byte[minBufferSize];
			int curBufPos = 0;

			int bytesRead;
			while ((bytesRead = inputstream.read(curBuf, curBufPos, curBuf.length - curBufPos)) != -1) {
				totalBytesRead += bytesRead;
				curBufPos += bytesRead;

				if (curBufPos == curBuf.length) {
					buffers.add(curBuf);
					curBuf = new byte[minBufferSize];
					curBufPos = 0;        	
				}
			}
			buffers.add(curBuf);
		} catch (IOException ex) {
			throw new RuntimeException("Unable to read file bytes", ex);
		}

		byte[] result = new byte[totalBytesRead];
		int pos = 0;
		Iterator it = buffers.iterator();
		while(it.hasNext()) {
			byte[] curBuf = (byte[]) it.next();
			int copy = curBuf.length;
			if (copy > (totalBytesRead - pos))
				copy = totalBytesRead - pos;
			System.arraycopy(curBuf, 0, result, pos, copy);
			pos += copy;
		}

		return result;
	}


	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent arg0) {
		prevImage = null;
		curImage = null;
		nextImage = null;
		loadMissingImages();
		displayImage(curImage);
	}

	public void componentShown(ComponentEvent arg0) {
	}
}
