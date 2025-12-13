package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ui.ResponsiveLayout;

public class Buttons {

    // Standard button with responsive sizing
    public static ButtonWidget create(Text label, int x, int y, ResponsiveLayout layout,
                                       ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action)
                .dimensions(x, y, layout.buttonWidth, layout.buttonHeight)
                .build();
    }

    // Button with custom width
    public static ButtonWidget create(Text label, int x, int y, int width, ResponsiveLayout layout,
                                       ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action)
                .dimensions(x, y, width, layout.buttonHeight)
                .build();
    }

    // Button with custom dimensions
    public static ButtonWidget create(Text label, int x, int y, int width, int height,
                                       ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action)
                .dimensions(x, y, width, height)
                .build();
    }

    // Small button (icon-sized, square)
    public static ButtonWidget small(Text label, int x, int y, ResponsiveLayout layout,
                                      ButtonWidget.PressAction action) {
        int size = layout.buttonHeight;
        return ButtonWidget.builder(label, action)
                .dimensions(x, y, size, size)
                .build();
    }

    // Small button with custom width
    public static ButtonWidget small(Text label, int x, int y, int width, ResponsiveLayout layout,
                                      ButtonWidget.PressAction action) {
        return ButtonWidget.builder(label, action)
                .dimensions(x, y, width, layout.buttonHeight)
                .build();
    }

    // Toggle/tab button with active state
    public static ButtonWidget toggle(Text label, int x, int y, int width, int height,
                                       boolean active, ButtonWidget.PressAction action) {
        ButtonWidget btn = ButtonWidget.builder(label, action)
                .dimensions(x, y, width, height)
                .build();
        btn.active = active;
        return btn;
    }

    // Toggle button with responsive height
    public static ButtonWidget toggle(Text label, int x, int y, int width,
                                       ResponsiveLayout layout, boolean active,
                                       ButtonWidget.PressAction action) {
        ButtonWidget btn = ButtonWidget.builder(label, action)
                .dimensions(x, y, width, layout.buttonHeight)
                .build();
        btn.active = active;
        return btn;
    }

    // Common icon buttons
    public static ButtonWidget back(int x, int y, ResponsiveLayout layout,
                                     ButtonWidget.PressAction action) {
        return create(Text.literal("\u2190 Back"), x, y, layout.buttonWidth, layout, action);
    }

    public static ButtonWidget close(int x, int y, ResponsiveLayout layout,
                                      ButtonWidget.PressAction action) {
        return small(Text.literal("\u2715"), x, y, layout, action);
    }

    public static ButtonWidget done(int x, int y, ResponsiveLayout layout,
                                     ButtonWidget.PressAction action) {
        return create(Text.literal("Done"), x, y, layout, action);
    }

    public static ButtonWidget add(int x, int y, ResponsiveLayout layout,
                                    ButtonWidget.PressAction action) {
        return small(Text.literal("+"), x, y, layout, action);
    }

    public static ButtonWidget remove(int x, int y, ResponsiveLayout layout,
                                       ButtonWidget.PressAction action) {
        return small(Text.literal("-"), x, y, layout, action);
    }

    public static ButtonWidget settings(int x, int y, ResponsiveLayout layout,
                                         ButtonWidget.PressAction action) {
        return small(Text.literal("\u2699"), x, y, layout, action);
    }

    public static ButtonWidget refresh(int x, int y, ResponsiveLayout layout,
                                        ButtonWidget.PressAction action) {
        return small(Text.literal("\u21BB"), x, y, layout, action);
    }
}
