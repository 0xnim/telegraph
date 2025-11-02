package xyz.nim.telegraph.client.util;

import net.minecraft.client.gui.DrawContext;

public class GuiUtil {
    public static void drawBorder(DrawContext ctx, int x, int y, int width, int height, int color) {
        int right = x + width;
        int bottom = y + height;
        ctx.fill(x, y, right, y + 1, color);
        ctx.fill(x, bottom - 1, right, bottom, color);
        ctx.fill(x, y, x + 1, bottom, color);
        ctx.fill(right - 1, y, right, bottom, color);
    }
}
