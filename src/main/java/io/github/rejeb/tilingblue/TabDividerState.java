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
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

/**
 * Remembers whether the lines between editor tabs are on because of {@link TabDividers}. Kept per machine, like the
 * registry setting itself. Written by the save right after each change (no save threshold, modification count), so
 * the flag and the setting reach the disk together.
 */
@State(name = "TilingBlueTabDividers", storages = @Storage(value = "tiling-blue.xml", roamingType = RoamingType.DISABLED,
  useSaveThreshold = ThreeState.NO))
final class TabDividerState implements PersistentStateComponentWithModificationTracker<TabDividerState.Bean> {
  /** The stored form. */
  public static final class Bean {
    public boolean applied;
  }

  private boolean applied;
  private long modificationCount;

  static TabDividerState getInstance() {
    return ApplicationManager.getApplication().getService(TabDividerState.class);
  }

  boolean applied() {
    return applied;
  }

  void setApplied(boolean applied) {
    if (applied != this.applied) modificationCount++;
    this.applied = applied;
  }

  @Override
  public long getStateModificationCount() {
    return modificationCount;
  }

  @Override
  public @NotNull Bean getState() {
    Bean b = new Bean();
    b.applied = applied;
    return b;
  }

  @Override
  public void loadState(@NotNull Bean b) {
    applied = b.applied;
  }
}
