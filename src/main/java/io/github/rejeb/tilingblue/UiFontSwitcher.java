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
import com.intellij.ide.ui.UISettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import io.github.rejeb.tilingblue.UiFontPolicy.Action;
import io.github.rejeb.tilingblue.UiFontPolicy.Choice;
import io.github.rejeb.tilingblue.UiFontPolicy.Memory;
import io.github.rejeb.tilingblue.UiFontPolicy.UiFont;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * Sets the UI font to IBM Plex Mono 14 while Tiling Blue is the theme, and gives the previous font back when another
 * theme is chosen. A custom font the owner picked is only replaced after asking (notification, remembered).
 * See {@link UiFontPolicy} for the decisions.
 */
final class UiFontSwitcher implements LafManagerListener, AppLifecycleListener, DynamicPluginListener {
  private static final Logger LOG = Logger.getInstance(UiFontSwitcher.class);
  private static final String GROUP = "Tiling Blue";

  // EDT-confined: whether Tiling Blue was the theme at the last check (null = not checked since start / load)
  private static Boolean wasActive;
  private static Notification asking;

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    later(UiFontSwitcher::sync);
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    later(UiFontSwitcher::sync);
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
    if (TilingBlueTheme.isThisPlugin(descriptor)) {
      later(() -> {
        wasActive = null; // a fresh load acts like a start
        sync();
      });
    }
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor descriptor, boolean isUpdate) {
    // an update keeps the font: the new version finds it applied and leaves it
    if (TilingBlueTheme.isThisPlugin(descriptor) && !isUpdate) {
      leave();
      wasActive = null;
    }
  }

  /** Compares the current theme with the last one seen; runs the startup, enter or leave decision. */
  private static void sync() {
    boolean active = TilingBlueTheme.isActive();
    Boolean before = wasActive;
    wasActive = active;
    if (before == null) {
      if (active) act(UiFontPolicy.onStartup(now(), state().memory()));
    } else if (active && !before) {
      act(UiFontPolicy.onEnter(now(), state().memory()));
    } else if (!active && before) {
      leave();
    }
  }

  private static void leave() {
    expireQuestion();
    UiFontState state = state();
    Memory mem = state.memory();
    if (UiFontPolicy.onLeave(now(), mem) == Action.RESTORE) {
      write(mem.previous());
    }
    if (mem.applied()) {
      // restored, or changed by the owner since: either way nothing of ours is left to give back
      state.setMemory(new Memory(false, null, mem.choice()));
    }
  }

  private static void act(Action action) {
    switch (action) {
      case APPLY -> apply();
      case ASK -> ask();
      case RESTORE, NOTHING -> {
      }
    }
  }

  private static void apply() {
    if (!BundledFonts.register()) {
      LOG.warn("Tiling Blue: " + BundledFonts.FAMILY + " is not available; the UI font is left as it is");
      return;
    }
    UiFontState state = state();
    Memory mem = state.memory();
    UiFont now = now();
    state.setMemory(new Memory(true, mem.applied() ? mem.previous() : now, mem.choice()));
    write(new UiFont(true, BundledFonts.FAMILY, UiFontPolicy.SIZE));
  }

  private static void ask() {
    expireQuestion();
    UiFont now = now();
    String face = now.face() == null || now.face().isBlank() ? "your font" : now.face();
    Notification n = NotificationGroupManager.getInstance().getNotificationGroup(GROUP).createNotification(
      "Tiling Blue UI font",
      // short enough for the two lines a collapsed balloon shows
      "Use the bundled IBM Plex Mono " + size(UiFontPolicy.SIZE) + " as the UI font? Yours is " + face + " "
      + size(now.size()) + ".",
      NotificationType.INFORMATION);
    n.addAction(NotificationAction.createSimpleExpiring("Use IBM Plex Mono", () -> answer(Choice.USE)));
    n.addAction(NotificationAction.createSimpleExpiring("Keep " + face, () -> answer(Choice.KEEP)));
    n.whenExpired(() -> {
      if (asking == n) asking = null;
    });
    asking = n;
    n.notify(null);
  }

  private static void answer(Choice choice) {
    UiFontState state = state();
    Memory mem = state.memory();
    state.setMemory(new Memory(mem.applied(), mem.previous(), choice));
    if (choice == Choice.USE && TilingBlueTheme.isActive()) apply();
  }

  private static void expireQuestion() {
    Notification n = asking;
    asking = null;
    if (n != null) n.expire();
  }

  private static UiFont now() {
    UISettings s = UISettings.getInstance();
    return new UiFont(s.getOverrideLafFonts(), s.getFontFace(), s.getFontSize2D());
  }

  private static void write(UiFont font) {
    UISettings s = UISettings.getInstance();
    s.setOverrideLafFonts(font.custom());
    if (font.face() != null && !font.face().isBlank()) s.setFontFace(font.face());
    if (font.size() > 0) s.setFontSize2D(font.size());
    s.fireUISettingsChanged();
    LafManager.getInstance().updateUI();
  }

  private static String size(float size) {
    return size == Math.rint(size) ? Integer.toString((int) size) : String.format(Locale.ROOT, "%.1f", size);
  }

  private static UiFontState state() {
    return UiFontState.getInstance();
  }

  /** On the EDT, outside modal dialogs: Settings would write its own font fields back over ours on OK. */
  private static void later(Runnable r) {
    ApplicationManager.getApplication().invokeLater(r, ModalityState.nonModal());
  }
}
