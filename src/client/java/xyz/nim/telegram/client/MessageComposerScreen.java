package xyz.nim.telegram.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegram.client.protocol.CarniteProtocol;
import xyz.nim.telegram.client.protocol.MapTelegraphProtocol;

import java.util.ArrayList;
import java.util.List;

public class MessageComposerScreen extends Screen {
    private static final int PANEL_MARGIN = 10;
    private static final int PANEL_PADDING = 6;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final int MAX_RECOMMENDED_LENGTH = 32;
    
    private final Screen parent;
    private final TelegramChannel channel;
    private final int mapId;
    private final ChannelSettings settings;
    
    private TextFieldWidget messageField;
    private ButtonWidget doneButton;
    private ButtonWidget copyButton;
    private ButtonWidget clearButton;
    private List<ButtonWidget> colorButtons = new ArrayList<>();
    private List<ButtonWidget> templateButtons = new ArrayList<>();
    
    private String selectedBannerColor = "white";
    
    public MessageComposerScreen(Screen parent, TelegramChannel channel, int mapId) {
        super(Text.literal("Message Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = channel.getOrCreateSettings(mapId);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = width / 2;
        int panelWidth = 500;
        int panelX = centerX - panelWidth / 2;
        
        messageField = new TextFieldWidget(textRenderer, panelX + PANEL_PADDING, 60, panelWidth - PANEL_PADDING * 2, 20, Text.literal("Message"));
        messageField.setMaxLength(64);
        messageField.setPlaceholder(Text.literal("Type your message..."));
        addDrawableChild(messageField);
        
        int y = 95;
        
        boolean isCarnite = settings.getProtocol() instanceof CarniteProtocol;
        
        if (isCarnite) {
            Text.literal("Select Banner Color (Tense):").getString();
            
            String[][] colors = {
                {"white", "White (Present)", "0xFFFFFFFF"},
                {"light_gray", "Lt.Grey (Past)", "0xFFAAAAAA"},
                {"gray", "Grey (Future)", "0xFF555555"},
                {"pink", "Pink (Might)", "0xFFFF88FF"},
                {"red", "Red (URGENT)", "0xFFFF0000"},
                {"light_blue", "Lt.Blue (Request)", "0xFF88DDFF"},
                {"black", "Black (Decided)", "0xFF222222"},
                {"blue", "Blue (Question)", "0xFF5555FF"},
                {"yellow", "Yellow (Trade)", "0xFFFFFF00"},
                {"purple", "Purple (Goal)", "0xFFAA00FF"}
            };
            
            int colorX = panelX + PANEL_PADDING;
            int colorY = y;
            
            for (String[] color : colors) {
                String colorKey = color[0];
                String label = color[1];
                
                ButtonWidget colorBtn = ButtonWidget.builder(Text.literal(label), button -> {
                    selectedBannerColor = colorKey;
                    updateColorButtons();
                }).dimensions(colorX, colorY, 140, 18).build();
                
                addDrawableChild(colorBtn);
                colorButtons.add(colorBtn);
                
                colorY += 20;
                if (colorY > height - 100) {
                    colorX += 145;
                    colorY = y;
                }
            }
        } else {
            addTemplateButtons(panelX, y);
        }
        
        copyButton = ButtonWidget.builder(Text.literal("Copy to Clipboard"), button -> {
            String message = getFormattedMessage();
            if (client != null) {
                client.keyboard.setClipboard(message);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§aCopied to clipboard!"), false);
                }
            }
        }).dimensions(panelX + PANEL_PADDING, height - 60, 150, 20).build();
        addDrawableChild(copyButton);
        
        clearButton = ButtonWidget.builder(Text.literal("Clear"), button -> {
            if (messageField != null) {
                messageField.setText("");
            }
        }).dimensions(panelX + PANEL_PADDING + 155, height - 60, 80, 20).build();
        addDrawableChild(clearButton);
        
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(panelX + panelWidth - PANEL_PADDING - 80, height - 60, 80, 20).build();
        addDrawableChild(doneButton);
        
        updateColorButtons();
    }
    
    private void addTemplateButtons(int panelX, int y) {
        String channelType = settings.getChannelType();
        List<String> templates = new ArrayList<>();
        
        if (MapTelegraphProtocol.KOS_CHANNEL.equals(channelType)) {
            templates.add("PlayerName - Raiding");
            templates.add("PlayerName - Griefing");
            templates.add("PlayerName - CLEARED");
        } else if (MapTelegraphProtocol.MILITARY_CHANNEL.equals(channelType)) {
            templates.add("Incoming raid at ");
            templates.add("Need backup at ");
            templates.add("All clear");
            templates.add("Mobilizing to ");
        } else {
            templates.add("Meeting at ");
            templates.add("Trade available: ");
            templates.add("Looking for: ");
        }
        
        int templateY = y;
        for (String template : templates) {
            ButtonWidget templateBtn = ButtonWidget.builder(Text.literal(template), button -> {
                if (messageField != null) {
                    messageField.setText(template);
                }
            }).dimensions(panelX + PANEL_PADDING, templateY, 200, 18).build();
            
            addDrawableChild(templateBtn);
            templateButtons.add(templateBtn);
            templateY += 20;
        }
    }
    
    private void updateColorButtons() {
        for (int i = 0; i < colorButtons.size(); i++) {
            String[] colors = {"white", "light_gray", "gray", "pink", "red", "light_blue", "black", "blue", "yellow", "purple"};
            if (i < colors.length) {
                colorButtons.get(i).active = !colors[i].equals(selectedBannerColor);
            }
        }
    }
    
    private String getFormattedMessage() {
        String message = messageField != null ? messageField.getText() : "";
        
        if (settings.getProtocol() instanceof CarniteProtocol) {
            return "[" + selectedBannerColor.toUpperCase() + " BANNER] " + message;
        }
        
        return message;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int centerX = width / 2;
        int panelWidth = 500;
        int panelX = centerX - panelWidth / 2;
        
        context.drawText(textRenderer, "Composing for: " + channel.getDisplayName(mapId), panelX + PANEL_PADDING, 40, 0xFFFFFFFF, false);
        
        String protocolName = settings.getProtocol() instanceof CarniteProtocol ? "Carnite Telegraphic" : "Map Telegraph";
        context.drawText(textRenderer, "Protocol: " + protocolName, panelX + PANEL_PADDING, 50, 0xFFAAAAAA, false);
        
        if (messageField != null && !messageField.getText().isEmpty()) {
            int length = messageField.getText().length();
            int color = length > MAX_RECOMMENDED_LENGTH ? 0xFFFF5555 : 0xFF55FF55;
            String lengthText = length + " chars" + (length > MAX_RECOMMENDED_LENGTH ? " (recommended: ≤32)" : "");
            context.drawText(textRenderer, lengthText, panelX + panelWidth - PANEL_PADDING - textRenderer.getWidth(lengthText), 50, color, false);
        }
        
        context.drawText(textRenderer, "Preview:", panelX + PANEL_PADDING, height - 85, 0xFFFFFFFF, false);
        String preview = getFormattedMessage();
        context.drawText(textRenderer, preview, panelX + PANEL_PADDING + 60, height - 85, 0xFFFFFF00, false);
        
        if (settings.getProtocol() instanceof CarniteProtocol) {
            context.drawText(textRenderer, "Select Banner Color:", panelX + PANEL_PADDING, 85, 0xFFFFFFFF, false);
            context.drawText(textRenderer, "Tip: Rename banner in anvil, place on map, right-click with map", panelX + PANEL_PADDING, height - 100, 0xFFAAAAAA, false);
        } else {
            context.drawText(textRenderer, "Quick Templates:", panelX + PANEL_PADDING, 85, 0xFFFFFFFF, false);
            
            if (settings.getChannelType() != null) {
                String typeDesc = settings.getProtocol().getChannelTypeDescription(settings.getChannelType());
                int typeColor = settings.getProtocol().getColorForChannelType(settings.getChannelType());
                context.drawText(textRenderer, "Channel: " + settings.getChannelType(), panelX + 220, 85, typeColor, false);
            }
        }
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
