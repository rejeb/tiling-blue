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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** The two UI themes as the build writes them from src/main/theme, and their registration in plugin.xml. */
class ThemeFilesTest {
  private static final String DARK = "/theme/tilingblue-dark.theme.json";
  private static final String ISLAND_DARK = "/theme/tilingblue-island-dark.theme.json";

  private static JsonObject theme(String path) throws Exception {
    try (InputStream in = ThemeFilesTest.class.getResourceAsStream(path)) {
      assertNotNull(in, path + " is not in the plugin's resources");
      return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
    }
  }

  private static JsonObject ui(String path, String group) throws Exception {
    return theme(path).getAsJsonObject("ui").getAsJsonObject(group);
  }

  @Test
  void dark_inheritsDark() throws Exception {
    JsonObject dark = theme(DARK);
    assertEquals("Tiling Blue Dark", dark.get("name").getAsString());
    assertEquals("ExperimentalDark", dark.get("parentTheme").getAsString());
    assertFalse(dark.getAsJsonObject("ui").has("Islands"));
  }

  @Test
  void dark_hasItsTabUnderlinesAndBorders() throws Exception {
    // used by IntelliJ's regular painters, which only Dark has
    assertEquals(2, ui(DARK, "EditorTabs").get("underlineHeight").getAsInt());
    assertEquals("tb-line", ui(DARK, "MainToolbar").get("borderColor").getAsString());
    assertEquals("tb-hi", ui(DARK, "ToolWindow").get("HeaderTab.underlineColor").getAsString());
  }

  @Test
  void islandDark_inheritsIslandsDark() throws Exception {
    JsonObject islandDark = theme(ISLAND_DARK);
    assertEquals("Tiling Blue Island Dark", islandDark.get("name").getAsString());
    assertEquals("Islands Dark", islandDark.get("parentTheme").getAsString());
    assertEquals(1, islandDark.getAsJsonObject("ui").get("Islands").getAsInt());
  }

  @Test
  void islandDark_withoutDarkOnlyKeys() throws Exception {
    assertFalse(ui(ISLAND_DARK, "EditorTabs").has("underlineHeight"));
    assertFalse(ui(ISLAND_DARK, "MainToolbar").has("borderColor"));
    assertFalse(ui(ISLAND_DARK, "ToolWindow").has("HeaderTab.underlineColor"));
  }

  @Test
  void islandDark_editorTabHeaderAsInVersion100() throws Exception {
    // the EditorTabs keys of 1.0.0 (commit 928d3de), unchanged: IntelliJ's own tab height, outlined tabs
    String v100 = """
      {
        "background": "tb-bar",
        "underTabsBorderColor": "tb-line",
        "regularBackground": "#00000000",
        "regularBorderColor": "tb-line",
        "hoverBackground": "tb-hover",
        "hoverInactiveBackground": "tb-hover",
        "hoverBorderColor": "tb-link",
        "underlinedTabBackground": "tb-hi-deep",
        "underlinedBorderColor": "tb-hi",
        "underlinedTabForeground": "tb-fg",
        "inactiveUnderlinedTabBackground": "tb-panel",
        "inactiveUnderlinedTabBorderColor": "tb-hi",
        "underlinedTabInactiveForeground": "tb-fg",
        "inactiveColoredFileBackground": "#00040c80",
        "unselectedAlpha": 1.0,
        "unselectedBlend": 1.0
      }""";
    assertEquals(JsonParser.parseString(v100), ui(ISLAND_DARK, "EditorTabs"));
  }

  @Test
  void islandDark_toolWindowHeaderHeightAsInVersion100() throws Exception {
    // the editor tab row is at least as tall as a tool window header (EditorTabs.minHeaderHeight): IntelliJ's height
    assertFalse(ui(ISLAND_DARK, "ToolWindow").has("Header.height"));
  }

  @Test
  void dark_keepsItsEditorTabHeader() throws Exception {
    // 36 px row (tabInsets, 34 px tool window headers), no outline around the tabs
    JsonObject tabs = ui(DARK, "EditorTabs");
    assertEquals("-4,8,-4,8", tabs.get("tabInsets").getAsString());
    assertEquals("#00000000", tabs.get("regularBorderColor").getAsString());
    assertEquals("tb-line", tabs.get("inactiveUnderlinedTabBorderColor").getAsString());
    assertEquals(34, ui(DARK, "ToolWindow").get("Header.height").getAsInt());
  }

  @Test
  void bothThemes_shareColorsIconsAndScheme() throws Exception {
    JsonObject dark = theme(DARK);
    JsonObject islandDark = theme(ISLAND_DARK);
    for (String key : List.of("dark", "author", "editorScheme", "colors", "icons", "iconColorsOnSelection")) {
      assertTrue(dark.has(key), key);
      assertEquals(dark.get(key), islandDark.get(key), key);
    }
    // a key shared by both, next to Dark's own ones
    assertEquals(ui(DARK, "EditorTabs").get("underTabsBorderColor"), ui(ISLAND_DARK, "EditorTabs").get("underTabsBorderColor"));
  }

  @Test
  void pluginXml_islandDarkLast_withTheIdOfEarlierVersions() throws Exception {
    // IntelliJ switches to the last theme a newly installed plugin adds: Island Dark is the default. Earlier versions
    // had one theme, based on Islands Dark, under this id: it stays selected after an update.
    List<String[]> providers = new ArrayList<>();
    try (InputStream in = ThemeFilesTest.class.getResourceAsStream("/META-INF/plugin.xml")) {
      NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in)
        .getElementsByTagName("themeProvider");
      for (int i = 0; i < nodes.getLength(); i++) {
        Element e = (Element)nodes.item(i);
        providers.add(new String[]{e.getAttribute("id"), e.getAttribute("path")});
      }
    }
    assertEquals(2, providers.size());
    assertEquals(List.of(TilingBlueTheme.DARK_ID, DARK), List.of(providers.get(0)));
    assertEquals(List.of(TilingBlueTheme.ISLAND_DARK_ID, ISLAND_DARK), List.of(providers.get(1)));
    assertEquals("566b065c-458c-4106-8861-d1f432b6a40d", TilingBlueTheme.ISLAND_DARK_ID);
  }
}
