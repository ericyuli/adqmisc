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
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.amazon.kindle.kindlet.ui.KComponent;
import com.amazon.kindle.kindlet.ui.KRepaintManager;
import com.amazon.kindle.kindlet.ui.KTextArea;
import com.amazon.kindle.kindlet.ui.KTextField;

public class InfocomBottomPanel extends KTextArea implements ComponentListener, TextListener {

	private static final long serialVersionUID = -6892004540095453828L;
	
	private List textLines = new ArrayList(); /* of LineDetails */
	private LineDetails curLine = null;
	
	private FontMetrics fontMetrics = null;
	private int linesPerPage = 0;
	private int lineHeight = 0;
	private int intercharacterSpaceBuffer = 0;
	
	private KifKindlet kindlet;

	
	public InfocomBottomPanel(KifKindlet kindlet) {
		this.kindlet = kindlet;
		this.curLine = new LineDetails("");
		this.textLines.add(this.curLine);
		
		this.addComponentListener(this);
		this.addTextListener(this);
	}
	
	public void appendString(String toAppend) {
		int toAppendLength = toAppend.length();
		if (toAppendLength == 0)
			return;
		
		// split string up at newlines and add into the textLines list
		int firstCharIdx = 0;
		int nextNewlineIdx = 0;
		while(nextNewlineIdx < toAppendLength) {
			// get the index of the /next/ newline
			nextNewlineIdx = toAppend.indexOf('\n', firstCharIdx);
			if (nextNewlineIdx == -1)
				nextNewlineIdx = toAppendLength;
			
			// append string to current line
			curLine.append(toAppend.substring(firstCharIdx, nextNewlineIdx - firstCharIdx - 1));
			
			// hit end of string => done!
			if (nextNewlineIdx < toAppendLength) {
				curLine = new LineDetails();
				textLines.add(curLine);
				firstCharIdx = nextNewlineIdx + 1;				
			}
		}
		
		// clear the user input buffer
		setText("");

		repaint();
	}
	
	public void textValueChanged(TextEvent txt) {
		String userInputText = getText();
		curLine.setUserText(userInputText);
		
		// if it ended in \n, or we're in character mode submit it to the VM!
		if (userInputText.endsWith("\n") || kindlet.inCharMode()) {
			kindlet.input(userInputText);
			setText("");
		}
		
		recalc();
		repaint();		// FIXME: repaint optimisation
	}
	
	public void clear() {
		textLines.clear();
		curLine = new LineDetails("");
		textLines.add(curLine);
		
		setText("");
		
		recalc();
		repaint();
	}
	
	public void setFont(Font f) {
		super.setFont(f);

		fontMetrics = getFontMetrics(f);
		lineHeight = fontMetrics.getHeight();
		linesPerPage = getHeight() / lineHeight;
		intercharacterSpaceBuffer = fontMetrics.charWidth(' ');
		
		for(int i=0; i< textLines.size(); i++)
			((LineDetails) textLines.get(i)).clearLines();
		recalc();
		repaint();
	}
	
	private void recalc() {
		int screenWidth = getWidth();
		if ((screenWidth == 0) || (linesPerPage == 0))
			return;

		// first of all we calculate the width of the screen lines each textLine would require for new or dirty lines
		for(int textLineIdx=0; textLineIdx < textLines.size(); textLineIdx++) {
			LineDetails ld = (LineDetails) textLines.get(textLineIdx);
			
			if (!ld.screenLineLengthsDirty)
				continue;
			
			int charIdx = 0;
			if (ld.screenLineLengths.size() > 0)
				charIdx = ((Integer) ld.screenLineLengths.get(ld.screenLineLengths.size() - 1)).intValue();

			String s = ld.toString();
			int sLength = s.length();
			int lineWidth = 0;
			for(; charIdx < sLength; charIdx++) {
				int charWidth = fontMetrics.charWidth(s.charAt(charIdx));
				if ((lineWidth + charWidth + intercharacterSpaceBuffer) > screenWidth) {
					ld.screenLineLengths.add(new Integer(charIdx));
					lineWidth = 0;
				} else {
					lineWidth += charWidth;
				}
			}
			
			ld.screenLineLengthsDirty = false;
		}
		
		// now we allocate textLines to screen lines
		int curScreenLine = linesPerPage - 1;
		int textLineIdx = textLines.size() - 1;
		for(; textLineIdx >= 0; textLineIdx--) {
			LineDetails ld = (LineDetails) textLines.get(textLineIdx);
			curScreenLine -= ld.screenLineLengths.size();
			ld.screenLineFirst = curScreenLine;
			if (curScreenLine < 0)
				break;
		}
		
		// delete leading lines which are now off the top of the page
		while(textLineIdx-- > 0)
			textLines.remove(0);
	}
	

	public void paint(Graphics g) {
		Rectangle clipBounds = g.getClipBounds();
		
		// figure out what screenlines we're drawing
		int redrawCurScreenLine = clipBounds.y / lineHeight;
		int redrawLastScreenLine = (clipBounds.y + clipBounds.height) / lineHeight;
		
		// figure out the LineDetails we should start drawing from
		int textLineDetailsIdx;
		for(textLineDetailsIdx = 0; textLineDetailsIdx < textLines.size(); textLineDetailsIdx++) {
			LineDetails curLineDetails = (LineDetails) textLines.get(textLineDetailsIdx);
			if (redrawCurScreenLine < curLineDetails.screenLineAfter())
				break;
		}

		// draw lines
		g.setColor(Color.BLACK);
		int y = ((redrawCurScreenLine + 1) * lineHeight) - fontMetrics.getDescent();
		while(redrawCurScreenLine <= redrawLastScreenLine) {
			if (textLineDetailsIdx >= textLines.size())
				break;
			
			LineDetails curLineDetails = (LineDetails) textLines.get(textLineDetailsIdx++);
			int screenLinesCount = curLineDetails.screenLineLengths.size();
			for(int localLineIdx = redrawCurScreenLine - curLineDetails.screenLineFirst; localLineIdx < screenLinesCount; localLineIdx++) {
				
				char[] stringChars = curLineDetails.toString().toCharArray();
				g.drawChars(stringChars, 0, stringChars.length, 0, y);
				y += lineHeight;
			}
		}
	}
	
	public void componentResized(ComponentEvent e) {
		if (lineHeight != 0)
			linesPerPage = getHeight() / lineHeight;

		for(int i=0; i< textLines.size(); i++)
			((LineDetails) textLines.get(i)).clearLines();
		recalc();
		repaint();
	}
	
	public void componentShown(ComponentEvent e) {
	}
	
	public void componentMoved(ComponentEvent e) {
	}
	
	public void componentHidden(ComponentEvent e) {
	}	
}
