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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.IdeGlassPane;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.tabs.JBTabs;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JRootPane;

/**
 * The {@link TabEndPainter}s of a project: one on each of its editor tabs components (one per split, and per editor
 * window) while Tiling Blue Dark is the theme, none otherwise. Each is taken off when its tabs leave the window for
 * good, at the next sync, and all of them when the project closes or the plugin is unloaded.
 */
@Service(Service.Level.PROJECT)
final class TabEndLines implements Disposable {
  private final Project project;
  private final Map<JComponent, Attachment> attached = new HashMap<>();

  TabEndLines(Project project) {
    this.project = project;
  }

  static TabEndLines getInstance(Project project) {
    return project.getService(TabEndLines.class);
  }

  /** On the EDT, after an editor or theme change ({@link TabEndLineSync}). */
  void sync() {
    boolean active = TilingBlueTheme.isActiveWithTabLines();
    attached.entrySet().removeIf(e -> {
      boolean drop = !active || !e.getKey().isDisplayable();
      if (drop) Disposer.dispose(e.getValue());
      return drop;
    });
    if (!active) return;
    for (FileEditor editor : FileEditorManager.getInstance(project).getAllEditors()) {
      // the tabs an editor is shown in hold its component
      JBTabs tabs = ComponentUtil.getParentOfType(JBTabs.class, editor.getComponent());
      if (tabs == null || attached.containsKey(tabs.getComponent())) continue;
      Attachment attachment = new Attachment(tabs);
      Disposer.register(this, attachment);
      attached.put(tabs.getComponent(), attachment);
    }
  }

  @Override
  public void dispose() {
    // the attachments are disposed with this service
    attached.clear();
  }

  /**
   * The painter of one tabs component, on the glass pane of its window while the tabs are showing (they leave and come
   * back when the editor is split or unsplit). A new painter each time: the glass pane takes a painter off in a later
   * event, which would also take off the same painter put back meanwhile.
   */
  private static final class Attachment implements HierarchyListener, Disposable {
    private final JBTabs tabs;
    private Disposable shown;

    Attachment(JBTabs tabs) {
      this.tabs = tabs;
      tabs.getComponent().addHierarchyListener(this);
      update();
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
      if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) update();
    }

    private void update() {
      takeOff();
      JComponent component = tabs.getComponent();
      JRootPane root = component.getRootPane();
      if (component.isShowing() && root != null && root.getGlassPane() instanceof IdeGlassPane pane) {
        shown = Disposer.newDisposable("Tiling Blue tab end line");
        pane.addPainter(component, new TabEndPainter(tabs), shown);
      }
    }

    private void takeOff() {
      if (shown != null) Disposer.dispose(shown);
      shown = null;
    }

    @Override
    public void dispose() {
      tabs.getComponent().removeHierarchyListener(this);
      takeOff();
    }
  }
}
