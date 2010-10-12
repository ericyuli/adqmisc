package net.lidskialf.kif;

import java.util.ArrayList;
import java.util.LinkedList;

import org.zmpp.windowing.AnnotatedText;

public class LineDetails {
	private LinkedList text = new LinkedList(); /* of AnnotatedText */
	private AnnotatedText inputText = null;

	public int screenLineFirst = 0;
	public boolean screenLineLengthsDirty = true;
	public ArrayList screenLineLengths = new ArrayList(); /* of TextOffset */
	
	public LineDetails() {
	}
	
	public LineDetails(AnnotatedText txt) {
		text.add(txt);
	}
	
	public void append(AnnotatedText txt) {
		text.add(txt);
		this.screenLineLengthsDirty = true;
	}
	
	public void setInputText(AnnotatedText ta) {
		inputText = ta;
		this.screenLineLengthsDirty = true;
		this.screenLineLengths.clear();
	}
	
	public LinkedList getText() {
		return text;
	}
	
	public AnnotatedText getInputText() {
		return inputText;
	}
	
	public int screenLineAfter() {
		return screenLineFirst + screenLineLengths.size();
	}
	
	public void clearLines() {
		screenLineFirst = -1;
		screenLineLengthsDirty = true;
		screenLineLengths.clear();
	}
	
	public static class TextOffset {
		public int atIdx;
		public int charIdx;
		
		public TextOffset(int atIdx, int charIdx) {
			this.atIdx = atIdx;
			this.charIdx = charIdx;
		}
	}
}
