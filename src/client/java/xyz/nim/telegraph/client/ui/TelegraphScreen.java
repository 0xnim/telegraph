package xyz.nim.telegraph.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class TelegraphScreen extends Screen {

    protected final ToastManager toastManager = new ToastManager();
    protected ConfirmDialog confirmDialog;
    protected ResponsiveLayout layout;

    protected TelegraphScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        layout = new ResponsiveLayout(width, height);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmDialog != null && confirmDialog.isVisible()) {
            return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == KeyboardConstants.KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmDialog != null && confirmDialog.isVisible()) {
            return confirmDialog.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. Render simple dark background (no blur to avoid "Can only blur once per frame" error)
        context.fill(0, 0, width, height, 0xC0101010);

        // 2. Subclasses should override renderPanels() to draw their panels
        renderPanels(context, mouseX, mouseY, delta);

        // 3. Render all widgets on top of panels (using children which includes all Drawables)
        for (var element : this.children()) {
            if (element instanceof net.minecraft.client.gui.Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }

        // 4. Render overlays (subclass-specific stuff that goes on top of widgets)
        renderOverlays(context, mouseX, mouseY, delta);

        // 5. Toasts and dialogs on top of everything
        toastManager.render(context, textRenderer, width, height);
        if (confirmDialog != null && confirmDialog.isVisible()) {
            confirmDialog.render(context, textRenderer, mouseX, mouseY);
        }
    }

    /**
     * Override this to render panel backgrounds before widgets.
     * Called after background, before widgets.
     */
    protected void renderPanels(DrawContext context, int mouseX, int mouseY, float delta) {
        // Default: no panels
    }

    /**
     * Override this to render overlays on top of widgets.
     * Called after widgets, before toasts/dialogs.
     */
    protected void renderOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        // Default: no overlays
    }

    // Helper: Draw themed panel
    protected void drawPanel(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, TelegraphTheme.PANEL_BG);
        drawBorder(ctx, x, y, w, h, TelegraphTheme.PANEL_BORDER);
    }

    // Helper: Draw themed panel with header
    protected void drawPanelWithHeader(DrawContext ctx, int x, int y, int w, int h, String title) {
        drawPanel(ctx, x, y, w, h);
        int headerH = layout.headerHeight;
        ctx.fill(x + 1, y + 1, x + w - 1, y + headerH, TelegraphTheme.HEADER_BG);
        ctx.drawText(textRenderer, title, x + layout.padding, y + (headerH - 8) / 2, TelegraphTheme.TEXT_PRIMARY, false);
    }

    // Helper: Draw border (since DrawContext.drawBorder might not exist in all versions)
    protected void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.drawHorizontalLine(x, x + w - 1, y, color);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, color);
        ctx.drawVerticalLine(x, y, y + h - 1, color);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, color);
    }

    // Helper: Show confirm dialog
    protected void confirm(String title, String msg, Runnable onConfirm) {
        confirmDialog = new ConfirmDialog(title, msg, onConfirm);
        confirmDialog.show(width, height, this::addDrawableChild);
    }

    // Helper: Show confirm dialog with cancel callback
    protected void confirm(String title, String msg, Runnable onConfirm, Runnable onCancel) {
        confirmDialog = new ConfirmDialog(title, msg, onConfirm, onCancel);
        confirmDialog.show(width, height, this::addDrawableChild);
    }

    // Convenience accessors for layout
    protected int margin() {
        return layout.margin;
    }

    protected int padding() {
        return layout.padding;
    }

    protected int spacing() {
        return layout.spacing;
    }
}
