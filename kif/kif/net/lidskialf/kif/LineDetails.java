package net.lidskialf.kif;

import java.util.ArrayList;

public class LineDetails {
	private StringBuffer sb = new StringBuffer();
	private String s = null;
	private int nonUserTextLength = 0;

	public int screenLineFirst = -1;
	public boolean screenLineLengthsDirty = true;
	public ArrayList screenLineLengths = new ArrayList();
	
	public LineDetails() {
	}
	
	public LineDetails(String s) {
		append(s);
	}
	
	public String toString() {
		if (s == null)
			s = sb.toString();
		return s;
	}
	
	public void append(String s) {
		sb.append(s);
		this.s = null;
		this.screenLineLengthsDirty = true;
		this.nonUserTextLength = sb.length();
	}
	
	public void setUserText(String s) {
		sb.setLength(nonUserTextLength);
		append(s);
	}
	
	public int screenLineAfter() {
		return screenLineFirst + screenLineLengths.size() + 1;
	}
	
	public void clearLines() {
		screenLineFirst = -1;
		screenLineLengthsDirty = true;
		screenLineLengths.clear();
	}
}
