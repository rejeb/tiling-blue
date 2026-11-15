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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.junit.jupiter.api.Test;

class UiClassCacheTest {
  private static final String STUB_UI_ID = "TilingBlueTestStubUI";

  /** Stands in for BootBarUI: a UI delegate a theme names by class, created by Swing through createUI. */
  public static final class StubUI extends ComponentUI {
    public static ComponentUI createUI(JComponent c) {
      return new StubUI();
    }
  }

  /** A component whose delegate is looked up under {@link #STUB_UI_ID}, as a JProgressBar's is under ProgressBarUI. */
  private static final class StubComponent extends JComponent {
    @Override
    public String getUIClassID() {
      return STUB_UI_ID;
    }
  }

  @Test
  void swingCachesTheUiClass_inUIManagerDefaults_pastALookAndFeelChange() throws Exception {
    // why the plugin can't be unloaded cleanly without forget(): the cache outlives the theme that named the class
    LookAndFeel before = UIManager.getLookAndFeel();
    UIDefaults defaults = UIManager.getDefaults();
    try {
      UIManager.put(STUB_UI_ID, StubUI.class.getName());
      assertInstanceOf(StubUI.class, UIManager.getUI(new StubComponent()));
      UIManager.setLookAndFeel(new MetalLookAndFeel());
      assertSame(StubUI.class, defaults.get(StubUI.class.getName()));
      assertNotNull(defaults.get(StubUI.class));
    }
    finally {
      defaults.remove(STUB_UI_ID);
      defaults.remove(StubUI.class.getName());
      defaults.remove(StubUI.class);
      if (before != null) UIManager.setLookAndFeel(before);
    }
  }

  @Test
  void forget_dropsTheCachedClassAndCreateUiMethod() throws Exception {
    UIDefaults defaults = UIManager.getDefaults();
    try {
      UIManager.put(STUB_UI_ID, StubUI.class.getName());
      UIManager.getUI(new StubComponent());
      UiClassCache.forget(defaults, List.of(StubUI.class));
      assertNull(defaults.get(StubUI.class.getName()));
      assertNull(defaults.get(StubUI.class));
    }
    finally {
      defaults.remove(STUB_UI_ID);
      defaults.remove(StubUI.class.getName());
      defaults.remove(StubUI.class);
    }
  }

  @Test
  void forget_keepsTheThemesOwnEntries() {
    UIDefaults defaults = new UIDefaults();
    defaults.put("ClassLoader", StubUI.class.getClassLoader());
    defaults.put(STUB_UI_ID, StubUI.class.getName());
    assertInstanceOf(StubUI.class, defaults.getUI(new StubComponent()));
    UiClassCache.forget(defaults, List.of(StubUI.class));
    assertEquals(StubUI.class.getName(), defaults.get(STUB_UI_ID));
    // the next delegate caches it again
    assertInstanceOf(StubUI.class, defaults.getUI(new StubComponent()));
  }

  @Test
  void uiClasses_areTheClassesBothThemesName() throws Exception {
    // a UI class added to the themes but not forgotten would keep the plugin loaded after an uninstall
    Set<String> named = new TreeSet<>();
    for (String path : List.of("/theme/tilingblue-dark.theme.json", "/theme/tilingblue-island-dark.theme.json")) {
      try (InputStream in = UiClassCacheTest.class.getResourceAsStream(path)) {
        assertNotNull(in, path);
        JsonObject ui = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
          .getAsJsonObject("ui");
        for (Map.Entry<String, JsonElement> e : ui.entrySet()) {
          if (e.getKey().endsWith("UI") && e.getValue().isJsonPrimitive()
              && e.getValue().getAsString().startsWith(UiClassCache.class.getPackageName() + ".")) {
            named.add(e.getValue().getAsString());
          }
        }
      }
    }
    assertEquals(Set.of(BootBarUI.class.getName()), named);
    assertEquals(named, UiClassCache.UI_CLASSES.stream().map(Class::getName).collect(Collectors.toCollection(TreeSet::new)));
  }
}
