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

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * IBM Plex Mono, bundled with the plugin (SIL Open Font License 1.1, see {@code /fonts/OFL.txt}) and registered with
 * the JVM, so the theme's editor and UI font work without installing anything.
 * <p>
 * Plain {@code java.awt}: no platform classes, so it can be unit tested.
 */
final class BundledFonts {
  static final String FAMILY = "IBM Plex Mono";
  private static final String[] FACES = {"Regular", "Italic", "Bold", "BoldItalic"};
  private static boolean registered;

  private BundledFonts() {
  }

  /**
   * Registers the four bundled faces once. Returns whether the family is available afterwards (also true when it was
   * installed on the system already, in which case the system's copy is used).
   */
  static synchronized boolean register() {
    if (registered) {
      return true;
    }
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (String face : FACES) {
      try (InputStream in = BundledFonts.class.getResourceAsStream("/fonts/IBMPlexMono-" + face + ".ttf")) {
        if (in != null) {
          env.registerFont(Font.createFont(Font.TRUETYPE_FONT, in));
        }
      } catch (IOException | FontFormatException e) {
        // a broken face leaves the others usable; isAvailable() decides
      }
    }
    registered = isAvailable(env);
    return registered;
  }

  /**
   * Whether a font asked for the bundled family ({@code name}, {@link Font#getName()}) but was resolved to another
   * one ({@code family}, {@link Font#getFamily()}): the IDE builds its UI fonts from the saved settings before
   * plugins can register anything, so after a restart the UI font is Plex Mono in name only until it is rebuilt.
   */
  static boolean isStale(String name, String family) {
    return FAMILY.equals(name) && !FAMILY.equals(family);
  }

  private static boolean isAvailable(GraphicsEnvironment env) {
    return Arrays.asList(env.getAvailableFontFamilyNames()).contains(FAMILY);
  }
}
