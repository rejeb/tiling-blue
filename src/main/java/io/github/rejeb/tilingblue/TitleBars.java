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
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialog title bars on Linux/Wayland.
 * <p>
 * IntelliJ draws its own themed header on dialogs only on Windows and macOS. On Wayland the JetBrains Runtime
 * draws dialog title bars itself by painting a GTK header bar, so they follow the desktop's GTK theme. While
 * Tiling Blue is active, this adds {@code /theme/gtk-titlebar.css} to that in-process GTK at USER priority,
 * and removes it again when another theme is chosen or the plugin is unloaded. Everywhere else it does nothing.
 */
public final class TitleBars implements AppLifecycleListener, LafManagerListener, DynamicPluginListener {
  private static final String KEY_PREFIX = "TilingBlue.TitleBar.";
  private static final Logger LOG = Logger.getInstance(TitleBars.class);

  /** CSS placeholder -> fallback colour when the theme doesn't define {@code TilingBlue.TitleBar.<name>}. */
  private static final Map<String, Color> DEFAULTS = Map.ofEntries(
    Map.entry("background", new Color(0x041430)),
    Map.entry("borderColor", new Color(0x2f8cff)),
    Map.entry("foreground", new Color(0xe8f1ff)),
    Map.entry("inactiveForeground", new Color(0x6c86b0)),
    Map.entry("buttonForeground", new Color(0xa9c4ea)),
    Map.entry("buttonBorderColor", new Color(0x2f, 0x8c, 0xff, 0x8c)),
    Map.entry("buttonInactiveBorderColor", new Color(0x2f, 0x8c, 0xff, 0x4d)),
    Map.entry("buttonHoverBackground", new Color(0x2f, 0x8c, 0xff, 0x2e)),
    Map.entry("buttonPressedBackground", new Color(0x2f, 0x8c, 0xff, 0x4d)),
    Map.entry("closeHoverBackground", new Color(0xff3b3b)),
    Map.entry("closeHoverForeground", new Color(0x00040c)));

  // EDT-confined state
  private static Boolean supported;
  private static GtkCss gtk;
  private static String template;
  private static String installedCss;
  private static AWTEventListener windowListener;
  /**
   * Set when this plugin starts unloading. IntelliJ replaces the theme of an unloaded plugin after
   * {@link #beforePluginUnload}; should that change still reach this listener, it must not put the window listener
   * back: the toolkit would keep the plugin loaded (each loaded version of the plugin has its own).
   */
  private static volatile boolean unloading;

  // Not appStarted(): it is @ApiStatus.Internal, which JetBrains Marketplace flags. appFrameCreated() runs before
  // the first window opens, and the AWT listener installed by start() handles every window after that.
  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    onEdt(TitleBars::start);
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    onEdt(TitleBars::start);
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
    if (TilingBlueTheme.isThisPlugin(descriptor)) onEdt(TitleBars::start);
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor descriptor, boolean isUpdate) {
    if (!TilingBlueTheme.isThisPlugin(descriptor)) return;
    unloading = true;
    onEdt(TitleBars::stop);
  }

  private static void start() {
    if (unloading || !isSupported()) return;
    if (windowListener == null) {
      // GTK is loaded by the runtime when the first window gets its peer; re-check whenever a window opens
      windowListener = event -> {
        if (event.getID() == WindowEvent.WINDOW_OPENED) refresh();
      };
      Toolkit.getDefaultToolkit().addAWTEventListener(windowListener, AWTEvent.WINDOW_EVENT_MASK);
    }
    refresh();
  }

  private static void stop() {
    if (windowListener != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(windowListener);
      windowListener = null;
    }
    uninstall();
  }

  private static void refresh() {
    if (!isSupported()) return;
    try {
      if (!TilingBlueTheme.isActive()) {
        uninstall();
        return;
      }
      if (!gtk.isGtkReady()) return;
      String css = css();
      if (!css.equals(installedCss)) {
        gtk.install(css);
        if (installedCss == null) LOG.info("Tiling Blue: styling GTK title bars");
        installedCss = css;
      }
    }
    catch (Throwable t) {
      disable(t);
    }
  }

  private static void uninstall() {
    if (installedCss == null || gtk == null) return;
    try {
      gtk.uninstall();
    }
    catch (Throwable t) {
      disable(t);
    }
    finally {
      installedCss = null;
    }
  }

  /** Any GTK failure switches the feature off for the session rather than risking repeated native calls. */
  private static void disable(Throwable t) {
    LOG.warn("Tiling Blue: styling GTK title bars failed; leaving them as they are", t);
    supported = false;
    installedCss = null;
    if (windowListener != null) {
      Toolkit.getDefaultToolkit().removeAWTEventListener(windowListener);
      windowListener = null;
    }
  }

  private static boolean isSupported() {
    if (supported == null) {
      supported = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("linux") &&
                  "sun.awt.wl.WLToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName());
      if (supported) gtk = new GtkCss();
    }
    return supported;
  }

  private static String css() throws IOException {
    if (template == null) {
      try (InputStream in = TitleBars.class.getResourceAsStream("/theme/gtk-titlebar.css")) {
        if (in == null) throw new IOException("/theme/gtk-titlebar.css not found");
        template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
    }
    String css = template;
    for (Map.Entry<String, Color> e : DEFAULTS.entrySet()) {
      Color themed = UIManager.getColor(KEY_PREFIX + e.getKey());
      css = css.replace("${" + e.getKey() + "}", toCss(themed != null ? themed : e.getValue()));
    }
    return css;
  }

  private static String toCss(Color c) {
    if (c.getAlpha() == 255) return String.format(Locale.ROOT, "#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)", c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
  }

  private static void onEdt(Runnable r) {
    if (SwingUtilities.isEventDispatchThread()) r.run();
    else SwingUtilities.invokeLater(r);
  }
}
