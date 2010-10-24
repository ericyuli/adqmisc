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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import org.kwt.ui.KWTSelectableLabel;

import com.amazon.kindle.kindlet.ui.KBoxLayout;
import com.amazon.kindle.kindlet.ui.KPanel;

public class LoadFilePanel extends KPanel {

	private static final long serialVersionUID = 8134629461211328457L;
	
	public LoadFilePanel(final KifKindlet kindlet, final String selectionType, final File basePath, FilenameFilter filter) {
	
		this.setLayout(new KBoxLayout(this, KBoxLayout.Y_AXIS));
		
        ActionListener gameSelectedAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				kindlet.fileSelected(selectionType, new File(basePath, ((KWTSelectableLabel) e.getSource()).getText()));
			}
		};
		
		File[] games = getFiles(basePath, filter);
        for (int i = 0; i < games.length; i++) {
            KWTSelectableLabel label = new KWTSelectableLabel(games[i].getName());
            label.setFocusable(true);
            label.setUnderlineStyle(KWTSelectableLabel.STYLE_DASHED);
            label.addActionListener(gameSelectedAction);
            add(label);
        }
	}
	
	
	
	private File[] getFiles(File basePath, FilenameFilter filter) {
		if (!basePath.exists())
			return new File[0];
		
		return basePath.listFiles(filter);
	}
}
