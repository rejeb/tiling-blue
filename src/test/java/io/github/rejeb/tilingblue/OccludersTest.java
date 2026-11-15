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
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import org.junit.jupiter.api.Test;

class OccludersTest {
  private static JPanel panel(int x, int y, int w, int h) {
    JPanel p = new JPanel(null);
    p.setBounds(x, y, w, h);
    return p;
  }

  @Test
  void siblingInFront_covers() {
    // Swing paints the first child last, on top
    JPanel parent = panel(0, 0, 500, 500);
    JPanel front = panel(50, 60, 100, 100);
    JPanel target = panel(20, 30, 300, 300);
    parent.add(front);
    parent.add(target);
    assertEquals(List.of(new Rectangle(30, 30, 100, 100)), Occluders.above(target));
  }

  @Test
  void siblingBehind_doesNotCover() {
    JPanel parent = panel(0, 0, 500, 500);
    JPanel target = panel(20, 30, 300, 300);
    parent.add(target);
    parent.add(panel(50, 60, 100, 100));
    assertEquals(List.of(), Occluders.above(target));
  }

  @Test
  void hiddenSibling_doesNotCover() {
    JPanel parent = panel(0, 0, 500, 500);
    JPanel front = panel(50, 60, 100, 100);
    front.setVisible(false);
    parent.add(front);
    parent.add(panel(20, 30, 300, 300));
    assertEquals(List.of(), Occluders.above((JPanel)parent.getComponent(1)));
  }

  @Test
  void popupOverAnAncestor_covers() {
    // a balloon or popup in a higher layer, over the panel that holds the tabs
    JLayeredPane layers = new JLayeredPane();
    layers.setBounds(0, 0, 800, 600);
    JPanel content = panel(0, 0, 800, 600);
    JPanel target = panel(10, 40, 400, 300);
    content.add(target);
    layers.add(content, JLayeredPane.DEFAULT_LAYER);
    layers.add(panel(100, 50, 200, 80), JLayeredPane.POPUP_LAYER);
    assertEquals(List.of(new Rectangle(90, 10, 200, 80)), Occluders.above(target));
  }

  @Test
  void glassPane_doesNotCover() {
    // the line itself is drawn on the glass pane
    JRootPane root = new JRootPane();
    root.setBounds(0, 0, 800, 600);
    root.getGlassPane().setBounds(0, 0, 800, 600);
    root.getGlassPane().setVisible(true);
    root.getLayeredPane().setBounds(0, 0, 800, 600);
    JPanel contentPane = panel(0, 0, 800, 600);
    root.setContentPane(contentPane);
    JPanel target = panel(10, 40, 400, 300);
    contentPane.add(target);
    assertEquals(List.of(), Occluders.above(target));
  }
}
