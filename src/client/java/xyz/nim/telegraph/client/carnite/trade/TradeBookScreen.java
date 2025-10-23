package xyz.nim.telegraph.client.carnite.trade;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Trade Book Screen - Browse and manage trade offers.
 * 
 * Shows:
 * - Incoming trade offers
 * - Your sent offers
 * - Trade history
 * - Accept/Reject/Counter actions
 */
public class TradeBookScreen extends Screen {
    private final Screen parent;
    private final String myCivilization;
    
    public TradeBookScreen(Screen parent, String myCiv) {
        super(Text.literal("Trade Book"));
        this.parent = parent;
        this.myCivilization = myCiv;
    }
    
    @Override
    protected void init() {
        super.init();
        
        // New Trade button
        addDrawableChild(ButtonWidget.builder(Text.literal("+ New Trade"), button -> {
            if (client != null) {
                client.setScreen(new TradeComposerScreen(this, myCivilization));
            }
        }).dimensions(width - 120, 10, 110, 20).build());
        
        // Back button
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), button -> {
            close();
        }).dimensions(10, height - 30, 80, 20).build());
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 20, 0xFFFFFF);
        
        // Placeholder
        String info = "Trade Book - Under Construction";
        context.drawCenteredTextWithShadow(textRenderer, info, width / 2, height / 2 - 20, 0xAAAAAA);
        
        String instruction = "This will show all your trade offers and history";
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
