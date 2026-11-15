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

import com.intellij.icons.AllIcons;
import com.intellij.ide.AppLifecycleListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.AnimatedIcon;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Switches IntelliJ to its frame-based loading spinner, whose frames the theme replaces with the focus cycle.
 * <p>
 * IntelliJ draws its current spinner in code ({@code SpinningProgressIcon}); a theme can only recolour it
 * ({@code ProgressIcon.color}). The older spinner shows the icons {@code /process/step_1..8.svg}, which
 * tilingblue.theme.json maps to {@code /theme/icons/process/}. IntelliJ picks one of the two once per session, when
 * the first spinner is created, from the system property {@code disable.new.spinning.icon}. This sets that property
 * as the first frame opens, if Tiling Blue is the active theme and the user hasn't set the property either way.
 * A session that starts with another theme keeps the code-drawn spinner (red under Tiling Blue) until the next
 * restart.
 */
public final class LoadingSpinner implements AppLifecycleListener {
  private static final String FRAMES_PROPERTY = "disable.new.spinning.icon";
  private static final Logger LOG = Logger.getInstance(LoadingSpinner.class);

  @Override
  public void appFrameCreated(@NotNull List<String> commandLineArgs) {
    if (System.getProperty(FRAMES_PROPERTY) != null || !TilingBlueTheme.isActive()) return;
    System.setProperty(FRAMES_PROPERTY, "true");
    // AnimatedIcon.Default reads the property when it is first used; if that already happened, it is too late.
    if (AnimatedIcon.Default.ICONS.get(0) == AllIcons.Process.Step_1) {
      LOG.info("Tiling Blue: using the focus-cycle loading spinner");
    }
    else {
      LOG.info("Tiling Blue: a loading spinner already existed, keeping IntelliJ's spinner for this session");
    }
  }
}
