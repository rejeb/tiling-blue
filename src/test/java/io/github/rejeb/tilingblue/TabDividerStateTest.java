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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TabDividerStateTest {
  @Test
  void appliedChange_bumpsModificationCount() {
    // a plain save (not the one on exit) writes the state only when the count moved
    TabDividerState state = new TabDividerState();
    long before = state.getStateModificationCount();
    state.setApplied(true);
    assertNotEquals(before, state.getStateModificationCount());
  }

  @Test
  void sameValue_keepsModificationCount() {
    TabDividerState state = new TabDividerState();
    long before = state.getStateModificationCount();
    state.setApplied(false);
    assertEquals(before, state.getStateModificationCount());
  }

  @Test
  void stateRoundTrip_keepsApplied() {
    TabDividerState state = new TabDividerState();
    state.setApplied(true);
    TabDividerState loaded = new TabDividerState();
    loaded.loadState(state.getState());
    assertTrue(loaded.applied());
  }
}
