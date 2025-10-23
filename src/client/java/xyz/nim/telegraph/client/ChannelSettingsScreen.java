package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.util.*;

public class ChannelSettingsScreen extends Screen {
    private final Screen parent;
    private final TelegraphChannel channel;
    private final int mapId;
    private final ChannelSettings settings;
    
    private ButtonWidget archiveButton;
    private ButtonWidget doneButton;
    private ButtonWidget showTranslationsButton;
    
    public ChannelSettingsScreen(Screen parent, TelegraphChannel channel, int mapId) {
        super(Text.literal("Channel Settings"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = channel.getOrCreateSettings(mapId);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int leftMargin = 20;
        int rightMargin = 20;
        int panelWidth = width - leftMargin - rightMargin;
        int startY = 40;
        
        // Header
        int y = startY + 20;
        
        // Show translations toggle
        showTranslationsButton = ButtonWidget.builder(
            Text.literal(settings.isShowTranslations() ? "Hide Translations" : "Show Translations"),
            button -> {
                settings.setShowTranslations(!settings.isShowTranslations());
                button.setMessage(Text.literal(settings.isShowTranslations() ? "Hide Translations" : "Show Translations"));
            })
            .dimensions(leftMargin, y, 180, 20)
            .build();
        addDrawableChild(showTranslationsButton);
        
        // Archive button
        archiveButton = ButtonWidget.builder(
            Text.literal(settings.isArchived() ? "Unarchive" : "Archive"),
            button -> {
                settings.setArchived(!settings.isArchived());
                button.setMessage(Text.literal(settings.isArchived() ? "Unarchive" : "Archive"));
            })
            .dimensions(leftMargin + 185, y, 100, 20)
            .build();
        addDrawableChild(archiveButton);
        
        // Done button
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(width / 2 - 75, height - 30, 150, 20).build();
        addDrawableChild(doneButton);
    }
    
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int leftMargin = 20;
        
        // Title
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "§7" + channel.getDisplayName(mapId), width / 2, 32, 0xFFAAAAAA);
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
