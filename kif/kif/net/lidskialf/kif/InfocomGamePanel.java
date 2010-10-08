package net.lidskialf.kif;

import java.awt.Font;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;

import org.zmpp.windowing.AnnotatedCharacter;
import org.zmpp.windowing.AnnotatedText;
import org.zmpp.windowing.BufferedScreenModel;
import org.zmpp.windowing.ScreenModel;
import org.zmpp.windowing.ScreenModelListener;

import com.amazon.kindle.kindlet.ui.KPanel;

public class InfocomGamePanel extends KPanel implements ComponentListener, ScreenModelListener  {
	
	private InfocomTopPanel topPanel;
	private InfocomBottomPanel botPanel;
	private int topWindowLines = 1;
	private KifKindlet kindlet;
	
	public InfocomGamePanel(KifKindlet kindlet) {
		this.kindlet = kindlet;
		
		this.addComponentListener(this);
		setLayout(null);
		
		this.topPanel = new InfocomTopPanel();
		this.topPanel.setFont(new Font("monospaced", Font.PLAIN, 21));
		add(this.topPanel);

		this.botPanel = new InfocomBottomPanel(kindlet);		
		this.botPanel.setFont(new Font("Serif-aa", Font.PLAIN, 21));
		add(this.botPanel);
		
		reset();
	}

	public int getTopCols() {
		return topPanel.getCols();
	}

	public int getTopRows() {
		return topPanel.getRows();
	}

	public void reset() {
		windowErased(-1);
		screenSplit(0);
	}
	
	public void focus() {
		botPanel.requestFocus();
	}	

	public void componentHidden(ComponentEvent arg0) {
	}

	public void componentMoved(ComponentEvent arg0) {
	}

	public void componentResized(ComponentEvent arg0) {
		screenSplit(topWindowLines);
	}

	public void componentShown(ComponentEvent arg0) {
	}
	
	
	
	
	
	
	

	public void screenModelUpdated(ScreenModel screenModel) {
		BufferedScreenModel bsm = (BufferedScreenModel) screenModel;
	    Iterator it = bsm.getLowerBuffer().listIterator();
	    while(it.hasNext()) {
	    	
	    	AnnotatedText txt = (AnnotatedText) it.next(); // FIXME: support annotated text properly

			botPanel.appendString(txt.getText());
	    }
	}

	public void topWindowUpdated(int cursorx, int cursory, AnnotatedCharacter c) {
		// FIXME: support annotated character properly
	    topPanel.setChar(cursory - 1, cursorx - 1, c.getCharacter());
	}

	public void screenSplit(int linesUpperWindow) {
		topWindowLines = linesUpperWindow;
		
		kindlet.getLogger().error("SPLIT " + linesUpperWindow);
		
		topPanel.setLocation(0, 0);
		topPanel.setSize(getWidth(), topPanel.getHeight());
		topPanel.setLines(linesUpperWindow);
		
		botPanel.setLocation(0, topPanel.getHeight());
		botPanel.setSize(getWidth(), getHeight() - topPanel.getHeight());

	    kindlet.getLogger().error("TOP " + topPanel.getWidth() + " " + topPanel.getHeight());
	    kindlet.getLogger().error("BOT " + botPanel.getWidth() + " " + botPanel.getHeight());
	}

	public void windowErased(int window) {
		// FIXME: need to reset colours
		
		switch(window) {
		case -1:
			topPanel.clear();
			botPanel.clear();
			break;
		case ScreenModel.WINDOW_BOTTOM:
			botPanel.clear();
			break;
		case ScreenModel.WINDOW_TOP:
			topPanel.clear();
			break;
		}
	}

	public void topWindowCursorMoving(int line, int column) {
		// FIXME: implement this
	}
}
