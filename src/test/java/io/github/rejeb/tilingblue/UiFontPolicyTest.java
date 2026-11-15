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

import static io.github.rejeb.tilingblue.UiFontPolicy.Action.APPLY;
import static io.github.rejeb.tilingblue.UiFontPolicy.Action.ASK;
import static io.github.rejeb.tilingblue.UiFontPolicy.Action.NOTHING;
import static io.github.rejeb.tilingblue.UiFontPolicy.Action.RESTORE;
import static io.github.rejeb.tilingblue.UiFontPolicy.Memory.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.rejeb.tilingblue.UiFontPolicy.Choice;
import io.github.rejeb.tilingblue.UiFontPolicy.Memory;
import io.github.rejeb.tilingblue.UiFontPolicy.UiFont;
import org.junit.jupiter.api.Test;

class UiFontPolicyTest {
  private static final UiFont PLEX = new UiFont(true, "IBM Plex Mono", 14);
  private static final UiFont DEFAULT_INTER = new UiFont(false, "Inter", 13);
  private static final UiFont FIRA = new UiFont(true, "Fira Sans", 13);

  @Test
  void noCustomFont_onEnter_applies() {
    assertEquals(APPLY, UiFontPolicy.onEnter(DEFAULT_INTER, EMPTY));
  }

  @Test
  void customOtherFont_onEnter_asks() {
    assertEquals(ASK, UiFontPolicy.onEnter(FIRA, EMPTY));
  }

  @Test
  void keptBefore_onEnter_nothing() {
    assertEquals(NOTHING, UiFontPolicy.onEnter(FIRA, new Memory(false, null, Choice.KEEP)));
  }

  @Test
  void usedBefore_onEnter_appliesWithoutAsking() {
    assertEquals(APPLY, UiFontPolicy.onEnter(FIRA, new Memory(false, null, Choice.USE)));
  }

  @Test
  void alreadyPlex14_onEnter_nothing() {
    assertEquals(NOTHING, UiFontPolicy.onEnter(PLEX, EMPTY));
  }

  @Test
  void customPlexOtherSize_onEnter_nothing() {
    assertEquals(NOTHING, UiFontPolicy.onEnter(new UiFont(true, "IBM Plex Mono", 13), EMPTY));
  }

  @Test
  void decidedBefore_onStartup_nothing() {
    assertEquals(NOTHING, UiFontPolicy.onStartup(FIRA, new Memory(true, DEFAULT_INTER, Choice.NONE)));
  }

  @Test
  void emptyMemory_onStartup_actsLikeEnter() {
    assertEquals(APPLY, UiFontPolicy.onStartup(DEFAULT_INTER, EMPTY));
    assertEquals(ASK, UiFontPolicy.onStartup(FIRA, EMPTY));
    assertEquals(NOTHING, UiFontPolicy.onStartup(PLEX, EMPTY));
  }

  @Test
  void appliedAndUnchanged_onLeave_restores() {
    assertEquals(RESTORE, UiFontPolicy.onLeave(PLEX, new Memory(true, DEFAULT_INTER, Choice.NONE)));
  }

  @Test
  void appliedButUserChanged_onLeave_nothing() {
    assertEquals(NOTHING, UiFontPolicy.onLeave(FIRA, new Memory(true, DEFAULT_INTER, Choice.NONE)));
  }

  @Test
  void notApplied_onLeave_nothing() {
    assertEquals(NOTHING, UiFontPolicy.onLeave(PLEX, EMPTY));
    assertEquals(NOTHING, UiFontPolicy.onLeave(FIRA, new Memory(false, null, Choice.KEEP)));
  }
}
