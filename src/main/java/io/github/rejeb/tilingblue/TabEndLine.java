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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the line after the last editor tab goes. With its registry setting on, IntelliJ draws a line between editor
 * tabs, on the right edge of each tab, but none after the last tab of a row; the design has one there too. Pure: works
 * on the tab label bounds, in the coordinates of the tabs component.
 */
final class TabEndLine {
  /** IntelliJ's tab border width in the new UI (EditorTabTheme.topBorderThickness), unscaled like there. */
  static final int THICKNESS = 1;
  /** IntelliJ leaves out the line of a tab that ends this close to the edge (EditorTabPainterAdapter). */
  private static final int EDGE_SLACK = 1;

  private TabEndLine() {
  }

  /**
   * One line per row of tabs, on the right edge of its last tab and its full height, as IntelliJ draws its own; none
   * when that tab reaches the edge of the tabs (or is scrolled past it). On the selected tab the line leaves its
   * underline clear (IntelliJ paints the underline over its own lines; this line is drawn over everything): along the
   * bottom of the tab, or along the top when the tabs are at the bottom.
   *
   * @param selected the selected tab's label bounds, or null
   */
  static List<Rectangle> lines(List<Rectangle> labels, int tabsWidth, Rectangle selected, int underlineHeight,
                                boolean tabsAtBottom) {
    Map<Integer, Rectangle> lastInRow = new LinkedHashMap<>();
    for (Rectangle label : labels) {
      if (label.isEmpty()) continue;
      lastInRow.merge(label.y, label, (a, b) -> b.x + b.width > a.x + a.width ? b : a);
    }
    List<Rectangle> lines = new ArrayList<>();
    for (Rectangle last : lastInRow.values()) {
      int right = last.x + last.width;
      if (right >= tabsWidth - EDGE_SLACK) continue;
      int top = last.y;
      int bottom = last.y + last.height;
      if (last.equals(selected)) {
        if (tabsAtBottom) top = Math.max(top, last.y + underlineHeight);
        else bottom = Math.min(bottom, last.y + last.height - underlineHeight);
      }
      if (bottom > top) lines.add(new Rectangle(right - THICKNESS, top, THICKNESS, bottom - top));
    }
    lines.sort((a, b) -> Integer.compare(a.y, b.y));
    return lines;
  }
}
