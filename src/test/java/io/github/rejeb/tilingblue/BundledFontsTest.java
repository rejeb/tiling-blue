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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.List;
import org.junit.jupiter.api.Test;

class BundledFontsTest {
  @Test
  void registersPlexMono() {
    assertTrue(BundledFonts.register());
    List<String> families = List.of(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
    assertTrue(families.contains("IBM Plex Mono"), "IBM Plex Mono not among " + families);
    assertTrue(BundledFonts.register(), "a second call still reports the family as available");
  }

  @Test
  void boldAndItalicFacesAreThePlexOnes() {
    assertTrue(BundledFonts.register());
    for (int style : new int[] {Font.PLAIN, Font.BOLD, Font.ITALIC, Font.BOLD | Font.ITALIC}) {
      Font font = new Font(BundledFonts.FAMILY, style, 14);
      assertEquals(BundledFonts.FAMILY, font.getFamily(), "style " + style);
      assertTrue(font.getFontName().startsWith("IBM Plex Mono"), "style " + style + ": " + font.getFontName());
    }
  }

  @Test
  void plexNamedFontResolvedToFallback_isStale() {
    // what the LaF holds after a restart: built from the saved UI font before the bundled faces were registered
    // (Font.getName() = the requested name, Font.getFamily() = what Java resolved it to)
    assertTrue(BundledFonts.isStale("IBM Plex Mono", "Dialog"));
    assertFalse(BundledFonts.isStale("IBM Plex Mono", "IBM Plex Mono"));
    assertFalse(BundledFonts.isStale("Inter", "Inter"));
    assertFalse(BundledFonts.isStale(null, null));
  }
}
