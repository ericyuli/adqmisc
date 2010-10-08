package net.lidskialf.kif;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import org.kwt.ui.KWTSelectableLabel;

import com.amazon.kindle.kindlet.ui.KBoxLayout;
import com.amazon.kindle.kindlet.ui.KPanel;

public class GameSelectorPanel extends KPanel {

	private static final long serialVersionUID = 8134629461211328457L;
	
	private KifKindlet kindlet;
	
	public GameSelectorPanel(final KifKindlet kindlet) {
		
		this.kindlet = kindlet;

		this.setLayout(new KBoxLayout(this, KBoxLayout.Y_AXIS));
		
        ActionListener gameSelectedAction = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				kindlet.loadGame(((KWTSelectableLabel) e.getSource()).getText());
			}
		};
		
		File[] games = getGames();
        for (int i = 0; i < games.length; i++) {
            KWTSelectableLabel label = new KWTSelectableLabel(games[i].getName());
            label.setFocusable(true);
            label.setEnabled(true);
            label.setUnderlineStyle(KWTSelectableLabel.STYLE_DASHED);
            label.addActionListener(gameSelectedAction);
            add(label);
        }        
	}
	
	private File[] getGames() {
		ArrayList results = new ArrayList();
		File homeDir = kindlet.getContext().getHomeDirectory();
		if (!homeDir.exists())
			return new File[0];
		
		File[] files = homeDir.listFiles();
		for(int i=0; i< files.length; i++) {
			if (files[i].isDirectory())
				continue;
			
			String filename = files[i].getName();
			int dotPos = filename.indexOf('.');
			if (dotPos == -1)
				continue;
			
			String ext = filename.substring(dotPos+1).toLowerCase();
			if (ext.charAt(0) != 'z')
				continue;
			
			results.add(files[i]);
		}
		
		return (File[]) results.toArray(new File[results.size()]);
	}
}
