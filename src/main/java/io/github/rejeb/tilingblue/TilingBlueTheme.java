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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.UIThemeLookAndFeelInfo;

/** The two Tiling Blue UI themes registered by the {@code themeProvider}s in plugin.xml. */
final class TilingBlueTheme {
  /** Tiling Blue Dark: based on Dark, full-height editor tabs with lines between them. */
  static final String DARK_ID = "a2065515-ee33-47ac-91e5-4bae224eb426";
  /**
   * Tiling Blue Island Dark, the default: based on Islands Dark. The id of the single theme of earlier versions (also
   * based on Islands Dark), so it stays selected after an update.
   */
  static final String ISLAND_DARK_ID = "566b065c-458c-4106-8861-d1f432b6a40d";
  /** The plugin id in plugin.xml. */
  static final String PLUGIN_ID = "io.github.rejeb.tilingblue";

  private TilingBlueTheme() {
  }

  /** Either Tiling Blue theme: fonts, title bars and the spinner follow both. */
  static boolean isTilingBlue(String themeId) {
    return DARK_ID.equals(themeId) || ISLAND_DARK_ID.equals(themeId);
  }

  /**
   * The theme whose editor tabs get lines between them and after the last one ({@link TabDividers},
   * {@link TabEndLines}): only Dark has IntelliJ's regular tab painters; Island Dark's tabs are islands boxes.
   */
  static boolean hasTabLines(String themeId) {
    return DARK_ID.equals(themeId);
  }

  static boolean isActive() {
    return isTilingBlue(currentId());
  }

  static boolean isActiveWithTabLines() {
    return hasTabLines(currentId());
  }

  private static String currentId() {
    UIThemeLookAndFeelInfo laf = LafManager.getInstance().getCurrentUIThemeLookAndFeel();
    return laf == null ? null : laf.getId();
  }

  /** Whether a dynamic plugin event is about this plugin. */
  static boolean isThisPlugin(IdeaPluginDescriptor descriptor) {
    return PLUGIN_ID.equals(descriptor.getPluginId().getIdString());
  }
}
