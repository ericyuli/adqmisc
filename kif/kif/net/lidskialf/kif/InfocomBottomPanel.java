package net.lidskialf.kif;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.zmpp.windowing.AnnotatedText;
import org.zmpp.windowing.ScreenModel;
import org.zmpp.windowing.TextAnnotation;

import com.amazon.kindle.kindlet.ui.KTextArea;

public class InfocomBottomPanel extends KTextArea implements TextListener {

	private static final long serialVersionUID = -6892004540095453828L;

	private List textLines = new ArrayList(); /* of LineDetails */
	private LineDetails curLine = null;

	private FontMetrics fontMetrics = null;
	private int linesPerPage = 0;
	private int lineHeight = 0;
	private int intercharacterSpaceBuffer = 0;

	private TextAnnotation userInputTa;

	private KifKindlet kindlet;
	private int defaultBgColor;
	private int defaultFgColor;


	public InfocomBottomPanel(KifKindlet kindlet) {
		this.kindlet = kindlet;
		this.curLine = new LineDetails(new AnnotatedText(""));
		this.textLines.add(this.curLine);
		this.userInputTa = new TextAnnotation(ScreenModel.FONT_NORMAL, ScreenModel.TEXTSTYLE_ROMAN, kindlet.getDefaultBackground(), kindlet.getDefaultForeground());
		this.defaultBgColor = kindlet.getDefaultBackground();
		this.defaultFgColor = kindlet.getDefaultForeground();

		this.addTextListener(this);
		this.setFocusable(true);
	}

	public void appendString(AnnotatedText toAppend) {
		TextAnnotation ta = toAppend.getAnnotation();
		String txt = toAppend.getText().replace('\r', '\n');
		
		int toAppendLength = txt.length();
		if (toAppendLength == 0)
			return;

		kindlet.getLogger().error("APPEND " + txt);
		
		// split string up at newlines and add into the textLines list
		int firstCharIdx = 0;
		int nextNewlineIdx = 0;
		while(nextNewlineIdx < toAppendLength) {
			// get the index of the /next/ newline
			nextNewlineIdx = txt.indexOf('\n', firstCharIdx);
			if (nextNewlineIdx == -1)
				nextNewlineIdx = toAppendLength;
			
			// append string to current line
			curLine.append(new AnnotatedText(ta, txt.substring(firstCharIdx, nextNewlineIdx)));
			if (curLine.screenLineLengths.size() > 0)
				curLine.screenLineLengths.remove(curLine.screenLineLengths.size() - 1);
			
			// hit end of string => done!
			if (nextNewlineIdx < toAppendLength) {
				curLine = new LineDetails();
				textLines.add(curLine);
				firstCharIdx = nextNewlineIdx + 1;				
			}
		}
	}
	
	public void setUserInputStyle(TextAnnotation ta) {
		userInputTa = ta;
	}
	
	public String getUserInput(boolean appendToVisible) {
		// need to strip any \n or \r characters from the text
		StringBuffer sb = new StringBuffer();
		char[] raw = getText().toCharArray();
		for(int i=0; i< raw.length; i++) {
			if ((raw[i] != '\n') && (raw[i] != '\r'))
				sb.append(raw[i]);
		}
		String userInputText = sb.toString();

		if (appendToVisible) {
			curLine.clearScreenLines();
			appendString(new AnnotatedText(userInputTa, userInputText + "\n"));
		}
		
		setText("");
		return userInputText;
	}

	public void textValueChanged(TextEvent txt) {
		// in character mode, react to text changes immediately and send the last entered character to the VM
		if (kindlet.inCharMode()) {
			String s = getText();
			if (s.length() == 0)
				return;
			kindlet.userInput(s.substring(s.length() - 1, s.length()));
			setText("");
			return;
		}
		
		// in line mode, only send line to VM when it has a \n in it. Otherwise, show it on the screen
		if (kindlet.inLineMode()) {
			if (getText().indexOf('\n') == -1) {
				int oldStartLine = curLine.screenLineFirst;
				int oldEndLine = oldStartLine + curLine.screenLineLengths.size();
				
				curLine.clearScreenLines();
				recalc(); // FIXME: optimise this?
				
				int startLine = Math.min(oldStartLine, curLine.screenLineFirst);
				int endLine = Math.max(oldEndLine, curLine.screenLineFirst + curLine.screenLineLengths.size());
				repaint(0, startLine * lineHeight, getWidth(), (endLine - startLine) * lineHeight);
			} else {
				kindlet.userInput(getUserInput(true));
			}
		}
	}

	public void clear(int bgColour, int fgColour) {
		this.defaultBgColor = bgColour;
		this.defaultFgColor = fgColour;
		textLines.clear();

		TextAnnotation ta = new TextAnnotation(ScreenModel.FONT_NORMAL, ScreenModel.TEXTSTYLE_ROMAN, bgColour, fgColour);
		curLine = new LineDetails(new AnnotatedText(ta, ""));
		textLines.add(curLine);

		setText("");
	}

	public void recalc() {
		int screenWidth = getWidth();
		if ((screenWidth == 0) || (linesPerPage == 0))
			return;

		// first of all we calculate the width of the screen lines each textLine would require for new or dirty lines
		int totalScreenLines = 0;
		for(int textLineIdx=0; textLineIdx < textLines.size(); textLineIdx++) {
			LineDetails ld = (LineDetails) textLines.get(textLineIdx);
			
			// skip current line if its not dirty
			if (!ld.screenLineLengthsDirty) {
				totalScreenLines += ld.screenLineLengths.size();
				continue;
			}

			// get the current TextOffset
			int curAtIdx = 0;
			int curCharIdx = 0;
			if (ld.screenLineLengths.size() > 0) {
				LineDetails.TextOffset curTextOffset = (LineDetails.TextOffset) ld.screenLineLengths.get(ld.screenLineLengths.size() - 1);
				curAtIdx = curTextOffset.atIdx;
				curCharIdx = curTextOffset.charIdx;
			}

			// go through AnnotatedTexts
			LinkedList ats = ld.getText();
			int atsLength = ats.size();
			int curLineWidth = 0;
			int curWordWidth = 0;
			char prevC = '\0';
			int lastWordAtIdx = -1;
			int lastWordCharIdx = -1;
			for(; curAtIdx <= atsLength; curAtIdx++) { /* note the "<=" so we have an extra idx for the userinputtext */
				// get current AnnotatedText details
				TextAnnotation ta = null;
				String txt = null;
				if (curAtIdx < atsLength) {
					AnnotatedText curAt = (AnnotatedText) ats.get(curAtIdx);
					ta = curAt.getAnnotation();
					txt = curAt.getText();
				} else {
					ta = userInputTa;
					txt = getText();
					if ((txt == null) || (txt.length() == 0))
						break;
				}
				int txtLength = txt.length();
				FontMetrics fontMetrics = getFontMetrics(kindlet.getAWTFont(ta));

				// go through characters
				for(; curCharIdx < txtLength; curCharIdx++) {
					char c = txt.charAt(curCharIdx);
					if (prevC == ' ' && c != ' ') {
						lastWordAtIdx = curAtIdx;
						lastWordCharIdx = curCharIdx;
						curWordWidth = 0;
					}

					int charWidth = fontMetrics.charWidth(c);
					curWordWidth += charWidth;

					boolean lineOverflowsScreenWidth = (curLineWidth + charWidth + intercharacterSpaceBuffer) > screenWidth;
					boolean charCanBeginNewLine = c != ' ';
					if (lineOverflowsScreenWidth && charCanBeginNewLine) {
						if (lastWordCharIdx == -1) {
							ld.screenLineLengths.add(new LineDetails.TextOffset(curAtIdx, curCharIdx));
							curLineWidth = charWidth;
						} else {
							ld.screenLineLengths.add(new LineDetails.TextOffset(lastWordAtIdx, lastWordCharIdx));
							curLineWidth = curWordWidth;
						}
						lastWordAtIdx = -1;
						lastWordCharIdx = -1;
						curWordWidth = 0;
					} else {
						curLineWidth += charWidth;
					}
					prevC = c;
				}
				curCharIdx = 0;
			}
			
			ld.screenLineLengths.add(new LineDetails.TextOffset(curAtIdx, curCharIdx));
			ld.screenLineLengthsDirty = false;
			totalScreenLines += ld.screenLineLengths.size();
		}
		
		// now we allocate textLines to screen lines
		int curScreenLine = 0;
		if (totalScreenLines > (linesPerPage - 1))
			curScreenLine = linesPerPage - 1 - totalScreenLines;
		for(int textLineIdx = 0; textLineIdx < textLines.size(); textLineIdx++) {
			LineDetails ld = (LineDetails) textLines.get(textLineIdx);
			ld.screenLineFirst = curScreenLine;
			curScreenLine += ld.screenLineLengths.size();
		}
		
		// delete leading textlines which are now completely off the top of the page
		while(textLines.size() > 0) {
			LineDetails ld = (LineDetails) textLines.get(0);
			if ((ld.screenLineFirst + ld.screenLineLengths.size()) > 0)
				break;

			textLines.remove(0);
		}
	}
	
	public void setFont(Font f) {
		super.setFont(f);

		fontMetrics = getFontMetrics(f);
		lineHeight = fontMetrics.getHeight();
		linesPerPage = getHeight() / lineHeight;
		intercharacterSpaceBuffer = fontMetrics.charWidth(' ');
		
		clear(defaultBgColor, defaultFgColor);
	}
	
	public void paint(Graphics g) {
		if (linesPerPage == 0)
			return;

		Rectangle clipBounds = g.getClipBounds();

		// figure out what screenlines we're drawing
		int redrawCurScreenLine = clipBounds.y / lineHeight;
		int redrawLastScreenLine = (clipBounds.y + clipBounds.height) / lineHeight;
		
		// now, draw lines
		int curLineDetailsIdx = 0;
		LineDetails curLineDetails = (LineDetails) textLines.get(curLineDetailsIdx++);
		for(; redrawCurScreenLine <= redrawLastScreenLine; redrawCurScreenLine++) {
			// get the local line offset in the LineDetails, or move to the next if we've run out
			int localLineIdx = redrawCurScreenLine - curLineDetails.screenLineFirst;
			while(localLineIdx >= curLineDetails.screenLineLengths.size()) {
				curLineDetails = (LineDetails) textLines.get(curLineDetailsIdx++);
				localLineIdx = redrawCurScreenLine - curLineDetails.screenLineFirst;
			}
			
			// figure out the start and ending AT/chars to draw at
			LineDetails.TextOffset startOffset = null;
			if (localLineIdx == 0)
				startOffset = new LineDetails.TextOffset(0, 0);
			else
				startOffset = (LineDetails.TextOffset) curLineDetails.screenLineLengths.get(localLineIdx - 1);
			LineDetails.TextOffset endOffset = (LineDetails.TextOffset) curLineDetails.screenLineLengths.get(localLineIdx);

			// Now, draw stuff!
			int startCharIdx = startOffset.charIdx;
			int atsLength = curLineDetails.getText().size();
			int x = 0;
			int y = (redrawCurScreenLine * lineHeight) + lineHeight - fontMetrics.getDescent();
			for(int curAtIdx = startOffset.atIdx; curAtIdx <= endOffset.atIdx; curAtIdx++) {
				TextAnnotation ta = null;
				String txt = null;
				if (curAtIdx < atsLength) {
					AnnotatedText at = (AnnotatedText) curLineDetails.getText().get(curAtIdx);
					ta = at.getAnnotation();
					txt = at.getText();
				} else {
					ta = userInputTa;
					txt = getText();
					if ((txt == null) || (txt.length() == 0))
						break;						
				}
				
				// figure out the font and the row/col offset to draw at
				g.setFont(kindlet.getAWTFont(ta));
				FontMetrics fontMetrics = g.getFontMetrics();

				// figure out what to draw this time
				int drawLength = txt.length() - startCharIdx;
				if (curAtIdx == endOffset.atIdx)
					drawLength = endOffset.charIdx - startCharIdx;
				char[] chars = txt.toCharArray();
				int width = fontMetrics.charsWidth(chars, startCharIdx, drawLength);

				// draw the background
				g.setColor(kindlet.getAWTBackgroundColor(ta));
				g.fillRect(x, y, width, lineHeight);

				// draw the foregound
				g.setColor(kindlet.getAWTForegroundColor(ta));
				g.drawChars(chars, startCharIdx, drawLength, x, y);
				
				startCharIdx = 0;
				x += width;
			}
		}
	}
}
