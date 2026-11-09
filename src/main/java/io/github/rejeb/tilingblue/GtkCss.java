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

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * A GTK 3 CSS provider on the default screen of the GTK instance the JetBrains Runtime already loaded, called
 * through the Foreign Function API. Calls must happen on the EDT, the thread the runtime uses for its GTK title bars.
 */
final class GtkCss {
  /** GTK_STYLE_PROVIDER_PRIORITY_USER: wins over any installed GTK theme, whatever its selectors. */
  private static final int PRIORITY_USER = 800;

  private boolean gtkLoaded;
  private Native gtk;
  private MemorySegment provider = MemorySegment.NULL;

  /** True once the runtime has loaded and initialised GTK in this process; never loads GTK itself. */
  boolean isGtkReady() {
    if (!gtkLoaded) {
      gtkLoaded = isLibraryMapped("libgtk-3.so");
      if (!gtkLoaded) return false;
    }
    try {
      return !native_().screenGetDefault().equals(MemorySegment.NULL);
    }
    catch (Throwable t) {
      throw new IllegalStateException("GTK call failed", t);
    }
  }

  /** Adds the provider at GTK's USER priority, or replaces its content if already added. */
  void install(String css) {
    try {
      Native n = native_();
      MemorySegment screen = n.screenGetDefault();
      if (screen.equals(MemorySegment.NULL)) return;
      boolean added = !provider.equals(MemorySegment.NULL);
      if (!added) provider = (MemorySegment)n.cssProviderNew.invokeExact();
      try (Arena arena = Arena.ofConfined()) {
        int ignored = (int)n.cssProviderLoadFromData.invokeExact(provider, arena.allocateFrom(css), -1L, MemorySegment.NULL);
      }
      if (!added) n.addProviderForScreen.invokeExact(screen, provider, PRIORITY_USER);
    }
    catch (Throwable t) {
      throw new IllegalStateException("GTK call failed", t);
    }
  }

  /** Removes the provider; no-op if not installed. */
  void uninstall() {
    if (provider.equals(MemorySegment.NULL)) return;
    try {
      Native n = native_();
      MemorySegment screen = n.screenGetDefault();
      if (!screen.equals(MemorySegment.NULL)) n.removeProviderForScreen.invokeExact(screen, provider);
      n.objectUnref.invokeExact(provider);
    }
    catch (Throwable t) {
      throw new IllegalStateException("GTK call failed", t);
    }
    finally {
      provider = MemorySegment.NULL;
    }
  }

  private Native native_() {
    if (gtk == null) gtk = new Native();
    return gtk;
  }

  /** Whether a shared library is already mapped into this process (so looking it up won't load it). */
  private static boolean isLibraryMapped(String name) {
    try (Stream<String> lines = Files.lines(Path.of("/proc/self/maps"))) {
      return lines.anyMatch(line -> line.contains(name));
    }
    catch (IOException | RuntimeException e) {
      return false;
    }
  }

  /** Downcall handles; libraries are looked up only after GTK is known to be mapped, so dlopen just returns it. */
  private static final class Native {
    final MethodHandle cssProviderNew;
    final MethodHandle cssProviderLoadFromData;
    final MethodHandle addProviderForScreen;
    final MethodHandle removeProviderForScreen;
    final MethodHandle objectUnref;
    private final MethodHandle screenGetDefault;

    Native() {
      Linker linker = Linker.nativeLinker();
      SymbolLookup gtk = SymbolLookup.libraryLookup("libgtk-3.so.0", Arena.global());
      SymbolLookup gdk = SymbolLookup.libraryLookup("libgdk-3.so.0", Arena.global());
      SymbolLookup gobject = SymbolLookup.libraryLookup("libgobject-2.0.so.0", Arena.global());
      cssProviderNew = linker.downcallHandle(gtk.find("gtk_css_provider_new").orElseThrow(), FunctionDescriptor.of(ADDRESS));
      cssProviderLoadFromData = linker.downcallHandle(gtk.find("gtk_css_provider_load_from_data").orElseThrow(),
                                                      FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_LONG, ADDRESS));
      addProviderForScreen = linker.downcallHandle(gtk.find("gtk_style_context_add_provider_for_screen").orElseThrow(),
                                                   FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT));
      removeProviderForScreen = linker.downcallHandle(gtk.find("gtk_style_context_remove_provider_for_screen").orElseThrow(),
                                                      FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
      screenGetDefault = linker.downcallHandle(gdk.find("gdk_screen_get_default").orElseThrow(), FunctionDescriptor.of(ADDRESS));
      objectUnref = linker.downcallHandle(gobject.find("g_object_unref").orElseThrow(), FunctionDescriptor.ofVoid(ADDRESS));
    }

    MemorySegment screenGetDefault() throws Throwable {
      return (MemorySegment)screenGetDefault.invokeExact();
    }
  }
}
