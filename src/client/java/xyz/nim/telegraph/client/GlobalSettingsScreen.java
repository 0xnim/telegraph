package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.util.*;

public class GlobalSettingsScreen extends Screen {
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final int HEADER_COLOR = 0xFF222222;
    
    private final Screen parent;
    
    private TextFieldWidget civCodeField;
    private TextFieldWidget civNameField;
    private ButtonWidget addCivButton;
    private ButtonWidget doneButton;
    private ButtonWidget mapRefreshToggle;
    private CivilizationListWidget civList;
    private boolean mapRefreshEnabled = false;
    
    public GlobalSettingsScreen(Screen parent) {
        super(Text.literal("Global Telegraph Settings"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        MapDecorationTracker tracker = TelegraphClient.getMapDecorationTracker();
        if (tracker != null) {
            mapRefreshEnabled = tracker.isMapRefreshEnabled();
        }
        
        int margin = 20;
        int controlHeight = 20;
        int rowSpacing = 30;
        int headerHeight = 30;
        int panelWidth = width - (margin * 2);
        int y = 60;
        
        // Map Refresh Toggle
        mapRefreshToggle = ButtonWidget.builder(
            Text.literal("Auto Map Refresh: " + (mapRefreshEnabled ? "§aON" : "§cOFF")),
            button -> {
                mapRefreshEnabled = !mapRefreshEnabled;
                if (tracker != null) {
                    tracker.setMapRefreshEnabled(mapRefreshEnabled);
                }
                button.setMessage(Text.literal("Auto Map Refresh: " + (mapRefreshEnabled ? "§aON" : "§cOFF")));
            }
        ).dimensions(margin, y, 200, controlHeight).build();
        addDrawableChild(mapRefreshToggle);
        
        y += rowSpacing;
        y += headerHeight + 10;
        
        // Input fields - responsive widths
        int codeFieldWidth = Math.min(80, width / 8);
        int nameFieldWidth = Math.min(250, width / 3);
        int addButtonWidth = Math.min(130, width / 6);
        
        civCodeField = new TextFieldWidget(textRenderer, margin, y, codeFieldWidth, controlHeight, Text.literal("Code"));
        civCodeField.setMaxLength(4);
        civCodeField.setPlaceholder(Text.literal("Code..."));
        addDrawableChild(civCodeField);
        
        civNameField = new TextFieldWidget(textRenderer, margin + codeFieldWidth + 5, y, nameFieldWidth, controlHeight, Text.literal("Name"));
        civNameField.setMaxLength(64);
        civNameField.setPlaceholder(Text.literal("Civilization name..."));
        addDrawableChild(civNameField);
        
        addCivButton = ButtonWidget.builder(Text.literal("Add Civilization"), button -> {
            String code = civCodeField.getText().trim().toUpperCase();
            String name = civNameField.getText().trim();
            
            if (!code.isEmpty() && !name.isEmpty()) {
                if (code.matches("[A-Z]{2,4}")) {
                    CarniteVocabulary.registerCivilization(code, name);
                    civCodeField.setText("");
                    civNameField.setText("");
                    updateCivList();
                    
                    if (client != null && client.player != null) {
                        client.player.sendMessage(Text.literal("§aAdded: " + code + " = " + name), false);
                    }
                } else {
                    if (client != null && client.player != null) {
                        client.player.sendMessage(Text.literal("§cCode must be 2-4 uppercase letters"), false);
                    }
                }
            }
        }).dimensions(margin + codeFieldWidth + nameFieldWidth + 10, y, addButtonWidth, controlHeight).build();
        addDrawableChild(addCivButton);
        
        y += rowSpacing;
        
        // Civilization list - fills remaining space
        int listHeight = height - y - 60;
        civList = new CivilizationListWidget(
            client,
            panelWidth,
            listHeight,
            y,
            24
        );
        civList.setX(margin);
        addDrawableChild(civList);
        
        updateCivList();
        
        // Done button - centered at bottom
        int buttonWidth = 150;
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(width / 2 - buttonWidth / 2, height - 28, buttonWidth, controlHeight).build();
        addDrawableChild(doneButton);
    }
    
    private void updateCivList() {
        if (civList == null) return;
        
        civList.clear();
        
        Map<String, String> allCivs = CarniteVocabulary.getAllCivilizations();
        List<Map.Entry<String, String>> sortedCivs = new ArrayList<>(allCivs.entrySet());
        sortedCivs.sort(Map.Entry.comparingByKey());
        
        for (Map.Entry<String, String> entry : sortedCivs) {
            civList.addCivEntry(new CivilizationListWidget.CivEntry(
                client,
                entry.getKey(),
                entry.getValue(),
                this::onRemoveCiv
            ));
        }
    }
    
    private void onRemoveCiv(String code) {
        CarniteVocabulary.removeCivilization(code);
        updateCivList();
        
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal("§cRemoved civilization: " + code), false);
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        
        int margin = 20;
        int headerHeight = 30;
        int panelWidth = width - (margin * 2);
        
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 20, 0xFFFFFFFF);
        
        int y = 90;
        int civHeaderY = y;
        
        context.fill(margin, civHeaderY, margin + panelWidth, civHeaderY + headerHeight, HEADER_COLOR);
        context.drawBorder(margin, civHeaderY, panelWidth, headerHeight, PANEL_BORDER_COLOR);
        context.drawText(textRenderer, "§eGlobal Civilizations", margin + 8, civHeaderY + 10, 0xFFFFFFFF, false);
        
        y += 120;
        context.drawText(textRenderer, "§7Civilizations are shared across all channels.", 
            margin + 8, y, 0xFF888888, false);
        y += 12;
        context.drawText(textRenderer, "§7Auto Map Refresh: Periodically cycles maps to force server updates (every 60s)", 
            margin + 8, y, 0xFF888888, false);
    }
    
    @Override
    public void close() {
        if (client != null) {
            MapDecorationTracker tracker = TelegraphClient.getMapDecorationTracker();
            if (tracker != null) {
                tracker.getPersistenceManager().saveCivilizations();
            }
            client.setScreen(parent);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
    
    private static class CivilizationListWidget extends AlwaysSelectedEntryListWidget<CivilizationListWidget.CivEntry> {
        public CivilizationListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
        
        public void clear() {
            children().clear();
        }
        
        public void addCivEntry(CivEntry entry) {
            super.addEntry(entry);
        }
        
        @Override
        public int getRowWidth() {
            return width - 20;
        }
        
        @Override
        protected int getScrollbarX() {
            return getX() + width - 6;
        }
        
        public static class CivEntry extends Entry<CivEntry> {
            private final net.minecraft.client.MinecraftClient client;
            private final String code;
            private final String name;
            private final java.util.function.Consumer<String> onRemove;
            private ButtonWidget removeButton;
            
            public CivEntry(net.minecraft.client.MinecraftClient client, String code, String name, 
                           java.util.function.Consumer<String> onRemove) {
                this.client = client;
                this.code = code;
                this.name = name;
                this.onRemove = onRemove;
            }
            
            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                              int mouseX, int mouseY, boolean hovered, float tickDelta) {
                
                context.drawText(client.textRenderer, "§6" + code, x + 5, y + 6, 0xFFFFFFFF, false);
                context.drawText(client.textRenderer, "§f→ " + name, x + 60, y + 6, 0xFFFFFFFF, false);
                
                if (removeButton == null) {
                    removeButton = ButtonWidget.builder(Text.literal("Remove"), button -> {
                        onRemove.accept(code);
                    }).dimensions(x + entryWidth - 80, y + 2, 75, 20).build();
                } else {
                    removeButton.setX(x + entryWidth - 80);
                    removeButton.setY(y + 2);
                }
                
                removeButton.render(context, mouseX, mouseY, tickDelta);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                return (removeButton != null && removeButton.mouseClicked(mouseX, mouseY, button)) || super.mouseClicked(mouseX, mouseY, button);
            }
            
            @Override
            public Text getNarration() {
                return Text.literal("Civilization: " + code + " - " + name);
            }
        }
    }
}
