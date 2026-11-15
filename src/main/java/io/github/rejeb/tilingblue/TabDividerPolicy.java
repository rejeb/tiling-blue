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
 * Decides the lines between editor tabs (IntelliJ's registry setting {@code ide.new.editor.tabs.vertical.borders}, off
 * by default): on while Tiling Blue Dark is the theme, as in the design, and given back afterwards. Only a value the
 * plugin turned on itself is ever turned off again. Pure: {@link TabDividers} reads and writes the setting.
 */
final class TabDividerPolicy {
  /**
   * @param setTo   the value to write, or null to leave the setting as it is
   * @param applied whether the setting is now on because of the plugin (remembered across restarts)
   */
  record Outcome(Boolean setTo, boolean applied) {
  }

  private TabDividerPolicy() {
  }

  /** Tiling Blue Dark is the theme (start, theme change, plugin load). */
  static Outcome onActive(boolean applied, boolean on) {
    if (!applied && !on) return new Outcome(true, true);
    // the owner's own dividers stay theirs; ours turned off by hand stay off until the theme is chosen again
    return new Outcome(null, applied);
  }

  /** Another theme is chosen, or the plugin is unloaded. */
  static Outcome onInactive(boolean applied, boolean on) {
    return new Outcome(applied && on ? Boolean.FALSE : null, false);
  }
}
