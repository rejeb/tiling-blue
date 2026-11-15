/*
 * Copyright 2026 rbenrejeb
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.rejeb.tilingblue;

import com.intellij.openapi.ui.Painter;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.JBColor;
import com.intellij.ui.tabs.JBTabs;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.ui.JBUI;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the {@link TabEndLine} of one editor tabs component, on the glass pane of its window (installed by
 * {@link TabEndLines} through the public IdeGlassPane API). Drawn only with the lines between tabs, in their colour,
 * and never over what covers the tabs.
 */
final class TabEndPainter implements Painter {
  private final JBTabs tabs;

  TabEndPainter(JBTabs tabs) {
    this.tabs = tabs;
  }

  @Override
  public boolean needsRepaint() {
    // drawn again with every repaint of the window part it is in, so the line follows the tabs
    return true;
  }

  @Override
  public void paint(@NotNull Component component, @NotNull Graphics2D g) {
    // the theme may change before TabEndLines takes the painter off; the lines between tabs may be turned off by hand
    if (!TilingBlueTheme.isActiveWithTabLines() || !Registry.is(TabDividers.KEY, false)) return;
    JBTabsPosition position = tabs.getPresentation().getTabsPosition();
    if (position.isSide()) return;
    JComponent tabsComponent = tabs.getComponent();
    List<Rectangle> labels = new ArrayList<>();
    Rectangle selected = null;
    for (TabInfo info : tabs.getTabs()) {
      Rectangle bounds = bounds(tabs.getTabLabel(info), tabsComponent);
      if (bounds == null) continue;
      labels.add(bounds);
      if (info == tabs.getSelectedInfo()) selected = bounds;
    }
    Rectangle visible = tabsComponent.getVisibleRect();
    Area area = new Area();
    for (Rectangle line : TabEndLine.lines(labels, tabsComponent.getWidth(), selected,
      JBUI.CurrentTheme.EditorTabs.underlineHeight(), position == JBTabsPosition.bottom)) {
      area.add(new Area(line.intersection(visible)));
    }
    Rectangle clip = g.getClipBounds();
    if (area.isEmpty() || clip != null && !area.intersects(clip)) return;
    for (Rectangle cover : Occluders.above(tabsComponent)) {
      area.subtract(new Area(cover));
    }
    g.setColor(JBColor.namedColor("EditorTabs.underTabsBorderColor", JBUI.CurrentTheme.EditorTabs.borderColor()));
    g.fill(area);
  }

  /** A tab label's bounds in the tabs' coordinates, or null when it is not shown. */
  private static Rectangle bounds(Component label, JComponent tabsComponent) {
    if (label == null || !label.isVisible() || label.getParent() == null) return null;
    return SwingUtilities.convertRectangle(label.getParent(), label.getBounds(), tabsComponent);
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    // never asks for a repaint of its own: the tabs repaint when they change
  }

  @Override
  public void removeListener(Listener listener) {
  }
}
