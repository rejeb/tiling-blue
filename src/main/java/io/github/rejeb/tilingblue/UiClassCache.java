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

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.List;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import org.jetbrains.annotations.NotNull;

/**
 * Takes the themes' own UI classes out of Swing's cache when the plugin is unloaded, so that nothing keeps it loaded.
 * <p>
 * Both themes name {@link BootBarUI} as their {@code ProgressBarUI}. When Swing creates such a delegate
 * ({@code UIDefaults.getUI}), it caches the class under its name and its {@code createUI} method under the class, in
 * the defaults of {@code UIManager}: a table that outlives every theme. Left there after an uninstall, they keep the
 * plugin's classes loaded and IntelliJ advises a restart; after an update, progress bars would keep the old version's
 * class.
 * <p>
 * While a Tiling Blue theme is in place, a progress bar created meanwhile caches the class again, and IntelliJ replaces
 * the theme of an unloaded plugin only after the plugin's listeners are gone: so this also forgets them once more when
 * that theme change comes.
 */
final class UiClassCache implements DynamicPluginListener {
  /** The UI classes the themes name (src/main/theme/tilingblue.theme.json). */
  static final List<Class<?>> UI_CLASSES = List.of(BootBarUI.class);

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor descriptor, boolean isUpdate) {
    if (!TilingBlueTheme.isThisPlugin(descriptor)) return;
    forget(UIManager.getDefaults(), UI_CLASSES);
    if (TilingBlueTheme.isActive()) forgetAfterNextThemeChange();
  }

  /**
   * Once Swing has the next look and feel, whose defaults no longer name these classes; before the components get their
   * new delegates. Swing's own listener list: IntelliJ drops the plugin's message bus connections when it unloads it.
   */
  private static void forgetAfterNextThemeChange() {
    UIManager.addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent event) {
        if (!"lookAndFeel".equals(event.getPropertyName())) return;
        UIManager.removePropertyChangeListener(this);
        forget(UIManager.getDefaults(), UI_CLASSES);
      }
    });
  }

  /** Removes what {@code UIDefaults.getUI} cached for these classes; the theme's own entries stay. */
  static void forget(UIDefaults defaults, Collection<Class<?>> uiClasses) {
    for (Class<?> uiClass : uiClasses) {
      defaults.remove(uiClass.getName());
      defaults.remove(uiClass);
    }
  }
}
