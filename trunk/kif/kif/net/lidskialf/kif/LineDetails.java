package net.lidskialf.kif;

import java.util.ArrayList;

public class LineDetails {
	private StringBuffer sb = new StringBuffer();
	private String s = null;
	public int nonUserTextLength = 0;

	public int screenLineFirst = 0;
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
		sb.append(s);
		this.s = null;
		this.screenLineLengthsDirty = true;
		this.screenLineLengths.clear();
	}
	
	public int screenLineAfter() {
		return screenLineFirst + screenLineLengths.size();
	}
	
	public void clearLines() {
		screenLineFirst = -1;
		screenLineLengthsDirty = true;
		screenLineLengths.clear();
	}
}
