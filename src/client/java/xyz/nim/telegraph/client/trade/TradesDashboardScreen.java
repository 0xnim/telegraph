package xyz.nim.telegraph.client.trade;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ChannelSettings;
import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.carnite.CarniteComposerScreen;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TelegraphListWidget;
import xyz.nim.telegraph.client.ui.components.TextFields;

import java.time.format.DateTimeFormatter;

public class TradesDashboardScreen extends TelegraphScreen {
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

    private ButtonWidget acceptButton;
    private ButtonWidget declineButton;
    private ButtonWidget counterButton;
    private ButtonWidget markOpenButton;

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

        int topBarY = layout.margin;
        int topBarHeight = layout.buttonHeight + layout.padding * 2;

        // Top bar buttons
        backButton = Buttons.back(layout.margin + layout.padding, topBarY + layout.padding, layout, button -> close());
        addDrawableChild(backButton);

        int btnX = backButton.getX() + backButton.getWidth() + layout.spacing;
        refreshButton = Buttons.create(Text.literal("\u21BB Refresh"), btnX, topBarY + layout.padding,
                layout.smallButtonWidth + 20, layout, button -> refreshTrades());
        addDrawableChild(refreshButton);

        btnX += refreshButton.getWidth() + layout.spacing;
        newTradeButton = Buttons.create(Text.literal("+ New Trade"), btnX, topBarY + layout.padding,
                layout.buttonWidth, layout, button -> openNewTradeComposer());
        addDrawableChild(newTradeButton);

        int searchWidth = Math.min(150, layout.contentWidth() / 5);
        searchField = TextFields.search(textRenderer, width - layout.margin - layout.padding - searchWidth,
                topBarY + layout.padding, searchWidth, layout);
        searchField.setChangedListener(text -> {
            currentFilter.setSearchText(text);
            updateTradeList();
        });
        addDrawableChild(searchField);

        // Filter bar
        int filterBarY = topBarY + topBarHeight + layout.spacing;
        int filterBarHeight = layout.buttonHeight + layout.padding * 2;
        int filterButtonWidth = Math.min(80, layout.contentWidth() / 8);
        int statusButtonWidth = Math.min(60, layout.contentWidth() / 10);

        int filterX = layout.margin + layout.padding;
        allTradesButton = Buttons.toggle(Text.literal("All Trades"), filterX, filterBarY + layout.padding,
                filterButtonWidth, layout.buttonHeight, filterMode != FilterMode.ALL, button -> {
                    filterMode = FilterMode.ALL;
                    updateFilterButtons();
                    applyFilters();
                });
        addDrawableChild(allTradesButton);

        filterX += filterButtonWidth + layout.spacing;
        myTradesButton = Buttons.toggle(Text.literal("My Trades"), filterX, filterBarY + layout.padding,
                filterButtonWidth, layout.buttonHeight, filterMode != FilterMode.MY_TRADES, button -> {
                    filterMode = FilterMode.MY_TRADES;
                    updateFilterButtons();
                    applyFilters();
                });
        addDrawableChild(myTradesButton);

        filterX += filterButtonWidth + layout.spacing;
        incomingButton = Buttons.toggle(Text.literal("Incoming"), filterX, filterBarY + layout.padding,
                filterButtonWidth, layout.buttonHeight, filterMode != FilterMode.INCOMING, button -> {
                    filterMode = FilterMode.INCOMING;
                    updateFilterButtons();
                    applyFilters();
                });
        addDrawableChild(incomingButton);

        filterX += filterButtonWidth + layout.spacing * 3;
        allStatusButton = Buttons.toggle(Text.literal("All"), filterX, filterBarY + layout.padding,
                statusButtonWidth, layout.buttonHeight, statusFilter != StatusFilter.ALL, button -> {
                    statusFilter = StatusFilter.ALL;
                    updateFilterButtons();
                    applyFilters();
                });
        addDrawableChild(allStatusButton);

        filterX += statusButtonWidth + layout.spacing;
        openStatusButton = Buttons.toggle(Text.literal("Open"), filterX, filterBarY + layout.padding,
                statusButtonWidth, layout.buttonHeight, statusFilter != StatusFilter.OPEN, button -> {
                    statusFilter = StatusFilter.OPEN;
                    updateFilterButtons();
                    applyFilters();
                });
        addDrawableChild(openStatusButton);

        filterX += statusButtonWidth + layout.spacing;
        acceptedStatusButton = Buttons.toggle(Text.literal("Accepted"), filterX, filterBarY + layout.padding,
                statusButtonWidth + 20, layout.buttonHeight, statusFilter != StatusFilter.ACCEPTED, button -> {
                    statusFilter = StatusFilter.ACCEPTED;
                    updateFilterButtons();
                    applyFilters();
                });
        addDrawableChild(acceptedStatusButton);

        // Two-panel layout
        var split = layout.split(0.55f);
        int leftPanelY = filterBarY + filterBarHeight + layout.spacing;
        int leftPanelHeight = height - leftPanelY - layout.margin;

        int listY = leftPanelY + layout.padding + layout.headerHeight;
        int listHeight = leftPanelHeight - layout.padding * 2 - layout.headerHeight;

        tradeList = new TradeListWidget(client, split.leftWidth() - layout.padding * 2, listHeight, listY, 60);
        tradeList.setX(split.x() + layout.padding);
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
            tradeList.clearEntries();
            for (TradeOffer trade : tradeManager.filterTrades(currentFilter)) {
                tradeList.addEntryToList(new TradeEntry(trade));
            }
        }
    }

    private void updateFilterButtons() {
        if (allTradesButton != null) allTradesButton.active = (filterMode != FilterMode.ALL);
        if (myTradesButton != null) myTradesButton.active = (filterMode != FilterMode.MY_TRADES);
        if (incomingButton != null) incomingButton.active = (filterMode != FilterMode.INCOMING);
        if (allStatusButton != null) allStatusButton.active = (statusFilter != StatusFilter.ALL);
        if (openStatusButton != null) openStatusButton.active = (statusFilter != StatusFilter.OPEN);
        if (acceptedStatusButton != null) acceptedStatusButton.active = (statusFilter != StatusFilter.ACCEPTED);
    }

    private void openNewTradeComposer() {
        if (client == null) return;

        int firstChannelId = channel.getAllChannels().keySet().stream().findFirst().orElse(-1);
        if (firstChannelId == -1) return;

        ChannelSettings settings = channel.getOrCreateSettings(firstChannelId);
        client.setScreen(new TradeComposerScreen(this, channel, firstChannelId, settings));
    }

    @Override
    protected void renderPanels(DrawContext context, int mouseX, int mouseY, float delta) {
        int topBarY = layout.margin;
        int topBarHeight = layout.buttonHeight + layout.padding * 2;
        drawPanel(context, layout.margin, topBarY, layout.contentWidth(), topBarHeight);

        int filterBarY = topBarY + topBarHeight + layout.spacing;
        int filterBarHeight = layout.buttonHeight + layout.padding * 2;
        drawPanel(context, layout.margin, filterBarY, layout.contentWidth(), filterBarHeight);

        var split = layout.split(0.55f);
        int leftPanelY = filterBarY + filterBarHeight + layout.spacing;
        int leftPanelHeight = height - leftPanelY - layout.margin;

        // Left panel
        drawPanel(context, split.x(), leftPanelY, split.leftWidth(), leftPanelHeight);
        context.fill(split.x() + layout.padding, leftPanelY + layout.padding,
                split.x() + split.leftWidth() - layout.padding, leftPanelY + layout.padding + layout.headerHeight,
                TelegraphTheme.HEADER_BG);
        context.drawText(textRenderer, "Trade Offers", split.x() + layout.padding * 2,
                leftPanelY + layout.padding + (layout.headerHeight - 8) / 2, TelegraphTheme.TEXT_PRIMARY, false);

        // Right panel
        int rightPanelX = split.rightX();
        int rightPanelWidth = split.rightWidth();
        drawPanel(context, rightPanelX, leftPanelY, rightPanelWidth, leftPanelHeight);
        context.fill(rightPanelX + layout.padding, leftPanelY + layout.padding,
                rightPanelX + rightPanelWidth - layout.padding, leftPanelY + layout.padding + layout.headerHeight,
                TelegraphTheme.HEADER_BG);
        context.drawText(textRenderer, "Trade Details", rightPanelX + layout.padding * 2,
                leftPanelY + layout.padding + (layout.headerHeight - 8) / 2, TelegraphTheme.TEXT_PRIMARY, false);
    }

    @Override
    protected void renderOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        int topBarY = layout.margin;
        int topBarHeight = layout.buttonHeight + layout.padding * 2;
        int filterBarY = topBarY + topBarHeight + layout.spacing;
        int filterBarHeight = layout.buttonHeight + layout.padding * 2;
        var split = layout.split(0.55f);
        int leftPanelY = filterBarY + filterBarHeight + layout.spacing;
        int leftPanelHeight = height - leftPanelY - layout.margin;
        int rightPanelX = split.rightX();
        int rightPanelWidth = split.rightWidth();

        if (selectedTrade != null) {
            renderTradeDetails(context, rightPanelX, leftPanelY, rightPanelWidth, leftPanelHeight);
        } else {
            int centerY = leftPanelY + leftPanelHeight / 2;
            context.drawCenteredTextWithShadow(textRenderer, "Select a trade to view details",
                    rightPanelX + rightPanelWidth / 2, centerY, TelegraphTheme.TEXT_MUTED);
        }
    }

    private void renderTradeDetails(DrawContext context, int x, int y, int panelWidth, int panelHeight) {
        int contentX = x + layout.padding;
        int contentY = y + layout.headerHeight + layout.padding * 2;

        // Remove old action buttons
        if (acceptButton != null) {
            remove(acceptButton);
            remove(declineButton);
            remove(counterButton);
            remove(markOpenButton);
        }

        context.drawText(textRenderer, "\u00A7eChannel: \u00A7f" + selectedTrade.getChannelName(),
                contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        context.drawText(textRenderer, "\u00A7eFrom: \u00A7f" + selectedTrade.getFromCiv(),
                contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        context.drawText(textRenderer, "\u00A7eTo: \u00A7f" + selectedTrade.getToCiv(),
                contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        context.drawText(textRenderer, "\u00A7eTime: \u00A7f" + selectedTrade.getTimestamp().format(TIME_FORMATTER),
                contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        int statusColor = selectedTrade.getStatus().getColor();
        String statusText = selectedTrade.getStatus().getDisplayName();
        context.drawText(textRenderer, "\u00A7eStatus: ", contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        context.drawText(textRenderer, statusText, contentX + textRenderer.getWidth("\u00A7eStatus: "), contentY, statusColor, false);
        contentY += 16;

        context.drawText(textRenderer, "\u00A76Offering:", contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        if (selectedTrade.getOfferingItems().isEmpty()) {
            context.drawText(textRenderer, "  \u00A77" + selectedTrade.getOfferingRaw(),
                    contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
            contentY += 12;
        } else {
            for (TradeItem item : selectedTrade.getOfferingItems()) {
                context.drawText(textRenderer, "  \u00A7f\u2022 " + item.toString(),
                        contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
                contentY += 12;
            }
        }

        contentY += 6;
        context.drawText(textRenderer, "\u00A76Requesting:", contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        if (selectedTrade.getRequestingItems().isEmpty()) {
            String requesting = selectedTrade.getRequestingRaw().isEmpty() ?
                    "Anything / Open to offers" : selectedTrade.getRequestingRaw();
            context.drawText(textRenderer, "  \u00A77" + requesting,
                    contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
            contentY += 12;
        } else {
            for (TradeItem item : selectedTrade.getRequestingItems()) {
                context.drawText(textRenderer, "  \u00A7f\u2022 " + item.toString(),
                        contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
                contentY += 12;
            }
        }

        contentY += 10;
        context.drawText(textRenderer, "\u00A7eOriginal Message:", contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;
        context.drawText(textRenderer, "\u00A77" + selectedTrade.getOriginalMessage(),
                contentX + 10, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 16;

        // Action buttons
        int buttonY = contentY + 10;
        int buttonWidth = Math.min(100, (panelWidth - layout.padding * 4) / 2);

        acceptButton = Buttons.create(Text.literal("\u2713 Accept"), contentX, buttonY,
                buttonWidth, layout, button -> {
                    selectedTrade.setStatus(TradeStatus.ACCEPTED);
                    tradeManager.saveTradeStatuses();
                    updateTradeList();
                    respondToTrade("^acpt");
                });
        addDrawableChild(acceptButton);

        declineButton = Buttons.create(Text.literal("\u2717 Decline"), contentX + buttonWidth + layout.spacing, buttonY,
                buttonWidth, layout, button -> {
                    selectedTrade.setStatus(TradeStatus.DECLINED);
                    tradeManager.saveTradeStatuses();
                    updateTradeList();
                    respondToTrade("^-acpt");
                });
        addDrawableChild(declineButton);

        counterButton = Buttons.create(Text.literal("\u2194 Counter"), contentX, buttonY + layout.buttonHeight + layout.spacing,
                buttonWidth * 2 + layout.spacing, layout, button -> {
                    selectedTrade.setStatus(TradeStatus.COUNTER);
                    tradeManager.saveTradeStatuses();
                    updateTradeList();
                    openCounterOffer();
                });
        addDrawableChild(counterButton);

        markOpenButton = Buttons.create(Text.literal("\u27F2 Mark as Open"), contentX,
                buttonY + layout.buttonHeight * 2 + layout.spacing * 2,
                buttonWidth * 2 + layout.spacing, layout, button -> {
                    selectedTrade.setStatus(TradeStatus.OPEN);
                    tradeManager.saveTradeStatuses();
                    updateTradeList();
                });
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

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private class TradeListWidget extends TelegraphListWidget<TradeEntry> {
        public TradeListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
    }

    private class TradeEntry extends TelegraphListWidget.Entry<TradeEntry> {
        private final TradeOffer trade;

        public TradeEntry(TradeOffer trade) {
            this.trade = trade;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {

            renderBackground(context, x, y, entryWidth, entryHeight, hovered, trade == selectedTrade);

            int statusColor = trade.getStatus().getColor();
            String statusText = trade.getStatus().getDisplayName();

            context.drawText(textRenderer, trade.getChannelName(), x + 5, y + 2, TelegraphTheme.TEXT_PRIMARY, false);
            context.drawText(textRenderer, statusText, x + 5, y + 13, statusColor, false);

            String offeringPreview = trade.getOfferingRaw();
            if (offeringPreview.length() > 20) {
                offeringPreview = offeringPreview.substring(0, 20) + "...";
            }
            context.drawText(textRenderer, "\u00A76\u2192 \u00A7f" + offeringPreview, x + 5, y + 26, TelegraphTheme.TEXT_PRIMARY, false);

            String requestingPreview = trade.getRequestingRaw();
            if (requestingPreview.isEmpty()) {
                requestingPreview = "?";
            } else if (requestingPreview.length() > 20) {
                requestingPreview = requestingPreview.substring(0, 20) + "...";
            }
            context.drawText(textRenderer, "\u00A76\u2190 \u00A7f" + requestingPreview, x + 5, y + 38, TelegraphTheme.TEXT_PRIMARY, false);

            String timeStr = trade.getTimestamp().format(TIME_FORMATTER);
            context.drawText(textRenderer, timeStr, x + entryWidth - 40, y + 2, TelegraphTheme.TEXT_MUTED, false);
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
