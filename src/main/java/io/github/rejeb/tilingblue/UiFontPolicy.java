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

/**
 * When the theme sets the UI font to IBM Plex Mono 14, when it asks first, and when it gives the previous font back.
 * <p>
 * Pure decisions (no platform classes) so they can be unit tested; {@link UiFontSwitcher} carries them out and
 * {@link UiFontState} keeps the {@link Memory} between restarts.
 */
final class UiFontPolicy {
  static final float SIZE = 14f;

  private UiFontPolicy() {
  }

  /**
   * The IDE's UI font setting: {@code custom} = Settings › Appearance › "Use custom font" is on; {@code face} and
   * {@code size} are what the IDE shows then (or the theme's default font when not custom).
   */
  record UiFont(boolean custom, String face, float size) {
    boolean isPlex() {
      return custom && BundledFonts.FAMILY.equals(face);
    }

    boolean isPlex14() {
      return isPlex() && Float.compare(size, SIZE) == 0;
    }
  }

  /** The owner's answer to "use IBM Plex Mono instead of your custom font?". */
  enum Choice { NONE, USE, KEEP }

  /** What the theme did: {@code applied} = it set Plex Mono 14, over {@code previous}; {@code choice} = the answer. */
  record Memory(boolean applied, UiFont previous, Choice choice) {
    static final Memory EMPTY = new Memory(false, null, Choice.NONE);

    boolean isEmpty() {
      return !applied && previous == null && choice == Choice.NONE;
    }
  }

  enum Action { APPLY, ASK, RESTORE, NOTHING }

  /** The IDE switched to Tiling Blue. */
  static Action onEnter(UiFont now, Memory mem) {
    if (now.isPlex()) return Action.NOTHING; // already Plex Mono, at whatever size the owner picked
    if (mem.choice() == Choice.KEEP) return Action.NOTHING;
    if (!now.custom() || mem.choice() == Choice.USE) return Action.APPLY;
    return Action.ASK;
  }

  /** The IDE started with Tiling Blue active: only a first run decides; later runs keep what is set. */
  static Action onStartup(UiFont now, Memory mem) {
    return mem.isEmpty() ? onEnter(now, mem) : Action.NOTHING;
  }

  /** The IDE switched away from Tiling Blue: give the font back, unless the owner changed it since. */
  static Action onLeave(UiFont now, Memory mem) {
    return mem.applied() && now.isPlex14() ? Action.RESTORE : Action.NOTHING;
  }
}
