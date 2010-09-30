package net.lidskialf.ktest;

import com.amazon.kindle.kindlet.*;
import com.amazon.kindle.kindlet.ui.*;

public class ktest extends AbstractKindlet {
	
	private KindletContext ctx;

	public void create(KindletContext context) {
		this.ctx = context;
	}

	public void start() {
		try {
			KOptionPane.showConfirmDialog(ctx.getRootContainer(), "HELLO!");
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}	
}
