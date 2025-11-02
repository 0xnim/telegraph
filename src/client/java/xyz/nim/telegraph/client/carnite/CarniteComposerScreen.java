package xyz.nim.telegraph.client.carnite;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.ui.DropdownWidget;
import xyz.nim.telegraph.client.ui.SimpleLayout;
import xyz.nim.telegraph.client.ui.SimpleLayout.Box;
import xyz.nim.telegraph.client.util.GuiUtil;

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
    private boolean showHelp = false;
    private ViewMode currentMode = ViewMode.INFO;
    private int scrollOffset = 0;
    private int hoveredPartIndex = -1;
    
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
        
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int leftPanelWidth = width - margin - rightPanelWidth - margin - 10;
        
        // Create layout for left panel
        var leftPanel = SimpleLayout.vstack(margin, 40, leftPanelWidth, 5);
        
        // Message input
        Box inputBox = leftPanel.add(20);
        messageField = new TextFieldWidget(textRenderer, inputBox.x, inputBox.y, inputBox.width, inputBox.height, Text.literal("Message"));
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
        
        // Only show symbols if enough vertical space (need ~350px minimum)
        if (height >= 350) {
            leftPanel.addGap(10);
            
            // Symbol buttons using grid - limit to 3 rows max
            Map<String, String> symbols = CarniteVocabulary.getSymbols();
            int symbolsPerRow = Math.max(1, leftPanelWidth / 35);
            int maxSymbolRows = height >= 500 ? 3 : 2;
            this.symbolGridY = leftPanel.getCurrentY();
            var symbolGrid = SimpleLayout.grid(margin, this.symbolGridY + 4, symbolsPerRow, 30, 18, 5, 5);
            
            int count = 0;
            for (Map.Entry<String, String> entry : symbols.entrySet()) {
                if (count >= symbolsPerRow * maxSymbolRows) break;
                
                String symbol = entry.getKey();
                Box symbolBox = symbolGrid.next();
                ButtonWidget btn = ButtonWidget.builder(Text.literal(symbol), button -> {
                    if (messageField != null) {
                        String current = messageField.getText();
                        messageField.setText(current + symbol);
                    }
                }).dimensions(symbolBox.x, symbolBox.y, symbolBox.width, symbolBox.height).build();
                
                addDrawableChild(btn);
                symbolButtons.add(btn);
                count++;
            }
            
            int symbolGridHeight = Math.min(symbolGrid.getHeight(), maxSymbolRows * 18 + (maxSymbolRows - 1) * 5);
            leftPanel.add(symbolGridHeight);
            leftPanel.addGap(5);
        }
        
        // Template dropdown - only show if enough space
        if (height >= 400) {
            Box templateBox = leftPanel.add(20);
            
            List<DropdownWidget.DropdownOption> templateOptions = new ArrayList<>();
            for (String[] template : TEMPLATES) {
                templateOptions.add(new DropdownWidget.DropdownOption(template[0], template[1]));
            }
            
            templateDropdown = new DropdownWidget(client, templateBox.x, templateBox.y, templateBox.width, templateBox.height, 
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
        }
        
        // Right panel layout
        int rightPanelX = width - margin - rightPanelWidth;
        var rightPanel = SimpleLayout.vstack(rightPanelX, 70, rightPanelWidth, 10);
        
        // Banner color dropdown (right panel)
        Box colorBox = rightPanel.add(20);
        
        List<DropdownWidget.DropdownOption> colorOptions = new ArrayList<>();
        for (String[] color : BANNER_COLORS) {
            colorOptions.add(new DropdownWidget.DropdownOption(color[0], color[1]));
        }
        
        colorDropdown = new DropdownWidget(client, colorBox.x, colorBox.y, colorBox.width, colorBox.height,
            colorOptions, selectedColor -> {
                selectedBannerColor = selectedColor;
                validationResult = CarniteValidator.validate(messageField.getText(), selectedBannerColor);
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            });
        colorDropdown.setSelected(selectedBannerColor);
        addDrawableChild(colorDropdown.getButton());
        
        // Calculate positions from bottom up
        this.bottomButtonY = height - 30;
        this.infoPreviewY = this.bottomButtonY - 30;
        
        // Calculate tab position - start after left panel content with some gap
        leftPanel.addGap(10);
        this.tabY = leftPanel.getCurrentY();
        this.panelY = this.tabY + 25;
        this.panelHeight = this.infoPreviewY - this.panelY - 10;
        
        // Tab buttons using HStack
        var tabs = SimpleLayout.hstack(margin, this.tabY, 20, 5);
        
        Box infoTab = tabs.add(80);
        infoTabButton = ButtonWidget.builder(Text.literal("Info"), button -> {
            currentMode = ViewMode.INFO;
            updateTabButtons();
        }).dimensions(infoTab.x, infoTab.y, infoTab.width, infoTab.height).build();
        addDrawableChild(infoTabButton);
        
        Box learnTab = tabs.add(80);
        learnTabButton = ButtonWidget.builder(Text.literal("Learn"), button -> {
            currentMode = ViewMode.LEARN;
            explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            updateTabButtons();
        }).dimensions(learnTab.x, learnTab.y, learnTab.width, learnTab.height).build();
        addDrawableChild(learnTabButton);
        
        Box expandTab = tabs.add(80);
        expandTabButton = ButtonWidget.builder(Text.literal("Expand"), button -> {
            currentMode = ViewMode.EXPAND;
            explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            updateTabButtons();
        }).dimensions(expandTab.x, expandTab.y, expandTab.width, expandTab.height).build();
        addDrawableChild(expandTabButton);
        
        // Bottom buttons using HStack
        var bottomButtons = SimpleLayout.hstack(margin, this.bottomButtonY, 20, 5);
        
        Box helpBox = bottomButtons.add(Math.min(70, width / 12));
        helpButton = ButtonWidget.builder(Text.literal("Help"), button -> {
            showHelp = !showHelp;
        }).dimensions(helpBox.x, helpBox.y, helpBox.width, helpBox.height).build();
        addDrawableChild(helpButton);
        
        Box copyBox = bottomButtons.add(Math.min(70, width / 12));
        copyButton = ButtonWidget.builder(Text.literal("Copy"), button -> {
            String message = getFormattedMessage();
            if (client != null) {
                client.keyboard.setClipboard(message);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§aCopied: " + message), false);
                }
            }
        }).dimensions(copyBox.x, copyBox.y, copyBox.width, copyBox.height).build();
        addDrawableChild(copyButton);
        
        // Done button on right side
        int doneButtonWidth = Math.min(80, width / 10);
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
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int margin = 20;
        int rightPanelWidth = Math.min(180, width / 4);
        int rightPanelX = width - margin - rightPanelWidth;
        
        // Title
        context.drawText(textRenderer, "Carnite Message Composer", margin, 20, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "Channel: " + channel.getDisplayName(mapId), margin, 30, 0xFFAAAAAA, false);
        
        // Symbol helper section
        if (symbolGridY > 0) {
            context.drawText(textRenderer, "Quick Symbols:", margin, symbolGridY - 6, 0xFFFFFFFF, false);
        }
        
        // Color panel title
        context.drawText(textRenderer, "Banner Color:", rightPanelX, 55, 0xFFFFFFFF, false);
        
        // Message stats
        if (messageField != null && !messageField.getText().isEmpty()) {
            int length = messageField.getText().length();
            int color = length > 38 ? 0xFFFF0000 : length > 32 ? 0xFFFFAA00 : 0xFF55FF55;
            String stats = length + " chars" + (length > 32 ? " (readability warning)" : "");
            context.drawText(textRenderer, stats, margin, height - 80, color, false);
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
        
        // Help overlay
        if (showHelp) {
            renderHelpOverlay(context);
        }
        
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
        GuiUtil.drawBorder(context, margin, panelY, panelWidth, panelHeight, 0xFF00AAFF);
        
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
        GuiUtil.drawBorder(context, margin, panelY, panelWidth, panelHeight, 0xFF00FF00);
        
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
            GuiUtil.drawBorder(context, contentX, tooltipY, panelWidth - 10, 35, 0xFFFFFF00);
            
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
        GuiUtil.drawBorder(context, margin, panelY, panelWidth, panelHeight, 0xFF00FFFF);
        
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
    
    private void renderHelpOverlay(DrawContext context) {
        int overlayX = width / 4;
        int overlayY = 60;
        int overlayW = width / 2;
        int overlayH = height - 140;
        
        // Background
        context.fill(overlayX, overlayY, overlayX + overlayW, overlayY + overlayH, 0xE0000000);
        GuiUtil.drawBorder(context, overlayX, overlayY, overlayW, overlayH, 0xFFFFFFFF);
        
        // Content
        int contentY = overlayY + 10;
        int contentX = overlayX + 10;
        
        context.drawText(textRenderer, "§lCarnite Telegraphic Quick Guide", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 20;
        
        context.drawText(textRenderer, "§eWord Order: [What] [Where] [Who] [Action]", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        context.drawText(textRenderer, "Example: 'dmd CN ; take' = Diamonds from CN my-civ takes", contentX, contentY, 0xFFAAAAAA, false);
        contentY += 20;
        
        context.drawText(textRenderer, "§eKey Symbols:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        String[] helpLines = {
            "| = agent/player (after role/job)",
            ": = your civ (addressing someone)",
            "; = my civ (we/us/our)",
            ", = property of (links descriptors)",
            "& = and (joins items)",
            ". = stack of 64 (2.5 = 133 items)",
            "_ = question blank",
            "^ = response/because",
            "~ = plural/about/approximate",
            ":: = to all civs on channel",
            "- = negation/not"
        };
        
        for (String line : helpLines) {
            context.drawText(textRenderer, line, contentX, contentY, 0xFFCCCCCC, false);
            contentY += 11;
        }
        
        contentY += 10;
        context.drawText(textRenderer, "§eExamples:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        String[] examples = {
            "~rd| ; - Raiders at my civ",
            "2bld|5 CN: - 2 level-5 builders to CN",
            ".dmd CN ; trd - 1 stack diamonds CN my-civ traded",
            "_ CN atk - Who is attacking CN?",
            "^ y - Response: yes"
        };
        
        for (String ex : examples) {
            context.drawText(textRenderer, ex, contentX, contentY, 0xFF88FFFF, false);
            contentY += 11;
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (templateDropdown != null && templateDropdown.isExpanded() && templateDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (colorDropdown != null && colorDropdown.isExpanded() && colorDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return false;
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
