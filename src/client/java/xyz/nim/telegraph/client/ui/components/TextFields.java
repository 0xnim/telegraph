package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.nim.telegraph.client.ui.ResponsiveLayout;

public class TextFields {

    // Basic text field with custom dimensions
    public static EditBox create(Font font, int x, int y, int width, int height,
                                 String placeholder, int maxLength) {
        EditBox field = new EditBox(font, x, y, width, height, Component.literal(""));
        field.setHint(Component.literal(placeholder).withStyle(ChatFormatting.GRAY));
        field.setMaxLength(maxLength);
        return field;
    }

    // Text field with responsive height
    public static EditBox create(Font font, int x, int y, int width,
                                 ResponsiveLayout layout, String placeholder, int maxLength) {
        return create(font, x, y, width, layout.controlHeight, placeholder, maxLength);
    }

    // Search field (standard search input)
    public static EditBox search(Font font, int x, int y, int width,
                                 ResponsiveLayout layout) {
        return create(font, x, y, width, layout.controlHeight, "Search...", 64);
    }

    // Standard input field
    public static EditBox input(Font font, int x, int y, int width,
                                ResponsiveLayout layout, String placeholder, int maxLength) {
        return create(font, x, y, width, layout.controlHeight, placeholder, maxLength);
    }

    // Short code field (e.g., for abbreviations)
    public static EditBox code(Font font, int x, int y, int width,
                               ResponsiveLayout layout, String placeholder, int maxLength) {
        EditBox field = create(font, x, y, width, layout.controlHeight, placeholder, maxLength);
        // Code fields typically have uppercase text
        return field;
    }

    // Name field (standard name input)
    public static EditBox name(Font font, int x, int y, int width,
                               ResponsiveLayout layout) {
        return create(font, x, y, width, layout.controlHeight, "Name...", 64);
    }

    // Message field (longer input)
    public static EditBox message(Font font, int x, int y, int width,
                                  ResponsiveLayout layout, int maxLength) {
        return create(font, x, y, width, layout.controlHeight, "Message...", maxLength);
    }

    // Configure text field with change listener
    public static EditBox withChangeListener(EditBox field,
                                             java.util.function.Consumer<String> listener) {
        field.setResponder(listener);
        return field;
    }

    // Configure text field to be focused
    public static EditBox focused(EditBox field) {
        field.setFocused(true);
        return field;
    }
}
