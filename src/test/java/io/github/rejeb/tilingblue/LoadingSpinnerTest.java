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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LoadingSpinnerTest {
  @Test
  void restartSwitches_whenTilingBlueIsChosenInASessionStartedWithout() {
    // installed without a restart, or chosen after starting with another theme: nothing set the property at startup,
    // and the next start (with Tiling Blue) will
    assertTrue(LoadingSpinner.restartSwitchesSpinner(true, null));
  }

  @Test
  void noRestartNeeded_forOtherThemes() {
    assertFalse(LoadingSpinner.restartSwitchesSpinner(false, null));
  }

  @Test
  void noRestartNeeded_whenThePropertyIsAlreadySet() {
    // set at this start: the tiles are showing (or something drew a spinner first, which a restart doesn't change)
    assertFalse(LoadingSpinner.restartSwitchesSpinner(true, "true"));
    // the owner's own choice, which LoadingSpinner leaves alone
    assertFalse(LoadingSpinner.restartSwitchesSpinner(true, "false"));
  }
}
