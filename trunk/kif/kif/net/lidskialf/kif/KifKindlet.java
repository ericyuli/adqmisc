package net.lidskialf.kif;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;

import org.apache.log4j.*;

import com.amazon.kindle.kindlet.*;
import com.amazon.kindle.kindlet.event.KindleKeyCodes;
import com.amazon.kindle.kindlet.ui.*;
import com.amazon.kindle.kindlet.util.Timer;
import com.amazon.kindle.kindlet.util.TimerTask;

import org.zmpp.ExecutionControl;
import org.zmpp.base.DefaultMemory;
import org.zmpp.blorb.NativeImage;
import org.zmpp.blorb.NativeImageFactory;
import org.zmpp.iff.DefaultFormChunk;
import org.zmpp.iff.FormChunk;
import org.zmpp.iff.WritableFormChunk;
import org.zmpp.io.IOSystem;
import org.zmpp.vm.Instruction;
import org.zmpp.vm.InvalidStoryException;
import org.zmpp.vm.MachineRunState;
import org.zmpp.vm.SaveGameDataStore;
import org.zmpp.vm.MachineFactory.MachineInitStruct;
import org.zmpp.windowing.*;
import org.zmpp.windowing.BufferedScreenModel.StatusLineListener;

public class KifKindlet implements Kindlet, StatusLine, StatusLineListener, NativeImageFactory, SaveGameDataStore, IOSystem {
	private Container root;
	private Container content; 

	private KindletContext ctx;

	private Component noGameLoadedComponent;
	private InfocomGamePanel gameComponent;
	private static Logger logger;

	private static final int inset = 10;

	private boolean vmThreadCancelled;
	private Thread vmThread;
	private ExecutionControl executionControl;
	private MachineRunState runState;
	private StringBuffer inputBuffer = new StringBuffer();
	private String input = null;
	private String gameName = null;
	private File selectedFile = null;
	private Timer irqTimer = new Timer();
	private TimerTask irqTask = new IrqTimerTask();
	private BufferedScreenModel screenModel;
	private Boolean gameExecMonitor = new Boolean(true);
	private Boolean userActionMonitor = new Boolean(true);
	private String curSubTitle = "";

	private static final Color GREEN    = new Color(0, 190, 0);
	private static final Color RED      = new Color(190, 0, 0);
	private static final Color YELLOW   = new Color(190, 190, 0);
	private static final Color BLUE     = new Color(0, 0, 190);
	private static final Color MAGENTA  = new Color(190, 0, 190);
	private static final Color CYAN     = new Color(0, 190, 190);

	private static final int DEFAULT_FOREGROUND = ScreenModel.COLOR_BLACK;
	private static final int DEFAULT_BACKGROUND = ScreenModel.COLOR_WHITE;

	private Font fixedFont;
	private Font variableFont;

	public synchronized Logger getLogger() {
		if (logger != null)
			return logger;

		logger = Logger.getLogger("kif");
		try {
			logger.addAppender(new FileAppender(new PatternLayout("%m%n"), new File(ctx.getHomeDirectory(), "log.txt").getAbsolutePath()));		
		} catch (Throwable t) {
		}
		return logger;
	}

	public void create(KindletContext context) {
		this.ctx = context;
		this.root = ctx.getRootContainer();
		this.noGameLoadedComponent = createNoGameLoaded();
		this.gameComponent = createGameDisplay();
		this.vmThread = new Thread(new GameThread());
		this.fixedFont = new Font("monospaced-aa", Font.PLAIN, 14);
		this.variableFont = new Font("SansSerif-aa", Font.PLAIN, 21);

		initRootContent();
		showMainComponent();
		installGlobalKeyHandler();
		ctx.setMenu(createMenu());
		vmThread.start();

		// FIXME: load persisted game state
	}

	public void start() {
	}

	public void stop() {
	}

	public void destroy() {

		// FIXME: persist game state

		try {
			irqTimer.cancel();
			vmThreadCancelled = true;
			synchronized (userActionMonitor) {
				userActionMonitor.notifyAll();
			}
			synchronized (gameExecMonitor) {
				gameExecMonitor.notifyAll();
			}
		} catch (Throwable t) {
		}
	}


	public Color getAWTForegroundColor(TextAnnotation ta) {
		Color c;
		if (ta.isReverseVideo())
			c = getAWTColor(ta.getBackground(), getDefaultBackground());
		else
			c = getAWTColor(ta.getForeground(), getDefaultForeground());

		// Apparently this is the "frotz" trick, which sets the foreground colour slightly brighter; games such as
		// "Varicella" need it
		return c.brighter();
	}

	public Color getAWTBackgroundColor(TextAnnotation ta) {
		if (ta.isReverseVideo())
			return getAWTColor(ta.getForeground(), getDefaultForeground());
		return getAWTColor(ta.getBackground(), getDefaultBackground());
	}

	public Color getAWTColor(int colour, int defaultColour) {
		switch (colour) {
		case ScreenModel.COLOR_DEFAULT:
			return getAWTColor(defaultColour, ScreenModel.UNDEFINED);
		case ScreenModel.COLOR_BLACK:
			return Color.BLACK;
		case ScreenModel.COLOR_RED:
			return RED;
		case ScreenModel.COLOR_GREEN:
			return GREEN;
		case ScreenModel.COLOR_YELLOW:
			return YELLOW;
		case ScreenModel.COLOR_BLUE:
			return BLUE;
		case ScreenModel.COLOR_MAGENTA:
			return MAGENTA;
		case ScreenModel.COLOR_CYAN:
			return CYAN;
		case ScreenModel.COLOR_WHITE:
			return Color.WHITE;
		case ScreenModel.COLOR_MS_DOS_DARKISH_GREY:
			return Color.DARK_GRAY;
		default:
			break;
		}
		return Color.BLACK;
	}

	public Font getAWTFont(TextAnnotation ta) {

		Font font = variableFont;
		if (ta.isFixed())
			font = fixedFont;

		int style = Font.PLAIN;
		if (ta.isBold()) style |= Font.BOLD;
		if (ta.isItalic()) style |= Font.ITALIC;
		return new Font(font.getName(), style, font.getSize());
	}

	public KindletContext getContext() {
		return ctx;
	}

	public int getDefaultBackground() {
		int c = DEFAULT_BACKGROUND;
		if (executionControl != null)
			c = executionControl.getDefaultBackground();
		return c;
	}

	public int getDefaultForeground() {
		int c = DEFAULT_FOREGROUND;
		if (executionControl != null)
			c = executionControl.getDefaultForeground();
		return c;
	}

	public int getNumRowsUpper() {
		return ((BufferedScreenModel) executionControl.getMachine().getScreen()).getNumRowsUpper();
	}

	public void userInput(String i) {
		synchronized (gameExecMonitor) {
			input = i;
			gameExecMonitor.notifyAll();
		}
	}

	public boolean inCharMode() {
		if ((runState != null) && runState.isReadChar())
			return true;
		return false;
	}

	public boolean inLineMode() {
		if ((runState != null) && runState.isReadLine())
			return true;
		return false;
	}

	public int getVersion() {
		if (executionControl != null)
			return executionControl.getVersion();
		return 0;
	}

	public int getActiveWindow() {
		return screenModel.getActiveWindow();
	}

	public TextCursor getCursor() {
		return screenModel.getTextCursor();
	}





	public boolean saveFormChunk(WritableFormChunk formchunk) {
		File subDir = new File(ctx.getHomeDirectory(), gameName + "-saves");
		subDir.mkdirs();

		RandomAccessFile raf = null;
		try {
			synchronized (userActionMonitor) {
				showSelector(new SaveFilePanel(this, gameName + ".save", "savegame", subDir, new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						File cur = new File(dir, filename);
						if (cur.isDirectory())
							return false;
		
						return true;
					}
				}), "Select or enter filename...");

				userActionMonitor.wait();				
				if (selectedFile == null)
					return false;
	
				raf = new RandomAccessFile(selectedFile, "rw");  
				raf.write(formchunk.getBytes());
				return true;
			}
		} catch (Throwable t) {
			return false;
		} finally {
			if (raf != null) try { raf.close(); } catch (Exception ex) { }
		}
	}

	public FormChunk retrieveFormChunk() {
		File subDir = new File(ctx.getHomeDirectory(), gameName + "-saves");
		subDir.mkdirs();

		RandomAccessFile raf = null;
		try {
			synchronized (userActionMonitor) {
				showSelector(new LoadFilePanel(this, "savegame", subDir, new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						File cur = new File(dir, filename);
						if (cur.isDirectory())
							return false;
		
						return true;
					}
				}), "Please choose a saved game...");
				userActionMonitor.wait();
				if (selectedFile == null)
					return null;
	
				raf = new RandomAccessFile(selectedFile, "r");
				byte[] data = new byte[(int) raf.length()];
				raf.readFully(data);
				return new DefaultFormChunk(new DefaultMemory(data));
			}
		} catch (Throwable t) {
			return null;
		} finally {
			if (raf != null) try { raf.close(); } catch (Exception ex) { }
		}
	}

	public Writer getTranscriptWriter() {
		File subDir = new File(ctx.getHomeDirectory(), gameName + "-saves");
		subDir.mkdirs();

		RandomAccessFile raf = null;
		try {
			synchronized (userActionMonitor) {
				showSelector(new SaveFilePanel(this, gameName + ".save", "savegame", subDir, new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						File cur = new File(dir, filename);
						if (cur.isDirectory())
							return false;
		
						return true;
					}
				}), "Select or enter filename...");
		
				userActionMonitor.wait();				
				if (selectedFile == null)
					return null;
	
				return new FileWriter(selectedFile);
			}
		} catch (Throwable t) {
			return null;
		} finally {
			if (raf != null) try { raf.close(); } catch (Exception ex) { }
		}
	}

	public Reader getInputStreamReader() {
		File subDir = new File(ctx.getHomeDirectory(), gameName + "-saves");
		subDir.mkdirs();

		try {
			synchronized (userActionMonitor) {
				showSelector(new LoadFilePanel(this, "inputstream", subDir, new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						File cur = new File(dir, filename);
						if (cur.isDirectory())
							return false;

						return true;
					}
				}), "Please choose an input file...");

				userActionMonitor.wait();				
				if (selectedFile == null)
					return null;
	
				return new FileReader(selectedFile);
			}
		} catch (Throwable t) {
			return null;
		}
	}

	public void statusLineUpdated(String objectDescription, String status) {
		curSubTitle = objectDescription + " " + status;
		ctx.setSubTitle(curSubTitle);
	}

	public void updateStatusScore(String objectName, int score, int steps) {
		curSubTitle = objectName + " " + score + "/" + steps;
		ctx.setSubTitle(curSubTitle);
	}

	public void updateStatusTime(String objectName, int hours, int minutes) {
		curSubTitle = objectName + " " + hours + ":" + minutes;
		ctx.setSubTitle(curSubTitle);
	}

	public NativeImage createImage(InputStream inputStream) throws IOException {	
		// whee, nasty hacking ahead!

		// load the image from the input stream
		Image img = Toolkit.getDefaultToolkit().createImage(new HackImageSource(inputStream));		
		if (img == null)
			return null;

		return new AwtImage(((sun.awt.image.BufferedImagePeer) img).getSubimage(0, 0, img.getWidth(null), img.getHeight(null)));
	}

	public void fileSelected(String selectionType, File selectedFile)
	{
		this.selectedFile = selectedFile;

		if (selectionType.equals("game")) {
			loadGame();
		} else {
			showMainComponent();
			synchronized(userActionMonitor) {
				userActionMonitor.notifyAll();
			}
		}
	}







	private InfocomGamePanel createGameDisplay() {
		return new InfocomGamePanel(this);
	}

	private Component createNoGameLoaded() {
		KLabelMultiline label = new KLabelMultiline("Kif - an Infocom interpreter for the Kindle\nPlease press Menu to open a game");
		return label;
	}

	private void initRootContent() {
		root.add(KBox.createHorizontalStrut(inset), BorderLayout.EAST);
		root.add(KBox.createHorizontalStrut(inset), BorderLayout.WEST);
		root.add(KBox.createVerticalStrut(inset), BorderLayout.NORTH);

		content = new KPanel(new BorderLayout());
		root.add(content, BorderLayout.CENTER);
	}

	private KMenu createMenu() {
		KMenu menu = new KMenu();

		KMenuItem menuItem = new KMenuItem("Open game");
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showGameSelector();
			}			
		});
		menu.add(menuItem);

		return menu;
	}

	private void installGlobalKeyHandler() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			public boolean dispatchKeyEvent(KeyEvent e) {
				Component displayed = content.getComponent(0);
				if (e.isConsumed())
					return false;
				
				switch(e.getKeyCode()) {
				case KindleKeyCodes.VK_BACK:
					// if we're not on the main screen when we get a "BACK", trap it and return to the main game screen
					if ((displayed != gameComponent) && (displayed != noGameLoadedComponent)) {
						if (e.getID() == KeyEvent.KEY_RELEASED) {
							showMainComponent();
							synchronized (KifKindlet.this.userActionMonitor) {
								selectedFile = null;
								KifKindlet.this.userActionMonitor.notifyAll();
							}
						}
						e.consume();
						return true;
					}
					break;
				}
				
				/*
				
				// if we're in char mode, just forward the key directly to the VM
				// FIXME: need to handle SYM key stuff properly!
				if ((displayed == gameComponent) && inCharMode()) {
					// only react to key released events here
//					if (e.getID() != KeyEvent.KEY_RELEASED)
//						return consume(e);

					String s= gameComponent.getUserInput(false);
					getLogger().error("CHARINPUT '" + s + "'");
					
					userInput(String.valueOf(e.getKeyChar()));
					e.consume();
					return true;
				}
				*/

				return false;
			}
		});
	}

	private void showGameSelector() {
		showSelector(new LoadFilePanel(this, "game", ctx.getHomeDirectory(), new FilenameFilter() {
			public boolean accept(File dir, String filename) {
				File cur = new File(dir, filename);
				if (cur.isDirectory())
					return false;

				int dotPos = filename.indexOf('.');
				if (dotPos == -1)
					return false;

				String ext = filename.substring(dotPos+1).toLowerCase();
				if ((ext.charAt(0) != 'z') && (!ext.equals("zblorb")))
					return false;

				return true;
			}
		}), "Please select a game to play");
	}

	private void showSelector(Container container, String title) {
		content.removeAll();
		content.add(container, BorderLayout.CENTER);
		container.getComponent(0).requestFocus();
		ctx.setSubTitle(title);
	}

	private void showMainComponent() {
		content.removeAll();

		if (noGameLoadedComponent != null) {
			content.add(noGameLoadedComponent, BorderLayout.CENTER);
		} else {
			content.add(gameComponent, BorderLayout.CENTER);
			gameComponent.requestFocus();
		}

		ctx.setSubTitle(curSubTitle);
	}

	private void loadGame()
	{
		String chosenFilename = selectedFile.getName();
		this.gameName = chosenFilename.substring(0, chosenFilename.lastIndexOf('.'));

		noGameLoadedComponent = null;
		curSubTitle = "";
		showMainComponent();
		inputBuffer.setLength(0);

		try {
			if (chosenFilename.endsWith(".zblorb"))
				startGame(null, new FileInputStream(selectedFile));
			else
				startGame(new FileInputStream(selectedFile), null);

		} catch (Throwable t) {
			getLogger().error("Failed to load game", t);
			try {
				KOptionPane.showConfirmDialog(root, "Failed to start game " + t.getMessage(), "Error");
			} catch (Throwable t2) {
			}
		}
	}

	private void startGame(InputStream storyStream, InputStream blorbStream) throws IOException, InvalidStoryException
	{
		gameComponent.init(content.getWidth(), content.getHeight());

		screenModel = new BufferedScreenModel();
		screenModel.addStatusLineListener(this);
		screenModel.addScreenModelListener(this.gameComponent);

		MachineInitStruct initStruct = new MachineInitStruct();
		initStruct.blorbFile = blorbStream;
		initStruct.storyFile = storyStream;
		initStruct.ioSystem = this;
		initStruct.nativeImageFactory = this;
		initStruct.saveGameDataStore = this;
		initStruct.screenModel = screenModel;
		//	    initStruct.soundEffectFactory = ?;
		initStruct.statusLine = this;

		executionControl = new ExecutionControl(initStruct);
		screenModel.init(executionControl.getMachine(), executionControl.getZsciiEncoding());
		executionControl.setDefaultColors(DEFAULT_BACKGROUND, DEFAULT_FOREGROUND);
		screenModel.setNumCharsPerRow(gameComponent.getTopCols());
		executionControl.resizeScreen(gameComponent.getTopRows(), gameComponent.getTopCols());
		gameComponent.setUserInputStyle(screenModel.getBottomAnnotation());
		gameComponent.clear(getDefaultBackground(), getDefaultForeground());
		gameComponent.repaintDirty();
	
		userInput(null);
	}

	private class IrqTimerTask extends TimerTask {

		public void run() {
			synchronized (gameExecMonitor) {
				// FIXME: get current input
				String userInput = "";
				if (userInput != null)
					executionControl.setTextToInputBuffer(userInput);
	
				// output is supposed to be shown in the interrupt, so disable buffering temporarily
				screenModel.setBufferMode(false);

				switch(executionControl.callInterrupt(runState.getRoutine())) {
				case Instruction.TRUE:
					irqTimer.cancel();
					// FIXME: press enter key
					break;
	
				case Instruction.FALSE:
					// FIXME: what to do here?
					break;				
				}

				// paint anything that changed and reenable buffering
				gameComponent.repaintDirty();
				screenModel.setBufferMode(true);
			}
		} 
	}

	private class GameThread implements Runnable {

		public void run() {
			while(!vmThreadCancelled) {
				synchronized (gameExecMonitor) {
					try {
						gameExecMonitor.wait();
						if (vmThreadCancelled)
							break;

						irqTimer.cancel();

						if (input != null)
							runState = executionControl.resumeWithInput(input);
						else
							runState = executionControl.run();

//						if (runState.getRoutine() > 0)
//							irqTimer.scheduleAtFixedRate(irqTask, runState.getTime() * 100, runState.getTime() * 100);
						gameComponent.updateCursor(runState.isWaitingForInput());
						gameComponent.repaintDirty();
					} catch (Throwable t) {
						getLogger().error("VM Error", t);
					}
				}
			}
		}		
	}
}
