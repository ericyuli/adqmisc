/*
 * Copyright (c) 2010 Andrew de Quincey.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.lidskialf.kif;

import java.util.ArrayList;
import java.util.LinkedList;

import org.zmpp.windowing.AnnotatedText;
import org.zmpp.windowing.TextAnnotation;

public class LineDetails {
	private LinkedList text = new LinkedList(); /* of AnnotatedText */

	public int screenLineFirst = 0;
	public boolean screenLineLengthsDirty = true;
	public ArrayList screenLineLengths = new ArrayList(); /* of TextOffset */
	
	public LineDetails() {
	}
	
	public LineDetails(AnnotatedText txt) {
		text.add(txt);
	}
	
	public void append(AnnotatedText txt) {
		if (text.size() == 0) {
			text.add(txt);
		} else {
			AnnotatedText lastAT = (AnnotatedText) text.get(text.size() - 1);
			TextAnnotation lastTA = lastAT.getAnnotation();
			if (lastTA.equals(txt.getAnnotation())) {
				text.remove(text.size() - 1);
				text.add(new AnnotatedText(txt.getAnnotation(), lastAT.getText() + txt.getText()));
			} else {
				text.add(txt);
			}
		}
		
		this.screenLineLengthsDirty = true;
	}
	
	public LinkedList getText() {
		return text;
	}
	
	public int screenLineAfter() {
		return screenLineFirst + screenLineLengths.size();
	}
	
	public void clearScreenLines() {
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
