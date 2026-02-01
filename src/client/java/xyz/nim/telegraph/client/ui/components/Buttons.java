package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import xyz.nim.telegraph.client.ui.ResponsiveLayout;

public class Buttons {

    // Standard button with responsive sizing
    public static Button create(Component label, int x, int y, ResponsiveLayout layout,
                                Button.OnPress action) {
        return Button.builder(label, action)
                .bounds(x, y, layout.buttonWidth, layout.buttonHeight)
                .build();
    }

    // Button with custom width
    public static Button create(Component label, int x, int y, int width, ResponsiveLayout layout,
                                Button.OnPress action) {
        return Button.builder(label, action)
                .bounds(x, y, width, layout.buttonHeight)
                .build();
    }

    // Button with custom dimensions
    public static Button create(Component label, int x, int y, int width, int height,
                                Button.OnPress action) {
        return Button.builder(label, action)
                .bounds(x, y, width, height)
                .build();
    }

    // Small button (icon-sized, square)
    public static Button small(Component label, int x, int y, ResponsiveLayout layout,
                               Button.OnPress action) {
        int size = layout.buttonHeight;
        return Button.builder(label, action)
                .bounds(x, y, size, size)
                .build();
    }

    // Small button with custom width
    public static Button small(Component label, int x, int y, int width, ResponsiveLayout layout,
                               Button.OnPress action) {
        return Button.builder(label, action)
                .bounds(x, y, width, layout.buttonHeight)
                .build();
    }

    // Toggle/tab button with active state
    public static Button toggle(Component label, int x, int y, int width, int height,
                                boolean active, Button.OnPress action) {
        Button btn = Button.builder(label, action)
                .bounds(x, y, width, height)
                .build();
        btn.active = active;
        return btn;
    }

    // Toggle button with responsive height
    public static Button toggle(Component label, int x, int y, int width,
                                ResponsiveLayout layout, boolean active,
                                Button.OnPress action) {
        Button btn = Button.builder(label, action)
                .bounds(x, y, width, layout.buttonHeight)
                .build();
        btn.active = active;
        return btn;
    }

    // Common icon buttons
    public static Button back(int x, int y, ResponsiveLayout layout,
                              Button.OnPress action) {
        return create(Component.literal("\u2190 Back"), x, y, layout.buttonWidth, layout, action);
    }

    public static Button close(int x, int y, ResponsiveLayout layout,
                               Button.OnPress action) {
        return small(Component.literal("\u2715"), x, y, layout, action);
    }

    public static Button done(int x, int y, ResponsiveLayout layout,
                              Button.OnPress action) {
        return create(Component.literal("Done"), x, y, layout, action);
    }

    public static Button add(int x, int y, ResponsiveLayout layout,
                             Button.OnPress action) {
        return small(Component.literal("+"), x, y, layout, action);
    }

    public static Button remove(int x, int y, ResponsiveLayout layout,
                                Button.OnPress action) {
        return small(Component.literal("-"), x, y, layout, action);
    }

    public static Button settings(int x, int y, ResponsiveLayout layout,
                                  Button.OnPress action) {
        return small(Component.literal("\u2699"), x, y, layout, action);
    }

    public static Button refresh(int x, int y, ResponsiveLayout layout,
                                 Button.OnPress action) {
        return small(Component.literal("\u21BB"), x, y, layout, action);
    }
}
