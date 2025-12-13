package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.protocol.CarniteProtocol;
import xyz.nim.telegraph.client.protocol.MapTelegraphProtocol;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TextFields;

import java.util.ArrayList;
import java.util.List;

public class MessageComposerScreen extends TelegraphScreen {
    private static final int MAX_RECOMMENDED_LENGTH = 32;

    private final net.minecraft.client.gui.screen.Screen parent;
    private final TelegraphChannel channel;
    private final int mapId;
    private final ChannelSettings settings;

    private TextFieldWidget messageField;
    private ButtonWidget doneButton;
    private ButtonWidget copyButton;
    private ButtonWidget clearButton;
    private List<ButtonWidget> colorButtons = new ArrayList<>();
    private List<ButtonWidget> templateButtons = new ArrayList<>();

    private String selectedBannerColor = "white";

    public MessageComposerScreen(net.minecraft.client.gui.screen.Screen parent, TelegraphChannel channel, int mapId) {
        super(Text.literal("Message Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = channel.getOrCreateSettings(mapId);
    }

    @Override
    protected void init() {
        super.init();

        var centered = layout.centered(500);
        int panelX = centered.x;
        int panelWidth = centered.width;
        int inputFieldY = layout.margin + layout.headerHeight + layout.spacing;

        messageField = TextFields.message(textRenderer, panelX + layout.padding, inputFieldY,
                panelWidth - layout.padding * 2, layout, 64);
        messageField.setPlaceholder(Text.literal("Type your message..."));
        addDrawableChild(messageField);

        int y = inputFieldY + layout.controlHeight + layout.spacing * 2;

        boolean isCarnite = settings.getProtocol() instanceof CarniteProtocol;

        if (isCarnite) {
            String[][] colors = {
                {"white", "White (Present)"},
                {"light_gray", "Lt.Grey (Past)"},
                {"gray", "Grey (Future)"},
                {"pink", "Pink (Might)"},
                {"red", "Red (URGENT)"},
                {"light_blue", "Lt.Blue (Request)"},
                {"black", "Black (Decided)"},
                {"blue", "Blue (Question)"},
                {"yellow", "Yellow (Trade)"},
                {"purple", "Purple (Goal)"}
            };

            int colorX = panelX + layout.padding;
            int colorY = y;
            int colorBtnWidth = Math.min(140, (panelWidth - layout.padding * 2) / 2 - layout.spacing);

            for (String[] color : colors) {
                String colorKey = color[0];
                String label = color[1];

                ButtonWidget colorBtn = Buttons.create(Text.literal(label), colorX, colorY,
                        colorBtnWidth, layout.buttonHeight, button -> {
                            selectedBannerColor = colorKey;
                            updateColorButtons();
                        });

                addDrawableChild(colorBtn);
                colorButtons.add(colorBtn);

                colorY += layout.buttonHeight + 2;
                if (colorY > height - layout.margin - layout.buttonHeight * 3) {
                    colorX += colorBtnWidth + layout.spacing;
                    colorY = y;
                }
            }
        } else {
            addTemplateButtons(panelX, y, panelWidth);
        }

        int bottomButtonY = height - layout.margin - layout.buttonHeight;
        int copyButtonWidth = Math.min(150, panelWidth / 3);
        int clearButtonWidth = Math.min(80, panelWidth / 5);
        int doneButtonWidth = Math.min(80, panelWidth / 5);

        copyButton = Buttons.create(Text.literal("Copy to Clipboard"),
                panelX + layout.padding, bottomButtonY, copyButtonWidth, layout, button -> {
                    String message = getFormattedMessage();
                    if (client != null) {
                        client.keyboard.setClipboard(message);
                        toastManager.success("Copied to clipboard!");
                    }
                });
        addDrawableChild(copyButton);

        clearButton = Buttons.create(Text.literal("Clear"),
                panelX + layout.padding + copyButtonWidth + layout.spacing, bottomButtonY,
                clearButtonWidth, layout, button -> {
                    if (messageField != null) {
                        messageField.setText("");
                    }
                });
        addDrawableChild(clearButton);

        doneButton = Buttons.create(Text.literal("Done"),
                panelX + panelWidth - layout.padding - doneButtonWidth, bottomButtonY,
                doneButtonWidth, layout, button -> close());
        addDrawableChild(doneButton);

        updateColorButtons();
    }

    private void addTemplateButtons(int panelX, int y, int panelWidth) {
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
        int templateWidth = Math.min(200, panelWidth - layout.padding * 2);
        for (String template : templates) {
            ButtonWidget templateBtn = Buttons.create(Text.literal(template),
                    panelX + layout.padding, templateY, templateWidth, layout, button -> {
                        if (messageField != null) {
                            messageField.setText(template);
                        }
                    });

            addDrawableChild(templateBtn);
            templateButtons.add(templateBtn);
            templateY += layout.buttonHeight + 2;
        }
    }

    private void updateColorButtons() {
        String[] colors = {"white", "light_gray", "gray", "pink", "red", "light_blue", "black", "blue", "yellow", "purple"};
        for (int i = 0; i < colorButtons.size(); i++) {
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
    protected void renderPanels(DrawContext context, int mouseX, int mouseY, float delta) {
        var centered = layout.centered(500);
        int panelX = centered.x;
        int panelWidth = centered.width;

        // Channel info
        context.drawText(textRenderer, "Composing for: " + channel.getDisplayName(mapId),
                panelX + layout.padding, layout.margin, TelegraphTheme.TEXT_PRIMARY, false);

        String protocolName = settings.getProtocol() instanceof CarniteProtocol ? "Carnite Telegraphic" : "Map Telegraph";
        context.drawText(textRenderer, "Protocol: " + protocolName,
                panelX + layout.padding, layout.margin + 10, TelegraphTheme.TEXT_SECONDARY, false);

        // Section header
        int headerY = layout.margin + layout.headerHeight + layout.spacing + layout.controlHeight + layout.spacing;
        if (settings.getProtocol() instanceof CarniteProtocol) {
            context.drawText(textRenderer, "Select Banner Color:",
                    panelX + layout.padding, headerY, TelegraphTheme.TEXT_PRIMARY, false);
        } else {
            context.drawText(textRenderer, "Quick Templates:",
                    panelX + layout.padding, headerY, TelegraphTheme.TEXT_PRIMARY, false);

            if (settings.getChannelType() != null) {
                int typeColor = settings.getProtocol().getColorForChannelType(settings.getChannelType());
                context.drawText(textRenderer, "Channel: " + settings.getChannelType(),
                        panelX + 150, headerY, typeColor, false);
            }
        }
    }

    @Override
    protected void renderOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        var centered = layout.centered(500);
        int panelX = centered.x;
        int panelWidth = centered.width;

        // Character count
        if (messageField != null && !messageField.getText().isEmpty()) {
            int length = messageField.getText().length();
            int color = length > MAX_RECOMMENDED_LENGTH ? TelegraphTheme.ERROR : TelegraphTheme.SUCCESS;
            String lengthText = length + " chars" + (length > MAX_RECOMMENDED_LENGTH ? " (recommended: \u226432)" : "");
            context.drawText(textRenderer, lengthText,
                    panelX + panelWidth - layout.padding - textRenderer.getWidth(lengthText),
                    layout.margin + 10, color, false);
        }

        // Preview
        int previewY = height - layout.margin - layout.buttonHeight - layout.spacing - 20;
        context.drawText(textRenderer, "Preview:", panelX + layout.padding, previewY, TelegraphTheme.TEXT_PRIMARY, false);
        String preview = getFormattedMessage();
        context.drawText(textRenderer, preview, panelX + layout.padding + 60, previewY, TelegraphTheme.SELECTED, false);

        if (settings.getProtocol() instanceof CarniteProtocol) {
            context.drawText(textRenderer, "Tip: Rename banner in anvil, place on map, right-click with map",
                    panelX + layout.padding, previewY - 15, TelegraphTheme.TEXT_SECONDARY, false);
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
