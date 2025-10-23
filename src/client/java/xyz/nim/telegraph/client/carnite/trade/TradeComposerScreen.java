package xyz.nim.telegraph.client.carnite.trade;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Trade Composer Screen - Visual interface for creating Carnite trade offers.
 * 
 * Access via:
 * - Press 'T' key (default trade keybind)
 * - Click "Trade" button in Telegraph menu
 * - Command: /trade [civilization]
 * - Right-click on yellow banner → "Create Trade Offer"
 */
public class TradeComposerScreen extends Screen {
    private final Screen parent;
    private final String myCivilization;
    private final String targetCiv;
    
    // TODO: Add widgets for item selection, quantity input, etc.
    
    public TradeComposerScreen(Screen parent, String myCiv) {
        this(parent, myCiv, null);
    }
    
    public TradeComposerScreen(Screen parent, String myCiv, String targetCiv) {
        super(Text.literal("Trade Offer"));
        this.parent = parent;
        this.myCivilization = myCiv;
        this.targetCiv = targetCiv;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // TODO: Implement full UI (see TRADE_SYSTEM_DESIGN.md)
        
        // Placeholder: Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            close();
        }).dimensions(width / 2 - 100, height - 30, 200, 20).build());
        
        // Placeholder: Trade Book button
        addDrawableChild(ButtonWidget.builder(Text.literal("View Trade Book"), button -> {
            if (client != null) {
                client.setScreen(new TradeBookScreen(this, myCivilization));
            }
        }).dimensions(width / 2 - 100, height - 55, 200, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 20, 0xFFFFFF);
        
        // Placeholder UI
        String info = "Trade Composer - Under Construction";
        context.drawCenteredTextWithShadow(textRenderer, info, width / 2, height / 2 - 20, 0xAAAAAA);
        
        String instruction = "See TRADE_SYSTEM_DESIGN.md for full specification";
        context.drawCenteredTextWithShadow(textRenderer, instruction, width / 2, height / 2, 0x888888);
    }
    
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
