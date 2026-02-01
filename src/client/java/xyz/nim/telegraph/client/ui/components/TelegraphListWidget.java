package xyz.nim.telegraph.client.ui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import xyz.nim.telegraph.client.ui.TelegraphTheme;

public class TelegraphListWidget<E extends TelegraphListWidget.Entry<E>>
        extends ObjectSelectionList<E> {

    private static final int SCROLLBAR_WIDTH = 6;

    public TelegraphListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
        super(client, width, height, y, itemHeight);
    }

    @Override
    protected int scrollBarX() {
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
            extends ObjectSelectionList.Entry<E> {

        protected void renderBackground(GuiGraphics ctx, int x, int y, int width, int height, boolean hovered, boolean selected) {
            if (selected) {
                ctx.fill(x, y, x + width, y + height, TelegraphTheme.SELECTED & 0x40FFFFFF);
            } else if (hovered) {
                ctx.fill(x, y, x + width, y + height, TelegraphTheme.HOVER);
            }
        }

        protected void renderText(GuiGraphics ctx, net.minecraft.client.gui.Font font,
                                  String text, int x, int y, int color) {
            ctx.drawString(font, text, x, y, color, false);
        }

        protected void renderTextWithShadow(GuiGraphics ctx, net.minecraft.client.gui.Font font,
                                            String text, int x, int y, int color) {
            ctx.drawString(font, text, x, y, color, true);
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }
}
