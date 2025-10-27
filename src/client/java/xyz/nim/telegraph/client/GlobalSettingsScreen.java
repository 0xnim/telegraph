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
    private static final int MARGIN_LEFT = 20;
    private static final int MARGIN_RIGHT = 20;
    private static final int CONTROL_HEIGHT = 20;
    private static final int ROW_SPACING = 30;
    private static final int HEADER_HEIGHT = 30;
    private static final int START_Y = 40;
    private static final int BUTTON_WIDTH = 150;
    
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
        
        int panelWidth = width - MARGIN_LEFT - MARGIN_RIGHT;
        int y = START_Y + CONTROL_HEIGHT;
        
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
        ).dimensions(MARGIN_LEFT, y, 200, CONTROL_HEIGHT).build();
        addDrawableChild(mapRefreshToggle);
        
        y += ROW_SPACING;
        y += HEADER_HEIGHT + 10;
        
        // Input fields
        civCodeField = new TextFieldWidget(textRenderer, MARGIN_LEFT, y, 80, CONTROL_HEIGHT, Text.literal("Code"));
        civCodeField.setMaxLength(4);
        civCodeField.setPlaceholder(Text.literal("Code..."));
        addDrawableChild(civCodeField);
        
        civNameField = new TextFieldWidget(textRenderer, MARGIN_LEFT + 85, y, 250, CONTROL_HEIGHT, Text.literal("Name"));
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
        }).dimensions(MARGIN_LEFT + 340, y, 130, CONTROL_HEIGHT).build();
        addDrawableChild(addCivButton);
        
        y += ROW_SPACING;
        
        // Civilization list
        int listHeight = height - y - 60;
        civList = new CivilizationListWidget(
            client,
            panelWidth,
            listHeight,
            y,
            24
        );
        civList.setX(MARGIN_LEFT);
        addDrawableChild(civList);
        
        updateCivList();
        
        // Done button
        doneButton = ButtonWidget.builder(Text.literal("Done"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(width / 2 - BUTTON_WIDTH / 2, height - ROW_SPACING, BUTTON_WIDTH, CONTROL_HEIGHT).build();
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
        
        int panelWidth = width - MARGIN_LEFT - MARGIN_RIGHT;
        
        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 20, 0xFFFFFFFF);
        
        int y = START_Y + CONTROL_HEIGHT + ROW_SPACING;
        int civHeaderY = y;
        
        context.fill(MARGIN_LEFT, civHeaderY, MARGIN_LEFT + panelWidth, civHeaderY + HEADER_HEIGHT, HEADER_COLOR);
        context.drawBorder(MARGIN_LEFT, civHeaderY, panelWidth, HEADER_HEIGHT, PANEL_BORDER_COLOR);
        context.drawText(textRenderer, "§eGlobal Civilizations", MARGIN_LEFT + 8, civHeaderY + 10, 0xFFFFFFFF, false);
        
        y += 120;
        context.drawText(textRenderer, "§7Civilizations are shared across all channels.", 
            MARGIN_LEFT + 8, y, 0xFF888888, false);
        y += 12;
        context.drawText(textRenderer, "§7Auto Map Refresh: Periodically cycles maps to force server updates (every 60s)", 
            MARGIN_LEFT + 8, y, 0xFF888888, false);
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
