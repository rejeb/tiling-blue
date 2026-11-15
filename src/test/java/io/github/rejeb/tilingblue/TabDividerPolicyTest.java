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

import io.github.rejeb.tilingblue.TabDividerPolicy.Outcome;
import org.junit.jupiter.api.Test;

class TabDividerPolicyTest {
  @Test
  void active_dividersOff_turnsThemOnAndRemembers() {
    assertEquals(new Outcome(true, true), TabDividerPolicy.onActive(false, false));
  }

  @Test
  void active_ownerAlreadyHasDividers_leavesThemAndTheirs() {
    assertEquals(new Outcome(null, false), TabDividerPolicy.onActive(false, true));
  }

  @Test
  void active_oursAlreadyOn_nothing() {
    assertEquals(new Outcome(null, true), TabDividerPolicy.onActive(true, true));
  }

  @Test
  void active_ownerTurnedOursOff_staysOff() {
    // turned off by hand while the theme is active: not turned on again at the next start
    assertEquals(new Outcome(null, true), TabDividerPolicy.onActive(true, false));
  }

  @Test
  void inactive_oursStillOn_turnsThemOffAndForgets() {
    assertEquals(new Outcome(false, false), TabDividerPolicy.onInactive(true, true));
  }

  @Test
  void inactive_oursTurnedOffByOwner_forgets() {
    assertEquals(new Outcome(null, false), TabDividerPolicy.onInactive(true, false));
  }

  @Test
  void inactive_ownersOwnDividers_untouched() {
    assertEquals(new Outcome(null, false), TabDividerPolicy.onInactive(false, true));
  }

  @Test
  void inactive_nothingOfOurs_nothing() {
    assertEquals(new Outcome(null, false), TabDividerPolicy.onInactive(false, false));
  }
}
