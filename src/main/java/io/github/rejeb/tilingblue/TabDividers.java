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

import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import io.github.rejeb.tilingblue.TabDividerPolicy.Outcome;
import java.awt.Window;
import java.util.List;
import java.util.MissingResourceException;
import org.jetbrains.annotations.NotNull;

/**
 * Lines between editor tabs while Tiling Blue Dark is the theme, as in the design. IntelliJ draws them only when its
 * registry setting {@value #KEY} is on (no theme key exists), in the colour of {@code EditorTabs.underTabsBorderColor}.
 * The decisions are in {@link TabDividerPolicy}; this class reads and writes the setting through the public Registry
 * API.
 */
final class TabDividers implements LafManagerListener, AppLifecycleListener, DynamicPluginListener {
  private static final Logger LOG = Logger.getInstance(TabDividers.class);
  static final String KEY = "ide.new.editor.tabs.vertical.borders";
  /** Set when this plugin starts unloading (each loaded version of the plugin has its own). */
  private static volatile boolean unloading;

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    later(TabDividers::sync);
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    later(TabDividers::sync);
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
    if (TilingBlueTheme.isThisPlugin(descriptor)) later(TabDividers::sync);
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor descriptor, boolean isUpdate) {
    if (!TilingBlueTheme.isThisPlugin(descriptor)) return;
    unloading = true;
    // an update keeps the dividers: the new version finds them on and remembered
    if (!isUpdate) apply(false);
  }

  private static void sync() {
    apply(TilingBlueTheme.isActiveWithTabLines());
  }

  private static void apply(boolean active) {
    RegistryValue setting = Registry.get(KEY);
    boolean on;
    try {
      on = setting.asBoolean();
    } catch (MissingResourceException e) {
      // the key is gone in this IDE version: the tabs just keep IntelliJ's look
      LOG.info("Tiling Blue: no registry setting " + KEY + "; editor tabs are drawn without lines between them");
      return;
    }
    TabDividerState state = TabDividerState.getInstance();
    boolean applied = state.applied();
    Outcome outcome = active ? TabDividerPolicy.onActive(applied, on) : TabDividerPolicy.onInactive(applied, on);
    state.setApplied(outcome.applied());
    if (outcome.setTo() != null) {
      setting.setValue(outcome.setTo());
      // the setting and the flag are saved together: after a crash in between, the setting would be on with no
      // flag, and look like the owner's own (never turned off again)
      ApplicationManager.getApplication().saveSettings();
      // the tabs read the setting while painting
      for (Window window : Window.getWindows()) window.repaint();
    }
  }

  /**
   * On the EDT, outside modal dialogs (the same moment {@link UiFontSwitcher} acts). Not once the plugin is being
   * unloaded: the theme change that comes with the unload queues a sync that would find the plugin's state gone.
   */
  private static void later(Runnable r) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!unloading) r.run();
    }, ModalityState.nonModal());
  }
}
