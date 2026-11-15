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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import java.awt.Font;
import java.util.List;
import javax.swing.UIManager;
import org.jetbrains.annotations.NotNull;

/**
 * Registers the bundled IBM Plex Mono before the first frame (and its editors) is created, or as soon as the plugin is
 * loaded without a restart, and rebuilds the UI when its font was already set up from a saved Plex Mono setting while
 * the family was not registered yet.
 */
final class Fonts implements AppLifecycleListener, DynamicPluginListener {
  private static final Logger LOG = Logger.getInstance(Fonts.class);

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    register();
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
    // installed or enabled without a restart: appFrameCreated has passed, and the editor scheme asks for the family
    // whether or not the UI font is switched (the owner may keep their own)
    if (TilingBlueTheme.isThisPlugin(descriptor)) register();
  }

  private static void register() {
    if (!BundledFonts.register()) {
      LOG.warn("Could not register the bundled " + BundledFonts.FAMILY + "; editor and UI fall back to IntelliJ's fonts");
      return;
    }
    ApplicationManager.getApplication().invokeLater(() -> {
      Font font = UIManager.getFont("Label.font");
      if (font != null && BundledFonts.isStale(font.getName(), font.getFamily())) {
        LafManager.getInstance().updateUI();
      }
    }, ModalityState.any());
  }
}
