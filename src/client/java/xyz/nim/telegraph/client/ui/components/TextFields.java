package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nim.telegraph.client.ui.ResponsiveLayout;

public class TextFields {

    // Basic text field with custom dimensions
    public static TextFieldWidget create(TextRenderer font, int x, int y, int width, int height,
                                          String placeholder, int maxLength) {
        TextFieldWidget field = new TextFieldWidget(font, x, y, width, height, Text.literal(""));
        field.setPlaceholder(Text.literal(placeholder).formatted(Formatting.GRAY));
        field.setMaxLength(maxLength);
        return field;
    }

    // Text field with responsive height
    public static TextFieldWidget create(TextRenderer font, int x, int y, int width,
                                          ResponsiveLayout layout, String placeholder, int maxLength) {
        return create(font, x, y, width, layout.controlHeight, placeholder, maxLength);
    }

    // Search field (standard search input)
    public static TextFieldWidget search(TextRenderer font, int x, int y, int width,
                                          ResponsiveLayout layout) {
        return create(font, x, y, width, layout.controlHeight, "Search...", 64);
    }

    // Standard input field
    public static TextFieldWidget input(TextRenderer font, int x, int y, int width,
                                         ResponsiveLayout layout, String placeholder, int maxLength) {
        return create(font, x, y, width, layout.controlHeight, placeholder, maxLength);
    }

    // Short code field (e.g., for abbreviations)
    public static TextFieldWidget code(TextRenderer font, int x, int y, int width,
                                        ResponsiveLayout layout, String placeholder, int maxLength) {
        TextFieldWidget field = create(font, x, y, width, layout.controlHeight, placeholder, maxLength);
        // Code fields typically have uppercase text
        return field;
    }

    // Name field (standard name input)
    public static TextFieldWidget name(TextRenderer font, int x, int y, int width,
                                        ResponsiveLayout layout) {
        return create(font, x, y, width, layout.controlHeight, "Name...", 64);
    }

    // Message field (longer input)
    public static TextFieldWidget message(TextRenderer font, int x, int y, int width,
                                           ResponsiveLayout layout, int maxLength) {
        return create(font, x, y, width, layout.controlHeight, "Message...", maxLength);
    }

    // Configure text field with change listener
    public static TextFieldWidget withChangeListener(TextFieldWidget field,
                                                      java.util.function.Consumer<String> listener) {
        field.setChangedListener(listener);
        return field;
    }

    // Configure text field to be focused
    public static TextFieldWidget focused(TextFieldWidget field) {
        field.setFocused(true);
        return field;
    }
}
