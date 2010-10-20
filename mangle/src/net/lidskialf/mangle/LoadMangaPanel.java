package net.lidskialf.mangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;

import org.kwt.ui.KWTSelectableLabel;

import com.amazon.kindle.kindlet.ui.KBoxLayout;
import com.amazon.kindle.kindlet.ui.KPanel;

public class LoadMangaPanel extends KPanel {

	private static final long serialVersionUID = 8134629461211328457L;
	
	public LoadMangaPanel(final Manglet minglet, final File basePath, FilenameFilter filter) {
	
		this.setLayout(new KBoxLayout(this, KBoxLayout.Y_AXIS));
		
        ActionListener gameSelectedAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				minglet.fileSelected(new File(basePath, ((KWTSelectableLabel) e.getSource()).getText()));
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
