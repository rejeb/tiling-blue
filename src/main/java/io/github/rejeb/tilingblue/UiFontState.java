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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponentWithModificationTracker;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import io.github.rejeb.tilingblue.UiFontPolicy.Choice;
import io.github.rejeb.tilingblue.UiFontPolicy.Memory;
import io.github.rejeb.tilingblue.UiFontPolicy.UiFont;
import org.jetbrains.annotations.NotNull;

/**
 * Remembers what {@link UiFontSwitcher} did to the UI font (and the owner's answer) across restarts. Not roamed:
 * IntelliJ keeps the UI font itself per machine too ({@code other.xml}), and like it this state is written on exit and
 * by the periodic save; the modification count lets those saves skip it while nothing changed.
 */
@State(name = "TilingBlueUiFont", storages = @Storage(value = "tiling-blue.xml", roamingType = RoamingType.DISABLED))
final class UiFontState implements PersistentStateComponentWithModificationTracker<UiFontState.Bean> {
  /** The stored form of {@link Memory}. */
  public static final class Bean {
    public boolean applied;
    public boolean hasPrevious;
    public boolean previousCustom;
    public String previousFace;
    public float previousSize;
    public String choice = Choice.NONE.name();
  }

  private Memory memory = Memory.EMPTY;
  private long modificationCount;

  static UiFontState getInstance() {
    return ApplicationManager.getApplication().getService(UiFontState.class);
  }

  Memory memory() {
    return memory;
  }

  void setMemory(@NotNull Memory memory) {
    if (!memory.equals(this.memory)) modificationCount++;
    this.memory = memory;
  }

  @Override
  public long getStateModificationCount() {
    return modificationCount;
  }

  @Override
  public @NotNull Bean getState() {
    Bean b = new Bean();
    b.applied = memory.applied();
    UiFont p = memory.previous();
    if (p != null) {
      b.hasPrevious = true;
      b.previousCustom = p.custom();
      b.previousFace = p.face();
      b.previousSize = p.size();
    }
    b.choice = memory.choice().name();
    return b;
  }

  @Override
  public void loadState(@NotNull Bean b) {
    UiFont previous = b.hasPrevious ? new UiFont(b.previousCustom, b.previousFace, b.previousSize) : null;
    Choice choice;
    try {
      choice = Choice.valueOf(b.choice);
    } catch (IllegalArgumentException | NullPointerException e) {
      choice = Choice.NONE;
    }
    memory = new Memory(b.applied, previous, choice);
  }
}
