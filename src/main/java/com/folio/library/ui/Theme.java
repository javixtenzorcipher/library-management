package com.folio.library.ui;

import java.awt.Color;
import java.awt.Font;

final class Theme {
    static final Color INK = new Color(47, 62, 52);
    static final Color MUTED = new Color(79, 94, 81);
    static final Color CANVAS = new Color(255, 241, 210);
    static final Color WHITE = new Color(255, 252, 244);
    static final Color BORDER = new Color(233, 218, 182);
    static final Color NAVY = new Color(47, 62, 52);
    static final Color NAVY_LIGHT = new Color(64, 89, 74);
    static final Color TEAL = new Color(47, 62, 52);
    static final Color TEAL_DARK = new Color(38, 53, 44);
    static final Color TEAL_PALE = new Color(191, 208, 179);
    static final Color AMBER = new Color(129, 77, 20);
    static final Color AMBER_PALE = new Color(252, 241, 220);
    static final Color RED = new Color(169, 53, 49);
    static final Color RED_PALE = new Color(251, 232, 229);
    static final Color SAGE = new Color(127, 156, 134);
    static final Color FOCUS = new Color(47, 62, 52);
    static final Color ROW_ALT = new Color(255, 248, 231);
    static final Color HOVER = new Color(233, 218, 182);

    private Theme() {
    }

    static Font font(int size, int style) {
        return new Font("Segoe UI", style, size);
    }

    static double contrastRatio(Color foreground, Color background) {
        double lighter = Math.max(relativeLuminance(foreground), relativeLuminance(background));
        double darker = Math.min(relativeLuminance(foreground), relativeLuminance(background));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(Color color) {
        return 0.2126 * linearChannel(color.getRed())
                + 0.7152 * linearChannel(color.getGreen())
                + 0.0722 * linearChannel(color.getBlue());
    }

    private static double linearChannel(int channel) {
        double srgb = channel / 255.0;
        return srgb <= 0.04045 ? srgb / 12.92 : Math.pow((srgb + 0.055) / 1.055, 2.4);
    }
}
