package xyz.nim.telegraph.client.trade;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.carnite.CarniteComposerScreen;

import java.time.format.DateTimeFormatter;

public class TradesDashboardScreen extends Screen {
    private static final int PANEL_MARGIN = 10;
    private static final int PANEL_PADDING = 8;
    private static final int HEADER_HEIGHT = 24;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PANEL_COLOR = 0xA0000000;
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final int HEADER_COLOR = 0xFF222222;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    private final Screen parent;
    private final TelegraphChannel channel;
    private final TradeManager tradeManager;
    
    private TradeListWidget tradeList;
    private TextFieldWidget searchField;
    private ButtonWidget refreshButton;
    private ButtonWidget newTradeButton;
    private ButtonWidget backButton;
    private ButtonWidget allTradesButton;
    private ButtonWidget myTradesButton;
    private ButtonWidget incomingButton;
    private ButtonWidget openStatusButton;
    private ButtonWidget acceptedStatusButton;
    private ButtonWidget allStatusButton;
    
    private TradeOffer selectedTrade;
    private TradeManager.TradeFilter currentFilter;
    private FilterMode filterMode = FilterMode.ALL;
    private StatusFilter statusFilter = StatusFilter.ALL;
    
    private enum FilterMode {
        ALL("All Trades"),
        MY_TRADES("My Trades"),
        INCOMING("Incoming");
        
        final String label;
        FilterMode(String label) { this.label = label; }
    }
    
    private enum StatusFilter {
        ALL("All Status"),
        OPEN("Open Only"),
        ACCEPTED("Accepted");
        
        final String label;
        StatusFilter(String label) { this.label = label; }
    }
    
    public TradesDashboardScreen(Screen parent, TelegraphChannel channel) {
        super(Text.literal("Trades Dashboard"));
        this.parent = parent;
        this.channel = channel;
        this.tradeManager = new TradeManager(channel);
        this.currentFilter = new TradeManager.TradeFilter();
    }
    
    @Override
    protected void init() {
        super.init();
        
        int topBarY = PANEL_MARGIN;
        int topBarHeight = BUTTON_HEIGHT + PANEL_PADDING * 2;
        
        backButton = ButtonWidget.builder(Text.literal("← Back"), button -> {
            if (client != null) {
                client.setScreen(parent);
            }
        }).dimensions(PANEL_MARGIN + PANEL_PADDING, topBarY + PANEL_PADDING, 60, BUTTON_HEIGHT).build();
        addDrawableChild(backButton);
        
        refreshButton = ButtonWidget.builder(Text.literal("↻ Refresh"), button -> {
            refreshTrades();
        }).dimensions(PANEL_MARGIN + PANEL_PADDING + 65, topBarY + PANEL_PADDING, 70, BUTTON_HEIGHT).build();
        addDrawableChild(refreshButton);
        
        newTradeButton = ButtonWidget.builder(Text.literal("+ New Trade"), button -> {
            openNewTradeComposer();
        }).dimensions(PANEL_MARGIN + PANEL_PADDING + 140, topBarY + PANEL_PADDING, 90, BUTTON_HEIGHT).build();
        addDrawableChild(newTradeButton);
        
        int searchWidth = 150;
        searchField = new TextFieldWidget(textRenderer, width - PANEL_MARGIN - PANEL_PADDING - searchWidth, 
            topBarY + PANEL_PADDING, searchWidth, BUTTON_HEIGHT, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search trades..."));
        searchField.setChangedListener(text -> {
            currentFilter.setSearchText(text);
            updateTradeList();
        });
        addDrawableChild(searchField);
        
        int filterBarY = topBarY + topBarHeight + 5;
        int filterBarHeight = BUTTON_HEIGHT + PANEL_PADDING * 2;
        
        int filterX = PANEL_MARGIN + PANEL_PADDING;
        allTradesButton = ButtonWidget.builder(Text.literal("All Trades"), button -> {
            filterMode = FilterMode.ALL;
            updateFilterButtons();
            applyFilters();
        }).dimensions(filterX, filterBarY + PANEL_PADDING, 80, BUTTON_HEIGHT).build();
        addDrawableChild(allTradesButton);
        
        filterX += 85;
        myTradesButton = ButtonWidget.builder(Text.literal("My Trades"), button -> {
            filterMode = FilterMode.MY_TRADES;
            updateFilterButtons();
            applyFilters();
        }).dimensions(filterX, filterBarY + PANEL_PADDING, 80, BUTTON_HEIGHT).build();
        addDrawableChild(myTradesButton);
        
        filterX += 85;
        incomingButton = ButtonWidget.builder(Text.literal("Incoming"), button -> {
            filterMode = FilterMode.INCOMING;
            updateFilterButtons();
            applyFilters();
        }).dimensions(filterX, filterBarY + PANEL_PADDING, 80, BUTTON_HEIGHT).build();
        addDrawableChild(incomingButton);
        
        filterX += 100;
        allStatusButton = ButtonWidget.builder(Text.literal("All"), button -> {
            statusFilter = StatusFilter.ALL;
            updateFilterButtons();
            applyFilters();
        }).dimensions(filterX, filterBarY + PANEL_PADDING, 60, BUTTON_HEIGHT).build();
        addDrawableChild(allStatusButton);
        
        filterX += 65;
        openStatusButton = ButtonWidget.builder(Text.literal("Open"), button -> {
            statusFilter = StatusFilter.OPEN;
            updateFilterButtons();
            applyFilters();
        }).dimensions(filterX, filterBarY + PANEL_PADDING, 60, BUTTON_HEIGHT).build();
        addDrawableChild(openStatusButton);
        
        filterX += 65;
        acceptedStatusButton = ButtonWidget.builder(Text.literal("Accepted"), button -> {
            statusFilter = StatusFilter.ACCEPTED;
            updateFilterButtons();
            applyFilters();
        }).dimensions(filterX, filterBarY + PANEL_PADDING, 80, BUTTON_HEIGHT).build();
        addDrawableChild(acceptedStatusButton);
        
        int leftPanelWidth = (int) (width * 0.55);
        int leftPanelX = PANEL_MARGIN;
        int leftPanelY = filterBarY + filterBarHeight + 5;
        int leftPanelHeight = height - leftPanelY - PANEL_MARGIN;
        
        int listY = leftPanelY + PANEL_PADDING + HEADER_HEIGHT;
        int listHeight = leftPanelHeight - PANEL_PADDING * 2 - HEADER_HEIGHT;
        
        tradeList = new TradeListWidget(
            client,
            leftPanelWidth - PANEL_PADDING * 2,
            listHeight,
            listY,
            60
        );
        tradeList.setX(leftPanelX + PANEL_PADDING);
        addDrawableChild(tradeList);
        
        updateFilterButtons();
        refreshTrades();
    }
    
    private void refreshTrades() {
        tradeManager.refreshTrades();
        applyFilters();
    }
    
    private void applyFilters() {
        currentFilter = new TradeManager.TradeFilter();
        
        if (filterMode == FilterMode.MY_TRADES) {
            currentFilter.setMyTradesOnly(true);
        } else if (filterMode == FilterMode.INCOMING) {
            currentFilter.setIncomingOnly(true);
        }
        
        if (statusFilter == StatusFilter.OPEN) {
            currentFilter.setStatus(TradeStatus.OPEN);
        } else if (statusFilter == StatusFilter.ACCEPTED) {
            currentFilter.setStatus(TradeStatus.ACCEPTED);
        }
        
        if (searchField != null && !searchField.getText().isEmpty()) {
            currentFilter.setSearchText(searchField.getText());
        }
        
        updateTradeList();
    }
    
    private void updateTradeList() {
        if (tradeList != null) {
            tradeList.clear();
            for (TradeOffer trade : tradeManager.filterTrades(currentFilter)) {
                tradeList.addEntry(new TradeEntry(trade));
            }
        }
    }
    
    private void updateFilterButtons() {
        if (allTradesButton != null) {
            allTradesButton.active = (filterMode != FilterMode.ALL);
        }
        if (myTradesButton != null) {
            myTradesButton.active = (filterMode != FilterMode.MY_TRADES);
        }
        if (incomingButton != null) {
            incomingButton.active = (filterMode != FilterMode.INCOMING);
        }
        if (allStatusButton != null) {
            allStatusButton.active = (statusFilter != StatusFilter.ALL);
        }
        if (openStatusButton != null) {
            openStatusButton.active = (statusFilter != StatusFilter.OPEN);
        }
        if (acceptedStatusButton != null) {
            acceptedStatusButton.active = (statusFilter != StatusFilter.ACCEPTED);
        }
    }
    
    private void openNewTradeComposer() {
        if (client == null) return;
        
        int firstChannelId = channel.getAllChannels().keySet().stream().findFirst().orElse(-1);
        if (firstChannelId == -1) return;
        
        ChannelSettings settings = channel.getOrCreateSettings(firstChannelId);
        client.setScreen(new TradeComposerScreen(this, channel, firstChannelId, settings));
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int topBarY = PANEL_MARGIN;
        int topBarHeight = BUTTON_HEIGHT + PANEL_PADDING * 2;
        context.fill(PANEL_MARGIN, topBarY, width - PANEL_MARGIN, topBarY + topBarHeight, PANEL_COLOR);
        context.drawBorder(PANEL_MARGIN, topBarY, width - PANEL_MARGIN * 2, topBarHeight, PANEL_BORDER_COLOR);
        
        int filterBarY = topBarY + topBarHeight + 5;
        int filterBarHeight = BUTTON_HEIGHT + PANEL_PADDING * 2;
        context.fill(PANEL_MARGIN, filterBarY, width - PANEL_MARGIN, filterBarY + filterBarHeight, PANEL_COLOR);
        context.drawBorder(PANEL_MARGIN, filterBarY, width - PANEL_MARGIN * 2, filterBarHeight, PANEL_BORDER_COLOR);
        
        int leftPanelWidth = (int) (width * 0.55);
        int leftPanelX = PANEL_MARGIN;
        int leftPanelY = filterBarY + filterBarHeight + 5;
        int leftPanelHeight = height - leftPanelY - PANEL_MARGIN;
        
        context.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelWidth, leftPanelY + leftPanelHeight, PANEL_COLOR);
        context.drawBorder(leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight, PANEL_BORDER_COLOR);
        
        context.fill(leftPanelX + PANEL_PADDING, leftPanelY + PANEL_PADDING, leftPanelX + leftPanelWidth - PANEL_PADDING, leftPanelY + PANEL_PADDING + HEADER_HEIGHT, HEADER_COLOR);
        context.drawText(textRenderer, "Trade Offers", leftPanelX + PANEL_PADDING * 2, 
            leftPanelY + PANEL_PADDING * 2 + 2, 0xFFFFFFFF, false);
        
        int rightPanelX = leftPanelX + leftPanelWidth + PANEL_MARGIN;
        int rightPanelWidth = width - rightPanelX - PANEL_MARGIN;
        
        context.fill(rightPanelX, leftPanelY, rightPanelX + rightPanelWidth, leftPanelY + leftPanelHeight, PANEL_COLOR);
        context.drawBorder(rightPanelX, leftPanelY, rightPanelWidth, leftPanelHeight, PANEL_BORDER_COLOR);
        
        context.fill(rightPanelX + PANEL_PADDING, leftPanelY + PANEL_PADDING, rightPanelX + rightPanelWidth - PANEL_PADDING, leftPanelY + PANEL_PADDING + HEADER_HEIGHT, HEADER_COLOR);
        context.drawText(textRenderer, "Trade Details", rightPanelX + PANEL_PADDING * 2, 
            leftPanelY + PANEL_PADDING * 2 + 2, 0xFFFFFFFF, false);
        
        super.render(context, mouseX, mouseY, delta);
        
        if (selectedTrade != null) {
            renderTradeDetails(context, rightPanelX, leftPanelY, rightPanelWidth, leftPanelHeight);
        } else {
            int centerY = leftPanelY + leftPanelHeight / 2;
            context.drawCenteredTextWithShadow(textRenderer, "Select a trade to view details", 
                rightPanelX + rightPanelWidth / 2, centerY, 0xFF888888);
        }
    }
    
    private ButtonWidget acceptButton;
    private ButtonWidget declineButton;
    private ButtonWidget counterButton;
    private ButtonWidget markOpenButton;
    
    private void renderTradeDetails(DrawContext context, int x, int y, int width, int height) {
        int contentX = x + PANEL_PADDING;
        int contentY = y + HEADER_HEIGHT + PANEL_PADDING;
        int contentWidth = width - PANEL_PADDING * 2;
        
        if (acceptButton != null) {
            remove(acceptButton);
            remove(declineButton);
            remove(counterButton);
            remove(markOpenButton);
        }
        
        context.drawText(textRenderer, "§eChannel: §f" + selectedTrade.getChannelName(), 
            contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        context.drawText(textRenderer, "§eFrom: §f" + selectedTrade.getFromCiv(), 
            contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        context.drawText(textRenderer, "§eTo: §f" + selectedTrade.getToCiv(), 
            contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        context.drawText(textRenderer, "§eTime: §f" + selectedTrade.getTimestamp().format(TIME_FORMATTER), 
            contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        int statusColor = selectedTrade.getStatus().getColor();
        String statusText = selectedTrade.getStatus().getDisplayName();
        context.drawText(textRenderer, "§eStatus: ", contentX, contentY, 0xFFFFFFFF, false);
        context.drawText(textRenderer, statusText, contentX + textRenderer.getWidth("§eStatus: "), contentY, statusColor, false);
        contentY += 16;
        
        context.drawText(textRenderer, "§6Offering:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        if (selectedTrade.getOfferingItems().isEmpty()) {
            context.drawText(textRenderer, "  §7" + selectedTrade.getOfferingRaw(), 
                contentX + 10, contentY, 0xFFFFFFFF, false);
            contentY += 12;
        } else {
            for (TradeItem item : selectedTrade.getOfferingItems()) {
                context.drawText(textRenderer, "  §f• " + item.toString(), 
                    contentX + 10, contentY, 0xFFFFFFFF, false);
                contentY += 12;
            }
        }
        
        contentY += 6;
        context.drawText(textRenderer, "§6Requesting:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        
        if (selectedTrade.getRequestingItems().isEmpty()) {
            String requesting = selectedTrade.getRequestingRaw().isEmpty() ? 
                "Anything / Open to offers" : selectedTrade.getRequestingRaw();
            context.drawText(textRenderer, "  §7" + requesting, 
                contentX + 10, contentY, 0xFFFFFFFF, false);
            contentY += 12;
        } else {
            for (TradeItem item : selectedTrade.getRequestingItems()) {
                context.drawText(textRenderer, "  §f• " + item.toString(), 
                    contentX + 10, contentY, 0xFFFFFFFF, false);
                contentY += 12;
            }
        }
        
        contentY += 10;
        context.drawText(textRenderer, "§eOriginal Message:", contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;
        context.drawText(textRenderer, "§7" + selectedTrade.getOriginalMessage(), 
            contentX + 10, contentY, 0xFFFFFFFF, false);
        contentY += 16;
        
        int buttonY = contentY + 10;
        int buttonWidth = 100;
        int buttonSpacing = 10;
        
        acceptButton = ButtonWidget.builder(Text.literal("✓ Accept"), button -> {
            selectedTrade.setStatus(TradeStatus.ACCEPTED);
            tradeManager.saveTradeStatuses();
            updateTradeList();
            respondToTrade("^acpt");
        }).dimensions(contentX, buttonY, buttonWidth, BUTTON_HEIGHT).build();
        addDrawableChild(acceptButton);
        
        declineButton = ButtonWidget.builder(Text.literal("✗ Decline"), button -> {
            selectedTrade.setStatus(TradeStatus.DECLINED);
            tradeManager.saveTradeStatuses();
            updateTradeList();
            respondToTrade("^-acpt");
        }).dimensions(contentX + buttonWidth + buttonSpacing, buttonY, buttonWidth, BUTTON_HEIGHT).build();
        addDrawableChild(declineButton);
        
        counterButton = ButtonWidget.builder(Text.literal("↔ Counter"), button -> {
            selectedTrade.setStatus(TradeStatus.COUNTER);
            tradeManager.saveTradeStatuses();
            updateTradeList();
            openCounterOffer();
        }).dimensions(contentX, buttonY + BUTTON_HEIGHT + 5, buttonWidth * 2 + buttonSpacing, BUTTON_HEIGHT).build();
        addDrawableChild(counterButton);
        
        markOpenButton = ButtonWidget.builder(Text.literal("⟲ Mark as Open"), button -> {
            selectedTrade.setStatus(TradeStatus.OPEN);
            tradeManager.saveTradeStatuses();
            updateTradeList();
        }).dimensions(contentX, buttonY + BUTTON_HEIGHT * 2 + 10, buttonWidth * 2 + buttonSpacing, BUTTON_HEIGHT).build();
        addDrawableChild(markOpenButton);
    }
    
    private void respondToTrade(String response) {
        if (client == null || selectedTrade == null) return;
        
        ChannelSettings settings = channel.getOrCreateSettings(selectedTrade.getChannelId());
        client.setScreen(new CarniteComposerScreen(this, channel, selectedTrade.getChannelId(), settings, response, "white"));
    }
    
    private void openCounterOffer() {
        if (client == null || selectedTrade == null) return;
        
        ChannelSettings settings = channel.getOrCreateSettings(selectedTrade.getChannelId());
        String counterTemplate = "; _:";
        client.setScreen(new CarniteComposerScreen(this, channel, selectedTrade.getChannelId(), settings, counterTemplate, "yellow"));
    }
    
    private class TradeListWidget extends AlwaysSelectedEntryListWidget<TradeEntry> {
        public TradeListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
        
        public void clear() {
            clearEntries();
        }
        
        public int addEntry(TradeEntry entry) {
            return super.addEntry(entry);
        }
        
        @Override
        public int getRowWidth() {
            return width - 20;
        }
        
        @Override
        protected int getScrollbarX() {
            return getX() + width - 6;
        }
    }
    
    private class TradeEntry extends AlwaysSelectedEntryListWidget.Entry<TradeEntry> {
        private final TradeOffer trade;
        
        public TradeEntry(TradeOffer trade) {
            this.trade = trade;
        }
        
        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, 
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {
            
            int statusColor = trade.getStatus().getColor();
            String statusText = trade.getStatus().getDisplayName();
            
            context.drawText(textRenderer, trade.getChannelName(), x + 5, y + 2, 0xFFFFFFFF, false);
            context.drawText(textRenderer, statusText, x + 5, y + 13, statusColor, false);
            
            String offeringPreview = trade.getOfferingRaw();
            if (offeringPreview.length() > 20) {
                offeringPreview = offeringPreview.substring(0, 20) + "...";
            }
            context.drawText(textRenderer, "§6→ §f" + offeringPreview, x + 5, y + 26, 0xFFFFFFFF, false);
            
            String requestingPreview = trade.getRequestingRaw();
            if (requestingPreview.isEmpty()) {
                requestingPreview = "?";
            } else if (requestingPreview.length() > 20) {
                requestingPreview = requestingPreview.substring(0, 20) + "...";
            }
            context.drawText(textRenderer, "§6← §f" + requestingPreview, x + 5, y + 38, 0xFFFFFFFF, false);
            
            String timeStr = trade.getTimestamp().format(TIME_FORMATTER);
            context.drawText(textRenderer, timeStr, x + entryWidth - 40, y + 2, 0xFF888888, false);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectedTrade = trade;
            return true;
        }
        
        @Override
        public Text getNarration() {
            return Text.literal("Trade: " + trade.getOfferingRaw());
        }
    }
}