package net.lidskialf.kif;

import java.util.Iterator;

import org.zmpp.windowing.AnnotatedCharacter;
import org.zmpp.windowing.AnnotatedText;
import org.zmpp.windowing.BufferedScreenModel;
import org.zmpp.windowing.ScreenModel;
import org.zmpp.windowing.ScreenModelListener;
import org.zmpp.windowing.TextAnnotation;
import org.zmpp.windowing.TextCursor;

import com.amazon.kindle.kindlet.ui.KPanel;

public class InfocomGamePanel extends KPanel implements ScreenModelListener {
	
	private static final long serialVersionUID = -5395707640433304655L;
	
	private InfocomTopPanel topPanel;
	private InfocomBottomPanel botPanel;
	private boolean topDirty = true;
	private boolean botDirty = true;
	private KifKindlet kindlet;
	private boolean initialised = false;

	public InfocomGamePanel(KifKindlet kindlet) {
		this.kindlet = kindlet;

		setLayout(null);

		this.topPanel = new InfocomTopPanel(kindlet);
		add(this.topPanel);
		
		this.botPanel = new InfocomBottomPanel(kindlet);
		add(this.botPanel);
	}
	
	public int getTopCols() {
		return topPanel.getCols();
	}
		
	public int getTopRows() {
		return topPanel.getRows();
	}

	public void setUserInputStyle(TextAnnotation ta) {
		botPanel.setUserInputStyle(ta);
	}
	
	public void updateCursor(boolean shown) {
		switch(kindlet.getActiveWindow()) {
		case ScreenModel.WINDOW_TOP:
			TextCursor cursor = kindlet.getCursor();
			topPanel.setCursor(cursor.getLine() - 1, cursor.getColumn() - 1, shown);
			topDirty = true;
			break;
		case ScreenModel.WINDOW_BOTTOM:
			// FIXME: update bottom window cursor
			break;
		}
	}
	
	public void repaintDirty() {
		if (topDirty) {
			topPanel.repaint();
			topDirty = false;
		}
		if (botDirty) {
			botPanel.recalc();
			botPanel.repaint();
			botDirty = false;
		}
	}
	
	public void init(int width, int height) {
		botPanel.setLocation(0, 0);
		botPanel.setSize(width, height);
		if (!initialised)
			botPanel.init(kindlet.getAWTFont(new TextAnnotation(ScreenModel.FONT_NORMAL, ScreenModel.TEXTSTYLE_ROMAN)), width, height);

		topPanel.setLocation(0, 0);
		topPanel.setSize(width, height);
		if (!initialised)
			topPanel.init(kindlet.getAWTFont(new TextAnnotation(ScreenModel.FONT_FIXED, ScreenModel.TEXTSTYLE_ROMAN)), width, height);

		initialised = true;
	}

	public void clear(int bgColour, int fgColour) {
		topPanel.clear(bgColour, fgColour, 0);
		botPanel.clear(bgColour, fgColour);
		topDirty = true;
		botDirty = true;
	}
	
	public String getUserInput(boolean appendToVisible) {
		botDirty = true;
		return botPanel.getUserInput(appendToVisible);
	}

	public void requestFocus() {
		botPanel.requestFocus();
	}

	
	
	
	

	public void screenModelUpdated(ScreenModel screenModel) {
		BufferedScreenModel bsm = (BufferedScreenModel) screenModel;
	    Iterator it = bsm.getLowerBuffer().listIterator();
	    while(it.hasNext())
			botPanel.appendString((AnnotatedText) it.next());
	    botPanel.setUserInputStyle(screenModel.getBottomAnnotation());
	    botDirty = true;
	}

	public void windowErased(int window) {
		switch(window) {
		case -1:
			topPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground(), kindlet.getNumRowsUpper());
			botPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
			topDirty = true;
			botDirty = true;
			break;
		case ScreenModel.WINDOW_BOTTOM:
			botPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
			botDirty = true;
			break;
		case ScreenModel.WINDOW_TOP:
			topPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground(), kindlet.getNumRowsUpper());
			topDirty = true;
			break;
		}
	}

	public void screenSplit(int linesUpperWindow) {
		// slightly odd screen model; toppanel is treated as a transparent overlay covering the whole of the screen
		// botpanel is adjusted so its starting X position is the *guaranteed* visible top panel rows.
		int topHeight = linesUpperWindow * topPanel.getRowHeight();
		botPanel.setBounds(0, topHeight, getWidth(), getHeight() - topHeight);

		if (kindlet.getVersion() == 3)
			topPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground(), linesUpperWindow);
		topDirty = true;
	}

	public void topWindowUpdated(int cursorx, int cursory, AnnotatedCharacter c) {
		topPanel.setChar(cursory - 1, cursorx - 1, c);
		topDirty = true;
	}

	public void topWindowCursorMoving(int line, int column) {
		if (kindlet.inCharMode() && (kindlet.getActiveWindow() == ScreenModel.WINDOW_TOP)) {
			TextCursor cursor = kindlet.getCursor();
			topPanel.setCursor(cursor.getLine() - 1, cursor.getColumn() - 1, false);
			topDirty = true;
		}
	}
}
