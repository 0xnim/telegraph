package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ui.DropdownWidget;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ChannelBrowserScreen extends Screen {
    private static final int PANEL_MARGIN = 10;
    private static final int PANEL_PADDING = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 24;
    private static final int PANEL_COLOR = 0xA0000000;
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final Screen parent;
    private final TelegraphChannel channel;
    private final ChannelFilter filter;

    private TextFieldWidget searchField;
    private DropdownWidget sortDropdown;
    private ChannelBrowserListWidget channelList;
    private ButtonWidget backButton;
    private ButtonWidget showArchivedButton;

    private final Map<ChannelCategory, ButtonWidget> categoryButtons = new EnumMap<>(ChannelCategory.class);
    private int selectedMapId = -1;

    public ChannelBrowserScreen(Screen parent, TelegraphChannel channel) {
        super(Text.literal("Channel Browser"));
        this.parent = parent;
        this.channel = channel;
        this.filter = new ChannelFilter();
    }

    @Override
    protected void init() {
        super.init();

        int topBarY = PANEL_MARGIN;
        int topBarHeight = BUTTON_HEIGHT + PANEL_PADDING * 2;

        backButton = ButtonWidget.builder(Text.literal("<- Back"), button -> {
            if (client != null) client.setScreen(parent);
        }).dimensions(PANEL_MARGIN + PANEL_PADDING, topBarY + PANEL_PADDING, 60, BUTTON_HEIGHT).build();
        addDrawableChild(backButton);

        int searchWidth = Math.min(200, width / 4);
        searchField = new TextFieldWidget(textRenderer,
            PANEL_MARGIN + PANEL_PADDING + 70, topBarY + PANEL_PADDING,
            searchWidth, BUTTON_HEIGHT, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Search channels or tags..."));
        searchField.setMaxLength(64);
        searchField.setChangedListener(text -> {
            filter.setSearchText(text);
            updateChannelList();
        });
        addDrawableChild(searchField);

        List<DropdownWidget.DropdownOption> sortOptions = Arrays.stream(ChannelSortOption.values())
            .map(opt -> new DropdownWidget.DropdownOption(opt.name(), opt.getLabel()))
            .collect(Collectors.toList());
        sortDropdown = new DropdownWidget(client,
            PANEL_MARGIN + PANEL_PADDING + 80 + searchWidth,
            topBarY + PANEL_PADDING,
            140, BUTTON_HEIGHT, sortOptions,
            value -> {
                filter.setSortOption(ChannelSortOption.valueOf(value));
                updateChannelList();
            });
        addDrawableChild(sortDropdown.getButton());

        showArchivedButton = ButtonWidget.builder(
            Text.literal(filter.isIncludeArchived() ? "[X] Archived" : "[ ] Archived"),
            button -> {
                filter.setIncludeArchived(!filter.isIncludeArchived());
                button.setMessage(Text.literal(filter.isIncludeArchived() ? "[X] Archived" : "[ ] Archived"));
                updateChannelList();
            }
        ).dimensions(width - PANEL_MARGIN - PANEL_PADDING - 90, topBarY + PANEL_PADDING, 90, BUTTON_HEIGHT).build();
        addDrawableChild(showArchivedButton);

        int filterBarY = topBarY + topBarHeight + 5;
        int filterBarHeight = BUTTON_HEIGHT + PANEL_PADDING * 2;

        int filterX = PANEL_MARGIN + PANEL_PADDING;
        int buttonWidth = 70;
        int buttonSpacing = 4;

        addCategoryButton(ChannelCategory.ALL, filterX, filterBarY + PANEL_PADDING, 40);
        filterX += 40 + buttonSpacing;
        addCategoryButton(ChannelCategory.CARNITE, filterX, filterBarY + PANEL_PADDING, buttonWidth);
        filterX += buttonWidth + buttonSpacing;
        addCategoryButton(ChannelCategory.TELEGRAPH, filterX, filterBarY + PANEL_PADDING, buttonWidth);
        filterX += buttonWidth + 15;
        addCategoryButton(ChannelCategory.ACTIVE, filterX, filterBarY + PANEL_PADDING, 55);
        filterX += 55 + buttonSpacing;
        addCategoryButton(ChannelCategory.INACTIVE, filterX, filterBarY + PANEL_PADDING, 60);
        filterX += 60 + buttonSpacing;
        addCategoryButton(ChannelCategory.NEW, filterX, filterBarY + PANEL_PADDING, 40);

        int contentY = filterBarY + filterBarHeight + 5;
        int leftPanelWidth = (int) (width * 0.55);
        int contentHeight = height - contentY - PANEL_MARGIN;

        int listY = contentY + PANEL_PADDING + HEADER_HEIGHT;
        int listHeight = contentHeight - PANEL_PADDING * 2 - HEADER_HEIGHT;

        channelList = new ChannelBrowserListWidget(
            client,
            leftPanelWidth - PANEL_PADDING * 2,
            listHeight,
            listY,
            50
        );
        channelList.setX(PANEL_MARGIN + PANEL_PADDING);
        addDrawableChild(channelList);

        updateCategoryButtons();
        updateChannelList();
    }

    private void addCategoryButton(ChannelCategory category, int x, int y, int btnWidth) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(category.getLabel()), btn -> {
            if (category == ChannelCategory.ALL) {
                filter.clearCategories();
            } else {
                filter.toggleCategory(category);
            }
            updateCategoryButtons();
            updateChannelList();
        }).dimensions(x, y, btnWidth, BUTTON_HEIGHT).build();
        categoryButtons.put(category, button);
        addDrawableChild(button);
    }

    private void updateCategoryButtons() {
        Set<ChannelCategory> active = filter.getActiveCategories();
        for (Map.Entry<ChannelCategory, ButtonWidget> entry : categoryButtons.entrySet()) {
            boolean isActive = active.contains(entry.getKey()) ||
                (entry.getKey() == ChannelCategory.ALL && active.isEmpty());
            entry.getValue().active = !isActive;
        }
    }

    private void updateChannelList() {
        if (channelList == null) return;
        channelList.clearEntries();

        List<Integer> filteredIds = filter.apply(channel);

        for (int mapId : filteredIds) {
            TelegraphChannel.ChannelMetadata metadata = channel.getMetadata(mapId);
            channelList.addChannelEntry(new ChannelBrowserEntry(metadata));
        }
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

        int contentY = filterBarY + filterBarHeight + 5;
        int leftPanelWidth = (int) (width * 0.55);
        int contentHeight = height - contentY - PANEL_MARGIN;

        context.fill(PANEL_MARGIN, contentY, PANEL_MARGIN + leftPanelWidth, contentY + contentHeight, PANEL_COLOR);
        context.drawBorder(PANEL_MARGIN, contentY, leftPanelWidth, contentHeight, PANEL_BORDER_COLOR);
        context.drawText(textRenderer, "Channels (" + channelList.children().size() + ")",
            PANEL_MARGIN + PANEL_PADDING, contentY + PANEL_PADDING + 4, 0xFFFFFFFF, false);

        int rightPanelX = PANEL_MARGIN + leftPanelWidth + PANEL_MARGIN;
        int rightPanelWidth = width - rightPanelX - PANEL_MARGIN;
        context.fill(rightPanelX, contentY, rightPanelX + rightPanelWidth, contentY + contentHeight, PANEL_COLOR);
        context.drawBorder(rightPanelX, contentY, rightPanelWidth, contentHeight, PANEL_BORDER_COLOR);
        context.drawText(textRenderer, "Channel Details", rightPanelX + PANEL_PADDING, contentY + PANEL_PADDING + 4, 0xFFFFFFFF, false);

        if (selectedMapId != -1) {
            renderChannelDetails(context, rightPanelX, contentY, rightPanelWidth, contentHeight);
        } else {
            context.drawCenteredTextWithShadow(textRenderer, "Select a channel to view details",
                rightPanelX + rightPanelWidth / 2, contentY + contentHeight / 2, 0xFF888888);
        }

        super.render(context, mouseX, mouseY, delta);

        sortDropdown.render(context, mouseX, mouseY, delta);
    }

    private void renderChannelDetails(DrawContext context, int x, int y, int panelWidth, int panelHeight) {
        TelegraphChannel.ChannelMetadata metadata = channel.getMetadata(selectedMapId);
        if (metadata == null) return;

        int contentX = x + PANEL_PADDING;
        int contentY = y + HEADER_HEIGHT + PANEL_PADDING;

        context.drawText(textRenderer, "Name: " + metadata.displayName(), contentX, contentY, 0xFFFFFF00, false);
        contentY += 14;

        context.drawText(textRenderer, "Map ID: " + metadata.mapId(), contentX, contentY, 0xFFAAAAAA, false);
        contentY += 12;

        String protocolColor = metadata.protocolName().equals("Carnite") ? "§6" : "§b";
        context.drawText(textRenderer, "Protocol: " + protocolColor + metadata.protocolName(), contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;

        context.drawText(textRenderer, "Messages: " + metadata.messageCount(), contentX, contentY, 0xFFAAAAAA, false);
        contentY += 12;

        if (metadata.lastActivity() != null) {
            String timeStr = metadata.lastActivity().atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
            String timeAgo = getTimeAgo(metadata.lastActivity());
            context.drawText(textRenderer, "Last Active: " + timeStr + " (" + timeAgo + ")", contentX, contentY, 0xFFAAAAAA, false);
        } else {
            context.drawText(textRenderer, "Last Active: Never", contentX, contentY, 0xFFAAAAAA, false);
        }
        contentY += 12;

        String statusText = metadata.archived() ? "§cArchived" : "§aActive";
        context.drawText(textRenderer, "Status: " + statusText, contentX, contentY, 0xFFFFFFFF, false);
        contentY += 12;

        context.drawText(textRenderer, "Notifications: " + metadata.notificationLevel().name(),
            contentX, contentY, 0xFFAAAAAA, false);
        contentY += 14;

        if (!metadata.tags().isEmpty()) {
            context.drawText(textRenderer, "Tags:", contentX, contentY, 0xFFAAAAAA, false);
            contentY += 12;
            for (String tag : metadata.tags()) {
                context.drawText(textRenderer, "  - " + tag, contentX, contentY, 0xFF888888, false);
                contentY += 10;
            }
        }
    }

    private String getTimeAgo(Instant instant) {
        Duration duration = Duration.between(instant, Instant.now());
        if (duration.toMinutes() < 1) return "just now";
        if (duration.toMinutes() < 60) return duration.toMinutes() + "m ago";
        if (duration.toHours() < 24) return duration.toHours() + "h ago";
        if (duration.toDays() < 7) return duration.toDays() + "d ago";
        return instant.atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (sortDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (sortDropdown.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class ChannelBrowserListWidget extends AlwaysSelectedEntryListWidget<ChannelBrowserEntry> {
        public ChannelBrowserListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }

        public void clearEntries() {
            this.children().clear();
        }

        public void addChannelEntry(ChannelBrowserEntry entry) {
            this.addEntry(entry);
        }

        @Override
        public int getRowWidth() {
            return this.width - 12;
        }

        @Override
        protected int getScrollbarX() {
            return this.getX() + this.width - 6;
        }
    }

    private class ChannelBrowserEntry extends AlwaysSelectedEntryListWidget.Entry<ChannelBrowserEntry> {
        private final TelegraphChannel.ChannelMetadata metadata;

        public ChannelBrowserEntry(TelegraphChannel.ChannelMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth,
                          int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean selected = selectedMapId == metadata.mapId();

            int bgColor = selected ? 0x60FFFFFF : (hovered ? 0x40FFFFFF : 0x20000000);
            context.fill(x, y, x + entryWidth, y + entryHeight, bgColor);

            int nameColor = selected ? 0xFFFFFF00 : 0xFFFFFFFF;
            context.drawText(client.textRenderer, metadata.displayName(), x + 5, y + 3, nameColor, false);

            String protocolBadge = metadata.protocolName().equals("Carnite") ? "§6[C]" : "§b[T]";
            context.drawText(client.textRenderer, protocolBadge, x + entryWidth - 25, y + 3, 0xFFFFFFFF, false);

            String infoLine = "§7Map " + metadata.mapId() + " - " + metadata.messageCount() + " msgs";
            context.drawText(client.textRenderer, infoLine, x + 5, y + 15, 0xFFAAAAAA, false);

            if (metadata.lastActivity() != null) {
                String timeAgo = getTimeAgo(metadata.lastActivity());
                context.drawText(client.textRenderer, "§7" + timeAgo, x + 5, y + 27, 0xFF888888, false);
            }

            if (metadata.lastActivity() != null) {
                boolean isActive = metadata.lastActivity().isAfter(Instant.now().minus(Duration.ofHours(24)));
                int dotColor = isActive ? 0xFF55FF55 : 0xFF555555;
                context.fill(x + entryWidth - 8, y + entryHeight - 8, x + entryWidth - 4, y + entryHeight - 4, dotColor);
            }

            if (metadata.archived()) {
                context.drawText(client.textRenderer, "§c[A]", x + entryWidth - 45, y + 3, 0xFFFFFFFF, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectedMapId = metadata.mapId();
            return true;
        }

        @Override
        public Text getNarration() {
            return Text.literal(metadata.displayName());
        }
    }
}
