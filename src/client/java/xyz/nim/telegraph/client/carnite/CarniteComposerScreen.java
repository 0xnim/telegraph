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
import xyz.nim.telegraph.client.ui.SimpleLayout;
import xyz.nim.telegraph.client.ui.SimpleLayout.Box;
import xyz.nim.telegraph.client.ui.ToastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CarniteComposerScreen extends Screen {
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
    
    private int selectedColorIndex = 0;
    private int selectedTemplateIndex = 0;
    
    private String selectedBannerColor = "white";
    private String initialMessage = null;
    private CarniteValidator.ValidationResult validationResult = null;
    private CarniteExplainer.ExplanationResult explanationResult = null;
    private boolean showHelpSidebar = false;
    private ViewMode currentMode = ViewMode.INFO;
    private int scrollOffset = 0;
    private int hoveredPartIndex = -1;
    private int hoveredSymbolIndex = -1;

    private final ToastManager toastManager = new ToastManager();
    private static final int HELP_SIDEBAR_WIDTH = 200;
    
    private int tabY;
    private int panelY;
    private int panelHeight;
    private int infoPreviewY;
    private int symbolGridY;
    private int bottomButtonY;
    
    private enum ViewMode {
        INFO,     // Default - civs, validation, info
        LEARN,    // Interactive learning
        EXPAND    // Translation
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

        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int leftPanelWidth = width - margin - rightPanelWidth - margin - 10;

        // Fixed layout zones (from bottom up)
        this.bottomButtonY = height - 30;
        this.infoPreviewY = this.bottomButtonY - 15;

        // Tab panel gets fixed height based on remaining space
        int topSectionHeight = 95; // Title + message field + symbols area
        this.tabY = topSectionHeight;
        this.panelY = this.tabY + 25;
        // Panel must end before the preview area (leave room for progress bar too)
        this.panelHeight = Math.max(60, this.infoPreviewY - this.panelY - 25);

        // Message input at top
        int inputY = 45;
        messageField = new TextFieldWidget(textRenderer, margin, inputY, leftPanelWidth, 20, Text.literal("Message"));
        messageField.setMaxLength(64);
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

        // Symbol buttons - single row below message field
        this.symbolGridY = inputY + 28;
        Map<String, String> symbols = CarniteVocabulary.getSymbols();
        int symbolBtnWidth = 28;
        int symbolSpacing = 3;
        int symbolX = margin;
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

        // Right panel - Banner color dropdown (label drawn in render)
        int rightPanelX = width - margin - rightPanelWidth;
        int colorDropdownY = 45;

        List<DropdownWidget.DropdownOption> colorOptions = new ArrayList<>();
        for (String[] color : BANNER_COLORS) {
            colorOptions.add(new DropdownWidget.DropdownOption(color[0], color[1]));
        }

        colorDropdown = new DropdownWidget(client, rightPanelX, colorDropdownY, rightPanelWidth, 20,
            colorOptions, selectedColor -> {
                selectedBannerColor = selectedColor;
                validationResult = CarniteValidator.validate(messageField.getText(), selectedBannerColor);
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            });
        colorDropdown.setSelected(selectedBannerColor);
        addDrawableChild(colorDropdown.getButton());

        // Template dropdown below color dropdown (label drawn in render)
        int templateDropdownY = colorDropdownY + 35;

        List<DropdownWidget.DropdownOption> templateOptions = new ArrayList<>();
        for (String[] template : TEMPLATES) {
            templateOptions.add(new DropdownWidget.DropdownOption(template[0], template[1]));
        }

        templateDropdown = new DropdownWidget(client, rightPanelX, templateDropdownY, rightPanelWidth, 20,
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

        // Tab buttons
        int tabBtnWidth = 70;
        infoTabButton = ButtonWidget.builder(Text.literal("Info"), button -> {
            currentMode = ViewMode.INFO;
            updateTabButtons();
        }).dimensions(margin, this.tabY, tabBtnWidth, 20).build();
        addDrawableChild(infoTabButton);

        learnTabButton = ButtonWidget.builder(Text.literal("Learn"), button -> {
            currentMode = ViewMode.LEARN;
            explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            updateTabButtons();
        }).dimensions(margin + tabBtnWidth + 5, this.tabY, tabBtnWidth, 20).build();
        addDrawableChild(learnTabButton);

        expandTabButton = ButtonWidget.builder(Text.literal("Expand"), button -> {
            currentMode = ViewMode.EXPAND;
            explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            updateTabButtons();
        }).dimensions(margin + (tabBtnWidth + 5) * 2, this.tabY, tabBtnWidth, 20).build();
        addDrawableChild(expandTabButton);

        // Bottom buttons
        helpButton = ButtonWidget.builder(Text.literal("?"), button -> {
            showHelpSidebar = !showHelpSidebar;
        }).dimensions(margin, this.bottomButtonY, 30, 20).build();
        addDrawableChild(helpButton);

        copyButton = ButtonWidget.builder(Text.literal("Copy"), button -> {
            copyMessageToClipboard();
        }).dimensions(margin + 35, this.bottomButtonY, 60, 20).build();
        addDrawableChild(copyButton);

        int doneButtonWidth = 70;
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(width - margin - doneButtonWidth, this.bottomButtonY, doneButtonWidth, 20).build();
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
            close();
            return true;
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int rightPanelX = width - margin - rightPanelWidth;
        
        // Title
        context.drawText(textRenderer, "Carnite Message Composer", margin, 20, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "Channel: " + channel.getDisplayName(mapId), margin, 30, 0xFFAAAAAA, false);

        // Right panel labels (positioned above dropdowns)
        context.drawText(textRenderer, "Banner Color:", rightPanelX, 33, 0xFFAAAAAA, false);
        context.drawText(textRenderer, "Templates:", rightPanelX, 68, 0xFFAAAAAA, false);

        // Message stats with progress bar (positioned in the gap between panel and preview)
        if (messageField != null) {
            int length = messageField.getText().length();
            int statsY = panelY + panelHeight + 5;

            int barWidth = 150;
            int barHeight = 8;
            int barX = margin;

            context.fill(barX, statsY, barX + barWidth, statsY + barHeight, 0xFF333333);

            int progressWidth = Math.min(barWidth, (int) (barWidth * length / 38.0));
            int barColor = length > 38 ? 0xFFFF0000 : length > 32 ? 0xFFFFAA00 : 0xFF55FF55;
            if (progressWidth > 0) {
                context.fill(barX, statsY, barX + progressWidth, statsY + barHeight, barColor);
            }

            int greenMark = (int) (barWidth * 32 / 38.0);
            context.fill(barX + greenMark - 1, statsY - 2, barX + greenMark + 1, statsY + barHeight + 2, 0xFFFFFF00);

            context.drawBorder(barX, statsY, barWidth, barHeight, 0xFFAAAAAA);

            String stats = length + "/38" + (length > 32 ? " ⚠" : "");
            int textColor = length > 38 ? 0xFFFF0000 : length > 32 ? 0xFFFFAA00 : 0xFF55FF55;
            context.drawText(textRenderer, stats, barX + barWidth + 10, statsY, textColor, false);
        }
        
        // Render current tab content
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
        
        // Help sidebar
        if (showHelpSidebar) {
            renderHelpSidebar(context);
        }

        // Symbol tooltips
        renderSymbolTooltips(context, mouseX, mouseY);

        // Toast notifications
        toastManager.render(context, textRenderer, width, height);
        
        // Preview
        context.drawText(textRenderer, "Preview:", margin, infoPreviewY, 0xFFAAAAAA, false);
        String preview = getFormattedMessage();
        context.drawText(textRenderer, preview, margin + 60, infoPreviewY, 0xFFFFFF00, false);
        
        // Render dropdowns last so they appear on top (only if they exist)
        if (templateDropdown != null) {
            templateDropdown.render(context, mouseX, mouseY, delta);
        }
        if (colorDropdown != null) {
            colorDropdown.render(context, mouseX, mouseY, delta);
        }
        
        // Show warning if screen too small
        if (height < 350) {
            context.drawCenteredTextWithShadow(textRenderer, "§cScreen too small - some features hidden", 
                width / 2, height / 2, 0xFFFF5555);
        }
    }
    
    private void renderInfoTab(DrawContext context) {
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int panelWidth = width - margin - rightPanelWidth - margin - 10;
        
        // Background panel
        context.fill(margin, panelY, margin + panelWidth, panelY + panelHeight, 0xE0000033);
        context.drawBorder(margin, panelY, panelWidth, panelHeight, 0xFF00AAFF);
        
        int contentY = panelY + 10;
        int contentX = margin + 10;
        
        context.drawText(textRenderer, "§b§lMESSAGE INFO", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 20;
        
        String message = messageField != null ? messageField.getText() : "";
        
        if (message.isEmpty()) {
            context.drawText(textRenderer, "§7Type a message to see info...", contentX, contentY, 0xFFAAAAAA, false);
            return;
        }
        
        // Found Civilizations
        List<String> civs = CarniteParser.extractCivAbbreviations(message);
        if (!civs.isEmpty()) {
            context.drawText(textRenderer, "§e📍 Found Civilizations:", contentX, contentY, 0xFFFFFFFF, false);
            contentY += 15;
            
            for (String civ : civs) {
                context.drawText(textRenderer, "  §b• " + civ, contentX + 10, contentY, 0xFFFFFFFF, false);
                contentY += 12;
            }
            contentY += 10;
        }
        
        // Trade detection
        if (CarniteParser.isTradeMessage(message)) {
            context.drawText(textRenderer, "§e💰 Trade Offer Detected", contentX, contentY, 0xFFFFFFFF, false);
            contentY += 15;
            
            CarniteParser.TradeOffer trade = CarniteParser.parseTradeOffer(message);
            if (trade != null) {
                context.drawText(textRenderer, "  §7Offering: §f" + trade.offering(), contentX + 10, contentY, 0xFFFFFFFF, false);
                contentY += 12;
                context.drawText(textRenderer, "  §7Requesting: §f" + trade.requesting(), contentX + 10, contentY, 0xFFFFFFFF, false);
                contentY += 12;
            }
            
            if (!selectedBannerColor.contains("yellow")) {
                contentY += 5;
                context.drawText(textRenderer, "  §6⚠ Tip: Use YELLOW banner for trades", contentX + 10, contentY, 0xFFFFAA00, false);
                contentY += 12;
            }
            contentY += 10;
        }
        
        // Validation Results
        if (validationResult != null) {
            context.drawText(textRenderer, "§e✓ Validation:", contentX, contentY, 0xFFFFFFFF, false);
            contentY += 15;
            
            if (validationResult.isValid() && validationResult.issues().isEmpty()) {
                context.drawText(textRenderer, "  §a✓ Message looks good!", contentX + 10, contentY, 0xFF55FF55, false);
                contentY += 12;
            }
            
            for (CarniteValidator.ValidationIssue issue : validationResult.issues()) {
                if (contentY > panelY + panelHeight - 20) break;
                
                int issueColor = switch (issue.severity()) {
                    case ERROR -> 0xFFFF0000;
                    case WARNING -> 0xFFFFAA00;
                    case INFO -> 0xFF00AAFF;
                };
                
                String prefix = switch (issue.severity()) {
                    case ERROR -> "  ⚠ ";
                    case WARNING -> "  ⚠ ";
                    case INFO -> "  ℹ ";
                };
                
                List<String> wrapped = wrapText(issue.message(), panelWidth - 40);
                for (String line : wrapped) {
                    if (contentY > panelY + panelHeight - 20) break;
                    context.drawText(textRenderer, prefix + line, contentX + 10, contentY, issueColor, false);
                    contentY += 11;
                    prefix = "    "; // Indent continuation lines
                }
            }
            
            if (!validationResult.suggestions().isEmpty() && contentY < panelY + panelHeight - 40) {
                contentY += 10;
                context.drawText(textRenderer, "§e💡 Suggestions:", contentX, contentY, 0xFFFFFFFF, false);
                contentY += 15;
                
                for (CarniteValidator.ValidationSuggestion suggestion : validationResult.suggestions()) {
                    if (contentY > panelY + panelHeight - 20) break;
                    
                    List<String> wrapped = wrapText(suggestion.suggestion(), panelWidth - 40);
                    for (String line : wrapped) {
                        if (contentY > panelY + panelHeight - 20) break;
                        context.drawText(textRenderer, "  • " + line, contentX + 10, contentY, 0xFF88FF88, false);
                        contentY += 11;
                    }
                }
            }
        }
        
        // Quick stats at bottom
        int statsY = panelY + panelHeight - 25;
        context.drawText(textRenderer, "§8Tense: " + CarniteParser.getTenseFromColor(selectedBannerColor), 
                        contentX, statsY, 0xFF888888, false);
        
        if (explanationResult != null) {
            String typeText = "Type: " + (explanationResult.structure().contains("QUESTION") ? "Question" : 
                                        explanationResult.structure().contains("RESPONSE") ? "Response" :
                                        explanationResult.structure().contains("TRADE") ? "Trade" : "Statement");
            context.drawText(textRenderer, "§8" + typeText, 
                           contentX + 200, statsY, 0xFF888888, false);
        }
    }
    
    private void renderLearningMode(DrawContext context, int mouseX, int mouseY) {
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int panelWidth = width - margin - rightPanelWidth - margin - 10;
        
        // Background panel
        context.fill(margin, panelY, margin + panelWidth, panelY + panelHeight, 0xE0000000);
        context.drawBorder(margin, panelY, panelWidth, panelHeight, 0xFF00FF00);
        
        int contentY = panelY + 5;
        int contentX = margin + 5;
        
        context.drawText(textRenderer, "§a§lLEARNING MODE - Hover over any part", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 15;
        
        // Draw message with interactive parts
        String message = messageField.getText();
        int drawX = contentX;
        
        hoveredPartIndex = -1;
        
        for (int i = 0; i < explanationResult.parts().size(); i++) {
            CarniteExplainer.MessagePart part = explanationResult.parts().get(i);
            
            int textWidth = textRenderer.getWidth(part.text());
            int color = getColorForPartType(part.type());
            
            // Check if mouse is hovering
            boolean hovered = mouseX >= drawX && mouseX <= drawX + textWidth && 
                             mouseY >= contentY && mouseY <= contentY + 10;
            
            if (hovered) {
                hoveredPartIndex = i;
                // Highlight background
                context.fill(drawX - 1, contentY - 1, drawX + textWidth + 1, contentY + 10, 0x80FFFF00);
            }
            
            context.drawText(textRenderer, part.text(), drawX, contentY, color, false);
            drawX += textWidth + textRenderer.getWidth(" ");
        }
        
        contentY += 20;
        
        // Show structure
        context.drawText(textRenderer, "§eStructure:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        String[] structureLines = explanationResult.structure().split("\n");
        for (String line : structureLines) {
            if (contentY > panelY + panelHeight - 15) break;
            context.drawText(textRenderer, line, contentX + 5, contentY, 0xFFCCCCCC, false);
            contentY += 10;
        }
        
        // Show hovered part explanation
        if (hoveredPartIndex >= 0 && hoveredPartIndex < explanationResult.parts().size()) {
            CarniteExplainer.MessagePart part = explanationResult.parts().get(hoveredPartIndex);
            
            int tooltipY = panelY + panelHeight - 40;
            context.fill(contentX, tooltipY, margin + panelWidth - 5, panelY + panelHeight - 5, 0xFF1A1A1A);
            context.drawBorder(contentX, tooltipY, panelWidth - 10, 35, 0xFFFFFF00);
            
            context.drawText(textRenderer, "§e'" + part.text() + "' §f→ §b" + part.expanded(), 
                           contentX + 5, tooltipY + 5, 0xFFFFFFFF, false);
            context.drawText(textRenderer, "§7" + part.explanation(), 
                           contentX + 5, tooltipY + 17, 0xFFFFFFFF, false);
        }
        
        // Legend
        int legendY = panelY + 5;
        int legendX = margin + panelWidth - 180;
        context.drawText(textRenderer, "§7Color Legend:", legendX, legendY, 0xFFFFFFFF, false);
        legendY += 12;
        
        String[] legend = {
            "§bEntity §7(civ/player)",
            "§aQuantity §7(numbers)",
            "§eAbbreviation",
            "§dModifier §7(~/-)  ",
            "§6Connector §7(,/&)"
        };
        
        for (String entry : legend) {
            context.drawText(textRenderer, entry, legendX, legendY, 0xFFFFFFFF, false);
            legendY += 10;
        }
    }
    
    private void renderExpandedTranslation(DrawContext context) {
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int panelWidth = width - margin - rightPanelWidth - margin - 10;
        
        // Background panel
        context.fill(margin, panelY, margin + panelWidth, panelY + panelHeight, 0xE0001100);
        context.drawBorder(margin, panelY, panelWidth, panelHeight, 0xFF00FFFF);
        
        int contentY = panelY + 10;
        int contentX = margin + 10;
        
        context.drawText(textRenderer, "§b§lEXPANDED TRANSLATION", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 20;
        
        // Translation
        context.drawText(textRenderer, "§eEnglish:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 15;
        
        String translation = explanationResult.translation();
        List<String> wrappedTranslation = wrapText(translation, panelWidth - 30);
        for (String line : wrappedTranslation) {
            context.drawText(textRenderer, "§f" + line, contentX + 10, contentY, 0xFFFFFFFF, false);
            contentY += 12;
        }
        
        contentY += 10;
        
        // Show all parts with expansions
        context.drawText(textRenderer, "§eWord-by-Word Breakdown:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 15;
        
        for (CarniteExplainer.MessagePart part : explanationResult.parts()) {
            if (contentY > panelY + panelHeight - 15) break;
            
            if (!part.text().equals(part.expanded())) {
                String line = "§7" + part.text() + " §f→ §b" + part.expanded();
                context.drawText(textRenderer, line, contentX + 10, contentY, 0xFFFFFFFF, false);
                contentY += 11;
            }
        }
    }
    
    private int getColorForPartType(CarniteExplainer.MessagePartType type) {
        return switch (type) {
            case ENTITY -> 0xFF55FFFF;        // Cyan
            case CONNECTOR -> 0xFFFFAA00;     // Orange
            case QUANTITY -> 0xFF55FF55;      // Green
            case QUESTION -> 0xFFFF55FF;      // Magenta
            case RESPONSE -> 0xFFFF88FF;      // Pink
            case MODIFIER -> 0xFFAA55FF;      // Purple
            case QUOTE -> 0xFFFFFF55;         // Yellow
            case ABBREVIATION -> 0xFFFFFF00;  // Bright Yellow
            case WORD -> 0xFFFFFFFF;          // White
            case SYMBOL -> 0xFFAAAAAA;        // Gray
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
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int sidebarX = width - margin - rightPanelWidth;
        int sidebarY = 105; // Below the dropdowns (color at 45, template at 80, +25 clearance)
        int sidebarH = bottomButtonY - sidebarY - 10;

        context.fill(sidebarX, sidebarY, sidebarX + rightPanelWidth, sidebarY + sidebarH, 0xF0101010);
        context.drawBorder(sidebarX, sidebarY, rightPanelWidth, sidebarH, 0xFF00AAFF);

        int contentY = sidebarY + 8;
        int contentX = sidebarX + 8;

        context.drawText(textRenderer, "§b§lQuick Reference", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 14;

        context.drawText(textRenderer, "§eWord Order:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 10;
        context.drawText(textRenderer, "§7[What][Where]", contentX, contentY, 0xFFAAAAAA, false);
        contentY += 9;
        context.drawText(textRenderer, "§7[Who][Action]", contentX, contentY, 0xFFAAAAAA, false);
        contentY += 12;

        context.drawText(textRenderer, "§eSymbols:", contentX, contentY, 0xFFFFFFFF, false);
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
            context.drawText(textRenderer, "§b" + sym[0] + " §7" + sym[1], contentX, contentY, 0xFFFFFFFF, false);
            contentY += 9;
        }

        context.drawText(textRenderer, "§8[Esc] close", contentX, sidebarY + sidebarH - 12, 0xFF666666, false);
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
                context.drawBorder(tooltipX - 2, tooltipY - 2, tooltipWidth + 2, 14, 0xFFFFFF00);
                context.drawText(textRenderer, meaning, tooltipX + 3, tooltipY, 0xFFFFFF00, false);
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
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
