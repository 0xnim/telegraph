package xyz.nim.telegraph.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class ConfirmDialog {
    private static final int DIALOG_WIDTH = 250;
    private static final int DIALOG_PADDING = 12;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_SPACING = 8;

    private static final int BG_COLOR = 0xF0101010;
    private static final int BORDER_COLOR = 0xFFAAAAAA;
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int MESSAGE_COLOR = 0xFFCCCCCC;

    private final String title;
    private final String message;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    private ButtonWidget confirmButton;
    private ButtonWidget cancelButton;
    private boolean visible = false;
    private int dialogX;
    private int dialogY;
    private int dialogHeight;

    public ConfirmDialog(String title, String message, Runnable onConfirm, Runnable onCancel) {
        this.title = title;
        this.message = message;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel != null ? onCancel : () -> {};
    }

    public ConfirmDialog(String title, String message, Runnable onConfirm) {
        this(title, message, onConfirm, null);
    }

    public void show(int screenWidth, int screenHeight, Consumer<ButtonWidget> addButton) {
        visible = true;

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int messageLines = (int) Math.ceil(textRenderer.getWidth(message) / (double) (DIALOG_WIDTH - DIALOG_PADDING * 2));
        messageLines = Math.max(1, Math.min(messageLines, 4));

        dialogHeight = DIALOG_PADDING * 3 + 12 + messageLines * 10 + BUTTON_HEIGHT;
        dialogX = (screenWidth - DIALOG_WIDTH) / 2;
        dialogY = (screenHeight - dialogHeight) / 2;

        int buttonY = dialogY + dialogHeight - DIALOG_PADDING - BUTTON_HEIGHT;
        int buttonsWidth = BUTTON_WIDTH * 2 + BUTTON_SPACING;
        int buttonStartX = dialogX + (DIALOG_WIDTH - buttonsWidth) / 2;

        confirmButton = ButtonWidget.builder(Text.literal("Confirm"), btn -> {
            hide();
            onConfirm.run();
        }).dimensions(buttonStartX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        cancelButton = ButtonWidget.builder(Text.literal("Cancel"), btn -> {
            hide();
            onCancel.run();
        }).dimensions(buttonStartX + BUTTON_WIDTH + BUTTON_SPACING, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build();

        addButton.accept(confirmButton);
        addButton.accept(cancelButton);
    }

    public void hide() {
        visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void render(DrawContext context, TextRenderer textRenderer, int mouseX, int mouseY) {
        if (!visible) return;

        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), 0x80000000);

        context.fill(dialogX, dialogY, dialogX + DIALOG_WIDTH, dialogY + dialogHeight, BG_COLOR);
        context.drawBorder(dialogX, dialogY, DIALOG_WIDTH, dialogHeight, BORDER_COLOR);

        context.drawCenteredTextWithShadow(textRenderer, title, dialogX + DIALOG_WIDTH / 2, dialogY + DIALOG_PADDING, TITLE_COLOR);

        int messageY = dialogY + DIALOG_PADDING + 14;
        int maxWidth = DIALOG_WIDTH - DIALOG_PADDING * 2;
        String remaining = message;
        int lines = 0;
        while (!remaining.isEmpty() && lines < 4) {
            String line = textRenderer.trimToWidth(remaining, maxWidth);
            context.drawCenteredTextWithShadow(textRenderer, line, dialogX + DIALOG_WIDTH / 2, messageY, MESSAGE_COLOR);
            remaining = remaining.substring(line.length()).trim();
            messageY += 10;
            lines++;
        }

        if (confirmButton != null) confirmButton.render(context, mouseX, mouseY, 0);
        if (cancelButton != null) cancelButton.render(context, mouseX, mouseY, 0);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        if (confirmButton != null && confirmButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (cancelButton != null && cancelButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return true;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        if (KeyboardConstants.isEnter(keyCode)) {
            hide();
            onConfirm.run();
            return true;
        }
        if (keyCode == KeyboardConstants.KEY_ESCAPE) {
            hide();
            onCancel.run();
            return true;
        }
        return true;
    }

    public ButtonWidget getConfirmButton() {
        return confirmButton;
    }

    public ButtonWidget getCancelButton() {
        return cancelButton;
    }
}
