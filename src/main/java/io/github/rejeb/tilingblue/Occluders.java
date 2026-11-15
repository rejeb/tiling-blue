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

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * What Swing paints over a component: a line drawn for it on the glass pane must leave those parts out, or it would
 * show through popups, balloons and sliding tool windows.
 */
final class Occluders {
  private Occluders() {
  }

  /**
   * Bounds, in {@code target}'s coordinates, of the visible components painted over it: the siblings in front of it
   * and of each of its parents, up to the root pane (whose glass pane is where the line is drawn). Swing paints a
   * container's first child last, so the siblings in front are the ones before it.
   */
  static List<Rectangle> above(Component target) {
    List<Rectangle> covers = new ArrayList<>();
    Component child = target;
    for (Container parent = child.getParent(); parent != null && !(parent instanceof JRootPane);
         child = parent, parent = parent.getParent()) {
      for (Component sibling : parent.getComponents()) {
        if (sibling == child) break;
        if (sibling.isVisible() && sibling.getWidth() > 0 && sibling.getHeight() > 0) {
          covers.add(SwingUtilities.convertRectangle(parent, sibling.getBounds(), target));
        }
      }
    }
    return covers;
  }
}
