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

import java.awt.image.BufferedImage;

import org.zmpp.blorb.NativeImage;

public class AwtImage implements NativeImage {		
	private BufferedImage image;
	
	public AwtImage(BufferedImage image) { 
		this.image = image; 
	}
	
	public BufferedImage getImage() { 
		return image; 
	}
	
	public int getWidth() { 
		return image.getWidth(); 
	}
	
	public int getHeight() { 
		return image.getHeight(); 
	}
}
