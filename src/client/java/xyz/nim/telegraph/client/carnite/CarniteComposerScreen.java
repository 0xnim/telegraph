package xyz.nim.telegraph.client.carnite;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.ui.DropdownWidget;
import xyz.nim.telegraph.client.ui.KeyboardConstants;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TextFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarniteComposerScreen extends TelegraphScreen {
    private final Screen parent;
    private final TelegraphChannel channel;
    private final int mapId;
    private final ChannelSettings settings;

    private TextFieldWidget messageField;
    private ButtonWidget doneButton;
    private ButtonWidget copyButton;
    private ButtonWidget helpButton;
    private ButtonWidget infoTabButton;
    private ButtonWidget learnTabButton;
    private ButtonWidget expandTabButton;
    private DropdownWidget colorDropdown;
    private DropdownWidget templateDropdown;
    private List<ButtonWidget> symbolButtons = new ArrayList<>();

    private static final String[][] BANNER_COLORS = {
        {"white", "White (Present)"},
        {"light_gray", "Lt.Gray (Past)"},
        {"gray", "Gray (Future)"},
        {"pink", "Pink (Might)"},
        {"red", "Red (URGENT)"},
        {"light_blue", "Lt.Blue (Request)"},
        {"black", "Black (Decided)"},
        {"blue", "Blue (Question)"},
        {"yellow", "Yellow (Trade)"},
        {"purple", "Purple (Goal)"}
    };

    private static final String[][] TEMPLATES = {
        {"~rd| ; ", "Raiders at my civ", "red"},
        {".dmd ; _:", "Trade: diamond for?", "yellow"},
        {".brd,32irn ; _:", "Trade: bread+iron for?", "yellow"},
        {"^ y", "Response: Yes", null},
        {"^ -acpt", "Response: Do not accept", null},
        {"_ :: ; atk", "Question: Who is attacking?", "blue"},
        {"bld| CN:", "Request: Builder to CN", "light_blue"},
        {"lib|5 _:", "Question: lvl 5 librarian?", "blue"}
    };

    private String selectedBannerColor = "white";
    private String initialMessage = null;
    private CarniteValidator.ValidationResult validationResult = null;
    private CarniteExplainer.ExplanationResult explanationResult = null;
    private boolean showHelpSidebar = false;
    private ViewMode currentMode = ViewMode.INFO;
    private int scrollOffset = 0;
    private int hoveredPartIndex = -1;
    private int hoveredSymbolIndex = -1;

    private int tabY;
    private int panelY;
    private int panelHeight;
    private int infoPreviewY;
    private int symbolGridY;
    private int bottomButtonY;

    private enum ViewMode {
        INFO,
        LEARN,
        EXPAND
    }

    public CarniteComposerScreen(Screen parent, TelegraphChannel channel, int mapId, ChannelSettings settings) {
        super(Text.literal("Carnite Message Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = settings;
    }

    public CarniteComposerScreen(Screen parent, TelegraphChannel channel, int mapId, ChannelSettings settings, String initialMessage, String bannerColor) {
        super(Text.literal("Carnite Message Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = settings;
        this.initialMessage = initialMessage;
        this.selectedBannerColor = bannerColor != null ? bannerColor : "white";
    }

    @Override
    protected void init() {
        super.init();
        symbolButtons.clear();

        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int leftPanelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        this.bottomButtonY = height - layout.margin - layout.buttonHeight;
        this.infoPreviewY = this.bottomButtonY - 15;

        int topSectionHeight = layout.margin + layout.headerHeight + layout.controlHeight + layout.spacing + 25;
        this.tabY = topSectionHeight;
        this.panelY = this.tabY + layout.buttonHeight + layout.spacing;
        this.panelHeight = Math.max(60, this.infoPreviewY - this.panelY - 25);

        int inputY = layout.margin + layout.headerHeight;
        messageField = TextFields.message(textRenderer, layout.margin, inputY, leftPanelWidth, layout, 64);
        messageField.setPlaceholder(Text.literal("Type Carnite message..."));
        if (initialMessage != null) {
            messageField.setText(initialMessage);
        }
        messageField.setChangedListener(text -> {
            validationResult = CarniteValidator.validate(text, selectedBannerColor);
            if (currentMode == ViewMode.LEARN || currentMode == ViewMode.EXPAND) {
                explanationResult = CarniteExplainer.explainMessage(text, selectedBannerColor);
            }
        });
        addDrawableChild(messageField);

        if (initialMessage != null) {
            validationResult = CarniteValidator.validate(initialMessage, selectedBannerColor);
            explanationResult = CarniteExplainer.explainMessage(initialMessage, selectedBannerColor);
        }

        this.symbolGridY = inputY + layout.controlHeight + layout.spacing;
        Map<String, String> symbols = CarniteVocabulary.getSymbols();
        int symbolBtnWidth = 28;
        int symbolSpacing = 3;
        int symbolX = layout.margin;
        int maxSymbols = Math.min(symbols.size(), (leftPanelWidth + symbolSpacing) / (symbolBtnWidth + symbolSpacing));

        int count = 0;
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            if (count >= maxSymbols) break;

            String symbol = entry.getKey();
            ButtonWidget btn = ButtonWidget.builder(Text.literal(symbol), button -> {
                if (messageField != null) {
                    String current = messageField.getText();
                    messageField.setText(current + symbol);
                }
            }).dimensions(symbolX, symbolGridY, symbolBtnWidth, 18).build();

            addDrawableChild(btn);
            symbolButtons.add(btn);
            symbolX += symbolBtnWidth + symbolSpacing;
            count++;
        }

        int rightPanelX = width - layout.margin - rightPanelWidth;
        int colorDropdownY = layout.margin + layout.headerHeight;

        List<DropdownWidget.DropdownOption> colorOptions = new ArrayList<>();
        for (String[] color : BANNER_COLORS) {
            colorOptions.add(new DropdownWidget.DropdownOption(color[0], color[1]));
        }

        colorDropdown = new DropdownWidget(client, rightPanelX, colorDropdownY, rightPanelWidth, layout.buttonHeight,
            colorOptions, selectedColor -> {
                selectedBannerColor = selectedColor;
                validationResult = CarniteValidator.validate(messageField.getText(), selectedBannerColor);
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            });
        colorDropdown.setSelected(selectedBannerColor);
        addDrawableChild(colorDropdown.getButton());

        int templateDropdownY = colorDropdownY + layout.buttonHeight + layout.spacing + 12;

        List<DropdownWidget.DropdownOption> templateOptions = new ArrayList<>();
        for (String[] template : TEMPLATES) {
            templateOptions.add(new DropdownWidget.DropdownOption(template[0], template[1]));
        }

        templateDropdown = new DropdownWidget(client, rightPanelX, templateDropdownY, rightPanelWidth, layout.buttonHeight,
            templateOptions, selectedTemplate -> {
                if (messageField != null) {
                    messageField.setText(selectedTemplate);
                }

                for (String[] template : TEMPLATES) {
                    if (template[0].equals(selectedTemplate)) {
                        String bannerColor = template[2];
                        if (bannerColor != null) {
                            selectedBannerColor = bannerColor;
                            colorDropdown.setSelected(bannerColor);
                        }
                        break;
                    }
                }

                validationResult = CarniteValidator.validate(selectedTemplate, selectedBannerColor);
                explanationResult = CarniteExplainer.explainMessage(selectedTemplate, selectedBannerColor);
            });
        addDrawableChild(templateDropdown.getButton());

        int tabBtnWidth = Math.min(70, (leftPanelWidth - layout.spacing * 2) / 3);
        infoTabButton = Buttons.create(Text.literal("Info"),
            layout.margin, this.tabY, tabBtnWidth, layout, button -> {
                currentMode = ViewMode.INFO;
                updateTabButtons();
            });
        addDrawableChild(infoTabButton);

        learnTabButton = Buttons.create(Text.literal("Learn"),
            layout.margin + tabBtnWidth + layout.spacing, this.tabY, tabBtnWidth, layout, button -> {
                currentMode = ViewMode.LEARN;
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
                updateTabButtons();
            });
        addDrawableChild(learnTabButton);

        expandTabButton = Buttons.create(Text.literal("Expand"),
            layout.margin + (tabBtnWidth + layout.spacing) * 2, this.tabY, tabBtnWidth, layout, button -> {
                currentMode = ViewMode.EXPAND;
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
                updateTabButtons();
            });
        addDrawableChild(expandTabButton);

        helpButton = Buttons.create(Text.literal("?"),
            layout.margin, this.bottomButtonY, 30, layout, button -> {
                showHelpSidebar = !showHelpSidebar;
            });
        addDrawableChild(helpButton);

        copyButton = Buttons.create(Text.literal("Copy"),
            layout.margin + 35, this.bottomButtonY, 60, layout, button -> {
                copyMessageToClipboard();
            });
        addDrawableChild(copyButton);

        doneButton = Buttons.done(width - layout.margin - layout.buttonWidth, this.bottomButtonY, layout, button -> close());
        addDrawableChild(doneButton);

        updateTabButtons();
    }

    private void updateTabButtons() {
        if (infoTabButton != null) infoTabButton.active = currentMode != ViewMode.INFO;
        if (learnTabButton != null) learnTabButton.active = currentMode != ViewMode.LEARN;
        if (expandTabButton != null) expandTabButton.active = currentMode != ViewMode.EXPAND;
    }

    private String getFormattedMessage() {
        String message = messageField != null ? messageField.getText() : "";
        return "[" + selectedBannerColor.toUpperCase() + " BANNER] " + message;
    }

    private void copyMessageToClipboard() {
        String message = getFormattedMessage();
        if (client != null) {
            client.keyboard.setClipboard(message);
            toastManager.success("Copied to clipboard!");
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KeyboardConstants.KEY_ESCAPE) {
            if (showHelpSidebar) {
                showHelpSidebar = false;
                return true;
            }
        }

        if (KeyboardConstants.hasControl(modifiers) && KeyboardConstants.isEnter(keyCode)) {
            copyMessageToClipboard();
            return true;
        }

        if (!messageField.isFocused()) {
            if (keyCode == KeyboardConstants.KEY_1) {
                currentMode = ViewMode.INFO;
                updateTabButtons();
                return true;
            }
            if (keyCode == KeyboardConstants.KEY_2) {
                currentMode = ViewMode.LEARN;
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
                updateTabButtons();
                return true;
            }
            if (keyCode == KeyboardConstants.KEY_3) {
                currentMode = ViewMode.EXPAND;
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
                updateTabButtons();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderPanels(DrawContext context, int mouseX, int mouseY, float delta) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int rightPanelX = width - layout.margin - rightPanelWidth;

        context.drawText(textRenderer, "Carnite Message Composer", layout.margin, layout.margin, TelegraphTheme.TEXT_PRIMARY, false);
        context.drawText(textRenderer, "Channel: " + channel.getDisplayName(mapId), layout.margin, layout.margin + 10, TelegraphTheme.TEXT_SECONDARY, false);

        context.drawText(textRenderer, "Banner Color:", rightPanelX, layout.margin + layout.headerHeight - 12, TelegraphTheme.TEXT_SECONDARY, false);
        context.drawText(textRenderer, "Templates:", rightPanelX, layout.margin + layout.headerHeight + layout.buttonHeight + layout.spacing, TelegraphTheme.TEXT_SECONDARY, false);

        switch (currentMode) {
            case INFO -> renderInfoTab(context);
            case LEARN -> {
                if (explanationResult != null && !explanationResult.parts().isEmpty()) {
                    renderLearningMode(context, mouseX, mouseY);
                }
            }
            case EXPAND -> {
                if (explanationResult != null) {
                    renderExpandedTranslation(context);
                }
            }
        }
    }

    @Override
    protected void renderOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        if (messageField != null) {
            int length = messageField.getText().length();
            int statsY = panelY + panelHeight + 5;

            int barWidth = 150;
            int barHeight = 8;
            int barX = layout.margin;

            context.fill(barX, statsY, barX + barWidth, statsY + barHeight, 0xFF333333);

            int progressWidth = Math.min(barWidth, (int) (barWidth * length / 38.0));
            int barColor = length > 38 ? TelegraphTheme.ERROR : length > 32 ? TelegraphTheme.WARNING : TelegraphTheme.SUCCESS;
            if (progressWidth > 0) {
                context.fill(barX, statsY, barX + progressWidth, statsY + barHeight, barColor);
            }

            int greenMark = (int) (barWidth * 32 / 38.0);
            context.fill(barX + greenMark - 1, statsY - 2, barX + greenMark + 1, statsY + barHeight + 2, TelegraphTheme.SELECTED);

            drawBorder(context, barX, statsY, barWidth, barHeight, TelegraphTheme.TEXT_SECONDARY);

            String stats = length + "/38" + (length > 32 ? " !" : "");
            int textColor = length > 38 ? TelegraphTheme.ERROR : length > 32 ? TelegraphTheme.WARNING : TelegraphTheme.SUCCESS;
            context.drawText(textRenderer, stats, barX + barWidth + 10, statsY, textColor, false);
        }

        if (showHelpSidebar) {
            renderHelpSidebar(context);
        }

        renderSymbolTooltips(context, mouseX, mouseY);

        context.drawText(textRenderer, "Preview:", layout.margin, infoPreviewY, TelegraphTheme.TEXT_SECONDARY, false);
        String preview = getFormattedMessage();
        context.drawText(textRenderer, preview, layout.margin + 60, infoPreviewY, TelegraphTheme.SELECTED, false);

        if (templateDropdown != null) {
            templateDropdown.render(context, mouseX, mouseY, delta);
        }
        if (colorDropdown != null) {
            colorDropdown.render(context, mouseX, mouseY, delta);
        }

        if (height < 350) {
            context.drawCenteredTextWithShadow(textRenderer, "Screen too small - some features hidden",
                width / 2, height / 2, TelegraphTheme.ERROR);
        }
    }

    private void renderInfoTab(DrawContext context) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int panelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        context.fill(layout.margin, panelY, layout.margin + panelWidth, panelY + panelHeight, 0xE0000033);
        drawBorder(context, layout.margin, panelY, panelWidth, panelHeight, TelegraphTheme.INFO);

        int contentY = panelY + 10;
        int contentX = layout.margin + 10;

        context.drawText(textRenderer, "MESSAGE INFO", contentX, contentY, TelegraphTheme.INFO, false);
        contentY += 20;

        String message = messageField != null ? messageField.getText() : "";

        if (message.isEmpty()) {
            context.drawText(textRenderer, "Type a message to see info...", contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
            return;
        }

        List<String> civs = CarniteParser.extractCivAbbreviations(message);
        if (!civs.isEmpty()) {
            context.drawText(textRenderer, "Found Civilizations:", contentX, contentY, TelegraphTheme.SELECTED, false);
            contentY += 15;

            for (String civ : civs) {
                context.drawText(textRenderer, "  - " + civ, contentX + 10, contentY, TelegraphTheme.INFO, false);
                contentY += 12;
            }
            contentY += 10;
        }

        if (CarniteParser.isTradeMessage(message)) {
            context.drawText(textRenderer, "Trade Offer Detected", contentX, contentY, TelegraphTheme.SELECTED, false);
            contentY += 15;

            CarniteParser.TradeOffer trade = CarniteParser.parseTradeOffer(message);
            if (trade != null) {
                context.drawText(textRenderer, "  Offering: " + trade.offering(), contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
                contentY += 12;
                context.drawText(textRenderer, "  Requesting: " + trade.requesting(), contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
                contentY += 12;
            }

            if (!selectedBannerColor.contains("yellow")) {
                contentY += 5;
                context.drawText(textRenderer, "  Tip: Use YELLOW banner for trades", contentX + 10, contentY, TelegraphTheme.WARNING, false);
                contentY += 12;
            }
            contentY += 10;
        }

        if (validationResult != null) {
            context.drawText(textRenderer, "Validation:", contentX, contentY, TelegraphTheme.SELECTED, false);
            contentY += 15;

            if (validationResult.isValid() && validationResult.issues().isEmpty()) {
                context.drawText(textRenderer, "  Message looks good!", contentX + 10, contentY, TelegraphTheme.SUCCESS, false);
                contentY += 12;
            }

            for (CarniteValidator.ValidationIssue issue : validationResult.issues()) {
                if (contentY > panelY + panelHeight - 20) break;

                int issueColor = switch (issue.severity()) {
                    case ERROR -> TelegraphTheme.ERROR;
                    case WARNING -> TelegraphTheme.WARNING;
                    case INFO -> TelegraphTheme.INFO;
                };

                String prefix = switch (issue.severity()) {
                    case ERROR -> "  ! ";
                    case WARNING -> "  ! ";
                    case INFO -> "  i ";
                };

                List<String> wrapped = wrapText(issue.message(), panelWidth - 40);
                for (String line : wrapped) {
                    if (contentY > panelY + panelHeight - 20) break;
                    context.drawText(textRenderer, prefix + line, contentX + 10, contentY, issueColor, false);
                    contentY += 11;
                    prefix = "    ";
                }
            }

            if (!validationResult.suggestions().isEmpty() && contentY < panelY + panelHeight - 40) {
                contentY += 10;
                context.drawText(textRenderer, "Suggestions:", contentX, contentY, TelegraphTheme.SELECTED, false);
                contentY += 15;

                for (CarniteValidator.ValidationSuggestion suggestion : validationResult.suggestions()) {
                    if (contentY > panelY + panelHeight - 20) break;

                    List<String> wrapped = wrapText(suggestion.suggestion(), panelWidth - 40);
                    for (String line : wrapped) {
                        if (contentY > panelY + panelHeight - 20) break;
                        context.drawText(textRenderer, "  - " + line, contentX + 10, contentY, TelegraphTheme.SUCCESS, false);
                        contentY += 11;
                    }
                }
            }
        }

        int statsY = panelY + panelHeight - 25;
        context.drawText(textRenderer, "Tense: " + CarniteParser.getTenseFromColor(selectedBannerColor),
                        contentX, statsY, TelegraphTheme.TEXT_MUTED, false);

        if (explanationResult != null) {
            String typeText = "Type: " + (explanationResult.structure().contains("QUESTION") ? "Question" :
                                        explanationResult.structure().contains("RESPONSE") ? "Response" :
                                        explanationResult.structure().contains("TRADE") ? "Trade" : "Statement");
            context.drawText(textRenderer, typeText,
                           contentX + 200, statsY, TelegraphTheme.TEXT_MUTED, false);
        }
    }

    private void renderLearningMode(DrawContext context, int mouseX, int mouseY) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int panelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        context.fill(layout.margin, panelY, layout.margin + panelWidth, panelY + panelHeight, 0xE0000000);
        drawBorder(context, layout.margin, panelY, panelWidth, panelHeight, TelegraphTheme.SUCCESS);

        int contentY = panelY + 5;
        int contentX = layout.margin + 5;

        context.drawText(textRenderer, "LEARNING MODE - Hover over any part", contentX, contentY, TelegraphTheme.SUCCESS, false);
        contentY += 15;

        int drawX = contentX;

        hoveredPartIndex = -1;

        for (int i = 0; i < explanationResult.parts().size(); i++) {
            CarniteExplainer.MessagePart part = explanationResult.parts().get(i);

            int textWidth = textRenderer.getWidth(part.text());
            int color = getColorForPartType(part.type());

            boolean hovered = mouseX >= drawX && mouseX <= drawX + textWidth &&
                             mouseY >= contentY && mouseY <= contentY + 10;

            if (hovered) {
                hoveredPartIndex = i;
                context.fill(drawX - 1, contentY - 1, drawX + textWidth + 1, contentY + 10, 0x80FFFF00);
            }

            context.drawText(textRenderer, part.text(), drawX, contentY, color, false);
            drawX += textWidth + textRenderer.getWidth(" ");
        }

        contentY += 20;

        context.drawText(textRenderer, "Structure:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 12;

        String[] structureLines = explanationResult.structure().split("\n");
        for (String line : structureLines) {
            if (contentY > panelY + panelHeight - 15) break;
            context.drawText(textRenderer, line, contentX + 5, contentY, TelegraphTheme.TEXT_SECONDARY, false);
            contentY += 10;
        }

        if (hoveredPartIndex >= 0 && hoveredPartIndex < explanationResult.parts().size()) {
            CarniteExplainer.MessagePart part = explanationResult.parts().get(hoveredPartIndex);

            int tooltipY = panelY + panelHeight - 40;
            context.fill(contentX, tooltipY, layout.margin + panelWidth - 5, panelY + panelHeight - 5, 0xFF1A1A1A);
            drawBorder(context, contentX, tooltipY, panelWidth - 10, 35, TelegraphTheme.SELECTED);

            context.drawText(textRenderer, "'" + part.text() + "' -> " + part.expanded(),
                           contentX + 5, tooltipY + 5, TelegraphTheme.INFO, false);
            context.drawText(textRenderer, part.explanation(),
                           contentX + 5, tooltipY + 17, TelegraphTheme.TEXT_MUTED, false);
        }

        int legendY = panelY + 5;
        int legendX = layout.margin + panelWidth - 180;
        context.drawText(textRenderer, "Color Legend:", legendX, legendY, TelegraphTheme.TEXT_MUTED, false);
        legendY += 12;

        String[][] legend = {
            {"Entity", "(civ/player)", "0xFF55FFFF"},
            {"Quantity", "(numbers)", "0xFF55FF55"},
            {"Abbreviation", "", "0xFFFFFF00"},
            {"Modifier", "(~/-)", "0xFFAA55FF"},
            {"Connector", "(,/&)", "0xFFFFAA00"}
        };

        for (String[] entry : legend) {
            int legendColor = Integer.parseUnsignedInt(entry[2].substring(2), 16);
            context.drawText(textRenderer, entry[0] + " " + entry[1], legendX, legendY, legendColor, false);
            legendY += 10;
        }
    }

    private void renderExpandedTranslation(DrawContext context) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int panelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        context.fill(layout.margin, panelY, layout.margin + panelWidth, panelY + panelHeight, 0xE0001100);
        drawBorder(context, layout.margin, panelY, panelWidth, panelHeight, TelegraphTheme.INFO);

        int contentY = panelY + 10;
        int contentX = layout.margin + 10;

        context.drawText(textRenderer, "EXPANDED TRANSLATION", contentX, contentY, TelegraphTheme.INFO, false);
        contentY += 20;

        context.drawText(textRenderer, "English:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 15;

        String translation = explanationResult.translation();
        List<String> wrappedTranslation = wrapText(translation, panelWidth - 30);
        for (String line : wrappedTranslation) {
            context.drawText(textRenderer, line, contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
            contentY += 12;
        }

        contentY += 10;

        context.drawText(textRenderer, "Word-by-Word Breakdown:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 15;

        for (CarniteExplainer.MessagePart part : explanationResult.parts()) {
            if (contentY > panelY + panelHeight - 15) break;

            if (!part.text().equals(part.expanded())) {
                String line = part.text() + " -> " + part.expanded();
                context.drawText(textRenderer, line, contentX + 10, contentY, TelegraphTheme.INFO, false);
                contentY += 11;
            }
        }
    }

    private int getColorForPartType(CarniteExplainer.MessagePartType type) {
        return switch (type) {
            case ENTITY -> 0xFF55FFFF;
            case CONNECTOR -> 0xFFFFAA00;
            case QUANTITY -> 0xFF55FF55;
            case QUESTION -> 0xFFFF55FF;
            case RESPONSE -> 0xFFFF88FF;
            case MODIFIER -> 0xFFAA55FF;
            case QUOTE -> 0xFFFFFF55;
            case ABBREVIATION -> 0xFFFFFF00;
            case WORD -> 0xFFFFFFFF;
            case SYMBOL -> 0xFFAAAAAA;
        };
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            if (textRenderer.getWidth(test) <= maxWidth) {
                if (current.length() > 0) current.append(" ");
                current.append(word);
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private void renderHelpSidebar(DrawContext context) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int sidebarX = width - layout.margin - rightPanelWidth;
        int sidebarY = layout.margin + layout.headerHeight + layout.buttonHeight * 2 + layout.spacing * 3;
        int sidebarH = bottomButtonY - sidebarY - 10;

        context.fill(sidebarX, sidebarY, sidebarX + rightPanelWidth, sidebarY + sidebarH, 0xF0101010);
        drawBorder(context, sidebarX, sidebarY, rightPanelWidth, sidebarH, TelegraphTheme.INFO);

        int contentY = sidebarY + 8;
        int contentX = sidebarX + 8;

        context.drawText(textRenderer, "Quick Reference", contentX, contentY, TelegraphTheme.INFO, false);
        contentY += 14;

        context.drawText(textRenderer, "Word Order:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 10;
        context.drawText(textRenderer, "[What][Where]", contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
        contentY += 9;
        context.drawText(textRenderer, "[Who][Action]", contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
        contentY += 12;

        context.drawText(textRenderer, "Symbols:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 10;

        String[][] symbols = {
            {"|", "Agent/Player"},
            {":", "Your civ"},
            {";", "My civ"},
            {",", "Property of"},
            {".", "Stack (64)"},
            {"_", "Question"},
            {"^", "Response"},
            {"~", "Plural"},
            {"-", "Negation"}
        };

        for (String[] sym : symbols) {
            if (contentY > sidebarY + sidebarH - 30) break;
            context.drawText(textRenderer, sym[0] + " " + sym[1], contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
            contentY += 9;
        }

        context.drawText(textRenderer, "[Esc] close", contentX, sidebarY + sidebarH - 12, TelegraphTheme.TEXT_MUTED, false);
    }

    private void renderSymbolTooltips(DrawContext context, int mouseX, int mouseY) {
        hoveredSymbolIndex = -1;

        Map<String, String> symbols = CarniteVocabulary.getSymbols();
        String[] symbolKeys = symbols.keySet().toArray(new String[0]);

        for (int i = 0; i < symbolButtons.size() && i < symbolKeys.length; i++) {
            ButtonWidget btn = symbolButtons.get(i);
            if (mouseX >= btn.getX() && mouseX <= btn.getX() + btn.getWidth() &&
                mouseY >= btn.getY() && mouseY <= btn.getY() + btn.getHeight()) {

                hoveredSymbolIndex = i;
                String symbol = symbolKeys[i];
                String meaning = symbols.get(symbol);

                int tooltipX = mouseX + 10;
                int tooltipY = mouseY - 20;
                int tooltipWidth = textRenderer.getWidth(meaning) + 10;

                if (tooltipX + tooltipWidth > width) {
                    tooltipX = width - tooltipWidth - 5;
                }

                context.fill(tooltipX - 2, tooltipY - 2, tooltipX + tooltipWidth, tooltipY + 12, 0xF0000000);
                drawBorder(context, tooltipX - 2, tooltipY - 2, tooltipWidth + 2, 14, TelegraphTheme.SELECTED);
                context.drawText(textRenderer, meaning, tooltipX + 3, tooltipY, TelegraphTheme.SELECTED, false);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (templateDropdown != null && templateDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (colorDropdown != null && colorDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (templateDropdown != null && templateDropdown.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if (colorDropdown != null && colorDropdown.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        scrollOffset += (int) (verticalAmount * 10);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
}
