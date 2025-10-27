package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.time.format.DateTimeFormatter;
import java.util.*;

public class MapDecorationsScreen extends Screen {
    private static final int PANEL_MARGIN = 10;
    private static final int PANEL_PADDING = 6;
    private static final int HEADER_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PANEL_COLOR = 0xA0000000;
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private final TelegraphChannel channel;
    private int selectedMapId = -1;
    private ViewTab currentTab = ViewTab.MESSAGES;
    private TextFieldWidget renameField;
    private ButtonWidget renameButton;
    private ButtonWidget rawTabButton;
    private ButtonWidget messagesTabButton;
    private ButtonWidget protocolButton;
    private ButtonWidget channelTypeButton;
    private ButtonWidget settingsButton;
    private ChannelListWidget channelList;
    private MessageListWidget messageList;
    
    private enum ViewTab {
        RAW("Raw"),
        MESSAGES("Messages");
        
        final String label;
        
        ViewTab(String label) {
            this.label = label;
        }
    }
    
    public MapDecorationsScreen(TelegraphChannel channel) {
        super(Text.literal("Map Decorations"));
        this.channel = channel;
    }
    
    @Override
    protected void init() {
        super.init();
        
        Map<Integer, String> channels = channel.getAllChannels();
        if (!channels.isEmpty() && selectedMapId == -1) {
            selectedMapId = channels.keySet().iterator().next();
        }
        
        int leftPanelWidth = 180;
        int leftPanelX = PANEL_MARGIN;
        int leftPanelY = PANEL_MARGIN;
        int leftPanelHeight = height - PANEL_MARGIN * 2;
        
        int channelListY = leftPanelY + PANEL_PADDING + HEADER_HEIGHT;
        channelList = new ChannelListWidget(
            client,
            leftPanelWidth - PANEL_PADDING * 2,
            leftPanelHeight - PANEL_PADDING * 2 - BUTTON_HEIGHT * 4 - 10 - HEADER_HEIGHT,
            channelListY,
            BUTTON_HEIGHT
        );
        channelList.setX(leftPanelX + PANEL_PADDING);
        addDrawableChild(channelList);
        
        int bottomButtonY = height - PANEL_MARGIN - PANEL_PADDING - BUTTON_HEIGHT;
        
        settingsButton = ButtonWidget.builder(Text.literal("Advanced"), button -> {
            if (selectedMapId != -1 && client != null) {
                client.setScreen(new ChannelSettingsScreen(this, channel, selectedMapId));
            }
        }).dimensions(leftPanelX + PANEL_PADDING, bottomButtonY - BUTTON_HEIGHT * 4 - 8, leftPanelWidth - PANEL_PADDING * 2, BUTTON_HEIGHT).build();
        addDrawableChild(settingsButton);
        
        ButtonWidget globalSettingsButton = ButtonWidget.builder(Text.literal("⚙ Global Settings"), button -> {
            if (client != null) {
                client.setScreen(new GlobalSettingsScreen(this));
            }
        }).dimensions(leftPanelX + PANEL_PADDING, bottomButtonY - BUTTON_HEIGHT * 3 - 6, leftPanelWidth - PANEL_PADDING * 2, BUTTON_HEIGHT).build();
        addDrawableChild(globalSettingsButton);
        
        protocolButton = ButtonWidget.builder(Text.literal("Protocol: ..."), button -> {
            if (selectedMapId != -1) {
                cycleProtocol();
                updateProtocolButtons();
            }
        }).dimensions(leftPanelX + PANEL_PADDING, bottomButtonY - BUTTON_HEIGHT * 2 - 4, leftPanelWidth - PANEL_PADDING * 2, BUTTON_HEIGHT).build();
        addDrawableChild(protocolButton);
        
        channelTypeButton = ButtonWidget.builder(Text.literal("Type: ..."), button -> {
            if (selectedMapId != -1) {
                cycleChannelType();
                updateProtocolButtons();
            }
        }).dimensions(leftPanelX + PANEL_PADDING, bottomButtonY - BUTTON_HEIGHT - 2, leftPanelWidth - PANEL_PADDING * 2, BUTTON_HEIGHT).build();
        addDrawableChild(channelTypeButton);
        
        renameButton = ButtonWidget.builder(Text.literal("Rename"), button -> {
            if (selectedMapId != -1 && renameField != null) {
                renameField.setFocused(true);
            }
        }).dimensions(leftPanelX + PANEL_PADDING, bottomButtonY, leftPanelWidth - PANEL_PADDING * 2, BUTTON_HEIGHT).build();
        addDrawableChild(renameButton);
        
        int rightPanelX = leftPanelX + leftPanelWidth + PANEL_MARGIN;
        int rightPanelY = PANEL_MARGIN;
        int rightPanelWidth = width - rightPanelX - PANEL_MARGIN;
        int rightPanelHeight = height - PANEL_MARGIN * 2;
        
        int tabY = rightPanelY + PANEL_PADDING + HEADER_HEIGHT;
        
        messagesTabButton = ButtonWidget.builder(Text.literal("Messages"), button -> {
            currentTab = ViewTab.MESSAGES;
            updateMessageList();
        }).dimensions(rightPanelX + PANEL_PADDING, tabY, 80, BUTTON_HEIGHT).build();
        
        rawTabButton = ButtonWidget.builder(Text.literal("Raw"), button -> {
            currentTab = ViewTab.RAW;
            updateMessageList();
        }).dimensions(rightPanelX + PANEL_PADDING + 85, tabY, 60, BUTTON_HEIGHT).build();
        
        ButtonWidget composeButton = ButtonWidget.builder(Text.literal("Compose"), button -> {
            if (selectedMapId != -1 && client != null) {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                if (settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.CarniteProtocol) {
                    client.setScreen(new xyz.nim.telegraph.client.carnite.CarniteComposerScreen(this, channel, selectedMapId, settings));
                } else {
                    client.setScreen(new MessageComposerScreen(this, channel, selectedMapId));
                }
            }
        }).dimensions(rightPanelX + PANEL_PADDING + 150, tabY, 70, BUTTON_HEIGHT).build();
        
        ButtonWidget tradesButton = ButtonWidget.builder(Text.literal("💰 Trades"), button -> {
            if (client != null && xyz.nim.telegraph.client.trade.TradeManager.TRADES_ENABLED) {
                client.setScreen(new xyz.nim.telegraph.client.trade.TradesDashboardScreen(this, channel));
            }
        }).dimensions(rightPanelX + PANEL_PADDING + 225, tabY, 80, BUTTON_HEIGHT).build();
        tradesButton.active = xyz.nim.telegraph.client.trade.TradeManager.TRADES_ENABLED;
        
        addDrawableChild(messagesTabButton);
        addDrawableChild(rawTabButton);
        addDrawableChild(composeButton);
        addDrawableChild(tradesButton);
        
        renameField = new TextFieldWidget(textRenderer, rightPanelX + PANEL_PADDING + 310, tabY, 150, BUTTON_HEIGHT, Text.literal("Channel Name"));
        renameField.setMaxLength(32);
        renameField.setPlaceholder(Text.literal("Enter channel name..."));
        addDrawableChild(renameField);
        
        int messageListY = tabY + BUTTON_HEIGHT + 4;
        int messageListHeight = rightPanelHeight - PANEL_PADDING * 2 - BUTTON_HEIGHT - 4 - HEADER_HEIGHT;
        
        messageList = new MessageListWidget(
            client,
            rightPanelWidth - PANEL_PADDING * 2,
            messageListHeight,
            messageListY,
            24
        );
        messageList.setX(rightPanelX + PANEL_PADDING);
        addDrawableChild(messageList);
        
        updateChannelList();
        updateMessageList();
        updateProtocolButtons();
    }
    
    private void cycleProtocol() {
        ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
        if (settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.MapTelegraphProtocol) {
            settings.setProtocol(new xyz.nim.telegraph.client.protocol.CarniteProtocol());
        } else {
            settings.setProtocol(new xyz.nim.telegraph.client.protocol.MapTelegraphProtocol());
        }
        updateProtocolButtons();
        updateMessageList();
    }
    
    private void cycleChannelType() {
        ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
        List<String> types = settings.getProtocol().getChannelTypes();
        if (types.isEmpty()) return;
        
        int currentIndex = types.indexOf(settings.getChannelType());
        int nextIndex = (currentIndex + 1) % types.size();
        settings.setChannelType(types.get(nextIndex));
        updateMessageList();
    }
    
    private void updateProtocolButtons() {
        if (selectedMapId == -1) {
            if (protocolButton != null) protocolButton.setMessage(Text.literal("Protocol: ..."));
            if (channelTypeButton != null) {
                channelTypeButton.setMessage(Text.literal("Type: ..."));
                channelTypeButton.visible = false;
            }
            return;
        }
        
        ChannelSettings settings = channel.getSettings(selectedMapId);
        if (settings == null) return;
        
        String protocolName = settings.getProtocol().getName();
        String shortName = protocolName.contains("Carnite") ? "Carnite" : 
                          protocolName.contains("Telegraph") ? "Telegraph" : protocolName;
        
        if (protocolButton != null) {
            protocolButton.setMessage(Text.literal("Protocol: " + shortName));
        }
        
        boolean isTelegraphProtocol = settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.MapTelegraphProtocol;
        
        if (channelTypeButton != null) {
            if (isTelegraphProtocol && settings.getChannelType() != null) {
                String shortType = settings.getChannelType().length() > 12 ? 
                                  settings.getChannelType().substring(0, 12) : 
                                  settings.getChannelType();
                channelTypeButton.setMessage(Text.literal("Type: " + shortType));
                channelTypeButton.visible = true;
            } else {
                channelTypeButton.visible = false;
            }
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int leftPanelWidth = 180;
        int leftPanelX = PANEL_MARGIN;
        int leftPanelY = PANEL_MARGIN;
        int leftPanelHeight = height - PANEL_MARGIN * 2;
        
        drawPanel(context, leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight);
        context.drawText(textRenderer, "Channels", leftPanelX + PANEL_PADDING, leftPanelY + PANEL_PADDING + 4, 0xFFFFFFFF, false);
        
        int rightPanelX = leftPanelX + leftPanelWidth + PANEL_MARGIN;
        int rightPanelY = PANEL_MARGIN;
        int rightPanelWidth = width - rightPanelX - PANEL_MARGIN;
        int rightPanelHeight = height - PANEL_MARGIN * 2;
        
        drawPanel(context, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight);
        
        if (selectedMapId != -1) {
            String channelName = channel.getDisplayName(selectedMapId);
            context.drawText(textRenderer, channelName, rightPanelX + PANEL_PADDING, rightPanelY + PANEL_PADDING + 4, 0xFFFFFFFF, false);
        }
        
        rawTabButton.active = currentTab != ViewTab.RAW;
        messagesTabButton.active = currentTab != ViewTab.MESSAGES;
        renameButton.active = selectedMapId != -1;
        protocolButton.active = selectedMapId != -1;
        settingsButton.active = selectedMapId != -1;
        
        if (selectedMapId != -1) {
            ChannelSettings settings = channel.getSettings(selectedMapId);
            boolean isTelegraphProtocol = settings != null && 
                settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.MapTelegraphProtocol;
            channelTypeButton.visible = isTelegraphProtocol;
            channelTypeButton.active = isTelegraphProtocol;
        } else {
            channelTypeButton.visible = false;
        }
    }
    
    private void drawPanel(DrawContext context, int x, int y, int width, int height) {
        context.drawHorizontalLine(x, x + width - 1, y, PANEL_BORDER_COLOR);
        context.drawHorizontalLine(x, x + width - 1, y + height - 1, PANEL_BORDER_COLOR);
        context.drawVerticalLine(x, y, y + height - 1, PANEL_BORDER_COLOR);
        context.drawVerticalLine(x + width - 1, y, y + height - 1, PANEL_BORDER_COLOR);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renameField != null && renameField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                String newName = renameField.getText();
                if (!newName.isBlank() && selectedMapId != -1) {
                    channel.setUserChannelName(selectedMapId, newName);
                    updateChannelList();
                }
                renameField.setText("");
                renameField.setFocused(false);
                return true;
            } else if (keyCode == 256) {
                renameField.setText("");
                renameField.setFocused(false);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    private void updateChannelList() {
        if (channelList == null) return;
        
        channelList.clearEntries();
        
        Map<Integer, String> channels = channel.getAllChannels();
        for (Map.Entry<Integer, String> entry : channels.entrySet()) {
            channelList.addChannelEntry(new ChannelListWidget.ChannelEntry(
                client,
                entry.getKey(),
                entry.getValue(),
                entry.getKey() == selectedMapId,
                this::onChannelSelected
            ));
        }
    }
    
    private void onChannelSelected(int mapId) {
        selectedMapId = mapId;
        updateChannelList();
        updateMessageList();
        updateProtocolButtons();
    }
    
    private void updateMessageList() {
        if (messageList == null) return;
        
        messageList.clearEntries();
        
        if (selectedMapId == -1) {
            messageList.addMessageEntry(new MessageListWidget.MessageEntry(client, "No map selected", 0xFFAAAAAA, null));
            return;
        }
        
        List<TelegraphMessage> messages = new ArrayList<>(channel.getMessages(selectedMapId));
        messages.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
        
        if (messages.isEmpty()) {
            messageList.addMessageEntry(new MessageListWidget.MessageEntry(client, "No messages recorded", 0xFFAAAAAA, null));
            return;
        }
        
        if (currentTab == ViewTab.RAW) {
            ChannelSettings settings = channel.getSettings(selectedMapId);
            boolean isCarnite = settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.CarniteProtocol;
            
            for (TelegraphMessage message : messages) {
                String timestamp = message.timestamp().atZone(java.time.ZoneId.systemDefault()).format(TIME_FORMATTER);
                int typeColor = getColorForType(message.type());
                String typePrefix = getTypePrefix(message.type());
                String fullMessage = "[" + timestamp + "] " + typePrefix + message.content();
                
                String tooltip = null;
                if (isCarnite && message.decoration() != null && message.decoration().name() != null) {
                    String tense = xyz.nim.telegraph.client.carnite.CarniteParser.getTenseFromColor(message.decoration().type());
                    String expanded = xyz.nim.telegraph.client.carnite.CarniteVocabulary.formatWithExpansion(message.decoration().name());
                    tooltip = "§e" + tense + "\n§7" + expanded;
                }
                
                messageList.addMessageEntry(new MessageListWidget.MessageEntry(client, fullMessage, typeColor, tooltip));
            }
        } else {
            List<FormattedMapMessage> formattedMessages = extractMapMessages(messages);
            
            if (formattedMessages.isEmpty()) {
                messageList.addMessageEntry(new MessageListWidget.MessageEntry(client, "No banner messages on this map", 0xFFAAAAAA, null));
            } else {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                boolean isCarnite = settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.CarniteProtocol;
                
                for (FormattedMapMessage msg : formattedMessages) {
                    String colorCode = getBannerColorFormatting(msg.bannerType);
                    String displayText = colorCode + msg.name;
                    
                    String tooltip = null;
                    String carniteTranslation = null;
                    
                    if (isCarnite) {
                        xyz.nim.telegraph.client.carnite.CarniteParser.ParsedCarniteMessage parsed = 
                            xyz.nim.telegraph.client.carnite.CarniteParser.parse(msg.name, msg.bannerType);
                        String tense = xyz.nim.telegraph.client.carnite.CarniteParser.getTenseFromColor(msg.bannerType);
                        String expanded = xyz.nim.telegraph.client.carnite.CarniteVocabulary.formatWithExpansion(msg.name);
                        tooltip = "§e" + tense + "\n§7" + expanded;
                        
                        // Get English translation if enabled
                        if (settings.isShowTranslations()) {
                            xyz.nim.telegraph.client.carnite.CarniteTranslator.TranslationResult translation = 
                                xyz.nim.telegraph.client.carnite.CarniteTranslator.translate(msg.name, msg.bannerType);
                            carniteTranslation = translation.translation();
                        }
                    }
                    
                    messageList.addMessageEntry(new MessageListWidget.MessageEntry(client, displayText, 0xFFFFFFFF, tooltip, carniteTranslation));
                }
            }
        }
    }
    
    private List<FormattedMapMessage> extractMapMessages(List<TelegraphMessage> messages) {
        List<FormattedMapMessage> result = new ArrayList<>();
        
        ChannelSettings settings = selectedMapId != -1 ? channel.getSettings(selectedMapId) : null;
        
        for (TelegraphMessage message : messages) {
            if (message.decoration() != null && message.decoration().type().contains("banner")) {
                if (message.type() == TelegraphMessage.ChangeType.ADDED && message.decoration().name() != null) {
                    String formattedText = message.decoration().name();
                    String bannerColor = message.decoration().type();
                    
                    if (settings != null && settings.getProtocol() != null && settings.getChannelType() != null) {
                        formattedText = settings.getProtocol().formatMessage(message, settings.getChannelType());
                    }
                    
                    result.add(new FormattedMapMessage(
                        formattedText,
                        bannerColor,
                        message.decoration().x(),
                        message.decoration().z(),
                        message.timestamp()
                    ));
                }
            }
        }
        
        result.sort(Comparator.comparing(FormattedMapMessage::timestamp).reversed());
        
        return result;
    }
    
    private String getBannerColorFormatting(String bannerType) {
        if (bannerType.contains("white")) return "§f";
        if (bannerType.contains("orange")) return "§6";
        if (bannerType.contains("magenta")) return "§d";
        if (bannerType.contains("light_blue")) return "§b";
        if (bannerType.contains("yellow")) return "§e";
        if (bannerType.contains("lime")) return "§a";
        if (bannerType.contains("pink")) return "§d";
        if (bannerType.contains("gray") && !bannerType.contains("light")) return "§8";
        if (bannerType.contains("light_gray")) return "§7";
        if (bannerType.contains("cyan")) return "§3";
        if (bannerType.contains("purple")) return "§5";
        if (bannerType.contains("blue") && !bannerType.contains("light")) return "§9";
        if (bannerType.contains("brown")) return "§6";
        if (bannerType.contains("green")) return "§2";
        if (bannerType.contains("red")) return "§c";
        if (bannerType.contains("black")) return "§0";
        return "§f";
    }
    
    private record FormattedMapMessage(String name, String bannerType, double x, double z, java.time.Instant timestamp) {}
    
    private int getColorForType(TelegraphMessage.ChangeType type) {
        if (type == null) return 0xFFAAAAAA;
        return switch (type) {
            case ADDED -> 0xFF55FFFF;
            case REMOVED -> 0xFFFF5555;
            case CHANGED -> 0xFFFFFF55;
        };
    }
    
    private String getTypePrefix(TelegraphMessage.ChangeType type) {
        if (type == null) return "";
        return switch (type) {
            case ADDED -> "[+] ";
            case REMOVED -> "[-] ";
            case CHANGED -> "[~] ";
        };
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private static class ChannelListWidget extends AlwaysSelectedEntryListWidget<ChannelListWidget.ChannelEntry> {
        
        public ChannelListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }
        
        public void clearEntries() {
            this.children().clear();
        }
        
        public void addChannelEntry(ChannelEntry entry) {
            this.addEntry(entry);
        }
        
        @Override
        public int getRowWidth() {
            return this.width;
        }
        
        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }
        
        public static class ChannelEntry extends Entry<ChannelEntry> {
            private final net.minecraft.client.MinecraftClient client;
            private final int mapId;
            private final String displayName;
            private final boolean selected;
            private final java.util.function.Consumer<Integer> onSelect;
            
            public ChannelEntry(net.minecraft.client.MinecraftClient client, int mapId, String displayName, boolean selected, java.util.function.Consumer<Integer> onSelect) {
                this.client = client;
                this.mapId = mapId;
                this.displayName = displayName;
                this.selected = selected;
                this.onSelect = onSelect;
            }
            
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                int bgColor = selected ? 0x60FFFFFF : (hovered ? 0x40FFFFFF : 0x00000000);
                if (bgColor != 0) {
                    context.fill(x, y, x + entryWidth, y + entryHeight, bgColor);
                }
                
                int textColor = selected ? 0xFFFFFF00 : 0xFFFFFFFF;
                context.drawText(client.textRenderer, displayName, x + 4, y + (entryHeight - 8) / 2, textColor, false);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    onSelect.accept(mapId);
                    return true;
                }
                return false;
            }
            
            @Override
            public Text getNarration() {
                return Text.literal(displayName);
            }
        }
    }
    
    private static class MessageListWidget extends AlwaysSelectedEntryListWidget<MessageListWidget.MessageEntry> {
        
        public MessageListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }
        
        public void clearEntries() {
            this.children().clear();
        }
        
        public void addMessageEntry(MessageEntry entry) {
            this.addEntry(entry);
        }
        
        @Override
        public int getRowWidth() {
            return this.width;
        }
        
        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }
        
        public static class MessageEntry extends Entry<MessageEntry> {
            private final String message;
            private final int color;
            private final String tooltip;
            private final String translation;
            private final net.minecraft.client.MinecraftClient client;
            
            public MessageEntry(net.minecraft.client.MinecraftClient client, String message, int color, String tooltip) {
                this(client, message, color, tooltip, null);
            }
            
            public MessageEntry(net.minecraft.client.MinecraftClient client, String message, int color, String tooltip, String translation) {
                this.client = client;
                this.message = message;
                this.color = color;
                this.tooltip = tooltip;
                this.translation = translation;
            }
            
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                context.drawText(client.textRenderer, message, x + 4, y + 2, color, false);
                
                // Draw translation below the Carnite message
                if (translation != null && !translation.isEmpty()) {
                    String translationPrefix = "  → ";
                    String truncatedTranslation = translation;
                    
                    // Truncate if too long to fit on one line
                    int maxWidth = entryWidth - 30;
                    if (client.textRenderer.getWidth(translationPrefix + translation) > maxWidth) {
                        while (client.textRenderer.getWidth(translationPrefix + truncatedTranslation + "...") > maxWidth && truncatedTranslation.length() > 10) {
                            truncatedTranslation = truncatedTranslation.substring(0, truncatedTranslation.length() - 1);
                        }
                        truncatedTranslation += "...";
                    }
                    
                    context.drawText(client.textRenderer, translationPrefix + truncatedTranslation, x + 4, y + 12, 0xFFAAAAAA, false);
                }
                
                if (hovered && tooltip != null && mouseX >= x && mouseX <= x + entryWidth && mouseY >= y && mouseY <= y + entryHeight) {
                    List<Text> tooltipLines = new ArrayList<>();
                    for (String line : tooltip.split("\n")) {
                        tooltipLines.add(Text.literal(line));
                    }
                    
                    // Add full translation to tooltip if it was truncated
                    if (translation != null && !translation.isEmpty() && client.textRenderer.getWidth("  → " + translation) > (entryWidth - 30)) {
                        tooltipLines.add(Text.literal(""));
                        tooltipLines.add(Text.literal("§bFull Translation:"));
                        tooltipLines.add(Text.literal("§f" + translation));
                    }
                    
                    context.drawTooltip(client.textRenderer, tooltipLines, mouseX, mouseY);
                }
            }
            
            @Override
            public Text getNarration() {
                return Text.literal(message);
            }
        }
    }
}
