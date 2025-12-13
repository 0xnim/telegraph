package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ui.TelegraphTheme;

public class TelegraphListWidget<E extends TelegraphListWidget.Entry<E>>
        extends AlwaysSelectedEntryListWidget<E> {

    private static final int SCROLLBAR_WIDTH = 6;

    public TelegraphListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Override
    protected int getScrollbarX() {
        return getX() + getWidth() - SCROLLBAR_WIDTH;
    }

    @Override
    public int getRowWidth() {
        return getWidth() - SCROLLBAR_WIDTH - 6;
    }

    public void clearEntries() {
        children().clear();
    }

    public void addEntryToList(E entry) {
        addEntry(entry);
    }

    public int getEntryCount() {
        return children().size();
    }

    public abstract static class Entry<E extends Entry<E>>
            extends AlwaysSelectedEntryListWidget.Entry<E> {

        protected void renderBackground(DrawContext ctx, int x, int y, int width, int height, boolean hovered, boolean selected) {
            if (selected) {
                ctx.fill(x, y, x + width, y + height, TelegraphTheme.SELECTED & 0x40FFFFFF);
            } else if (hovered) {
                ctx.fill(x, y, x + width, y + height, TelegraphTheme.HOVER);
            }
        }

        protected void renderText(DrawContext ctx, net.minecraft.client.font.TextRenderer font,
                                  String text, int x, int y, int color) {
            ctx.drawText(font, text, x, y, color, false);
        }

        protected void renderTextWithShadow(DrawContext ctx, net.minecraft.client.font.TextRenderer font,
                                            String text, int x, int y, int color) {
            ctx.drawTextWithShadow(font, text, x, y, color);
        }

        @Override
        public Text getNarration() {
            return Text.empty();
        }
    }
}
