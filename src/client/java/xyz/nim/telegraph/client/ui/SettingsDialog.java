package xyz.nim.telegraph.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.List;
import java.util.function.Consumer;

public class SettingsDialog {
    private static final int DIALOG_WIDTH = 280;
    private static final int DIALOG_PADDING = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_HEIGHT = 26;

    private static final int BG_COLOR = 0xF0101010;
    private static final int BORDER_COLOR = 0xFFAAAAAA;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int LABEL_COLOR = 0xFFCCCCCC;

    private final String title;
    private final List<SettingRow> rows;
    private final Runnable onClose;

    private ButtonWidget closeButton;
    private boolean visible = false;
    private int dialogX;
    private int dialogY;
    private int dialogHeight;

    public SettingsDialog(String title, List<SettingRow> rows, Runnable onClose) {
        this.title = title;
        this.rows = rows;
        this.onClose = onClose != null ? onClose : () -> {};
    }

    public void show(int screenWidth, int screenHeight, Consumer<ButtonWidget> addButton) {
        visible = true;

        dialogHeight = DIALOG_PADDING * 2 + 14 + rows.size() * ROW_HEIGHT + BUTTON_HEIGHT + 8;
        dialogX = (screenWidth - DIALOG_WIDTH) / 2;
        dialogY = (screenHeight - dialogHeight) / 2;

        int currentY = dialogY + DIALOG_PADDING + 18;
        int buttonWidth = DIALOG_WIDTH - DIALOG_PADDING * 2 - 80;
        int buttonX = dialogX + DIALOG_PADDING + 75;

        for (SettingRow row : rows) {
            row.button = ButtonWidget.builder(Text.literal(row.currentValue), btn -> {
                row.onCycle.run();
                btn.setMessage(Text.literal(row.getCurrentValue()));
            }).dimensions(buttonX, currentY, buttonWidth, BUTTON_HEIGHT).build();
            row.button.visible = row.isVisible();
            row.button.active = row.isEnabled();
            addButton.accept(row.button);
            currentY += ROW_HEIGHT;
        }

        int closeY = dialogY + dialogHeight - DIALOG_PADDING - BUTTON_HEIGHT;
        closeButton = ButtonWidget.builder(Text.literal("Done"), btn -> {
            hide();
            onClose.run();
        }).dimensions(dialogX + (DIALOG_WIDTH - 80) / 2, closeY, 80, BUTTON_HEIGHT).build();
        addButton.accept(closeButton);
    }

    public void hide() {
        visible = false;
        if (closeButton != null) {
            closeButton.visible = false;
        }
        for (SettingRow row : rows) {
            if (row.button != null) {
                row.button.visible = false;
            }
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public void updateRows() {
        for (SettingRow row : rows) {
            if (row.button != null) {
                row.button.setMessage(Text.literal(row.getCurrentValue()));
                row.button.visible = visible && row.isVisible();
                row.button.active = row.isEnabled();
            }
        }
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (!visible) return;

        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0x80000000);

        context.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + dialogHeight, BG_COLOR);
        context.drawBorder(dialogX, dialogY, DIALOG_WIDTH, dialogHeight, BORDER_COLOR);

        context.drawCenteredTextWithShadow(textRenderer, title, dialogX + DIALOG_WIDTH / 2, dialogY + DIALOG_PADDING, TITLE_COLOR);

        int labelX = dialogX + DIALOG_PADDING;
        int currentY = dialogY + DIALOG_PADDING + 18 + 5;

        for (SettingRow row : rows) {
            if (row.isVisible()) {
                context.drawText(textRenderer, row.label, labelX, currentY, LABEL_COLOR, false);
            }
            currentY += ROW_HEIGHT;
        }

        for (SettingRow row : rows) {
            if (row.button != null && row.button.visible) {
                row.button.render(context, mouseX, mouseY, 0);
            }
        }
        if (closeButton != null) closeButton.render(context, mouseX, mouseY, 0);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        for (SettingRow row : rows) {
            if (row.button != null && row.button.visible && row.button.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        if (closeButton != null && closeButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        if (keyCode == KeyboardConstants.KEY_ESCAPE || KeyboardConstants.isEnter(keyCode)) {
            hide();
            onClose.run();
            return true;
        }
        return true;
    }

    public static class SettingRow {
        private final String label;
        private final java.util.function.Supplier<String> valueSupplier;
        private final Runnable onCycle;
        private final java.util.function.BooleanSupplier visibilitySupplier;
        private final java.util.function.BooleanSupplier enabledSupplier;
        private String currentValue;
        private ButtonWidget button;

        public SettingRow(String label, java.util.function.Supplier<String> valueSupplier, Runnable onCycle) {
            this(label, valueSupplier, onCycle, () -> true, () -> true);
        }

        public SettingRow(String label, java.util.function.Supplier<String> valueSupplier, Runnable onCycle,
                         java.util.function.BooleanSupplier visibilitySupplier,
                         java.util.function.BooleanSupplier enabledSupplier) {
            this.label = label;
            this.valueSupplier = valueSupplier;
            this.onCycle = onCycle;
            this.visibilitySupplier = visibilitySupplier;
            this.enabledSupplier = enabledSupplier;
            this.currentValue = valueSupplier.get();
        }

        public String getCurrentValue() {
            currentValue = valueSupplier.get();
            return currentValue;
        }

        public boolean isVisible() {
            return visibilitySupplier.getAsBoolean();
        }

        public boolean isEnabled() {
            return enabledSupplier.getAsBoolean();
        }
    }
}
