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

import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo;

/** The Tiling Blue UI theme registered by the {@code themeProvider} in plugin.xml. */
final class TilingBlueTheme {
  /** The {@code themeProvider} id in plugin.xml. */
  static final String ID = "566b065c-458c-4106-8861-d1f432b6a40d";

  private TilingBlueTheme() {
  }

  static boolean isActive() {
    UIThemeLookAndFeelInfo laf = LafManager.getInstance().getCurrentUIThemeLookAndFeel();
    return laf != null && ID.equals(laf.getId());
  }
}
