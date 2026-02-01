package xyz.nim.telegraph.client.trade;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.carnite.CarniteComposerScreen;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TextFields;

import java.util.ArrayList;
import java.util.List;

public class TradeComposerScreen extends TelegraphScreen {

    private final Screen parent;
    private final TelegraphChannel channel;
    private final int mapId;
    private final ChannelSettings settings;

    private List<ItemInputGroup> offeringInputs = new ArrayList<>();
    private List<ItemInputGroup> requestingInputs = new ArrayList<>();

    private Button addOfferingButton;
    private Button addRequestingButton;
    private Button previewButton;
    private Button sendButton;
    private Button backButton;

    private String previewMessage = "";

    public TradeComposerScreen(Screen parent, TelegraphChannel channel, int mapId, ChannelSettings settings) {
        super(Component.literal("Trade Composer"));
        this.parent = parent;
        this.channel = channel;
        this.mapId = mapId;
        this.settings = settings;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = width / 2;
        int startY = layout.margin + layout.headerHeight;
        int panelHalfWidth = Math.min(220, layout.contentWidth() / 2);

        backButton = Buttons.back(layout.margin, layout.margin, layout, button -> onClose());
        addRenderableWidget(backButton);

        int offeringY = startY + layout.spacing;
        addOfferingButton = Buttons.create(Component.literal("+ Add Item"),
                centerX - panelHalfWidth, offeringY, layout.buttonWidth, layout, button -> addOfferingInput());
        addRenderableWidget(addOfferingButton);

        int requestingY = startY + 140 + layout.spacing;
        addRequestingButton = Buttons.create(Component.literal("+ Add Item"),
                centerX - panelHalfWidth, requestingY, layout.buttonWidth, layout, button -> addRequestingInput());
        addRenderableWidget(addRequestingButton);

        int bottomButtonY = height - layout.margin - layout.buttonHeight;
        int buttonWidth = Math.min(120, layout.contentWidth() / 4);

        previewButton = Buttons.create(Component.literal("\u21BB Update Preview"),
                centerX - buttonWidth - layout.spacing / 2, bottomButtonY, buttonWidth, layout, button -> updatePreview());
        addRenderableWidget(previewButton);

        sendButton = Buttons.create(Component.literal("\u2713 Send Trade"),
                centerX + layout.spacing / 2, bottomButtonY, buttonWidth, layout, button -> sendTrade());
        addRenderableWidget(sendButton);

        addOfferingInput();
        addRequestingInput();

        updatePreview();
    }

    private void addOfferingInput() {
        int index = offeringInputs.size();
        int y = layout.margin + layout.headerHeight + 30 + index * (layout.controlHeight + layout.spacing);
        int panelHalfWidth = Math.min(220, layout.contentWidth() / 2);

        if (y > layout.margin + layout.headerHeight + 120) return;

        ItemInputGroup group = new ItemInputGroup(this, width / 2 - panelHalfWidth, y, true, index);
        offeringInputs.add(group);
    }

    private void addRequestingInput() {
        int index = requestingInputs.size();
        int y = layout.margin + layout.headerHeight + 170 + index * (layout.controlHeight + layout.spacing);
        int panelHalfWidth = Math.min(220, layout.contentWidth() / 2);

        if (y > layout.margin + layout.headerHeight + 260) return;

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
        int panelHalfWidth = Math.min(220, layout.contentWidth() / 2);
        for (int i = 0; i < offeringInputs.size(); i++) {
            offeringInputs.get(i).setPosition(width / 2 - panelHalfWidth,
                    layout.margin + layout.headerHeight + 30 + i * (layout.controlHeight + layout.spacing));
        }
        for (int i = 0; i < requestingInputs.size(); i++) {
            requestingInputs.get(i).setPosition(width / 2 - panelHalfWidth,
                    layout.margin + layout.headerHeight + 170 + i * (layout.controlHeight + layout.spacing));
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

        if (item.contains(" ")) {
            String[] parts = item.split("\\s+");
            if (parts.length == 2) {
                String adj = abbreviateWord(parts[0]);
                String noun = abbreviateWord(parts[1]);
                return adj + "," + noun;
            } else if (parts.length > 2) {
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < parts.length - 1; i++) {
                    result.append(abbreviateWord(parts[i])).append(",");
                }
                result.append(abbreviateWord(parts[parts.length - 1]));
                return result.toString();
            }
        }

        String vocab = CarniteVocabulary.abbreviate(item);
        if (!vocab.equals(item)) {
            return vocab;
        }

        return abbreviateWord(item);
    }

    private String abbreviateWord(String word) {
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
                String shortened = word.replaceAll("[aeiou]", "");
                if (shortened.isEmpty()) {
                    shortened = word;
                }
                return shortened.substring(0, Math.min(4, shortened.length()));
        }
    }

    private void sendTrade() {
        if (minecraft == null) return;

        updatePreview();
        minecraft.setScreen(new CarniteComposerScreen(parent, channel, mapId, settings, previewMessage, "yellow"));
    }

    @Override
    protected void renderPanels(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int centerX = width / 2;
        int startY = layout.margin + layout.headerHeight;
        int panelHalfWidth = Math.min(220, layout.contentWidth() / 2);
        int panelWidth = panelHalfWidth * 2;

        // Title
        context.drawCenteredString(font, this.title, centerX, layout.margin, TelegraphTheme.TEXT_PRIMARY);

        // Offering panel
        int offeringPanelY = startY;
        int offeringPanelHeight = 130;
        drawPanel(context, centerX - panelHalfWidth, offeringPanelY, panelWidth, offeringPanelHeight);
        context.fill(centerX - panelHalfWidth + 1, offeringPanelY + 1,
                centerX + panelHalfWidth - 1, offeringPanelY + layout.headerHeight, TelegraphTheme.HEADER_BG);
        context.drawString(font, "\u00A76I'm Offering:",
                centerX - panelHalfWidth + layout.padding, offeringPanelY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);

        // Requesting panel
        int requestingPanelY = offeringPanelY + offeringPanelHeight + layout.spacing;
        int requestingPanelHeight = 130;
        drawPanel(context, centerX - panelHalfWidth, requestingPanelY, panelWidth, requestingPanelHeight);
        context.fill(centerX - panelHalfWidth + 1, requestingPanelY + 1,
                centerX + panelHalfWidth - 1, requestingPanelY + layout.headerHeight, TelegraphTheme.HEADER_BG);
        context.drawString(font, "\u00A76I'm Requesting:",
                centerX - panelHalfWidth + layout.padding, requestingPanelY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);

        // Preview panel
        int previewY = requestingPanelY + requestingPanelHeight + layout.spacing;
        int previewHeight = 50;
        drawPanel(context, centerX - panelHalfWidth, previewY, panelWidth, previewHeight);
        context.drawString(font, "\u00A7ePreview (Carnite):",
                centerX - panelHalfWidth + layout.padding, previewY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);
        context.drawString(font, "\u00A77" + previewMessage,
                centerX - panelHalfWidth + layout.padding, previewY + layout.padding + 15, TelegraphTheme.TEXT_PRIMARY, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private class ItemInputGroup {
        private final EditBox itemField;
        private final EditBox quantityField;
        private final Button removeButton;
        private final boolean isOffering;
        private final int index;

        public ItemInputGroup(TradeComposerScreen screen, int x, int y, boolean isOffering, int index) {
            this.isOffering = isOffering;
            this.index = index;

            int fieldWidth = Math.min(200, layout.contentWidth() / 2 - 100);
            int qtyWidth = Math.min(80, layout.contentWidth() / 6);

            itemField = TextFields.input(font, x, y, fieldWidth, layout, "Item name...", 32);
            itemField.setResponder(text -> updatePreview());
            addRenderableWidget(itemField);

            quantityField = TextFields.input(font, x + fieldWidth + layout.spacing, y, qtyWidth, layout, "Qty...", 6);
            quantityField.setResponder(text -> updatePreview());
            addRenderableWidget(quantityField);

            removeButton = Buttons.small(Component.literal("\u2717"),
                    x + fieldWidth + qtyWidth + layout.spacing * 2, y, layout, button -> {
                        if (isOffering) {
                            removeOfferingInput(this.index);
                        } else {
                            removeRequestingInput(this.index);
                        }
                    });
            addRenderableWidget(removeButton);
        }

        public String getItemName() {
            return itemField.getValue();
        }

        public String getQuantity() {
            return quantityField.getValue();
        }

        public void setPosition(int x, int y) {
            int fieldWidth = Math.min(200, layout.contentWidth() / 2 - 100);
            int qtyWidth = Math.min(80, layout.contentWidth() / 6);

            itemField.setX(x);
            itemField.setY(y);
            quantityField.setX(x + fieldWidth + layout.spacing);
            quantityField.setY(y);
            removeButton.setX(x + fieldWidth + qtyWidth + layout.spacing * 2);
            removeButton.setY(y);
        }

        public void remove() {
            TradeComposerScreen.this.removeWidget(itemField);
            TradeComposerScreen.this.removeWidget(quantityField);
            TradeComposerScreen.this.removeWidget(removeButton);
        }
    }
}
