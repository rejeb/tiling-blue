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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

class TilingBlueThemeTest {
  @Test
  void bothThemes_areTilingBlue() {
    // fonts, title bars and the spinner follow either theme
    assertTrue(TilingBlueTheme.isTilingBlue(TilingBlueTheme.DARK_ID));
    assertTrue(TilingBlueTheme.isTilingBlue(TilingBlueTheme.ISLAND_DARK_ID));
  }

  @Test
  void otherThemes_areNotTilingBlue() {
    assertFalse(TilingBlueTheme.isTilingBlue("ExperimentalDark"));
    assertFalse(TilingBlueTheme.isTilingBlue("Islands Dark"));
    assertFalse(TilingBlueTheme.isTilingBlue(null));
  }

  @Test
  void pluginId_isThePluginXmlId() throws Exception {
    // every DynamicPluginListener tells its own plugin's unload apart by this id: with another one (TitleBars had the
    // id of the first prototype) it never cleans up, and the uninstall leaves the plugin loaded
    try (InputStream in = TilingBlueThemeTest.class.getResourceAsStream("/META-INF/plugin.xml")) {
      assertNotNull(in);
      String id = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in)
        .getDocumentElement().getElementsByTagName("id").item(0).getTextContent().trim();
      assertEquals(id, TilingBlueTheme.PLUGIN_ID);
    }
  }

  @Test
  void tabLines_onlyWithDark() {
    // Island Dark's tabs are boxes drawn by IntelliJ's islands painter: no lines between or after them
    assertTrue(TilingBlueTheme.hasTabLines(TilingBlueTheme.DARK_ID));
    assertFalse(TilingBlueTheme.hasTabLines(TilingBlueTheme.ISLAND_DARK_ID));
    assertFalse(TilingBlueTheme.hasTabLines("ExperimentalDark"));
    assertFalse(TilingBlueTheme.hasTabLines(null));
  }
}
