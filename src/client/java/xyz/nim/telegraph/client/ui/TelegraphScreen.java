package xyz.nim.telegraph.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.network.chat.Component;

import java.util.List;

public abstract class TelegraphScreen extends Screen {

    protected final ToastManager toastManager = new ToastManager();
    protected ConfirmDialog confirmDialog;
    protected ResponsiveLayout layout;
    private List<Component> pendingTooltip;
    private int pendingTooltipX, pendingTooltipY;

    protected TelegraphScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();
        layout = new ResponsiveLayout(width, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmDialog != null && confirmDialog.isVisible()) {
            return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == KeyboardConstants.KEY_ESCAPE) {
            onClose();
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
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Clear pending tooltip at start of each frame
        pendingTooltip = null;

        // 1. Render simple dark background (no blur to avoid "Can only blur once per frame" error)
        context.fill(0, 0, width, height, 0xC0101010);

        // 2. Subclasses should override renderPanels() to draw their panels
        renderPanels(context, mouseX, mouseY, delta);

        // 3. Render all widgets on top of panels (using children which includes all Drawables)
        for (var element : this.children()) {
            if (element instanceof Renderable renderable) {
                renderable.render(context, mouseX, mouseY, delta);
            }
        }

        // 4. Render overlays (subclass-specific stuff that goes on top of widgets)
        renderOverlays(context, mouseX, mouseY, delta);

        // 5. Toasts and dialogs on top of everything
        toastManager.render(context, font, width, height);
        if (confirmDialog != null && confirmDialog.isVisible()) {
            confirmDialog.render(context, font, mouseX, mouseY);
        }

        // 6. Render pending tooltip (must be last, on top of everything)
        if (pendingTooltip != null && !pendingTooltip.isEmpty()) {
            List<net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent> clientComponents = pendingTooltip.stream()
                    .map(c -> net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent.create(c.getVisualOrderText()))
                    .toList();
            context.renderTooltip(font, clientComponents, pendingTooltipX, pendingTooltipY,
                    net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner.INSTANCE,
                    net.minecraft.resources.ResourceLocation.withDefaultNamespace("tooltip"));
        }
    }

    /**
     * Queue a tooltip to be rendered at the end of the frame.
     */
    public void setTooltipForNextRenderPass(List<Component> tooltip, int mouseX, int mouseY) {
        this.pendingTooltip = tooltip;
        this.pendingTooltipX = mouseX;
        this.pendingTooltipY = mouseY;
    }

    /**
     * Override this to render panel backgrounds before widgets.
     * Called after background, before widgets.
     */
    protected void renderPanels(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Default: no panels
    }

    /**
     * Override this to render overlays on top of widgets.
     * Called after widgets, before toasts/dialogs.
     */
    protected void renderOverlays(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Default: no overlays
    }

    // Helper: Draw themed panel
    protected void drawPanel(GuiGraphics ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, TelegraphTheme.PANEL_BG);
        drawBorder(ctx, x, y, w, h, TelegraphTheme.PANEL_BORDER);
    }

    // Helper: Draw themed panel with header
    protected void drawPanelWithHeader(GuiGraphics ctx, int x, int y, int w, int h, String title) {
        drawPanel(ctx, x, y, w, h);
        int headerH = layout.headerHeight;
        ctx.fill(x + 1, y + 1, x + w - 1, y + headerH, TelegraphTheme.HEADER_BG);
        ctx.drawString(font, title, x + layout.padding, y + (headerH - 8) / 2, TelegraphTheme.TEXT_PRIMARY, false);
    }

    // Helper: Draw border (since GuiGraphics.renderOutline might not exist in all versions)
    protected void drawBorder(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.hLine(x, x + w - 1, y, color);
        ctx.hLine(x, x + w - 1, y + h - 1, color);
        ctx.vLine(x, y, y + h - 1, color);
        ctx.vLine(x + w - 1, y, y + h - 1, color);
    }

    // Helper: Show confirm dialog
    protected void confirm(String title, String msg, Runnable onConfirm) {
        confirmDialog = new ConfirmDialog(title, msg, onConfirm);
        confirmDialog.show(width, height, this::addRenderableWidget);
    }

    // Helper: Show confirm dialog with cancel callback
    protected void confirm(String title, String msg, Runnable onConfirm, Runnable onCancel) {
        confirmDialog = new ConfirmDialog(title, msg, onConfirm, onCancel);
        confirmDialog.show(width, height, this::addRenderableWidget);
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
