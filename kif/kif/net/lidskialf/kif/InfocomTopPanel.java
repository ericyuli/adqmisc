package net.lidskialf.kif;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.zmpp.windowing.AnnotatedCharacter;
import org.zmpp.windowing.ScreenModel;
import org.zmpp.windowing.TextAnnotation;

import com.amazon.kindle.kindlet.ui.KComponent;


public class InfocomTopPanel extends KComponent {
	
	private static final long serialVersionUID = 7383736491580909999L;
	
	private AnnotatedCharacter cursorChar;
	private AnnotatedCharacter defaultChar;
	private int defaultBgColor;
	private int defaultFgColor;

	private int numRows = 0;
	private int numCols = 0;
	private AnnotatedCharacter[][] charArray = new AnnotatedCharacter[0][0];
	private boolean cursorShown = false;

	private int charHeight;
	private int charWidth;

	private KifKindlet kindlet;
	
	
	public InfocomTopPanel(KifKindlet kindlet) {
		this.kindlet = kindlet;
	}
	
	public int getRows() {
		return numRows;
	}
	
	public int getCols() {
		return numCols;
	}
	
	public int getRowHeight() {
		return charHeight;
	}

	public void setChar(int row, int col, AnnotatedCharacter c) {
		if ((row < 0) || (row >= numRows))
			return;
		if ((col < 0) || (col >= numCols))
			return;

		charArray[row][col] = c;
	}

	public void setCursor(int row, int col, boolean shown) {
		if ((row < 0) || (row >= numRows))
			return;
		if ((col < 0) || (col >= numCols))
			return;
		
		if (!shown) {
			if (cursorShown)
				charArray[row][col] = null;
		} else {
			charArray[row][col] = cursorChar;
		}
		
		cursorShown = shown;
	}

	public void clear(int bgColour, int fgColour, int visibleRows) {
		this.defaultBgColor = bgColour;
		this.defaultFgColor = fgColour;
		defaultChar = new AnnotatedCharacter(new TextAnnotation(ScreenModel.FONT_FIXED, ScreenModel.TEXTSTYLE_ROMAN, defaultBgColor, defaultFgColor), ' ');
		cursorChar = new AnnotatedCharacter(new TextAnnotation(ScreenModel.FONT_FIXED, ScreenModel.TEXTSTYLE_REVERSE_VIDEO, defaultBgColor, defaultFgColor), ' ');

		for(int row = 0; row < numRows && row < visibleRows; row++)
			for(int col = 0; col < numCols; col++)
				charArray[row][col] = defaultChar;
		
		for(int row = visibleRows; row < numRows; row++)
			for(int col = 0; col < numCols; col++)
				charArray[row][col] = null;
	}

	public void init(Font f, int width, int height) {
		FontMetrics fontMetrics = getFontMetrics(f);
		charWidth = fontMetrics.charWidth('0');
		charHeight = fontMetrics.getHeight();
		numRows = height / charHeight;
		numCols = width / charWidth;
		charArray = new AnnotatedCharacter[numRows][numCols];

		clear(kindlet.getDefaultBackground(), kindlet.getDefaultForeground(), 0);
	}

	public void paint(Graphics g) {
		Rectangle redraw = g.getClipBounds();

		int minRedrawRow = redraw.y / charHeight;
		int maxRedrawRow = ((redraw.y + redraw.height) / charHeight) + 1;
		if (maxRedrawRow > numRows)
			maxRedrawRow = numRows;
		int minRedrawCol = redraw.x / charWidth;
		int maxRedrawCol = ((redraw.x + redraw.width) / charWidth) + 1;
		if (maxRedrawCol > numCols)
			maxRedrawCol = numCols;

		char[] tmp = new char[1];
		for(int curRow = minRedrawRow; curRow < maxRedrawRow; curRow++) {
			for(int curCol = minRedrawCol; curCol < maxRedrawCol; curCol++) {
				if (charArray[curRow][curCol] == null)
					continue;

				// get details of what we're drawing
				char c = charArray[curRow][curCol].getCharacter();
				TextAnnotation ta = charArray[curRow][curCol].getAnnotation();

				// figure out the font and the row/col offset to draw at
				g.setFont(kindlet.getAWTFont(ta));
				FontMetrics fontMetrics = g.getFontMetrics();

				// draw the background
				g.setColor(kindlet.getAWTBackgroundColor(ta));
				g.fillRect(curCol * charWidth, (curRow) * charHeight, charWidth, charHeight);

				// draw the foregound
				g.setColor(kindlet.getAWTForegroundColor(ta));
				tmp[0] = c;
				int xoff = (charWidth - fontMetrics.charWidth(c)) / 2;
				g.drawChars(tmp, 0, 1, (curCol * charWidth) + xoff, ((curRow + 1) * charHeight) - fontMetrics.getDescent());
			}
		}
	}
}
