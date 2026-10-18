package com.folio.library.ui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;

public final class LibraryUiDesignTest {
    private static final double MINIMUM_TEXT_CONTRAST = 4.5;

    private LibraryUiDesignTest() {
    }

    public static void main(String[] args) {
        requireContrast("primary text on cards", Theme.INK, Theme.WHITE);
        requireContrast("primary text on the canvas", Theme.INK, Theme.CANVAS);
        requireContrast("secondary text on cards", Theme.MUTED, Theme.WHITE);
        requireContrast("table headings on white", Theme.MUTED, Theme.WHITE);
        requireContrast("button text", Theme.WHITE, Theme.TEAL);
        requireContrast("primary navigation text", Color.WHITE, Theme.NAVY);
        requireContrast("secondary navigation text", new Color(221, 230, 235), Theme.NAVY);
        requireContrast("section heading in the navigation", new Color(215, 227, 232), Theme.NAVY);
        requireContrast("success status text", Theme.TEAL_DARK, Theme.TEAL_PALE);
        requireContrast("attention status text", Theme.AMBER, Theme.AMBER_PALE);
        requireContrast("error status text", Theme.RED, Theme.RED_PALE);
        requireContrast("keyboard focus outline", Theme.FOCUS, Theme.WHITE);
        requirePrimaryButtonBackground();
        System.out.println("LibraryUiDesignTest: all 12 text-contrast checks and button rendering passed.");
    }

    private static void requirePrimaryButtonBackground() {
        JButton button = UiSupport.button("New checkout", true);
        if (!(button.getUI() instanceof BasicButtonUI)) {
            throw new AssertionError("Primary button must use the cross-platform button painter.");
        }

        button.setSize(button.getPreferredSize());
        BufferedImage image = new BufferedImage(button.getWidth(), button.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        button.paint(graphics);
        graphics.dispose();

        Color renderedBackground = new Color(image.getRGB(8, 8));
        if (!Theme.TEAL.equals(renderedBackground)) {
            throw new AssertionError("Primary button background rendered as " + renderedBackground
                    + "; expected " + Theme.TEAL + ".");
        }
    }

    private static void requireContrast(String name, Color foreground, Color background) {
        double ratio = Theme.contrastRatio(foreground, background);
        if (ratio < MINIMUM_TEXT_CONTRAST) {
            throw new AssertionError(name + " has a contrast ratio of "
                    + String.format("%.2f:1", ratio) + "; expected at least 4.5:1.");
        }
    }
}
