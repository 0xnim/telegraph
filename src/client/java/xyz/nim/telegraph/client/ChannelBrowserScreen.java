package xyz.nim.telegraph.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import xyz.nim.telegraph.client.ui.DropdownWidget;
import xyz.nim.telegraph.client.ui.KeyboardConstants;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TelegraphListWidget;
import xyz.nim.telegraph.client.ui.components.TextFields;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ChannelBrowserScreen extends TelegraphScreen {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, HH:mm");

    private final Screen parent;
    private final TelegraphChannel channel;
    private final ChannelFilter filter;

    private EditBox searchField;
    private DropdownWidget sortDropdown;
    private ChannelBrowserListWidget channelList;
    private Button backButton;
    private Button showArchivedButton;

    private final Map<ChannelCategory, Button> categoryButtons = new EnumMap<>(ChannelCategory.class);
    private int selectedMapId = -1;

    private Button openChannelButton;
    private Button archiveButton;
    private Button translationsButton;
    private DropdownWidget notificationDropdown;
    private EditBox tagInputField;
    private Button addTagButton;

    public ChannelBrowserScreen(Screen parent, TelegraphChannel channel) {
        super(Component.literal("Channel Browser"));
        this.parent = parent;
        this.channel = channel;
        this.filter = new ChannelFilter();
    }

    @Override
    protected void init() {
        super.init();

        int topBarY = layout.margin;
        int topBarHeight = layout.buttonHeight + layout.padding * 2;

        backButton = Buttons.back(layout.margin + layout.padding, topBarY + layout.padding, layout, button -> onClose());
        addRenderableWidget(backButton);

        int searchWidth = Math.min(200, layout.contentWidth() / 4);
        searchField = TextFields.search(font,
            layout.margin + layout.padding + layout.buttonWidth + layout.spacing, topBarY + layout.padding,
            searchWidth, layout);
        searchField.setHint(Component.literal("Search channels or tags..."));
        searchField.setResponder(text -> {
            filter.setSearchText(text);
            updateChannelList();
        });
        addRenderableWidget(searchField);

        List<DropdownWidget.DropdownOption> sortOptions = Arrays.stream(ChannelSortOption.values())
            .map(opt -> new DropdownWidget.DropdownOption(opt.name(), opt.getLabel()))
            .collect(Collectors.toList());
        sortDropdown = new DropdownWidget(minecraft,
            layout.margin + layout.padding + layout.buttonWidth + searchWidth + layout.spacing * 2,
            topBarY + layout.padding,
            140, layout.buttonHeight, sortOptions,
            value -> {
                filter.setSortOption(ChannelSortOption.valueOf(value));
                updateChannelList();
            });
        addRenderableWidget(sortDropdown.getButton());

        int archivedBtnWidth = Math.min(100, layout.contentWidth() / 6);
        showArchivedButton = Button.builder(
            Component.literal(filter.isIncludeArchived() ? "[X] Archived" : "[ ] Archived"),
            button -> {
                filter.setIncludeArchived(!filter.isIncludeArchived());
                button.setMessage(Component.literal(filter.isIncludeArchived() ? "[X] Archived" : "[ ] Archived"));
                updateChannelList();
            }
        ).bounds(width - layout.margin - layout.padding - archivedBtnWidth, topBarY + layout.padding,
            archivedBtnWidth, layout.buttonHeight).build();
        addRenderableWidget(showArchivedButton);

        int filterBarY = topBarY + topBarHeight + layout.spacing;
        int filterBarHeight = layout.buttonHeight + layout.padding * 2;

        int filterX = layout.margin + layout.padding;
        int buttonSpacing = 4;
        int smallBtnWidth = Math.max(40, layout.contentWidth() / 16);
        int medBtnWidth = Math.max(55, layout.contentWidth() / 12);

        addCategoryButton(ChannelCategory.ALL, filterX, filterBarY + layout.padding, smallBtnWidth);
        filterX += smallBtnWidth + buttonSpacing;
        addCategoryButton(ChannelCategory.CARNITE, filterX, filterBarY + layout.padding, medBtnWidth + 15);
        filterX += medBtnWidth + 15 + buttonSpacing;
        addCategoryButton(ChannelCategory.TELEGRAPH, filterX, filterBarY + layout.padding, medBtnWidth + 15);
        filterX += medBtnWidth + 15 + layout.spacing * 2;
        addCategoryButton(ChannelCategory.ACTIVE, filterX, filterBarY + layout.padding, medBtnWidth);
        filterX += medBtnWidth + buttonSpacing;
        addCategoryButton(ChannelCategory.INACTIVE, filterX, filterBarY + layout.padding, medBtnWidth + 5);
        filterX += medBtnWidth + 5 + buttonSpacing;
        addCategoryButton(ChannelCategory.NEW, filterX, filterBarY + layout.padding, smallBtnWidth);

        int contentY = filterBarY + filterBarHeight + layout.spacing;
        var splitLayout = layout.split(0.55f);
        int contentHeight = height - contentY - layout.margin;

        int listY = contentY + layout.padding + layout.headerHeight;
        int listHeight = contentHeight - layout.padding * 2 - layout.headerHeight;

        channelList = new ChannelBrowserListWidget(
            minecraft,
            splitLayout.leftWidth() - layout.padding * 2,
            listHeight,
            listY,
            50
        );
        channelList.setX(layout.margin + layout.padding);
        addRenderableWidget(channelList);

        int rightPanelX = splitLayout.rightX();
        int rightPanelWidth = splitLayout.rightWidth();
        int settingsY = contentY + contentHeight - layout.padding - layout.buttonHeight;

        int halfButtonWidth = rightPanelWidth / 2 - layout.padding - 2;
        openChannelButton = Buttons.create(Component.literal("Open Channel"),
            rightPanelX + layout.padding, settingsY, halfButtonWidth, layout, button -> {
                if (selectedMapId != -1 && minecraft != null) {
                    minecraft.setScreen(new MapDecorationsScreen(channel, selectedMapId));
                }
            });
        openChannelButton.active = false;
        addRenderableWidget(openChannelButton);

        archiveButton = Buttons.create(Component.literal("Archive"),
            rightPanelX + rightPanelWidth / 2 + 2, settingsY, halfButtonWidth, layout, button -> {
                if (selectedMapId != -1) {
                    showArchiveConfirmation();
                }
            });
        archiveButton.active = false;
        addRenderableWidget(archiveButton);

        settingsY -= layout.buttonHeight + 4;

        translationsButton = Buttons.create(Component.literal("[ ] Translations"),
            rightPanelX + layout.padding, settingsY, rightPanelWidth - layout.padding * 2, layout, button -> {
                if (selectedMapId != -1) {
                    ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
                    settings.setShowTranslations(!settings.isShowTranslations());
                    updateSettingsButtons();
                    toastManager.success("Translations " + (settings.isShowTranslations() ? "enabled" : "disabled"));
                }
            });
        translationsButton.active = false;
        addRenderableWidget(translationsButton);

        settingsY -= layout.buttonHeight + 4;

        List<DropdownWidget.DropdownOption> notifOptions = Arrays.stream(ChannelSettings.NotificationLevel.values())
            .map(level -> new DropdownWidget.DropdownOption(level.name(), level.name().replace("_", " ")))
            .collect(Collectors.toList());
        notificationDropdown = new DropdownWidget(minecraft,
            rightPanelX + layout.padding,
            settingsY,
            rightPanelWidth - layout.padding * 2, layout.buttonHeight, notifOptions,
            value -> {
                if (selectedMapId != -1) {
                    ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
                    settings.setNotificationLevel(ChannelSettings.NotificationLevel.valueOf(value));
                    toastManager.success("Notifications set to " + value.replace("_", " "));
                }
            });
        notificationDropdown.getButton().active = false;
        addRenderableWidget(notificationDropdown.getButton());

        settingsY -= layout.buttonHeight + 4;

        int tagFieldWidth = rightPanelWidth - layout.padding * 2 - 50;
        tagInputField = TextFields.input(font,
            rightPanelX + layout.padding, settingsY,
            tagFieldWidth, layout, "Add tag...", 20);
        tagInputField.active = false;
        addRenderableWidget(tagInputField);

        addTagButton = Buttons.create(Component.literal("+"),
            rightPanelX + layout.padding + tagFieldWidth + 4, settingsY, 44, layout, button -> {
                if (selectedMapId != -1 && !tagInputField.getValue().isBlank()) {
                    channel.addTag(selectedMapId, tagInputField.getValue().trim());
                    tagInputField.setValue("");
                    toastManager.success("Tag added");
                }
            });
        addTagButton.active = false;
        addRenderableWidget(addTagButton);

        updateCategoryButtons();
        updateChannelList();
    }

    private void addCategoryButton(ChannelCategory category, int x, int y, int btnWidth) {
        Button button = Button.builder(Component.literal(category.getLabel()), btn -> {
            if (category == ChannelCategory.ALL) {
                filter.clearCategories();
            } else {
                filter.toggleCategory(category);
            }
            updateCategoryButtons();
            updateChannelList();
        }).bounds(x, y, btnWidth, layout.buttonHeight).build();
        categoryButtons.put(category, button);
        addRenderableWidget(button);
    }

    private void updateCategoryButtons() {
        Set<ChannelCategory> active = filter.getActiveCategories();
        for (Map.Entry<ChannelCategory, Button> entry : categoryButtons.entrySet()) {
            boolean isActive = active.contains(entry.getKey()) ||
                (entry.getKey() == ChannelCategory.ALL && active.isEmpty());
            entry.getValue().active = !isActive;
        }
    }

    private void updateSettingsButtons() {
        boolean hasSelection = selectedMapId != -1;

        openChannelButton.active = hasSelection;
        archiveButton.active = hasSelection;
        translationsButton.active = hasSelection;
        notificationDropdown.getButton().active = hasSelection;
        tagInputField.active = hasSelection;
        addTagButton.active = hasSelection;

        if (hasSelection) {
            ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
            boolean archived = settings.isArchived();
            archiveButton.setMessage(Component.literal(archived ? "Unarchive" : "Archive"));

            boolean showTrans = settings.isShowTranslations();
            translationsButton.setMessage(Component.literal(showTrans ? "[X] Translations" : "[ ] Translations"));

            notificationDropdown.setSelected(settings.getNotificationLevel().name());
        }
    }

    private void showArchiveConfirmation() {
        if (selectedMapId == -1) return;

        TelegraphChannel.ChannelMetadata metadata = channel.getMetadata(selectedMapId);
        boolean isArchived = metadata.archived();
        String action = isArchived ? "unarchive" : "archive";

        confirm((isArchived ? "Unarchive" : "Archive") + " Channel",
            "Are you sure you want to " + action + " \"" + metadata.displayName() + "\"?",
            () -> {
                channel.setArchived(selectedMapId, !isArchived);
                updateSettingsButtons();
                updateChannelList();
                toastManager.success("Channel " + (isArchived ? "unarchived" : "archived"));
            });
    }

    private void onChannelSelected(int mapId) {
        selectedMapId = mapId;
        updateSettingsButtons();
    }

    private void updateChannelList() {
        if (channelList == null) return;
        channelList.clearEntries();

        List<Integer> filteredIds = filter.apply(channel);

        for (int mapId : filteredIds) {
            TelegraphChannel.ChannelMetadata metadata = channel.getMetadata(mapId);
            channelList.addEntryToList(new ChannelBrowserEntry(metadata));
        }
    }

    @Override
    protected void renderPanels(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int topBarY = layout.margin;
        int topBarHeight = layout.buttonHeight + layout.padding * 2;
        drawPanel(context, layout.margin, topBarY, layout.contentWidth(), topBarHeight);

        int filterBarY = topBarY + topBarHeight + layout.spacing;
        int filterBarHeight = layout.buttonHeight + layout.padding * 2;
        drawPanel(context, layout.margin, filterBarY, layout.contentWidth(), filterBarHeight);

        int contentY = filterBarY + filterBarHeight + layout.spacing;
        var splitLayout = layout.split(0.55f);
        int contentHeight = height - contentY - layout.margin;

        drawPanel(context, layout.margin, contentY, splitLayout.leftWidth(), contentHeight);
        context.fill(layout.margin + 1, contentY + 1, layout.margin + splitLayout.leftWidth() - 1,
            contentY + layout.headerHeight, TelegraphTheme.HEADER_BG);
        context.drawString(font, "Channels (" + channelList.children().size() + ")",
            layout.margin + layout.padding, contentY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);

        int rightPanelX = splitLayout.rightX();
        int rightPanelWidth = splitLayout.rightWidth();
        drawPanel(context, rightPanelX, contentY, rightPanelWidth, contentHeight);
        context.fill(rightPanelX + 1, contentY + 1, rightPanelX + rightPanelWidth - 1,
            contentY + layout.headerHeight, TelegraphTheme.HEADER_BG);
        context.drawString(font, "Channel Details",
            rightPanelX + layout.padding, contentY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);
    }

    @Override
    protected void renderOverlays(GuiGraphics context, int mouseX, int mouseY, float delta) {
        int topBarY = layout.margin;
        int topBarHeight = layout.buttonHeight + layout.padding * 2;
        int filterBarY = topBarY + topBarHeight + layout.spacing;
        int filterBarHeight = layout.buttonHeight + layout.padding * 2;
        int contentY = filterBarY + filterBarHeight + layout.spacing;
        var splitLayout = layout.split(0.55f);
        int contentHeight = height - contentY - layout.margin;
        int rightPanelX = splitLayout.rightX();
        int rightPanelWidth = splitLayout.rightWidth();

        if (selectedMapId != -1) {
            renderChannelDetails(context, rightPanelX, contentY, rightPanelWidth, contentHeight);
        } else {
            context.drawCenteredString(font, "Select a channel to view details",
                rightPanelX + rightPanelWidth / 2, contentY + contentHeight / 2, TelegraphTheme.TEXT_MUTED);
        }

        sortDropdown.render(context, mouseX, mouseY, delta);
        notificationDropdown.render(context, mouseX, mouseY, delta);
    }

    private void renderChannelDetails(GuiGraphics context, int x, int y, int panelWidth, int panelHeight) {
        TelegraphChannel.ChannelMetadata metadata = channel.getMetadata(selectedMapId);
        if (metadata == null) return;

        int contentX = x + layout.padding;
        int contentY = y + layout.headerHeight + layout.padding;

        context.drawString(font, "Name: " + metadata.displayName(), contentX, contentY, TelegraphTheme.SELECTED, false);
        contentY += 14;

        context.drawString(font, "Map ID: " + metadata.mapId(), contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
        contentY += 12;

        String protocolColor = metadata.protocolName().equals("Carnite") ? "§6" : "§b";
        context.drawString(font, "Protocol: " + protocolColor + metadata.protocolName(), contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        context.drawString(font, "Messages: " + metadata.messageCount(), contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
        contentY += 12;

        if (metadata.lastActivity() != null) {
            String timeStr = metadata.lastActivity().atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
            String timeAgo = getTimeAgo(metadata.lastActivity());
            context.drawString(font, "Last Active: " + timeStr + " (" + timeAgo + ")", contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
        } else {
            context.drawString(font, "Last Active: Never", contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
        }
        contentY += 12;

        String statusText = metadata.archived() ? "§cArchived" : "§aActive";
        context.drawString(font, "Status: " + statusText, contentX, contentY, TelegraphTheme.TEXT_PRIMARY, false);
        contentY += 12;

        context.drawString(font, "Notifications: " + metadata.notificationLevel().name(),
            contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
        contentY += 14;

        if (!metadata.tags().isEmpty()) {
            context.drawString(font, "Tags:", contentX, contentY, TelegraphTheme.TEXT_SECONDARY, false);
            contentY += 12;
            for (String tag : metadata.tags()) {
                context.drawString(font, "  - " + tag, contentX, contentY, TelegraphTheme.TEXT_MUTED, false);
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
        if (notificationDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == KeyboardConstants.KEY_ESCAPE) {
            if (!searchField.getValue().isEmpty()) {
                searchField.setValue("");
                return true;
            }
        }

        if (KeyboardConstants.hasControl(modifiers) && keyCode == KeyboardConstants.KEY_F) {
            searchField.setFocused(true);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (sortDropdown.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        if (notificationDropdown.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private class ChannelBrowserListWidget extends TelegraphListWidget<ChannelBrowserEntry> {
        public ChannelBrowserListWidget(net.minecraft.client.Minecraft client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
    }

    private class ChannelBrowserEntry extends TelegraphListWidget.Entry<ChannelBrowserEntry> {
        private final TelegraphChannel.ChannelMetadata metadata;

        public ChannelBrowserEntry(TelegraphChannel.ChannelMetadata metadata) {
            this.metadata = metadata;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth,
                          int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            boolean selected = selectedMapId == metadata.mapId();

            renderBackground(context, x, y, entryWidth, entryHeight, hovered, selected);

            int nameColor = selected ? TelegraphTheme.SELECTED : TelegraphTheme.TEXT_PRIMARY;
            context.drawString(minecraft.font, metadata.displayName(), x + 5, y + 3, nameColor, false);

            String protocolBadge = metadata.protocolName().equals("Carnite") ? "§6[C]" : "§b[T]";
            context.drawString(minecraft.font, protocolBadge, x + entryWidth - 25, y + 3, TelegraphTheme.TEXT_PRIMARY, false);

            String infoLine = "§7Map " + metadata.mapId() + " - " + metadata.messageCount() + " msgs";
            context.drawString(minecraft.font, infoLine, x + 5, y + 15, TelegraphTheme.TEXT_SECONDARY, false);

            if (metadata.lastActivity() != null) {
                String timeAgo = getTimeAgo(metadata.lastActivity());
                context.drawString(minecraft.font, "§7" + timeAgo, x + 5, y + 27, TelegraphTheme.TEXT_MUTED, false);
            }

            if (metadata.lastActivity() != null) {
                boolean isActive = metadata.lastActivity().isAfter(Instant.now().minus(Duration.ofHours(24)));
                int dotColor = isActive ? TelegraphTheme.SUCCESS : 0xFF555555;
                context.fill(x + entryWidth - 8, y + entryHeight - 8, x + entryWidth - 4, y + entryHeight - 4, dotColor);
            }

            if (metadata.archived()) {
                context.drawString(minecraft.font, "§c[A]", x + entryWidth - 45, y + 3, TelegraphTheme.TEXT_PRIMARY, false);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            onChannelSelected(metadata.mapId());
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(metadata.displayName());
        }
    }
}
