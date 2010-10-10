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
	
	private boolean userInput = false;
	
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
			curLine.append(toAppend.substring(firstCharIdx, nextNewlineIdx));
			if (curLine.screenLineLengths.size() > 0)
				curLine.screenLineLengths.remove(curLine.screenLineLengths.size() - 1);
			
			// hit end of string => done!
			if (nextNewlineIdx < toAppendLength) {
				curLine = new LineDetails();
				textLines.add(curLine);
				firstCharIdx = nextNewlineIdx + 1;				
			}
		}
		
		recalcAndRepaint();
	}
	
	public void textValueChanged(TextEvent txt) {
		String userInputText = getText();
		if ((!userInput) && (userInputText.length() == 0))
			return;

		// if it ended in \n, or we're in character mode, submit it to the VM!
		if (userInputText.endsWith("\n") || kindlet.inCharMode()) {
			kindlet.input(userInputText);
			
			curLine.setUserText("");
			appendString(userInputText);
			userInput = false;
			setText("");
		} else {
			curLine.setUserText(userInputText);
			userInput = true;
		}
		
		recalcAndRepaint(); // FIXME: optimise
	}
	
	private void recalcAndRepaint() {
		recalc();
		repaint();
	}
	
	public void clear() {
		textLines.clear();
		curLine = new LineDetails("");
		textLines.add(curLine);
		
		setText("");
		
		recalcAndRepaint();
	}
	
	public void setFont(Font f) {
		super.setFont(f);

		fontMetrics = getFontMetrics(f);
		lineHeight = fontMetrics.getHeight();
		linesPerPage = getHeight() / lineHeight;
		intercharacterSpaceBuffer = fontMetrics.charWidth(' ');
		
		for(int i=0; i< textLines.size(); i++)
			((LineDetails) textLines.get(i)).clearLines();
		recalcAndRepaint();
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
			boolean addFinalLength = true;
			for(; charIdx < sLength; charIdx++) {
				char c = s.charAt(charIdx);
				int charWidth = fontMetrics.charWidth(c);
				boolean lineOverflowsScreenWidth = (lineWidth + charWidth + intercharacterSpaceBuffer) > screenWidth;
				boolean charCanBeginNewLine = c != ' ';
				if (lineOverflowsScreenWidth && charCanBeginNewLine) {
					ld.screenLineLengths.add(new Integer(charIdx));
					lineWidth = 0;
					addFinalLength = false;
				} else {
					lineWidth += charWidth;
					addFinalLength = true;
				}
			}
			if (addFinalLength)
				ld.screenLineLengths.add(new Integer(charIdx));

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
		if (linesPerPage == 0)
			return;
		
		Rectangle clipBounds = g.getClipBounds();
		g.setColor(Color.BLACK);
		
		// figure out what screenlines we're drawing
		int redrawCurScreenLine = clipBounds.y / lineHeight;
		int redrawLastScreenLine = ((clipBounds.y + clipBounds.height) / lineHeight) + 1;
		
		// draw lines
		int startCharIdx = 0;
		int curLineDetailsIdx = 0;
		LineDetails curLineDetails = (LineDetails) textLines.get(curLineDetailsIdx++);
		char[] stringChars = curLineDetails.toString().toCharArray();
		int y = ((redrawCurScreenLine + 1) * lineHeight) - fontMetrics.getDescent();
		for(int lineIdx = redrawCurScreenLine; lineIdx < redrawLastScreenLine; lineIdx++) {
			// keep looping if we're before the first line
			if (lineIdx < curLineDetails.screenLineFirst)
				continue;

			// figure out the local line and move to the next LineDetails
			int localLineIdx = lineIdx - curLineDetails.screenLineFirst;
			if (localLineIdx >= curLineDetails.screenLineLengths.size()) {
				if (curLineDetailsIdx >= textLines.size())
					break;
				curLineDetails = (LineDetails) textLines.get(curLineDetailsIdx++);
				stringChars = curLineDetails.toString().toCharArray();
				localLineIdx = 0;
				startCharIdx = 0;
			}

			// draw  the string
			int lastCharIdx = ((Integer) curLineDetails.screenLineLengths.get(localLineIdx)).intValue();
			g.drawChars(stringChars, startCharIdx, lastCharIdx - startCharIdx, 0, y);

			// update for next iteration
			y += lineHeight;
			startCharIdx = lastCharIdx;
		}
	}
	
	public void componentResized(ComponentEvent e) {
		if (lineHeight != 0)
			linesPerPage = getHeight() / lineHeight;

		for(int i=0; i< textLines.size(); i++)
			((LineDetails) textLines.get(i)).clearLines();
		recalcAndRepaint();
	}
	
	public void componentShown(ComponentEvent e) {
	}
	
	public void componentMoved(ComponentEvent e) {
	}
	
	public void componentHidden(ComponentEvent e) {
	}	
}
