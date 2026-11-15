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

import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI;
import com.intellij.openapi.progress.util.ColorProgressBar;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBInsets;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.plaf.ComponentUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;

/**
 * Progress bars drawn as a row of square segments instead of IntelliJ's rounded stripe.
 * <p>
 * Determinate bars fill the segments up to the current value; the segment being filled grows in the lead colour,
 * so even small steps show. Indeterminate bars show a soft wave rolling along the segments, its crest in the lead
 * colour. Status colours work as in {@link DarculaProgressBarUI}: a failed, passed or warning bar fills with that
 * status colour and uses its {@code *EndColor} partner as the lead colour. Vertical bars and bars with a custom
 * {@code ProgressBar.progressPaint} are left to IntelliJ.
 * <p>
 * Registered as {@code ProgressBarUI} in both Tiling Blue themes (src/main/theme/tilingblue.theme.json), so it is only
 * used while one of them is active.
 * docs/ui-prototype shows the alternatives that were considered.
 */
public final class BootBarUI extends DarculaProgressBarUI {
  private static final Color LEAD = JBColor.namedColor("TilingBlue.ProgressBar.leadColor", new JBColor(0xff8f8f, 0xff8f8f));
  /** Segment length, gap and bar height, before UI scaling. IntelliJ's own bars are 4 px tall. */
  private static final int SEGMENT = 12;
  private static final int GAP = 3;
  private static final int HEIGHT = 6;
  /** Alpha of the fill colour on an unlit segment. */
  private static final double UNLIT_ALPHA = 0.2;
  /** Indeterminate wave: one crest every WAVE_LENGTH segments, moving one wavelength every WAVE_MS. */
  private static final double WAVE_LENGTH = 10;
  private static final long WAVE_MS = 1600;
  /** Wave brightness above which the crest blends into the lead colour. */
  private static final double CREST = 0.9;

  @SuppressWarnings("unused") // looked up by Swing (UIDefaults.getUI)
  public static ComponentUI createUI(JComponent c) {
    return new BootBarUI();
  }

  /** {@link #barHeight} tall, unless the bar paints its text. */
  @Override
  public Dimension getPreferredSize(JComponent c) {
    Dimension size = super.getPreferredSize(c);
    if (c instanceof JProgressBar bar && bar.getOrientation() == SwingConstants.HORIZONTAL && !bar.isStringPainted()) {
      size.height = JBUIScale.scale(barHeight(bar.getClientProperty("ProgressBar.stripeWidth")));
    }
    return size;
  }

  /**
   * Bar height before UI scaling: {@link #HEIGHT}, or the owner's {@code ProgressBar.stripeWidth} when that is thicker.
   * A thinner request does not shrink the segments: the test runner's status line asks for 3 px, the loading decorator
   * for 2 px, and the prototype draws every bar {@link #HEIGHT} tall.
   */
  static int barHeight(@Nullable Object requested) {
    if (requested == null) return HEIGHT;
    try {
      return Math.max(HEIGHT, Integer.parseInt(requested.toString().trim()));
    }
    catch (NumberFormatException e) {
      return HEIGHT;
    }
  }

  @Override
  protected void paintDeterminate(Graphics g, JComponent c) {
    if (!isHorizontal() || progressBar.getClientProperty("ProgressBar.progressPaint") instanceof Paint) {
      super.paintDeterminate(g, c);
      return;
    }
    Color[] status = statusColors();
    Color fill = status != null ? status[0] : getFinishedColor(c);
    Color lead = status != null ? status[1] : LEAD;

    Graphics2D g2 = (Graphics2D)g.create();
    try {
      Rectangle bar = stripe(g2, c);
      int n = segmentCount(bar.width);
      double done = Math.max(0, Math.min(1, progressBar.getPercentComplete()));
      double[] level = new double[n];
      int full = done >= 1 ? n : (int)(done * n);
      for (int k = 0; k < full; k++) level[k] = 1;
      paintSegments(g2, bar, level, full, done * n - full, fill, lead);
    }
    finally {
      g2.dispose();
    }
    paintText(g, true);
  }

  @Override
  protected void paintIndeterminate(Graphics g, JComponent c) {
    if (!isHorizontal()) {
      super.paintIndeterminate(g, c);
      return;
    }
    Color[] status = statusColors();
    Color fill;
    Color lead;
    if (status != null) {
      fill = status[0];
      lead = status[1];
    }
    else {
      fill = progressBar.getClientProperty("ProgressBar.indeterminateStartColor") instanceof Color start ? start : getStartColor(c);
      lead = progressBar.getClientProperty("ProgressBar.indeterminateEndColor") instanceof Color end ? end : getEndColor(c);
    }

    Graphics2D g2 = (Graphics2D)g.create();
    try {
      Rectangle bar = stripe(g2, c);
      int n = segmentCount(bar.width);
      // Wall-clock phase keeps every bar in step; DarculaProgressBarUI's animation timer drives the repaints.
      double phase = (System.currentTimeMillis() % WAVE_MS) / (double)WAVE_MS;
      double[] level = new double[n];
      for (int k = 0; k < n; k++) {
        double wave = Math.max(0, Math.cos(2 * Math.PI * (k / WAVE_LENGTH - phase)));
        double v = wave * wave;
        level[k] = v > CREST ? 1 + (v - CREST) / (1 - CREST) : v;
      }
      paintSegments(g2, bar, level, -1, 0, fill, lead);
    }
    finally {
      g2.dispose();
    }
    paintText(g, false);
  }

  private boolean isHorizontal() {
    return progressBar.getOrientation() == SwingConstants.HORIZONTAL;
  }

  /**
   * {fill, lead} for a failed, passed or warning bar, else null; the same checks as DarculaProgressBarUI.
   * ColorProgressBar is deprecated, but some callers still mark the status by using its colours as foreground.
   */
  @SuppressWarnings("deprecation")
  private Color @Nullable [] statusColors() {
    Object status = progressBar.getClientProperty("ProgressBar.status");
    Color foreground = progressBar.getForeground();
    if ("failed".equals(status) || foreground == ColorProgressBar.RED) {
      return new Color[]{JBUI.CurrentTheme.ProgressBar.FAILED, JBUI.CurrentTheme.ProgressBar.FAILED_END};
    }
    if ("passed".equals(status) || foreground == ColorProgressBar.GREEN) {
      return new Color[]{JBUI.CurrentTheme.ProgressBar.PASSED, JBUI.CurrentTheme.ProgressBar.PASSED_END};
    }
    if ("warning".equals(status) || foreground == ColorProgressBar.YELLOW) {
      return new Color[]{JBUI.CurrentTheme.ProgressBar.WARNING, JBUI.CurrentTheme.ProgressBar.WARNING_END};
    }
    return null;
  }

  /** Clears the background like DarculaProgressBarUI and returns the stripe: preferred height, centred vertically. */
  private Rectangle stripe(Graphics2D g2, JComponent c) {
    Rectangle r = new Rectangle(progressBar.getSize());
    if (c.isOpaque() && c.getParent() != null) {
      g2.setColor(c.getParent().getBackground());
      g2.fill(r);
    }
    JBInsets.removeFrom(r, progressBar.getInsets());
    int height = Math.min(r.height, progressBar.getPreferredSize().height);
    r.y += (r.height - height) / 2;
    r.height = height;
    return r;
  }

  private static int segmentCount(int width) {
    int gap = JBUIScale.scale(GAP);
    return Math.max(1, (width + gap) / (JBUIScale.scale(SEGMENT) + gap));
  }

  /**
   * Paints one segment per level, spread over the full width (whole pixels, lengths differ by at most one).
   * A level from 0 to 1 is the fill colour from {@link #UNLIT_ALPHA} to opaque; above 1 the lead colour is laid over
   * the fill with alpha (level - 1). Segment {@code partialIndex}, if any, also gets the lead colour over the first
   * {@code partial} (0..1) of its length.
   */
  private static void paintSegments(Graphics2D g, Rectangle bar, double[] level, int partialIndex, double partial,
                                    Color fill, Color lead) {
    if (bar.width <= 0 || bar.height <= 0) return;
    int n = level.length;
    int gap = JBUIScale.scale(GAP);
    for (int k = 0; k < n; k++) {
      int x0 = bar.x + (int)((long)k * (bar.width + gap) / n);
      int x1 = bar.x + (int)((long)(k + 1) * (bar.width + gap) / n) - gap;
      int width = Math.max(1, x1 - x0);
      double v = level[k];
      g.setColor(v < 1 ? ColorUtil.withAlpha(fill, UNLIT_ALPHA + (1 - UNLIT_ALPHA) * v) : fill);
      g.fillRect(x0, bar.y, width, bar.height);
      if (v > 1) {
        g.setColor(ColorUtil.withAlpha(lead, Math.min(1, v - 1)));
        g.fillRect(x0, bar.y, width, bar.height);
      }
      if (k == partialIndex && partial > 0) {
        g.setColor(lead);
        g.fillRect(x0, bar.y, Math.max(1, (int)Math.round(width * partial)), bar.height);
      }
    }
  }

  private void paintText(Graphics g, boolean determinate) {
    if (!progressBar.isStringPainted()) return;
    Insets i = progressBar.getInsets();
    int width = progressBar.getWidth() - i.left - i.right;
    int height = progressBar.getHeight() - i.top - i.bottom;
    paintString(g, i.left, i.top, width, height, determinate ? getAmountFull(i, width, height) : 0, i);
  }
}
