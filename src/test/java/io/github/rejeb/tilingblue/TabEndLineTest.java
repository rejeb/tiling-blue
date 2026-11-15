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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Rectangle;
import java.util.List;
import org.junit.jupiter.api.Test;

class TabEndLineTest {
  /** No tab selected (no underline to keep clear). */
  private static List<Rectangle> lines(List<Rectangle> labels, int tabsWidth) {
    return TabEndLine.lines(labels, tabsWidth, null, 2, false);
  }

  @Test
  void lastTabShortOfEdge_lineOnItsRightEdge() {
    // like IntelliJ's own line between two tabs: the last pixel column of the tab, its full height
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 100, 30), new Rectangle(100, 0, 80, 30));
    assertEquals(List.of(new Rectangle(179, 0, 1, 30)), lines(labels, 400));
  }

  @Test
  void lastTabAtEdge_noLine() {
    // IntelliJ draws no line where a tab ends at the edge (within one pixel)
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 100, 30), new Rectangle(100, 0, 299, 30));
    assertEquals(List.of(), lines(labels, 400));
  }

  @Test
  void lastTabTwoPixelsShortOfEdge_line() {
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 398, 30));
    assertEquals(List.of(new Rectangle(397, 0, 1, 30)), lines(labels, 400));
  }

  @Test
  void lastTabScrolledPastEdge_noLine() {
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 300, 30), new Rectangle(300, 0, 200, 30));
    assertEquals(List.of(), lines(labels, 400));
  }

  @Test
  void noTabs_noLine() {
    assertEquals(List.of(), lines(List.of(), 400));
  }

  @Test
  void emptyLabels_ignored() {
    // hidden tabs keep a label with no size
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 100, 30), new Rectangle(0, 0, 0, 0));
    assertEquals(List.of(new Rectangle(99, 0, 1, 30)), lines(labels, 400));
  }

  @Test
  void labelsInAnyOrder_lastByPosition() {
    List<Rectangle> labels = List.of(new Rectangle(100, 0, 80, 30), new Rectangle(0, 0, 100, 30));
    assertEquals(List.of(new Rectangle(179, 0, 1, 30)), lines(labels, 400));
  }

  @Test
  void severalRows_oneLinePerRow() {
    // tabs wrapped in rows (or pinned tabs in a row of their own): IntelliJ leaves out the line after the last tab of each
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 100, 30), new Rectangle(100, 0, 100, 30),
      new Rectangle(0, 30, 150, 30));
    assertEquals(List.of(new Rectangle(199, 0, 1, 30), new Rectangle(149, 30, 1, 30)), lines(labels, 400));
  }

  @Test
  void selectedLastTab_lineStopsAtUnderline() {
    // IntelliJ paints the underline of the selected tab over its own line: the line must not cover it
    Rectangle selected = new Rectangle(100, 0, 80, 30);
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 100, 30), selected);
    assertEquals(List.of(new Rectangle(179, 0, 1, 28)), TabEndLine.lines(labels, 400, selected, 2, false));
  }

  @Test
  void selectedLastTab_tabsAtBottom_lineStartsBelowUnderline() {
    // tabs at the bottom of the editor have the underline along the top of the tab
    Rectangle selected = new Rectangle(100, 0, 80, 30);
    List<Rectangle> labels = List.of(new Rectangle(0, 0, 100, 30), selected);
    assertEquals(List.of(new Rectangle(179, 2, 1, 28)), TabEndLine.lines(labels, 400, selected, 2, true));
  }

  @Test
  void selectedTabNotLast_lastLineFullHeight() {
    Rectangle selected = new Rectangle(0, 0, 100, 30);
    List<Rectangle> labels = List.of(selected, new Rectangle(100, 0, 80, 30));
    assertEquals(List.of(new Rectangle(179, 0, 1, 30)), TabEndLine.lines(labels, 400, selected, 2, false));
  }
}
