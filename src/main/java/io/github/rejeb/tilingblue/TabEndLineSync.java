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

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps {@link TabEndLines} in step: editors opened, closed or switched (a project listener), the theme changed or the
 * plugin loaded (application listeners).
 */
final class TabEndLineSync implements FileEditorManagerListener, LafManagerListener, DynamicPluginListener {
  /** Set when this plugin starts unloading (each loaded version of the plugin has its own). */
  private static volatile boolean unloading;

  @Override
  public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    later(source.getProject());
  }

  @Override
  public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
    later(source.getProject());
  }

  @Override
  public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    later(event.getManager().getProject());
  }

  @Override
  public void lookAndFeelChanged(@NotNull LafManager source) {
    allProjects();
  }

  @Override
  public void pluginLoaded(@NotNull IdeaPluginDescriptor descriptor) {
    if (TilingBlueTheme.isThisPlugin(descriptor)) allProjects();
  }

  @Override
  public void beforePluginUnload(@NotNull IdeaPluginDescriptor descriptor, boolean isUpdate) {
    // the painters go with the project services; syncs still queued must not bring them back
    if (TilingBlueTheme.isThisPlugin(descriptor)) unloading = true;
  }

  private static void allProjects() {
    for (Project project : ProjectManager.getInstance().getOpenProjects()) later(project);
  }

  /**
   * Once the editor is in its tabs. In any modality: it only adds or removes painters, and a theme chosen in the
   * Settings dialog shows behind it at once.
   */
  private static void later(Project project) {
    ApplicationManager.getApplication().invokeLater(() -> {
      if (!unloading) TabEndLines.getInstance(project).sync();
    }, ModalityState.any(), project.getDisposed());
  }
}
