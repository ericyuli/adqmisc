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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;

public class NullLayout implements LayoutManager2 {

	public void addLayoutComponent(String arg0, Component arg1) {
	}

	public void layoutContainer(Container arg0) {
	}

	public Dimension minimumLayoutSize(Container arg0) {
		return arg0.getSize();
	}

	public Dimension preferredLayoutSize(Container arg0) {
		return arg0.getSize();
	}

	public void removeLayoutComponent(Component arg0) {
	}

	public void addLayoutComponent(Component arg0, Object arg1) {
	}

	public float getLayoutAlignmentX(Container arg0) {
		return 0;
	}

	public float getLayoutAlignmentY(Container arg0) {
		return 0;
	}

	public void invalidateLayout(Container arg0) {
	}

	public Dimension maximumLayoutSize(Container arg0) {
		return arg0.getSize();
	}
}
