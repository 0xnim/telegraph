package xyz.nim.telegraph.client.carnite;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
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

    private EditBox messageField;
    private Button doneButton;
    private Button copyButton;
    private Button helpButton;
    private Button infoTabButton;
    private Button learnTabButton;
    private Button expandTabButton;
    private DropdownWidget colorDropdown;
    private DropdownWidget templateDropdown;
    private List<Button> symbolButtons = new ArrayList<>();

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
        super(Component.literal("Carnite Message Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = settings;
    }

    public CarniteComposerScreen(Screen parent, TelegraphChannel channel, int mapId, ChannelSettings settings, String initialMessage, String bannerColor) {
        super(Component.literal("Carnite Message Composer"));
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
        messageField = TextFields.message(font, layout.margin, inputY, leftPanelWidth, layout, 64);
        messageField.setHint(Component.literal("Type Carnite message..."));
        if (initialMessage != null) {
            messageField.setValue(initialMessage);
        }
        messageField.setResponder(text -> {
            validationResult = CarniteValidator.validate(text, selectedBannerColor);
            if (currentMode == ViewMode.LEARN || currentMode == ViewMode.EXPAND) {
                explanationResult = CarniteExplainer.explainMessage(text, selectedBannerColor);
            }
        });
        addRenderableWidget(messageField);

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
            Button btn = Button.builder(Component.literal(symbol), button -> {
                if (messageField != null) {
                    String current = messageField.getValue();
                    messageField.setValue(current + symbol);
                }
            }).bounds(symbolX, symbolGridY, symbolBtnWidth, 18).build();

            addRenderableWidget(btn);
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

        colorDropdown = new DropdownWidget(minecraft, rightPanelX, colorDropdownY, rightPanelWidth, layout.buttonHeight,
            colorOptions, selectedColor -> {
                selectedBannerColor = selectedColor;
                validationResult = CarniteValidator.validate(messageField.getValue(), selectedBannerColor);
                explanationResult = CarniteExplainer.explainMessage(messageField.getValue(), selectedBannerColor);
            });
        colorDropdown.setSelected(selectedBannerColor);
        addRenderableWidget(colorDropdown.getButton());

        int templateDropdownY = colorDropdownY + layout.buttonHeight + layout.spacing + 12;

        List<DropdownWidget.DropdownOption> templateOptions = new ArrayList<>();
        for (String[] template : TEMPLATES) {
            templateOptions.add(new DropdownWidget.DropdownOption(template[0], template[1]));
        }

        templateDropdown = new DropdownWidget(minecraft, rightPanelX, templateDropdownY, rightPanelWidth, layout.buttonHeight,
            templateOptions, selectedTemplate -> {
                if (messageField != null) {
                    messageField.setValue(selectedTemplate);
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
        addRenderableWidget(templateDropdown.getButton());

        int tabBtnWidth = Math.min(70, (leftPanelWidth - layout.spacing * 2) / 3);
        infoTabButton = Buttons.create(Component.literal("Info"),
            layout.margin, this.tabY, tabBtnWidth, layout, button -> {
                currentMode = ViewMode.INFO;
                updateTabButtons();
            });
        addRenderableWidget(infoTabButton);

        learnTabButton = Buttons.create(Component.literal("Learn"),
            layout.margin + tabBtnWidth + layout.spacing, this.tabY, tabBtnWidth, layout, button -> {
                currentMode = ViewMode.LEARN;
                explanationResult = CarniteExplainer.explainMessage(messageField.getValue(), selectedBannerColor);
                updateTabButtons();
            });
        addRenderableWidget(learnTabButton);

        expandTabButton = Buttons.create(Component.literal("Expand"),
            layout.margin + (tabBtnWidth + layout.spacing) * 2, this.tabY, tabBtnWidth, layout, button -> {
                currentMode = ViewMode.EXPAND;
                explanationResult = CarniteExplainer.explainMessage(messageField.getValue(), selectedBannerColor);
                updateTabButtons();
            });
        addRenderableWidget(expandTabButton);

        helpButton = Buttons.create(Component.literal("?"),
            layout.margin, this.bottomButtonY, 30, layout, button -> {
                showHelpSidebar = !showHelpSidebar;
            });
        addRenderableWidget(helpButton);

        copyButton = Buttons.create(Component.literal("Copy"),
            layout.margin + 35, this.bottomButtonY, 60, layout, button -> {
                copyMessageToClipboard();
            });
        addRenderableWidget(copyButton);

        doneButton = Buttons.done(width - layout.margin - layout.buttonWidth, this.bottomButtonY, layout, button -> onClose());
        addRenderableWidget(doneButton);

        updateTabButtons();
    }

    private void updateTabButtons() {
        if (infoTabButton != null) infoTabButton.active = currentMode != ViewMode.INFO;
        if (learnTabButton != null) learnTabButton.active = currentMode != ViewMode.LEARN;
        if (expandTabButton != null) expandTabButton.active = currentMode != ViewMode.EXPAND;
    }

    private String getFormattedMessage() {
        String message = messageField != null ? messageField.getValue() : "";
        return "[" + selectedBannerColor.toUpperCase() + " BANNER] " + message;
    }

    private void copyMessageToClipboard() {
        String message = getFormattedMessage();
        if (minecraft != null) {
            minecraft.keyboardHandler.setClipboard(message);
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
                explanationResult = CarniteExplainer.explainMessage(messageField.getValue(), selectedBannerColor);
                updateTabButtons();
                return true;
            }
            if (keyCode == KeyboardConstants.KEY_3) {
                currentMode = ViewMode.EXPAND;
                explanationResult = CarniteExplainer.explainMessage(messageField.getValue(), selectedBannerColor);
                updateTabButtons();
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderPanels(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int rightPanelX = width - layout.margin - rightPanelWidth;

        context.drawString(font, "Carnite Message Composer", layout.margin, layout.margin, TelegraphTheme.TEXT_PRIMARY, false);
        context.drawString(font, "Channel: " + channel.getDisplayName(mapId), layout.margin, layout.margin + 10, TelegraphTheme.TEXT_SECONDARY, false);

        context.drawString(font, "Banner Color:", rightPanelX, layout.margin + layout.headerHeight - 12, TelegraphTheme.TEXT_SECONDARY, false);
        context.drawString(font, "Templates:", rightPanelX, layout.margin + layout.headerHeight + layout.buttonHeight + layout.spacing, TelegraphTheme.TEXT_SECONDARY, false);

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
    protected void renderOverlays(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (messageField != null) {
            int length = messageField.getValue().length();
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
            context.drawString(font, stats, barX + barWidth + 10, statsY, textColor, false);
        }

        if (showHelpSidebar) {
            renderHelpSidebar(context);
        }

        renderSymbolTooltips(context, mouseX, mouseY);

        context.drawString(font, "Preview:", layout.margin, infoPreviewY, TelegraphTheme.TEXT_SECONDARY, false);
        String preview = getFormattedMessage();
        context.drawString(font, preview, layout.margin + 60, infoPreviewY, TelegraphTheme.SELECTED, false);

        if (templateDropdown != null) {
            templateDropdown.render(context, mouseX, mouseY, delta);
        }
        if (colorDropdown != null) {
            colorDropdown.render(context, mouseX, mouseY, delta);
        }

        if (height < 350) {
            context.drawCenteredString(font, "Screen too small - some features hidden",
                width / 2, height / 2, TelegraphTheme.ERROR);
        }
    }

    private void renderInfoTab(GuiGraphics context) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int panelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        context.fill(layout.margin, panelY, layout.margin + panelWidth, panelY + panelHeight, 0xE0000033);
        drawBorder(context, layout.margin, panelY, panelWidth, panelHeight, TelegraphTheme.INFO);

        int contentY = panelY + 10;
        int contentX = layout.margin + 10;

        context.drawString(font, "MESSAGE INFO", contentX, contentY, TelegraphTheme.INFO, false);
        contentY += 20;

        String message = messageField != null ? messageField.getValue() : "";

        if (message.isEmpty()) {
            context.drawString(font, "Type a message to see info...", contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
            return;
        }

        List<String> civs = CarniteParser.extractCivAbbreviations(message);
        if (!civs.isEmpty()) {
            context.drawString(font, "Found Civilizations:", contentX, contentY, TelegraphTheme.SELECTED, false);
            contentY += 15;

            for (String civ : civs) {
                context.drawString(font, "  - " + civ, contentX + 10, contentY, TelegraphTheme.INFO, false);
                contentY += 12;
            }
            contentY += 10;
        }

        if (CarniteParser.isTradeMessage(message)) {
            context.drawString(font, "Trade Offer Detected", contentX, contentY, TelegraphTheme.SELECTED, false);
            contentY += 15;

            CarniteParser.TradeOffer trade = CarniteParser.parseTradeOffer(message);
            if (trade != null) {
                context.drawString(font, "  Offering: " + trade.offering(), contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
                contentY += 12;
                context.drawString(font, "  Requesting: " + trade.requesting(), contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
                contentY += 12;
            }

            if (!selectedBannerColor.contains("yellow")) {
                contentY += 5;
                context.drawString(font, "  Tip: Use YELLOW banner for trades", contentX + 10, contentY, TelegraphTheme.WARNING, false);
                contentY += 12;
            }
            contentY += 10;
        }

        if (validationResult != null) {
            context.drawString(font, "Validation:", contentX, contentY, TelegraphTheme.SELECTED, false);
            contentY += 15;

            if (validationResult.isValid() && validationResult.issues().isEmpty()) {
                context.drawString(font, "  Message looks good!", contentX + 10, contentY, TelegraphTheme.SUCCESS, false);
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
                    context.drawString(font, prefix + line, contentX + 10, contentY, issueColor, false);
                    contentY += 11;
                    prefix = "    ";
                }
            }

            if (!validationResult.suggestions().isEmpty() && contentY < panelY + panelHeight - 40) {
                contentY += 10;
                context.drawString(font, "Suggestions:", contentX, contentY, TelegraphTheme.SELECTED, false);
                contentY += 15;

                for (CarniteValidator.ValidationSuggestion suggestion : validationResult.suggestions()) {
                    if (contentY > panelY + panelHeight - 20) break;

                    List<String> wrapped = wrapText(suggestion.suggestion(), panelWidth - 40);
                    for (String line : wrapped) {
                        if (contentY > panelY + panelHeight - 20) break;
                        context.drawString(font, "  - " + line, contentX + 10, contentY, TelegraphTheme.SUCCESS, false);
                        contentY += 11;
                    }
                }
            }
        }

        int statsY = panelY + panelHeight - 25;
        context.drawString(font, "Tense: " + CarniteParser.getTenseFromColor(selectedBannerColor),
                        contentX, statsY, TelegraphTheme.TEXT_MUTED, false);

        if (explanationResult != null) {
            String typeText = "Type: " + (explanationResult.structure().contains("QUESTION") ? "Question" :
                                        explanationResult.structure().contains("RESPONSE") ? "Response" :
                                        explanationResult.structure().contains("TRADE") ? "Trade" : "Statement");
            context.drawString(font, typeText,
                           contentX + 200, statsY, TelegraphTheme.TEXT_MUTED, false);
        }
    }

    private void renderLearningMode(GuiGraphics context, int mouseX, int mouseY) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int panelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        context.fill(layout.margin, panelY, layout.margin + panelWidth, panelY + panelHeight, 0xE0000000);
        drawBorder(context, layout.margin, panelY, panelWidth, panelHeight, TelegraphTheme.SUCCESS);

        int contentY = panelY + 5;
        int contentX = layout.margin + 5;

        context.drawString(font, "LEARNING MODE - Hover over any part", contentX, contentY, TelegraphTheme.SUCCESS, false);
        contentY += 15;

        int drawX = contentX;

        hoveredPartIndex = -1;

        for (int i = 0; i < explanationResult.parts().size(); i++) {
            CarniteExplainer.MessagePart part = explanationResult.parts().get(i);

            int textWidth = font.width(part.text());
            int color = getColorForPartType(part.type());

            boolean hovered = mouseX >= drawX && mouseX <= drawX + textWidth &&
                             mouseY >= contentY && mouseY <= contentY + 10;

            if (hovered) {
                hoveredPartIndex = i;
                context.fill(drawX - 1, contentY - 1, drawX + textWidth + 1, contentY + 10, 0x80FFFF00);
            }

            context.drawString(font, part.text(), drawX, contentY, color, false);
            drawX += textWidth + font.width(" ");
        }

        contentY += 20;

        context.drawString(font, "Structure:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 12;

        String[] structureLines = explanationResult.structure().split("\n");
        for (String line : structureLines) {
            if (contentY > panelY + panelHeight - 15) break;
            context.drawString(font, line, contentX + 5, contentY, TelegraphTheme.TEXT_SECONDARY, false);
            contentY += 10;
        }

        if (hoveredPartIndex >= 0 && hoveredPartIndex < explanationResult.parts().size()) {
            CarniteExplainer.MessagePart part = explanationResult.parts().get(hoveredPartIndex);

            int tooltipY = panelY + panelHeight - 40;
            context.fill(contentX, tooltipY, layout.margin + panelWidth - 5, panelY + panelHeight - 5, 0xFF1A1A1A);
            drawBorder(context, contentX, tooltipY, panelWidth - 10, 35, TelegraphTheme.SELECTED);

            context.drawString(font, "'" + part.text() + "' -> " + part.expanded(),
                           contentX + 5, tooltipY + 5, TelegraphTheme.INFO, false);
            context.drawString(font, part.explanation(),
                           contentX + 5, tooltipY + 17, TelegraphTheme.TEXT_MUTED, false);
        }

        int legendY = panelY + 5;
        int legendX = layout.margin + panelWidth - 180;
        context.drawString(font, "Color Legend:", legendX, legendY, TelegraphTheme.TEXT_MUTED, false);
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
            context.drawString(font, entry[0] + " " + entry[1], legendX, legendY, legendColor, false);
            legendY += 10;
        }
    }

    private void renderExpandedTranslation(GuiGraphics context) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int panelWidth = layout.contentWidth() - rightPanelWidth - layout.spacing;

        context.fill(layout.margin, panelY, layout.margin + panelWidth, panelY + panelHeight, 0xE0001100);
        drawBorder(context, layout.margin, panelY, panelWidth, panelHeight, TelegraphTheme.INFO);

        int contentY = panelY + 10;
        int contentX = layout.margin + 10;

        context.drawString(font, "EXPANDED TRANSLATION", contentX, contentY, TelegraphTheme.INFO, false);
        contentY += 20;

        context.drawString(font, "English:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 15;

        String translation = explanationResult.translation();
        List<String> wrappedTranslation = wrapText(translation, panelWidth - 30);
        for (String line : wrappedTranslation) {
            context.drawString(font, line, contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
            contentY += 12;
        }

        contentY += 10;

        context.drawString(font, "Word-by-Word Breakdown:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 15;

        for (CarniteExplainer.MessagePart part : explanationResult.parts()) {
            if (contentY > panelY + panelHeight - 15) break;

            if (!part.text().equals(part.expanded())) {
                String line = part.text() + " -> " + part.expanded();
                context.drawString(font, line, contentX + 10, contentY, TelegraphTheme.INFO, false);
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
            if (font.width(test) <= maxWidth) {
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

    private void renderHelpSidebar(GuiGraphics context) {
        int rightPanelWidth = Math.min(180, layout.contentWidth() / 4);
        int sidebarX = width - layout.margin - rightPanelWidth;
        int sidebarY = layout.margin + layout.headerHeight + layout.buttonHeight * 2 + layout.spacing * 3;
        int sidebarH = bottomButtonY - sidebarY - 10;

        context.fill(sidebarX, sidebarY, sidebarX + rightPanelWidth, sidebarY + sidebarH, 0xF0101010);
        drawBorder(context, sidebarX, sidebarY, rightPanelWidth, sidebarH, TelegraphTheme.INFO);

        int contentY = sidebarY + 8;
        int contentX = sidebarX + 8;

        context.drawString(font, "Quick Reference", contentX, contentY, TelegraphTheme.INFO, false);
        contentY += 14;

        context.drawString(font, "Word Order:", contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 10;
        context.drawString(font, "[What][Where]", contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
        contentY += 9;
        context.drawString(font, "[Who][Action]", contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
        contentY += 12;

        context.drawString(font, "Symbols:", contentX, contentY, TelegraphTheme.SELECTED, false);
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
            context.drawString(font, sym[0] + " " + sym[1], contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
            contentY += 9;
        }

        context.drawString(font, "[Esc] close", contentX, sidebarY + sidebarH - 12, TelegraphTheme.TEXT_MUTED, false);
    }

    private void renderSymbolTooltips(GuiGraphics context, int mouseX, int mouseY) {
        hoveredSymbolIndex = -1;

        Map<String, String> symbols = CarniteVocabulary.getSymbols();
        String[] symbolKeys = symbols.keySet().toArray(new String[0]);

        for (int i = 0; i < symbolButtons.size() && i < symbolKeys.length; i++) {
            Button btn = symbolButtons.get(i);
            if (mouseX >= btn.getX() && mouseX <= btn.getX() + btn.getWidth() &&
                mouseY >= btn.getY() && mouseY <= btn.getY() + btn.getHeight()) {

                hoveredSymbolIndex = i;
                String symbol = symbolKeys[i];
                String meaning = symbols.get(symbol);

                int tooltipX = mouseX + 10;
                int tooltipY = mouseY - 20;
                int tooltipWidth = font.width(meaning) + 10;

                if (tooltipX + tooltipWidth > width) {
                    tooltipX = width - tooltipWidth - 5;
                }

                context.fill(tooltipX - 2, tooltipY - 2, tooltipX + tooltipWidth, tooltipY + 12, 0xF0000000);
                drawBorder(context, tooltipX - 2, tooltipY - 2, tooltipWidth + 2, 14, TelegraphTheme.SELECTED);
                context.drawString(font, meaning, tooltipX + 3, tooltipY, TelegraphTheme.SELECTED, false);
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
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
