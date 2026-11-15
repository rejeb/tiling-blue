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

import io.github.rejeb.tilingblue.UiFontPolicy.Choice;
import io.github.rejeb.tilingblue.UiFontPolicy.Memory;
import io.github.rejeb.tilingblue.UiFontPolicy.UiFont;
import org.junit.jupiter.api.Test;

class UiFontStateTest {
  @Test
  void memoryChange_bumpsModificationCount() {
    // non-roamable state is only saved within IntelliJ's save threshold when the store sees it changed
    UiFontState state = new UiFontState();
    long before = state.getStateModificationCount();
    state.setMemory(new Memory(false, null, Choice.KEEP));
    assertNotEquals(before, state.getStateModificationCount());
  }

  @Test
  void stateRoundTrip_keepsMemory() {
    UiFontState state = new UiFontState();
    Memory mem = new Memory(true, new UiFont(true, "DejaVu Sans", 13), Choice.USE);
    state.setMemory(mem);
    UiFontState loaded = new UiFontState();
    loaded.loadState(state.getState());
    assertEquals(mem, loaded.memory());
  }
}
