package net.lidskialf.kif;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;

import org.zmpp.windowing.AnnotatedCharacter;
import org.zmpp.windowing.AnnotatedText;
import org.zmpp.windowing.BufferedScreenModel;
import org.zmpp.windowing.ScreenModel;
import org.zmpp.windowing.ScreenModelListener;
import org.zmpp.windowing.TextAnnotation;

import com.amazon.kindle.kindlet.ui.KPanel;

public class InfocomGamePanel extends KPanel implements ComponentListener, ScreenModelListener, KeyListener  {
	
	private static final long serialVersionUID = -5395707640433304655L;
	
	public InfocomTopPanel topPanel;
	public InfocomBottomPanel botPanel;
	private KifKindlet kindlet;

	public InfocomGamePanel(KifKindlet kindlet) {
		this.kindlet = kindlet;

		this.addComponentListener(this);
		setLayout(null);
		setFocusable(true);

		this.botPanel = new InfocomBottomPanel(kindlet);		
		add(this.botPanel);

		this.topPanel = new InfocomTopPanel(kindlet);
		add(this.topPanel);
	}
	
	public int getTopCols() {
		return topPanel.getCols();
	}
		
	public int getTopRows() {
		return topPanel.getRows();
	}

	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent arg0) {
	}

	public void componentShown(ComponentEvent arg0) {
		topPanel.setLocation(0, 0);
		topPanel.setSize(getWidth(), getHeight());
		topPanel.setFont(kindlet.getAWTFont(new TextAnnotation(ScreenModel.FONT_FIXED, ScreenModel.TEXTSTYLE_ROMAN)));
		
		botPanel.setLocation(0, 0);
		botPanel.setSize(getWidth(), getHeight());
		botPanel.setFont(kindlet.getAWTFont(new TextAnnotation(ScreenModel.FONT_NORMAL, ScreenModel.TEXTSTYLE_ROMAN)));
	}
	
	
	

	public void keyPressed(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	
	
	
	

	public void screenModelUpdated(ScreenModel screenModel) {
		BufferedScreenModel bsm = (BufferedScreenModel) screenModel;
	    Iterator it = bsm.getLowerBuffer().listIterator();
	    while(it.hasNext()) {
			botPanel.appendString((AnnotatedText) it.next());
	    }
	}

	public void windowErased(int window) {
		// FIXME: implement
		
		kindlet.getLogger().error("WINDOWERASED " + window);
		
		// FIXME: need to reset colours
		
		switch(window) {
		case -1:
			topPanel.clear(kindlet.getNumRowsUpper(), kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
			botPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
			break;
		case ScreenModel.WINDOW_BOTTOM:
			botPanel.clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
			break;
		case ScreenModel.WINDOW_TOP:
			topPanel.clear(kindlet.getNumRowsUpper(), kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
			break;
		}
	}

	public void screenSplit(int linesUpperWindow) {
		topPanel.setVisibleRows(linesUpperWindow);
	}

	public void topWindowUpdated(int cursorx, int cursory, AnnotatedCharacter c) {
		topPanel.setChar(cursory - 1, cursorx - 1, c);
	}

	public void topWindowCursorMoving(int line, int column) {
		topPanel.setCursor(true, line - 1, column - 1);
	}
}
