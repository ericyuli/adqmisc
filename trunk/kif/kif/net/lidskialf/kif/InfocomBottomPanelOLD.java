package net.lidskialf.kif;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import java.util.List;

import com.amazon.kindle.kindlet.ui.KComponent;
import com.amazon.kindle.kindlet.ui.KRepaintManager;

public class InfocomBottomPanelOLD extends KComponent implements ComponentListener {

	private static final long serialVersionUID = -6892004540095453828L;
	
	private List strings = new ArrayList();
	private List lines = new ArrayList(); /* of LineDetails */
	private int pageFirstLine = 0;
	private StringBuffer userInput = new StringBuffer();
	
	private FontMetrics fontMetrics = null;
	private int linesPerPage = 0;
	private int lineHeight = 0;
	private int intercharacterSpaceBuffer = 0;

	private int curLineStringIdx = 0;
	private int curLineCharIdx = 0;
	private int curLineWidth = 0;	

	
	public InfocomBottomPanelOLD() {
		this.addComponentListener(this);
	}
	
	public void appendString(String s) {
		int oldLineCount = lines.size();
		int oldLineWidth = curLineWidth;
		
		// clear the user input buffer
		userInput.setLength(0);
		
		// add the text and redraw
		strings.add(s);
		repaintNewText(oldLineCount, oldLineWidth);
	}
	
	public void appendChar(char c) {
		int oldLineCount = lines.size();
		int oldLineWidth = curLineWidth;
		
		// if this is not the first character, remove the last string entered as an optimisation (otherwise we end up with loads of 1 char length strings)
		if (userInput.length() != 0)
			strings.remove(strings.size() - 1);

		// deal with the delete key
		if (c == 8) {
			if (userInput.length() > 0) {
				userInput.setLength(userInput.length() - 1);
				curLineCharIdx--;
				curLineWidth = 0;
				
				LineDetails line = (LineDetails) lines.get(lines.size() - 1);
				line.charCount = 0;
				
				if (userInput.length() > 0)
					strings.add(userInput.toString());
				UpdateLines();
				// FIXME
				
				repaint();
			}
			return;
		}
		
		userInput.append(c);
		strings.add(userInput.toString());		
		repaintNewText(oldLineCount, oldLineWidth);	
	}
	
	public void clear() {
		strings.clear();
		userInput.setLength(0);
		lines.clear();
		UpdateLines();
		repaint();
	}

	public void setFont(Font f) {
		super.setFont(f);

		fontMetrics = getFontMetrics(f);
		lineHeight = fontMetrics.getHeight();
		linesPerPage = getHeight() / lineHeight;
		intercharacterSpaceBuffer = fontMetrics.charWidth(' ');
		UpdateLines();
	}

	public void paint(Graphics g) {
		Rectangle clipBounds = g.getClipBounds();
		
		char[] tmp = new char[1];
		
		int firstLineIdx = pageFirstLine + (clipBounds.y / lineHeight);
		int lineCount = ((clipBounds.y + clipBounds.height) / lineHeight) + 1;
		if (lineCount > linesPerPage)
			lineCount = linesPerPage;
		if ((firstLineIdx + lineCount) > lines.size())
			lineCount = lines.size() - firstLineIdx;
		
		g.setColor(Color.BLACK);
		int y = (((firstLineIdx - pageFirstLine) + 1) * lineHeight) - fontMetrics.getDescent();
		for(int i=0; i < lineCount; i++) {
			LineDetails curLine = (LineDetails) lines.get(firstLineIdx + i);
			int stringIdx = curLine.startStringIdx;
			int stringCharIdx = curLine.startCharIdx;
			String curString = (String) strings.get(stringIdx++);
			
			int x = 0;
			for(int lineCharIdx = 0; lineCharIdx < curLine.charCount; lineCharIdx++) {
				// FIXME: optimise to draw character arrays instead of individual characters??
				
				// reached the end of the string? move to the next!
				while (stringCharIdx >= curString.length()) {
					curString = (String) strings.get(stringIdx++);
					stringCharIdx = 0;
				}
				
				tmp[0] = curString.charAt(stringCharIdx++);
				if (tmp[0] != '\n') {
					g.drawChars(tmp, 0, 1, x, y);
					x += fontMetrics.charWidth(tmp[0]);
				}
			}
			
			y += lineHeight;
		}
	}

	/**
	 * Calculates any lineIndexes for new string data based on current dimensions of the 
	 * window and font settings. 
	 */
	private void UpdateLines() {
		if (fontMetrics == null)
			return;
		
		// figure out the line/character to start calculations from
		LineDetails curLine = null;
		if (lines.size() == 0) {
			curLine = new LineDetails(0, 0);
			lines.add(curLine);
		} else {
			curLine = (LineDetails) lines.get(lines.size() - 1);
		}
		
		// iterate over the new string data and figure out where the line breaks should be
		int lineMaxWidth = getWidth() - intercharacterSpaceBuffer;
		for(;curLineStringIdx < strings.size(); curLineStringIdx++) {
			String curString = (String) strings.get(curLineStringIdx);
			
			for(; curLineCharIdx < curString.length(); curLineCharIdx++) {
				// get the current character and check if its a newline
				char c = curString.charAt(curLineCharIdx);
				if (c == '\n') {
					curLine.charCount++; // include the newline as part of the current line
					curLine = new LineDetails(curLineStringIdx, curLineCharIdx + 1);
					lines.add(curLine);
					curLineWidth = 0;
					continue;
				}
				
				// get width of character and check if we've hit the edge of the screen yet
				int charWidth = fontMetrics.charWidth(c);
				if ((curLineWidth + charWidth) > lineMaxWidth) {
					curLine = new LineDetails(curLineStringIdx, curLineCharIdx);
					lines.add(curLine);
					curLineWidth = 0;
				}
				
				// ok, we're fine - move to next character
				curLine.charCount++;
				curLineWidth += charWidth;
			}
			
			// if we've run out of strings, avoid incrementing string index so stay pointing at the very last string (charidx will be pointing beyond the last
			// character)
			if (curLineStringIdx == (strings.size() - 1))
				break;
			
			// we start at char 0 for the next string
			curLineCharIdx = 0;
		}
	}
	
	private void moveLastLine()
	{
		pageFirstLine = lines.size() - linesPerPage;
		if (pageFirstLine < 0)
			pageFirstLine = 0;
	}
	
	private void repaintNewText(int oldLineCount, int oldLineWidth) {
		UpdateLines();
		moveLastLine();

		if (oldLineCount != lines.size()) {
			repaint();
		} else {
			repaint(oldLineWidth, (oldLineCount - pageFirstLine - 1) * lineHeight, curLineWidth - oldLineWidth + intercharacterSpaceBuffer, lineHeight);			
		}
	}
		
	public void componentShown(ComponentEvent e) {
	}
	
	public void componentResized(ComponentEvent e) {
		if (lineHeight != 0)
			linesPerPage = getHeight() / lineHeight;

		curLineStringIdx = 0;
		curLineCharIdx = 0;
		curLineWidth = 0;		
		lines.clear();
		UpdateLines();
		moveLastLine();
		
		repaint();
	}
	
	public void componentMoved(ComponentEvent e) {
	}
	
	public void componentHidden(ComponentEvent e) {
	}
	
	private class LineDetails {
		public int startStringIdx;
		public int startCharIdx;
		public int charCount;
		
		public LineDetails(int startStringIdx, int startCharIdx) {
			this.startStringIdx = startStringIdx;
			this.startCharIdx = startCharIdx;
			this.charCount = 0;
		}
	}
}
