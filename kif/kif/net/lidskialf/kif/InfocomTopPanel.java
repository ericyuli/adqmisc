package net.lidskialf.kif;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import com.amazon.kindle.kindlet.ui.KComponent;


public class InfocomTopPanel extends KComponent implements ComponentListener {
	
	private static final long serialVersionUID = 7383736491580909999L;
	
	int rows = 0;
	int cols = 0;
	private char[][] charArray = new char[0][0];
	private FontMetrics fontMetrics = null;
	private int charHeight;
	private int charWidth;
	
	public InfocomTopPanel() {
		this.addComponentListener(this);
	}
	
	public int getRows() {
		return rows;
	}
	
	public int getCols() {
		return cols;
	}
	
	public void setChar(int row, int col, char c) {
		if ((row < 0) || (row >= rows))
			return;
		if ((col < 0) || (col >= cols))
			return;
		charArray[row][col] = c;
		repaint(col * charWidth, row * charHeight, charWidth, charHeight);
	}
	
	public void setLines(int lines) {
		this.setSize(getWidth(), lines * charHeight);
	}
	
	public void clear() {
		for(int row = 0; row < rows; row++)
			for(int col = 0; col < cols; col++)
				charArray[row][col] = ' ';
		repaint();
	}


	public void setFont(Font f) {
		super.setFont(f);

		fontMetrics = getFontMetrics(f);
		charWidth = fontMetrics.charWidth('W');
		charHeight = fontMetrics.getHeight();
		updateCharArray();
	}
	
	public void paint(Graphics g) {
		
		Rectangle redraw = g.getClipBounds();
		
		int maxRedrawRow = ((redraw.y + redraw.height) / charHeight) + 1;
		if (maxRedrawRow > rows)
			maxRedrawRow = rows;
		int maxRedrawCol = ((redraw.x + redraw.width) / charWidth) + 1;
		if (maxRedrawCol > cols)
			maxRedrawCol = cols;
		
		for(int curRow = redraw.y / charHeight; curRow < maxRedrawRow; curRow++) {
			for(int curCol = redraw.x / charWidth; curCol < maxRedrawCol; curCol++) {
				int xoff = (charWidth - fontMetrics.charWidth(charArray[curRow][curCol])) / 2;
				g.drawChars(charArray[curRow], curCol, 1, (curCol * charWidth) + xoff, ((curRow + 1) * charHeight) - fontMetrics.getDescent());
			}
		}
	}
	
	private void updateCharArray() {
		if (charWidth == 0)
			return;

		int oldRows = rows;
		int oldCols = cols;
		int newRows = getHeight() / charHeight;
		int newCols = getWidth() / charWidth;

		char[][] newCharArray = new char[newRows][newCols];
		for(int row = 0; row < newRows; row++) {
			char[] newRow = newCharArray[row];

			// copy any data across from the old char array
			int copyLen = 0;
			if (row < oldRows) {
				char[] oldRow = charArray[row];
				copyLen = newCols;
				if (copyLen > oldCols)
					copyLen = oldCols;
				System.arraycopy(oldRow, 0, newRow, 0, copyLen);
			}
			
			// fill the rest with spaces
			for(int col = copyLen; col < newCols; col++)
				newRow[col] = ' ';
		}
		charArray = newCharArray;
		rows = newRows;
		cols = newCols;
	}

	public void componentResized(ComponentEvent e) {
		updateCharArray();
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	public void componentHidden(ComponentEvent e) {
	}	
}
