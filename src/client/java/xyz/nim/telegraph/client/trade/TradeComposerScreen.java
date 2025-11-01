package xyz.nim.telegraph.client.trade;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.carnite.CarniteComposerScreen;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.util.ArrayList;
import java.util.List;

public class TradeComposerScreen extends Screen {
    private static final int PANEL_COLOR = 0xA0000000;
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final int HEADER_COLOR = 0xFF222222;
    
    private final Screen parent;
    private final TelegraphChannel channel;
    private final int mapId;
    private final ChannelSettings settings;
    
    private List<ItemInputGroup> offeringInputs = new ArrayList<>();
    private List<ItemInputGroup> requestingInputs = new ArrayList<>();
    
    private ButtonWidget addOfferingButton;
    private ButtonWidget addRequestingButton;
    private ButtonWidget previewButton;
    private ButtonWidget sendButton;
    private ButtonWidget backButton;
    
    private String previewMessage = "";
    
    public TradeComposerScreen(Screen parent, TelegraphChannel channel, int mapId, ChannelSettings settings) {
        super(Text.literal("Trade Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = settings;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = width / 2;
        int startY = 40;
        int margin = 20;
        int controlHeight = 20;
        int bottomMargin = 90;
        int backButtonWidth = Math.min(60, width / 12);
        int addButtonWidth = Math.min(100, width / 10);
        int previewButtonWidth = Math.min(100, width / 10);
        int sendButtonWidth = Math.min(100, width / 10);
        int panelHalfWidth = Math.min(220, (width - margin * 2) / 2);
        
        backButton = ButtonWidget.builder(Text.literal("← Back"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(margin, margin, backButtonWidth, controlHeight).build();
        addDrawableChild(backButton);
        
        int offeringY = startY + 40;
        addOfferingButton = ButtonWidget.builder(Text.literal("+ Add Item"), button -> {
            addOfferingInput();
        }).dimensions(centerX - panelHalfWidth, offeringY, addButtonWidth, controlHeight).build();
        addDrawableChild(addOfferingButton);
        
        int requestingY = startY + 180;
        addRequestingButton = ButtonWidget.builder(Text.literal("+ Add Item"), button -> {
            addRequestingInput();
        }).dimensions(centerX - panelHalfWidth, requestingY, addButtonWidth, controlHeight).build();
        addDrawableChild(addRequestingButton);
        
        int bottomButtonY = height - bottomMargin;
        previewButton = ButtonWidget.builder(Text.literal("↻ Update Preview"), button -> {
            updatePreview();
        }).dimensions(centerX - previewButtonWidth - 5, bottomButtonY, previewButtonWidth, controlHeight).build();
        addDrawableChild(previewButton);
        
        sendButton = ButtonWidget.builder(Text.literal("✓ Send Trade"), button -> {
            sendTrade();
        }).dimensions(centerX + 5, bottomButtonY, sendButtonWidth, controlHeight).build();
        addDrawableChild(sendButton);
        
        addOfferingInput();
        addRequestingInput();
        
        updatePreview();
    }
    
    private void addOfferingInput() {
        int index = offeringInputs.size();
        int y = 100 + index * 30;
        int panelHalfWidth = Math.min(220, (width - 40) / 2);
        
        if (y > 160) return;
        
        ItemInputGroup group = new ItemInputGroup(this, width / 2 - panelHalfWidth, y, true, index);
        offeringInputs.add(group);
    }
    
    private void addRequestingInput() {
        int index = requestingInputs.size();
        int y = 240 + index * 30;
        int panelHalfWidth = Math.min(220, (width - 40) / 2);
        
        if (y > 300) return;
        
        ItemInputGroup group = new ItemInputGroup(this, width / 2 - panelHalfWidth, y, false, index);
        requestingInputs.add(group);
    }
    
    private void removeOfferingInput(int index) {
        if (index < offeringInputs.size()) {
            ItemInputGroup group = offeringInputs.get(index);
            group.remove();
            offeringInputs.remove(index);
            repositionInputs();
        }
    }
    
    private void removeRequestingInput(int index) {
        if (index < requestingInputs.size()) {
            ItemInputGroup group = requestingInputs.get(index);
            group.remove();
            requestingInputs.remove(index);
            repositionInputs();
        }
    }
    
    private void repositionInputs() {
        int panelHalfWidth = Math.min(220, (width - 40) / 2);
        for (int i = 0; i < offeringInputs.size(); i++) {
            offeringInputs.get(i).setPosition(width / 2 - panelHalfWidth, 100 + i * 30);
        }
        for (int i = 0; i < requestingInputs.size(); i++) {
            requestingInputs.get(i).setPosition(width / 2 - panelHalfWidth, 240 + i * 30);
        }
    }
    
    private void updatePreview() {
        StringBuilder offering = new StringBuilder();
        StringBuilder requesting = new StringBuilder();
        
        for (ItemInputGroup group : offeringInputs) {
            String item = group.getItemName().trim();
            String quantity = group.getQuantity().trim();
            
            if (!item.isEmpty()) {
                if (offering.length() > 0) offering.append(",");
                
                String abbr = abbreviateItem(item);
                
                if (!quantity.isEmpty()) {
                    try {
                        int qty = Integer.parseInt(quantity);
                        if (qty >= 64) {
                            int stacks = qty / 64;
                            int remainder = qty % 64;
                            if (remainder == 0) {
                                offering.append(stacks).append(".");
                            } else {
                                offering.append(stacks).append(".").append(remainder);
                            }
                        } else {
                            offering.append(qty);
                        }
                    } catch (NumberFormatException e) {
                        offering.append("~");
                    }
                }
                offering.append(abbr);
            }
        }
        
        for (ItemInputGroup group : requestingInputs) {
            String item = group.getItemName().trim();
            String quantity = group.getQuantity().trim();
            
            if (!item.isEmpty()) {
                if (requesting.length() > 0) requesting.append(",");
                
                String abbr = abbreviateItem(item);
                
                if (!quantity.isEmpty()) {
                    try {
                        int qty = Integer.parseInt(quantity);
                        if (qty >= 64) {
                            int stacks = qty / 64;
                            int remainder = qty % 64;
                            if (remainder == 0) {
                                requesting.append(stacks).append(".");
                            } else {
                                requesting.append(stacks).append(".").append(remainder);
                            }
                        } else {
                            requesting.append(qty);
                        }
                    } catch (NumberFormatException e) {
                        requesting.append("~");
                    }
                }
                requesting.append(abbr);
            }
        }
        
        if (offering.length() == 0) offering.append("_");
        if (requesting.length() == 0) requesting.append("_");
        
        previewMessage = offering + " ; " + requesting + ":";
    }
    
    private String abbreviateItem(String item) {
        item = item.toLowerCase().trim();
        
        // Handle compound terms (adjective + noun) like "blessed food"
        if (item.contains(" ")) {
            String[] parts = item.split("\\s+");
            if (parts.length == 2) {
                String adj = abbreviateWord(parts[0]);
                String noun = abbreviateWord(parts[1]);
                return adj + "," + noun;
            } else if (parts.length > 2) {
                // Multiple words - abbreviate each
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    result.append(abbreviateWord(parts[i])).append(",");
                }
                result.append(abbreviateWord(parts[parts.length - 1]));
                return result.toString();
            }
        }
        
        // Single word - try vocabulary first
        String vocab = CarniteVocabulary.abbreviate(item);
        if (!vocab.equals(item)) {
            return vocab;
        }
        
        // Fall back to manual abbreviation
        return abbreviateWord(item);
    }
    
    private String abbreviateWord(String word) {
        // Common Minecraft items
        switch (word.toLowerCase()) {
            case "blessed": return "blss";
            case "food": return "fd";
            case "bread": return "brd";
            case "diamond": case "diamonds": return "dmd";
            case "iron": return "irn";
            case "gold": return "gld";
            case "emerald": case "emeralds": return "emrld";
            case "bandage": case "bandages": return "bndg";
            case "gunpowder": return "gpdr";
            case "autocrafter": return "acft";
            case "enchant": case "enchants": return "ench";
            case "sword": case "swords": return "swd";
            case "armor": return "armr";
            case "arrow": case "arrows": return "arrw";
            case "bow": case "bows": return "bow";
            case "crossbow": case "crossbows": return "xbow";
            case "potion": case "potions": return "ptn";
            case "heal": case "healing": return "heal";
            case "totem": case "totems": return "ttm";
            case "elytra": return "eltr";
            case "pearl": case "pearls": return "prl";
            case "ender": return "endr";
            case "netherite": return "nthr";
            case "obsidian": return "obsd";
            case "wool": return "wl";
            case "wood": return "wd";
            case "stone": return "stn";
            case "cobblestone": case "cobble": return "cbbl";
            case "log": case "logs": return "lg";
            case "plank": case "planks": return "plnk";
            case "stick": case "sticks": return "stck";
            case "coal": return "cl";
            case "copper": return "cppr";
            case "redstone": return "rdst";
            case "lapis": return "lps";
            case "quartz": return "qrtz";
            case "glowstone": return "glw";
            case "book": case "books": return "bk";
            case "paper": return "ppr";
            case "leather": return "lthr";
            case "helmet": return "hlmt";
            case "chestplate": return "chst";
            case "leggings": return "lgs";
            case "boots": return "bts";
            case "shield": case "shields": return "shld";
            case "axe": return "ax";
            case "pickaxe": return "pck";
            case "shovel": return "shvl";
            case "hoe": return "ho";
            case "shears": return "shrs";
            default:
                // Remove vowels and shorten
                String shortened = word.replaceAll("[aeiou]", "");
                if (shortened.isEmpty()) {
                    shortened = word;
                }
                return shortened.substring(0, Math.min(4, shortened.length()));
        }
    }
    
    private void sendTrade() {
        if (client == null) return;
        
        updatePreview();
        client.setScreen(new CarniteComposerScreen(parent, channel, mapId, settings, previewMessage, "yellow"));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int startY = 40;
        int margin = 20;
        int panelHalfWidth = Math.min(220, (width - margin * 2) / 2);
        int panelWidth = panelHalfWidth * 2;
        
        int offeringPanelY = startY;
        int offeringPanelHeight = 150;
        context.fill(centerX - panelHalfWidth, offeringPanelY, centerX + panelHalfWidth, offeringPanelY + offeringPanelHeight, PANEL_COLOR);
        context.drawBorder(centerX - panelHalfWidth, offeringPanelY, panelWidth, offeringPanelHeight, PANEL_BORDER_COLOR);
        context.fill(centerX - panelHalfWidth, offeringPanelY, centerX + panelHalfWidth, offeringPanelY + 30, HEADER_COLOR);
        context.drawText(textRenderer, "§6I'm Offering:", centerX - panelHalfWidth + 10, offeringPanelY + 10, 0xFFFFFFFF, false);
        
        int requestingPanelY = offeringPanelY + offeringPanelHeight + 10;
        int requestingPanelHeight = 150;
        context.fill(centerX - panelHalfWidth, requestingPanelY, centerX + panelHalfWidth, requestingPanelY + requestingPanelHeight, PANEL_COLOR);
        context.drawBorder(centerX - panelHalfWidth, requestingPanelY, panelWidth, requestingPanelHeight, PANEL_BORDER_COLOR);
        context.fill(centerX - panelHalfWidth, requestingPanelY, centerX + panelHalfWidth, requestingPanelY + 30, HEADER_COLOR);
        context.drawText(textRenderer, "§6I'm Requesting:", centerX - panelHalfWidth + 10, requestingPanelY + 10, 0xFFFFFFFF, false);
        
        int previewY = requestingPanelY + requestingPanelHeight + 20;
        int previewHeight = 50;
        context.fill(centerX - panelHalfWidth, previewY, centerX + panelHalfWidth, previewY + previewHeight, PANEL_COLOR);
        context.drawBorder(centerX - panelHalfWidth, previewY, panelWidth, previewHeight, PANEL_BORDER_COLOR);
        context.drawText(textRenderer, "§ePreview (Carnite):", centerX - panelHalfWidth + 10, previewY + 10, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "§7" + previewMessage, centerX - panelHalfWidth + 10, previewY + 25, 0xFFFFFFFF, false);
        
        context.drawCenteredTextWithShadow(textRenderer, this.title, centerX, 20, 0xFFFFFFFF);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
    
    private class ItemInputGroup {
        private final TextFieldWidget itemField;
        private final TextFieldWidget quantityField;
        private final ButtonWidget removeButton;
        private final boolean isOffering;
        private final int index;
        
        public ItemInputGroup(Screen screen, int x, int y, boolean isOffering, int index) {
            this.isOffering = isOffering;
            this.index = index;
            
            itemField = new TextFieldWidget(textRenderer, x, y, 200, 20, Text.literal("Item"));
            itemField.setPlaceholder(Text.literal("Item name..."));
            itemField.setMaxLength(32);
            itemField.setChangedListener(text -> updatePreview());
            addDrawableChild(itemField);
            
            quantityField = new TextFieldWidget(textRenderer, x + 210, y, 80, 20, Text.literal("Quantity"));
            quantityField.setPlaceholder(Text.literal("Amount..."));
            quantityField.setMaxLength(6);
            quantityField.setChangedListener(text -> updatePreview());
            addDrawableChild(quantityField);
            
            removeButton = ButtonWidget.builder(Text.literal("✗"), button -> {
                if (isOffering) {
                    removeOfferingInput(this.index);
                } else {
                    removeRequestingInput(this.index);
                }
            }).dimensions(x + 300, y, 20, 20).build();
            addDrawableChild(removeButton);
        }
        
        public String getItemName() {
            return itemField.getText();
        }
        
        public String getQuantity() {
            return quantityField.getText();
        }
        
        public void setPosition(int x, int y) {
            itemField.setX(x);
            itemField.setY(y);
            quantityField.setX(x + 210);
            quantityField.setY(y);
            removeButton.setX(x + 300);
            removeButton.setY(y);
        }
        
        public void remove() {
            TradeComposerScreen.this.remove(itemField);
            TradeComposerScreen.this.remove(quantityField);
            TradeComposerScreen.this.remove(removeButton);
        }
    }
}
