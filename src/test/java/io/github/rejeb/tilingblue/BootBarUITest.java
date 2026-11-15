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

import org.junit.jupiter.api.Test;

class BootBarUITest {
  @Test
  void barHeight_noRequest_isSegmentHeight() {
    assertEquals(6, BootBarUI.barHeight(null));
  }

  @Test
  void barHeight_thinnerRequest_staysSegmentHeight() {
    // IntelliJ's test runner asks for a 3 px stripe, the loading decorator for 2 px, the agent quota bar for 4 px:
    // the prototype draws every bar 6 px tall
    assertEquals(6, BootBarUI.barHeight(3));
    assertEquals(6, BootBarUI.barHeight(2));
    assertEquals(6, BootBarUI.barHeight(4));
  }

  @Test
  void barHeight_thickerRequest_isHonoured() {
    assertEquals(8, BootBarUI.barHeight(8));
    assertEquals(8, BootBarUI.barHeight("8"));
  }

  @Test
  void barHeight_unreadableRequest_isSegmentHeight() {
    assertEquals(6, BootBarUI.barHeight("wide"));
  }
}
