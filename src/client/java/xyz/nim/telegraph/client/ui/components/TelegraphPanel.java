package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import xyz.nim.telegraph.client.ui.SimpleLayout;
import xyz.nim.telegraph.client.ui.TelegraphTheme;

public class TelegraphPanel {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int padding;
    private String title;
    private boolean hasHeader;
    private int headerHeight = 24;

    public TelegraphPanel(int x, int y, int width, int height, int padding) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.padding = padding;
    }

    public TelegraphPanel withTitle(String title) {
        this.title = title;
        this.hasHeader = true;
        return this;
    }

    public TelegraphPanel withTitle(String title, int headerHeight) {
        this.title = title;
        this.hasHeader = true;
        this.headerHeight = headerHeight;
        return this;
    }

    public void render(DrawContext ctx, TextRenderer font) {
        // Draw background
        ctx.fill(x, y, x + width, y + height, TelegraphTheme.PANEL_BG);
        drawBorder(ctx, x, y, width, height, TelegraphTheme.PANEL_BORDER);

        // Draw header if present
        if (hasHeader && title != null) {
            ctx.fill(x + 1, y + 1, x + width - 1, y + headerHeight, TelegraphTheme.HEADER_BG);
            ctx.drawText(font, title, x + padding, y + (headerHeight - 8) / 2, TelegraphTheme.TEXT_PRIMARY, false);
        }
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.drawHorizontalLine(x, x + w - 1, y, color);
        ctx.drawHorizontalLine(x, x + w - 1, y + h - 1, color);
        ctx.drawVerticalLine(x, y, y + h - 1, color);
        ctx.drawVerticalLine(x + w - 1, y, y + h - 1, color);
    }

    // Content area accessors
    public int contentX() {
        return x + padding;
    }

    public int contentY() {
        return y + padding + (hasHeader ? headerHeight : 0);
    }

    public int contentWidth() {
        return width - padding * 2;
    }

    public int contentHeight() {
        return height - padding * 2 - (hasHeader ? headerHeight : 0);
    }

    // Panel bounds accessors
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // Create VStack/HStack in content area
    public SimpleLayout.VStack vstack(int spacing) {
        return new SimpleLayout.VStack(contentX(), contentY(), contentWidth(), spacing);
    }

    public SimpleLayout.HStack hstack(int spacing) {
        return new SimpleLayout.HStack(contentX(), contentY(), contentHeight(), spacing);
    }

    // Static factory for quick creation
    public static TelegraphPanel create(int x, int y, int width, int height, int padding) {
        return new TelegraphPanel(x, y, width, height, padding);
    }

    public static TelegraphPanel create(SimpleLayout.Box box, int padding) {
        return new TelegraphPanel(box.x, box.y, box.width, box.height, padding);
    }
}
