package xyz.nim.telegraph.client.carnite;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;

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
    private List<ButtonWidget> symbolButtons = new ArrayList<>();
    private List<ButtonWidget> colorButtons = new ArrayList<>();
    private List<ButtonWidget> templateButtons = new ArrayList<>();
    
    private String selectedBannerColor = "white";
    private CarniteValidator.ValidationResult validationResult = null;
    private CarniteExplainer.ExplanationResult explanationResult = null;
    private boolean showHelp = false;
    private ViewMode currentMode = ViewMode.INFO;
    private int scrollOffset = 0;
    private int hoveredPartIndex = -1;
    
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
    
    @Override
    protected void init() {
        super.init();
        
        int leftMargin = 20;
        int rightPanelX = width - 200;
        
        // Message input
        messageField = new TextFieldWidget(textRenderer, leftMargin, 40, rightPanelX - leftMargin - 10, 20, Text.literal("Message"));
        messageField.setMaxLength(64);
        messageField.setPlaceholder(Text.literal("Type Carnite message..."));
        messageField.setChangedListener(text -> {
            validationResult = CarniteValidator.validate(text, selectedBannerColor);
            if (currentMode == ViewMode.LEARN || currentMode == ViewMode.EXPAND) {
                explanationResult = CarniteExplainer.explainMessage(text, selectedBannerColor);
            }
        });
        addDrawableChild(messageField);
        
        // Symbol buttons
        int symbolY = 70;
        int symbolX = leftMargin;
        Map<String, String> symbols = CarniteVocabulary.getSymbols();
        
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            String symbol = entry.getKey();
            ButtonWidget btn = ButtonWidget.builder(Text.literal(symbol), button -> {
                if (messageField != null) {
                    String current = messageField.getText();
                    messageField.setText(current + symbol);
                }
            }).dimensions(symbolX, symbolY, 30, 18).build();
            
            addDrawableChild(btn);
            symbolButtons.add(btn);
            
            symbolX += 35;
            if (symbolX > rightPanelX - 50) {
                symbolX = leftMargin;
                symbolY += 20;
            }
        }
        
        // Quick templates
        int templateY = symbolY + 30;
        addTemplate(leftMargin, templateY, "~rd| ; ", "Raiders at my civ");
        addTemplate(leftMargin, templateY + 22, "_; _:", "Trade: What I have for what you give");
        addTemplate(leftMargin, templateY + 44, "^ y", "Response: Yes");
        addTemplate(leftMargin, templateY + 66, "^ -acpt", "Response: Do not accept");
        addTemplate(leftMargin, templateY + 88, "_ :: ; atk", "Question: Which of you is attacking us?");
        
        // Banner color selection (right panel)
        int colorY = 40;
        Text.literal("Banner Color:").getString();
        
        String[][] colors = {
            {"white", "White\n(Present)"},
            {"light_gray", "Lt.Gray\n(Past)"},
            {"gray", "Gray\n(Future)"},
            {"pink", "Pink\n(Might)"},
            {"red", "Red\n(URGENT)"},
            {"light_blue", "Lt.Blue\n(Request)"},
            {"black", "Black\n(Decided)"},
            {"blue", "Blue\n(Question)"},
            {"yellow", "Yellow\n(Trade)"},
            {"purple", "Purple\n(Goal)"}
        };
        
        for (String[] color : colors) {
            ButtonWidget colorBtn = ButtonWidget.builder(Text.literal(color[1].split("\n")[0]), button -> {
                selectedBannerColor = color[0];
                updateColorButtons();
                // Update both validation and explanation immediately
                validationResult = CarniteValidator.validate(messageField.getText(), selectedBannerColor);
                explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            }).dimensions(rightPanelX, colorY, 180, 18).build();
            
            addDrawableChild(colorBtn);
            colorButtons.add(colorBtn);
            colorY += 20;
        }
        
        // Tab buttons (above panel area)
        int tabY = 275;
        
        infoTabButton = ButtonWidget.builder(Text.literal("Info"), button -> {
            currentMode = ViewMode.INFO;
            updateTabButtons();
        }).dimensions(leftMargin, tabY, 80, 20).build();
        addDrawableChild(infoTabButton);
        
        learnTabButton = ButtonWidget.builder(Text.literal("Learn"), button -> {
            currentMode = ViewMode.LEARN;
            explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            updateTabButtons();
        }).dimensions(leftMargin + 85, tabY, 80, 20).build();
        addDrawableChild(learnTabButton);
        
        expandTabButton = ButtonWidget.builder(Text.literal("Expand"), button -> {
            currentMode = ViewMode.EXPAND;
            explanationResult = CarniteExplainer.explainMessage(messageField.getText(), selectedBannerColor);
            updateTabButtons();
        }).dimensions(leftMargin + 170, tabY, 80, 20).build();
        addDrawableChild(expandTabButton);
        
        // Bottom buttons
        helpButton = ButtonWidget.builder(Text.literal("Help"), button -> {
            showHelp = !showHelp;
        }).dimensions(leftMargin, height - 60, 70, 20).build();
        addDrawableChild(helpButton);
        
        copyButton = ButtonWidget.builder(Text.literal("Copy"), button -> {
            String message = getFormattedMessage();
            if (client != null) {
                client.keyboard.setClipboard(message);
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§aCopied: " + message), false);
                }
            }
        }).dimensions(leftMargin + 75, height - 60, 70, 20).build();
        addDrawableChild(copyButton);
        
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(rightPanelX + 100, height - 60, 80, 20).build();
        addDrawableChild(doneButton);
        
        updateColorButtons();
        updateTabButtons();
    }
    
    private void updateTabButtons() {
        if (infoTabButton != null) infoTabButton.active = currentMode != ViewMode.INFO;
        if (learnTabButton != null) learnTabButton.active = currentMode != ViewMode.LEARN;
        if (expandTabButton != null) expandTabButton.active = currentMode != ViewMode.EXPAND;
    }
    
    private void addTemplate(int x, int y, String template, String description) {
        ButtonWidget btn = ButtonWidget.builder(Text.literal(template), button -> {
            if (messageField != null) {
                messageField.setText(template);
            }
        }).dimensions(x, y, 150, 18).build();
        addDrawableChild(btn);
        templateButtons.add(btn);
    }
    
    private void updateColorButtons() {
        String[] colors = {"white", "light_gray", "gray", "pink", "red", "light_blue", "black", "blue", "yellow", "purple"};
        for (int i = 0; i < colorButtons.size() && i < colors.length; i++) {
            colorButtons.get(i).active = !colors[i].equals(selectedBannerColor);
        }
    }
    
    private String getFormattedMessage() {
        String message = messageField != null ? messageField.getText() : "";
        return "[" + selectedBannerColor.toUpperCase() + " BANNER] " + message;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int leftMargin = 20;
        int rightPanelX = width - 200;
        
        // Title
        context.drawText(textRenderer, "Carnite Message Composer", leftMargin, 20, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "Channel: " + channel.getDisplayName(mapId), leftMargin, 30, 0xFFAAAAAA, false);
        
        // Symbol helper section
        context.drawText(textRenderer, "Quick Symbols:", leftMargin, 60, 0xFFFFFFFF, false);
        
        // Color panel title
        context.drawText(textRenderer, "Banner Color (Tense):", rightPanelX, 30, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "Selected: " + CarniteParser.getTenseFromColor(selectedBannerColor), 
            rightPanelX, 250, 0xFFFFFF00, false);
        
        // Message stats
        if (messageField != null && !messageField.getText().isEmpty()) {
            int length = messageField.getText().length();
            int color = length > 38 ? 0xFFFF0000 : length > 32 ? 0xFFFFAA00 : 0xFF55FF55;
            String stats = length + " chars" + (length > 32 ? " (readability warning)" : "");
            context.drawText(textRenderer, stats, leftMargin, height - 80, color, false);
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
        context.drawText(textRenderer, "Preview:", leftMargin, height - 95, 0xFFAAAAAA, false);
        String preview = getFormattedMessage();
        context.drawText(textRenderer, preview, leftMargin + 60, height - 95, 0xFFFFFF00, false);
    }
    
    private void renderInfoTab(DrawContext context) {
        int panelY = 300;
        int leftMargin = 20;
        int panelWidth = width - 240;
        int panelHeight = height - panelY - 130;
        
        // Background panel
        context.fill(leftMargin, panelY, leftMargin + panelWidth, panelY + panelHeight, 0xE0000033);
        context.drawBorder(leftMargin, panelY, panelWidth, panelHeight, 0xFF00AAFF);
        
        int contentY = panelY + 10;
        int contentX = leftMargin + 10;
        
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
        int panelY = 300;
        int leftMargin = 20;
        int panelWidth = width - 240;
        int panelHeight = height - panelY - 130;
        
        // Background panel
        context.fill(leftMargin, panelY, leftMargin + panelWidth, panelY + panelHeight, 0xE0000000);
        context.drawBorder(leftMargin, panelY, panelWidth, panelHeight, 0xFF00FF00);
        
        int contentY = panelY + 5;
        int contentX = leftMargin + 5;
        
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
            context.fill(contentX, tooltipY, leftMargin + panelWidth - 5, panelY + panelHeight - 5, 0xFF1A1A1A);
            context.drawBorder(contentX, tooltipY, panelWidth - 10, 35, 0xFFFFFF00);
            
            context.drawText(textRenderer, "§e'" + part.text() + "' §f→ §b" + part.expanded(), 
                           contentX + 5, tooltipY + 5, 0xFFFFFFFF, false);
            context.drawText(textRenderer, "§7" + part.explanation(), 
                           contentX + 5, tooltipY + 17, 0xFFFFFFFF, false);
        }
        
        // Legend
        int legendY = panelY + 5;
        int legendX = leftMargin + panelWidth - 180;
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
        int panelY = 300;
        int leftMargin = 20;
        int panelWidth = width - 240;
        int panelHeight = height - panelY - 130;
        
        // Background panel
        context.fill(leftMargin, panelY, leftMargin + panelWidth, panelY + panelHeight, 0xE0001100);
        context.drawBorder(leftMargin, panelY, panelWidth, panelHeight, 0xFF00FFFF);
        
        int contentY = panelY + 10;
        int contentX = leftMargin + 10;
        
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
        context.drawBorder(overlayX, overlayY, overlayW, overlayH, 0xFFFFFFFF);
        
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
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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
