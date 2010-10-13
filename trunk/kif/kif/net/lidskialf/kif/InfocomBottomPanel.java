package net.lidskialf.kif;

import java.awt.Color;
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
	private boolean userInput = false;

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
	}

	public void appendString(AnnotatedText toAppend) {
		TextAnnotation ta = toAppend.getAnnotation();
		String txt = toAppend.getText().replace('\r', '\n');

		kindlet.getLogger().error(txt);
		
		int toAppendLength = txt.length();
		if (toAppendLength == 0)
			return;
		
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
		
		recalc();
	}
	
	public void setUserInputStyle(TextAnnotation ta) {
		userInputTa = ta;
	}

	public void textValueChanged(TextEvent txt) {
		String userInputText = getText();
		if ((!userInput) && (userInputText.length() == 0))
			return;

		// if it ended in \n, or we're in character mode, submit it to the VM!
		// TODO: Fix bug where pressing left then return inserts \n into the input
		if (userInputText.endsWith("\n") || kindlet.inCharMode()) {
			kindlet.userInput(userInputText);
			
			curLine.setInputText(null);
			curLine.append(new AnnotatedText(userInputText));
			userInput = false;
			setText("");
		} else {
			curLine.setInputText(new AnnotatedText(userInputTa, userInputText));
			userInput = true;
		}
		
		recalc();
		repaint(); // FIXME: can this be optimised?
	}

	public void clear(int bgColour, int fgColour) {
		this.defaultBgColor = bgColour;
		this.defaultFgColor = fgColour;
		textLines.clear();

		TextAnnotation ta = new TextAnnotation(ScreenModel.FONT_NORMAL, ScreenModel.TEXTSTYLE_ROMAN, bgColour, fgColour);
		curLine = new LineDetails(new AnnotatedText(ta, ""));
		textLines.add(curLine);
		
		setText("");
		
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
			int atLength = ats.size();
			int lineWidth = 0;
			char prevC = '\0';
			int lastWordAtIdx = -1;
			int lastWordCharIdx = -1;
			int wordWidth = 0;
			for(; curAtIdx <= atLength; curAtIdx++) { /* note the "<=" so we have an extra idx for the userinputtext */
				// get current AnnotatedText details
				AnnotatedText curAt = null;
				if (curAtIdx < atLength) {
					curAt = (AnnotatedText) ats.get(curAtIdx);
				} else {
					curAt = ld.getInputText();
					if (curAt == null)
						break;
				}
				TextAnnotation ta = curAt.getAnnotation();
				String txt = curAt.getText();
				int txtLength = txt.length();
				FontMetrics fontMetrics = getFontMetrics(kindlet.getAWTFont(ta));

				// go through characters
				for(; curCharIdx < txtLength; curCharIdx++) {
					char c = txt.charAt(curCharIdx);
					if (prevC == ' ' && c != ' ') {
						lastWordAtIdx = curAtIdx;
						lastWordCharIdx = curCharIdx;
						wordWidth = 0;
					}

					int charWidth = fontMetrics.charWidth(c);
					wordWidth += charWidth;

					boolean lineOverflowsScreenWidth = (lineWidth + charWidth + intercharacterSpaceBuffer) > screenWidth;
					boolean charCanBeginNewLine = c != ' ';
					if (lineOverflowsScreenWidth && charCanBeginNewLine) {
						if (lastWordCharIdx == -1) {
							ld.screenLineLengths.add(new LineDetails.TextOffset(curAtIdx, curCharIdx));
							lineWidth = charWidth;
						} else {
							ld.screenLineLengths.add(new LineDetails.TextOffset(lastWordAtIdx, lastWordCharIdx));
							lineWidth = wordWidth;
						}
						lastWordAtIdx = -1;
						lastWordCharIdx = -1;
						wordWidth = 0;
					} else {
						lineWidth += charWidth;
					}
					prevC = c;
				}
				curCharIdx = 0;
			}
			
			ld.screenLineLengths.add(new LineDetails.TextOffset(curAtIdx, curCharIdx));
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
		
		kindlet.getLogger().error("" + textLines.size());
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
		int redrawLastScreenLine = ((clipBounds.y + clipBounds.height) / lineHeight) + 1;
		
		// draw lines
		int curLineDetailsIdx = 0;
		int startAtIdx = 0;
		int startCharIdx = 0;
		LineDetails curLineDetails = (LineDetails) textLines.get(curLineDetailsIdx++);
		LinkedList ats = curLineDetails.getText();
		int atLength = ats.size();
		int y = redrawCurScreenLine * lineHeight;
		for(int lineIdx = redrawCurScreenLine; lineIdx < redrawLastScreenLine; lineIdx++) {
			// keep looping if we're before the first line
			if (lineIdx < curLineDetails.screenLineFirst)
				continue;

			// figure out the local line and move to the next LineDetails if necessary
			int localLineIdx = lineIdx - curLineDetails.screenLineFirst;
			if (localLineIdx >= curLineDetails.screenLineLengths.size()) {
				if (curLineDetailsIdx >= textLines.size())
					break;

				curLineDetails = (LineDetails) textLines.get(curLineDetailsIdx++);
				ats = curLineDetails.getText();
				atLength = ats.size();
				localLineIdx = 0;
				startCharIdx = 0;
			}
			
			// figure out where this screenline ends
			LineDetails.TextOffset lastCharOffset = (LineDetails.TextOffset) curLineDetails.screenLineLengths.get(localLineIdx);
			int lastAtIdx = lastCharOffset.atIdx;
			int lastCharIdx = lastCharOffset.charIdx;
			
			// draw the screenline
			int x = 0;
			for(int curAtIdx = startAtIdx; curAtIdx <= lastAtIdx; curAtIdx++) {
				// get current AnnotatedText details
				AnnotatedText curAt = null;
				if (curAtIdx < atLength) {
					curAt = (AnnotatedText) ats.get(curAtIdx);
				} else {
					curAt = curLineDetails.getInputText();
					if (curAt == null)
						break;
				}
				TextAnnotation ta = curAt.getAnnotation();
				String txt = curAt.getText();
				
				// figure out the font and the row/col offset to draw at
				g.setFont(kindlet.getAWTFont(ta));
				FontMetrics fontMetrics = g.getFontMetrics();
				
				// figure out what to draw this time
				int drawLength = txt.length() - startCharIdx;
				if (curAtIdx == lastAtIdx)
					drawLength = lastCharIdx - startCharIdx;
				char[] chars = txt.toCharArray();
				int width = fontMetrics.charsWidth(chars, startCharIdx, drawLength);

				// draw the background
				g.setColor(kindlet.getAWTBackgroundColor(ta));
				g.fillRect(x, y, width, lineHeight);

				// draw the foregound
				g.setColor(kindlet.getAWTForegroundColor(ta));
				g.drawChars(chars, startCharIdx, drawLength, x, y + lineHeight - fontMetrics.getDescent());
				startCharIdx = 0;
				x += width;
			}

			// update for next iteration
			y += lineHeight;
			startCharIdx = lastCharIdx;
		}
	}
}
