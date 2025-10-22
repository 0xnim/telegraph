package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ChannelSettingsScreen extends Screen {
    private final Screen parent;
    private final TelegraphChannel channel;
    private final int mapId;
    private final ChannelSettings settings;
    
    private TextFieldWidget tagInputField;
    private ButtonWidget addTagButton;
    private ButtonWidget archiveButton;
    private ButtonWidget exportButton;
    private ButtonWidget doneButton;
    private CyclingButtonWidget<ChannelSettings.NotificationLevel> notificationLevelButton;
    private CyclingButtonWidget<Boolean> notificationsEnabledButton;
    private ButtonWidget channelTypeButton;
    private ButtonWidget protocolButton;
    
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
        
        int centerX = width / 2;
        int startY = 40;
        int buttonWidth = 200;
        int spacing = 25;
        
        int y = startY + spacing;
        
        notificationsEnabledButton = CyclingButtonWidget.onOffBuilder(settings.isNotificationsEnabled())
            .build(centerX - buttonWidth / 2, y, buttonWidth, 20, 
                Text.literal("Notifications"), 
                (button, enabled) -> settings.setNotificationsEnabled(enabled));
        addDrawableChild(notificationsEnabledButton);
        y += spacing;
        
        protocolButton = ButtonWidget.builder(
            Text.literal("Protocol: " + getProtocolShortName()),
            button -> {
                cycleProtocol();
                button.setMessage(Text.literal("Protocol: " + getProtocolShortName()));
                if (channelTypeButton != null) {
                    channelTypeButton.setMessage(Text.literal("Type: " + settings.getChannelType()));
                }
            })
            .dimensions(centerX - buttonWidth / 2, y, buttonWidth, 20)
            .build();
        addDrawableChild(protocolButton);
        y += spacing;
        
        notificationLevelButton = CyclingButtonWidget.<ChannelSettings.NotificationLevel>builder(
                level -> Text.literal("Level: " + level.name()))
            .values(ChannelSettings.NotificationLevel.values())
            .initially(settings.getNotificationLevel())
            .build(centerX - buttonWidth / 2, y, buttonWidth, 20, Text.literal("Notification Level"),
                (button, level) -> settings.setNotificationLevel(level));
        addDrawableChild(notificationLevelButton);
        y += spacing;
        
        channelTypeButton = ButtonWidget.builder(
            Text.literal("Type: " + settings.getChannelType()),
            button -> {
                cycleChannelType();
                button.setMessage(Text.literal("Type: " + settings.getChannelType()));
            })
            .dimensions(centerX - buttonWidth / 2, y, buttonWidth, 20)
            .build();
        addDrawableChild(channelTypeButton);
        y += spacing;
        
        archiveButton = ButtonWidget.builder(
            Text.literal(settings.isArchived() ? "Unarchive Channel" : "Archive Channel"),
            button -> {
                settings.setArchived(!settings.isArchived());
                button.setMessage(Text.literal(settings.isArchived() ? "Unarchive Channel" : "Archive Channel"));
            })
            .dimensions(centerX - buttonWidth / 2, y, buttonWidth, 20)
            .build();
        addDrawableChild(archiveButton);
        y += spacing + 25;
        
        tagInputField = new TextFieldWidget(textRenderer, centerX - buttonWidth / 2, y, buttonWidth - 60, 20, Text.literal("Tag"));
        tagInputField.setMaxLength(32);
        tagInputField.setPlaceholder(Text.literal("Enter tag..."));
        addDrawableChild(tagInputField);
        
        addTagButton = ButtonWidget.builder(Text.literal("+"), button -> {
            String tag = tagInputField.getText();
            if (!tag.isBlank()) {
                settings.addTag(tag);
                tagInputField.setText("");
            }
        }).dimensions(centerX + buttonWidth / 2 - 50, y, 50, 20).build();
        addDrawableChild(addTagButton);
        y += spacing;
        
        exportButton = ButtonWidget.builder(Text.literal("Export Channel Data"), button -> {
            if (client != null) {
                MapDecorationTracker tracker = TelegraphClient.getMapDecorationTracker();
                if (tracker != null) {
                    String filename = "channel_" + mapId + "_export.json";
                    java.nio.file.Path exportPath = client.runDirectory.toPath().resolve(filename);
                    tracker.getPersistenceManager().exportChannelData(mapId, channel, exportPath);
                    
                    if (client.player != null) {
                        client.player.sendMessage(Text.literal("§aExported to " + filename), false);
                    }
                }
            }
        }).dimensions(centerX - buttonWidth / 2, y + 40, buttonWidth, 20).build();
        addDrawableChild(exportButton);
        
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(centerX - buttonWidth / 2, height - 30, buttonWidth, 20).build();
        addDrawableChild(doneButton);
    }
    
    private void cycleProtocol() {
        if (settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.MapTelegraphProtocol) {
            settings.setProtocol(new xyz.nim.telegraph.client.protocol.CarniteProtocol());
        } else {
            settings.setProtocol(new xyz.nim.telegraph.client.protocol.MapTelegraphProtocol());
        }
    }
    
    private String getProtocolShortName() {
        String fullName = settings.getProtocol().getName();
        if (fullName.contains("Telegraph")) return "Telegraph";
        if (fullName.contains("Carnite")) return "Carnite";
        return fullName;
    }
    
    private void cycleChannelType() {
        List<String> types = settings.getProtocol().getChannelTypes();
        if (types.isEmpty()) return;
        
        int currentIndex = types.indexOf(settings.getChannelType());
        int nextIndex = (currentIndex + 1) % types.size();
        settings.setChannelType(types.get(nextIndex));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = width / 2;
        
        context.drawText(textRenderer, "Channel: " + channel.getDisplayName(mapId), centerX - 100, 40, 0xFFFFFFFF, false);
        
        String protocolName = settings.getProtocol().getName();
        context.drawText(textRenderer, "Protocol: " + protocolName, centerX - 100, 52, 0xFFAAAAAA, false);
        
        if (settings.getChannelType() != null) {
            String typeDesc = settings.getProtocol().getChannelTypeDescription(settings.getChannelType());
            int color = settings.getProtocol().getColorForChannelType(settings.getChannelType());
            
            List<String> wrappedDesc = wrapTextManual(typeDesc, 400);
            int descY = 105;
            for (String line : wrappedDesc) {
                context.drawText(textRenderer, line, centerX - 200, descY, color, false);
                descY += 10;
            }
        }
        
        context.drawText(textRenderer, "Tags:", centerX - 100, 175, 0xFFFFFFFF, false);
        
        int tagStartY = 190;
        
        List<String> tags = settings.getTags();
        if (!tags.isEmpty()) {
            int tagY = tagStartY;
            for (String tag : tags) {
                String tagText = "§7[" + tag + "]";
                int tagWidth = textRenderer.getWidth(tagText);
                context.drawText(textRenderer, tagText, centerX - 90, tagY, 0xFFAAAAAA, false);
                
                if (mouseX >= centerX - 90 && mouseX <= centerX - 90 + tagWidth &&
                    mouseY >= tagY && mouseY <= tagY + 10) {
                    context.drawTooltip(textRenderer, Text.literal("Click to remove"), mouseX, mouseY);
                }
                
                tagY += 12;
            }
        }
    }
    
    private List<String> wrapTextManual(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (textRenderer.getWidth(testLine) <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = width / 2;
        int tagStartY = 190;
        
        List<String> tags = settings.getTags();
        int tagY = tagStartY;
        for (String tag : tags) {
            String tagText = "§7[" + tag + "]";
            int tagWidth = textRenderer.getWidth(tagText);
            
            if (mouseX >= centerX - 90 && mouseX <= centerX - 90 + tagWidth &&
                mouseY >= tagY && mouseY <= tagY + 10) {
                settings.removeTag(tag);
                return true;
            }
            
            tagY += 12;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
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
