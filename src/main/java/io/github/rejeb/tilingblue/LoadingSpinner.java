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

import com.intellij.icons.AllIcons;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.AnimatedIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Switches IntelliJ to its frame-based loading spinner, whose frames the theme replaces with the focus cycle.
 * <p>
 * IntelliJ draws its current spinner in code ({@code SpinningProgressIcon}); a theme can only recolour it
 * ({@code ProgressIcon.color}). The older spinner shows the icons {@code /process/step_1..8.svg}, which the Tiling
 * Blue themes map to {@code /theme/icons/process/} (src/main/theme/tilingblue.theme.json). IntelliJ picks one of the
 * two once per session, when the first spinner is created, from the system property {@value #FRAMES_PROPERTY}.
 * This sets that property as the first frame opens, if a Tiling Blue theme is active and the user hasn't set the
 * property either way.
 * <p>
 * A session that starts with another theme keeps the code-drawn spinner (red under Tiling Blue) until the next
 * restart: when a Tiling Blue theme is chosen in such a session (the plugin installed without a restart, or the theme
 * picked later), this offers the restart, once.
 */
public final class LoadingSpinner implements AppLifecycleListener, LafManagerListener, DynamicPluginListener {
  private static final String FRAMES_PROPERTY = "disable.new.spinning.icon";
  private static final String GROUP = "Tiling Blue";
  private static final Logger LOG = Logger.getInstance(LoadingSpinner.class);

  // EDT-confined: offered in this load of the plugin, and the offer while it is showing
  private static boolean offered;
  private static Notification offer;

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    if (System.getProperty(FRAMES_PROPERTY) != null || !TilingBlueTheme.isActive()) return;
    System.setProperty(FRAMES_PROPERTY, "true");
    // AnimatedIcon.Default reads the property when it is first used; if that already happened, it is too late.
    if (AnimatedIcon.Default.ICONS.get(0) == AllIcons.Process.Step_1) {
      LOG.info("Tiling Blue: using the focus-cycle loading spinner");
    }
    else {
      LOG.info("Tiling Blue: a loading spinner already existed, keeping IntelliJ's spinner for this session");
    }
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    later(LoadingSpinner::offerRestartIfNeeded);
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
    // installed without a restart: IntelliJ switches to the new theme a little later (lookAndFeelChanged), unless the
    // theme follows the OS; an update finds its theme in place
    if (TilingBlueTheme.isThisPlugin(descriptor)) later(LoadingSpinner::offerRestartIfNeeded);
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor descriptor, boolean isUpdate) {
    // its actions are this plugin's classes: a balloon left up would keep the plugin loaded
    if (TilingBlueTheme.isThisPlugin(descriptor)) ApplicationManager.getApplication().invokeLater(() -> {
      Notification n = offer;
      offer = null;
      if (n != null) n.expire();
    }, ModalityState.any());
  }

  /**
   * Whether a restart would bring the focus-cycle spinner: a Tiling Blue theme is active, and nothing set
   * {@value #FRAMES_PROPERTY} in this session, so {@link #appFrameCreated} sets it at the next start. Once it is set
   * (by this class at startup, or by the owner), a restart changes nothing.
   */
  static boolean restartSwitchesSpinner(boolean tilingBlueActive, @Nullable String framesProperty) {
    return tilingBlueActive && framesProperty == null;
  }

  private static void offerRestartIfNeeded() {
    if (offered || !restartSwitchesSpinner(TilingBlueTheme.isActive(), System.getProperty(FRAMES_PROPERTY))) return;
    offered = true;
    Notification n = NotificationGroupManager.getInstance().getNotificationGroup(GROUP).createNotification(
      "Tiling Blue",
      // short enough for the two lines a collapsed balloon shows
      "The tiled loading spinner shows after a restart.",
      NotificationType.INFORMATION);
    n.addAction(NotificationAction.createSimpleExpiring("Restart", () -> ApplicationManager.getApplication().restart()));
    n.addAction(NotificationAction.createSimpleExpiring("Later", () -> {
    }));
    n.whenExpired(() -> {
      if (offer == n) offer = null;
    });
    offer = n;
    n.notify(null);
  }

  /** On the EDT, outside modal dialogs: a theme picked or a plugin installed in Settings shows it once Settings closes. */
  private static void later(Runnable r) {
    ApplicationManager.getApplication().invokeLater(r, ModalityState.nonModal());
  }
}
