package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.ui.DropdownWidget;
import xyz.nim.telegraph.client.ui.KeyboardConstants;
import xyz.nim.telegraph.client.ui.SettingsDialog;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TelegraphListWidget;
import xyz.nim.telegraph.client.ui.components.TextFields;
import xyz.nim.telegraph.client.protocol.transport.DecodeResult;
import xyz.nim.telegraph.client.protocol.transport.NoneTransport;
import xyz.nim.telegraph.client.protocol.transport.TTPTransport;
import xyz.nim.telegraph.client.protocol.transport.TransportEnvelope;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class MapDecorationsScreen extends TelegraphScreen {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TelegraphChannel channel;
    private int selectedMapId = -1;
    private ViewTab currentTab = ViewTab.MESSAGES;
    private ChannelListWidget channelList;
    private MessageListWidget messageList;

    // Header buttons
    private ButtonWidget editNameButton;
    private ButtonWidget settingsButton;

    // Settings dialog
    private SettingsDialog settingsDialog;

    // Action bar
    private ButtonWidget composeButton;
    private ButtonWidget tradesButton;
    private ButtonWidget viewToggleButton;

    // Left panel
    private final ChannelFilter channelFilter = new ChannelFilter();
    private TextFieldWidget searchField;
    private DropdownWidget sortDropdown;
    private ButtonWidget browseButton;

    // Rename dialog state
    private TextFieldWidget renameField;
    private boolean showingRenameDialog = false;

    private enum ViewTab {
        RAW("Raw"),
        MESSAGES("Messages");

        final String label;

        ViewTab(String label) {
            this.label = label;
        }
    }

    public MapDecorationsScreen(TelegraphChannel channel) {
        super(Text.literal("Map Decorations"));
        this.channel = channel;
    }

    public MapDecorationsScreen(TelegraphChannel channel, int preselectedMapId) {
        super(Text.literal("Map Decorations"));
        this.channel = channel;
        this.selectedMapId = preselectedMapId;
    }

    @Override
    protected void init() {
        super.init();

        Map<Integer, String> channels = channel.getAllChannels();
        if (!channels.isEmpty() && selectedMapId == -1) {
            selectedMapId = channels.keySet().iterator().next();
        }

        var splitLayout = layout.split(0.25f);
        int leftPanelX = layout.margin;
        int leftPanelY = layout.margin;
        int leftPanelWidth = splitLayout.leftWidth();
        int leftPanelHeight = height - layout.margin * 2;

        int filterBarY = leftPanelY + layout.padding + layout.headerHeight;

        int searchWidth = leftPanelWidth - layout.padding * 2 - 50;
        searchField = TextFields.search(textRenderer, leftPanelX + layout.padding, filterBarY, searchWidth, layout);
        searchField.setChangedListener(text -> {
            channelFilter.setSearchText(text);
            updateChannelList();
        });
        addDrawableChild(searchField);

        browseButton = Buttons.create(Text.literal("..."),
            leftPanelX + layout.padding + searchWidth + 4, filterBarY, 40, layout, button -> {
                if (client != null) {
                    client.setScreen(new ChannelBrowserScreen(this, channel));
                }
            });
        addDrawableChild(browseButton);

        int sortY = filterBarY + layout.buttonHeight + 4;
        List<DropdownWidget.DropdownOption> sortOptions = Arrays.stream(ChannelSortOption.values())
            .map(opt -> new DropdownWidget.DropdownOption(opt.name(), opt.getLabel()))
            .collect(Collectors.toList());
        sortDropdown = new DropdownWidget(client, leftPanelX + layout.padding, sortY,
            leftPanelWidth - layout.padding * 2, layout.buttonHeight, sortOptions, value -> {
                channelFilter.setSortOption(ChannelSortOption.valueOf(value));
                updateChannelList();
            });
        addDrawableChild(sortDropdown.getButton());

        int channelListY = sortY + layout.buttonHeight + 4;
        int channelListHeight = leftPanelHeight - layout.padding * 2 - layout.headerHeight -
            layout.buttonHeight - 8 - layout.buttonHeight - 4 - // search + sort
            layout.buttonHeight - 4; // global settings button at bottom
        channelList = new ChannelListWidget(
            client,
            leftPanelWidth - layout.padding * 2,
            channelListHeight,
            channelListY,
            layout.buttonHeight + 6
        );
        channelList.setX(leftPanelX + layout.padding);
        addDrawableChild(channelList);

        int bottomButtonY = height - layout.margin - layout.padding - layout.buttonHeight;

        ButtonWidget globalSettingsButton = Buttons.create(Text.literal("Global Settings"),
            leftPanelX + layout.padding, bottomButtonY,
            leftPanelWidth - layout.padding * 2, layout, button -> {
                if (client != null) {
                    client.setScreen(new GlobalSettingsScreen(this));
                }
            });
        addDrawableChild(globalSettingsButton);

        int rightPanelX = splitLayout.rightX();
        int rightPanelY = layout.margin;
        int rightPanelWidth = splitLayout.rightWidth();
        int rightPanelHeight = height - layout.margin * 2;

        // Header row: Channel name (drawn in renderPanels) + edit button + settings button
        int headerY = rightPanelY + layout.padding;
        int iconSize = layout.buttonHeight;

        settingsButton = Buttons.small(Text.literal("\u2699"),
            rightPanelX + rightPanelWidth - layout.padding - iconSize, headerY, layout, button -> {
                if (selectedMapId != -1) {
                    showSettingsDialog();
                }
            });
        addDrawableChild(settingsButton);

        editNameButton = Buttons.small(Text.literal("\u270E"),
            rightPanelX + rightPanelWidth - layout.padding - iconSize * 2 - 4, headerY, layout, button -> {
                if (selectedMapId != -1) {
                    showRenameDialog();
                }
            });
        addDrawableChild(editNameButton);

        // Action bar: Compose, Trades, and Raw/Messages toggle
        int actionBarY = rightPanelY + layout.headerHeight + layout.padding;

        composeButton = Buttons.create(Text.literal("+ Compose"),
            rightPanelX + layout.padding, actionBarY, 90, layout, button -> {
                if (selectedMapId != -1 && client != null) {
                    ChannelSettings settings = channel.getSettings(selectedMapId);
                    if (settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.CarniteProtocol) {
                        client.setScreen(new xyz.nim.telegraph.client.carnite.CarniteComposerScreen(this, channel, selectedMapId, settings));
                    } else {
                        client.setScreen(new MessageComposerScreen(this, channel, selectedMapId));
                    }
                }
            });
        addDrawableChild(composeButton);

        tradesButton = Buttons.create(Text.literal("\u2696 Trades"),
            rightPanelX + layout.padding + 95, actionBarY, 80, layout, button -> {
                if (client != null && xyz.nim.telegraph.client.trade.TradeManager.TRADES_ENABLED) {
                    client.setScreen(new xyz.nim.telegraph.client.trade.TradesDashboardScreen(this, channel));
                }
            });
        tradesButton.active = xyz.nim.telegraph.client.trade.TradeManager.TRADES_ENABLED;
        addDrawableChild(tradesButton);

        // View toggle (Raw / Messages)
        viewToggleButton = Buttons.create(Text.literal(getToggleLabel()),
            rightPanelX + rightPanelWidth - layout.padding - 100, actionBarY, 100, layout, button -> {
                currentTab = currentTab == ViewTab.MESSAGES ? ViewTab.RAW : ViewTab.MESSAGES;
                viewToggleButton.setMessage(Text.literal(getToggleLabel()));
                updateMessageList();
            });
        addDrawableChild(viewToggleButton);

        // Rename field (hidden by default, shown in inline edit mode)
        renameField = TextFields.input(textRenderer, 0, 0, 200, layout, "Channel name...", 32);
        renameField.visible = false;
        addDrawableChild(renameField);

        // Message list starts below action bar
        int messageListY = actionBarY + layout.buttonHeight + 4;
        int messageListHeight = rightPanelHeight - layout.padding * 2 - layout.buttonHeight - 4 - layout.headerHeight;

        messageList = new MessageListWidget(
            client,
            rightPanelWidth - layout.padding * 2,
            messageListHeight,
            messageListY,
            24
        );
        messageList.setX(rightPanelX + layout.padding);
        addDrawableChild(messageList);

        updateChannelList();
        updateMessageList();
    }

    private void cycleProtocol() {
        ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
        if (settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.MapTelegraphProtocol) {
            settings.setProtocol(new xyz.nim.telegraph.client.protocol.CarniteProtocol());
        } else {
            settings.setProtocol(new xyz.nim.telegraph.client.protocol.MapTelegraphProtocol());
        }
        if (settingsDialog != null) {
            settingsDialog.updateRows();
        }
        updateMessageList();
    }

    private void cycleChannelType() {
        ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
        List<String> types = settings.getProtocol().getChannelTypes();
        if (types.isEmpty()) return;

        int currentIndex = types.indexOf(settings.getChannelType());
        int nextIndex = (currentIndex + 1) % types.size();
        settings.setChannelType(types.get(nextIndex));
        updateMessageList();
    }

    private void cycleTransportProtocol() {
        ChannelSettings settings = channel.getOrCreateSettings(selectedMapId);
        if (settings.getTransportProtocol() instanceof TTPTransport) {
            settings.setTransportProtocol(new NoneTransport());
        } else {
            settings.setTransportProtocol(new TTPTransport());
        }
        if (settingsDialog != null) {
            settingsDialog.updateRows();
        }
        updateMessageList();
    }

    private void showSettingsDialog() {
        if (selectedMapId == -1) return;

        List<SettingsDialog.SettingRow> rows = new ArrayList<>();

        // Transport protocol row
        rows.add(new SettingsDialog.SettingRow(
            "Transport:",
            () -> {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                if (settings == null) return "...";
                return settings.getTransportProtocol().getName();
            },
            this::cycleTransportProtocol
        ));

        // Protocol row
        rows.add(new SettingsDialog.SettingRow(
            "Protocol:",
            () -> {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                if (settings == null) return "...";
                String name = settings.getProtocol().getName();
                return name.contains("Carnite") ? "Carnite" : name.contains("Telegraph") ? "Telegraph" : name;
            },
            this::cycleProtocol
        ));

        // Channel type row (only visible for Telegraph protocol)
        rows.add(new SettingsDialog.SettingRow(
            "Type:",
            () -> {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                if (settings == null || settings.getChannelType() == null) return "...";
                String type = settings.getChannelType();
                return type.length() > 15 ? type.substring(0, 15) : type;
            },
            this::cycleChannelType,
            () -> {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                return settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.MapTelegraphProtocol;
            },
            () -> selectedMapId != -1
        ));

        settingsDialog = new SettingsDialog("Channel Settings", rows, () -> {});
        settingsDialog.show(width, height, this::addDrawableChild);
    }

    private String getToggleLabel() {
        return currentTab == ViewTab.MESSAGES ? "\u25CF Messages" : "\u25CB Raw";
    }

    private void showRenameDialog() {
        if (selectedMapId == -1) return;

        var splitLayout = layout.split(0.25f);
        int rightPanelX = splitLayout.rightX();
        int rightPanelWidth = splitLayout.rightWidth();

        String currentName = channel.getDisplayName(selectedMapId);

        // Position rename field in the header area, inline with channel name
        int iconSize = layout.buttonHeight;
        int fieldWidth = rightPanelWidth - layout.padding * 3 - iconSize * 2 - 8;
        renameField.setX(rightPanelX + layout.padding);
        renameField.setY(layout.margin + layout.padding);
        renameField.setWidth(fieldWidth);
        renameField.setText(currentName);
        renameField.visible = true;
        renameField.setFocused(true);
        showingRenameDialog = true;
    }

    private void commitRename() {
        if (selectedMapId == -1 || renameField == null) return;

        String newName = renameField.getText().trim();
        if (!newName.isEmpty()) {
            channel.setUserChannelName(selectedMapId, newName);
            updateChannelList();
            toastManager.success("Channel renamed to \"" + newName + "\"");
        }
        cancelRename();
    }

    private void cancelRename() {
        if (renameField != null) {
            renameField.setText("");
            renameField.visible = false;
            renameField.setFocused(false);
        }
        showingRenameDialog = false;
    }

    @Override
    protected void renderPanels(DrawContext context, int mouseX, int mouseY, float delta) {
        var splitLayout = layout.split(0.25f);
        int leftPanelX = layout.margin;
        int leftPanelY = layout.margin;
        int leftPanelWidth = splitLayout.leftWidth();
        int leftPanelHeight = height - layout.margin * 2;

        drawPanel(context, leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight);
        context.fill(leftPanelX + 1, leftPanelY + 1, leftPanelX + leftPanelWidth - 1,
            leftPanelY + layout.headerHeight, TelegraphTheme.HEADER_BG);
        context.drawText(textRenderer, "Channels", leftPanelX + layout.padding, leftPanelY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);

        int rightPanelX = splitLayout.rightX();
        int rightPanelY = layout.margin;
        int rightPanelWidth = splitLayout.rightWidth();
        int rightPanelHeight = height - layout.margin * 2;

        drawPanel(context, rightPanelX, rightPanelY, rightPanelWidth, rightPanelHeight);
        context.fill(rightPanelX + 1, rightPanelY + 1, rightPanelX + rightPanelWidth - 1,
            rightPanelY + layout.headerHeight, TelegraphTheme.HEADER_BG);

        // Draw channel name in header (unless editing)
        if (!showingRenameDialog) {
            if (selectedMapId != -1) {
                String channelName = channel.getDisplayName(selectedMapId);
                context.drawText(textRenderer, "\u00A7f" + channelName, rightPanelX + layout.padding, rightPanelY + layout.padding, TelegraphTheme.TEXT_PRIMARY, false);
            } else {
                context.drawText(textRenderer, "\u00A78No channel selected", rightPanelX + layout.padding, rightPanelY + layout.padding, TelegraphTheme.TEXT_MUTED, false);
            }
        }

        // Update button states
        editNameButton.active = selectedMapId != -1;
        settingsButton.active = selectedMapId != -1;
        composeButton.active = selectedMapId != -1;
    }

    @Override
    protected void renderOverlays(DrawContext context, int mouseX, int mouseY, float delta) {
        if (sortDropdown != null) {
            sortDropdown.render(context, mouseX, mouseY, delta);
        }
        if (settingsDialog != null && settingsDialog.isVisible()) {
            settingsDialog.render(context, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (settingsDialog != null && settingsDialog.isVisible()) {
            return settingsDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (sortDropdown != null && sortDropdown.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (sortDropdown != null && sortDropdown.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Handle settings dialog
        if (settingsDialog != null && settingsDialog.isVisible()) {
            return settingsDialog.keyPressed(keyCode, scanCode, modifiers);
        }

        // Handle rename field in inline edit mode
        if (showingRenameDialog && renameField != null && renameField.isFocused()) {
            if (KeyboardConstants.isEnter(keyCode)) {
                commitRename();
                return true;
            } else if (keyCode == KeyboardConstants.KEY_ESCAPE) {
                cancelRename();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (searchField != null && searchField.isFocused()) {
            if (keyCode == KeyboardConstants.KEY_ESCAPE) {
                searchField.setText("");
                searchField.setFocused(false);
                channelFilter.setSearchText("");
                updateChannelList();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        if (KeyboardConstants.hasControl(modifiers) && keyCode == KeyboardConstants.KEY_F) {
            if (searchField != null) {
                searchField.setFocused(true);
            }
            return true;
        }

        if (keyCode == KeyboardConstants.KEY_TAB) {
            currentTab = currentTab == ViewTab.MESSAGES ? ViewTab.RAW : ViewTab.MESSAGES;
            if (viewToggleButton != null) {
                viewToggleButton.setMessage(Text.literal(getToggleLabel()));
            }
            updateMessageList();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateChannelList() {
        if (channelList == null) return;

        channelList.clearEntries();

        List<Integer> filteredIds = channelFilter.apply(channel);
        for (int mapId : filteredIds) {
            TelegraphChannel.ChannelMetadata metadata = channel.getMetadata(mapId);
            channelList.addEntryToList(new ChannelEntry(
                client,
                mapId,
                metadata.displayName(),
                mapId == selectedMapId,
                this::onChannelSelected,
                metadata
            ));
        }
    }

    private void onChannelSelected(int mapId) {
        // Cancel any active rename when switching channels
        if (showingRenameDialog) {
            cancelRename();
        }
        // Close settings dialog when switching channels
        if (settingsDialog != null && settingsDialog.isVisible()) {
            settingsDialog.hide();
        }

        selectedMapId = mapId;
        channel.markAsRead(mapId);
        updateChannelList();
        updateMessageList();
    }

    private void updateMessageList() {
        if (messageList == null) return;

        messageList.clearEntries();

        if (selectedMapId == -1) {
            messageList.addEntryToList(new MessageEntry(client, "No map selected", TelegraphTheme.TEXT_SECONDARY, null));
            return;
        }

        List<TelegraphMessage> messages = new ArrayList<>(channel.getMessages(selectedMapId));
        messages.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

        if (messages.isEmpty()) {
            messageList.addEntryToList(new MessageEntry(client, "No messages recorded", TelegraphTheme.TEXT_SECONDARY, null));
            return;
        }

        if (currentTab == ViewTab.RAW) {
            ChannelSettings settings = channel.getSettings(selectedMapId);
            boolean isCarnite = settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.CarniteProtocol;

            for (TelegraphMessage message : messages) {
                String timestamp = message.timestamp().atZone(java.time.ZoneId.systemDefault()).format(TIME_FORMATTER);
                int typeColor = getColorForType(message.type());
                String typePrefix = getTypePrefix(message.type());
                String fullMessage = "[" + timestamp + "] " + typePrefix + message.content();

                String tooltip = null;
                if (isCarnite && message.decoration() != null && message.decoration().name() != null) {
                    String tense = xyz.nim.telegraph.client.carnite.CarniteParser.getTenseFromColor(message.decoration().type());
                    String expanded = xyz.nim.telegraph.client.carnite.CarniteVocabulary.formatWithExpansion(message.decoration().name());
                    tooltip = tense + "\n" + expanded;
                }

                messageList.addEntryToList(new MessageEntry(client, fullMessage, typeColor, tooltip));
            }
        } else {
            List<FormattedMapMessage> formattedMessages = extractMapMessages(messages);

            if (formattedMessages.isEmpty()) {
                messageList.addEntryToList(new MessageEntry(client, "No banner messages on this map", TelegraphTheme.TEXT_SECONDARY, null));
            } else {
                ChannelSettings settings = channel.getSettings(selectedMapId);
                boolean isCarnite = settings != null && settings.getProtocol() instanceof xyz.nim.telegraph.client.protocol.CarniteProtocol;
                boolean isTTP = settings != null && settings.getTransportProtocol() instanceof TTPTransport;
                TTPTransport ttpTransport = isTTP ? (TTPTransport) settings.getTransportProtocol() : null;

                for (FormattedMapMessage msg : formattedMessages) {
                    String colorCode = getBannerColorFormatting(msg.bannerType);
                    String displayText = colorCode + msg.name;

                    String tooltip = null;
                    String translation = null;

                    if (isTTP && ttpTransport != null) {
                        DecodeResult decoded = ttpTransport.decode(msg.name);
                        TransportEnvelope env = decoded.envelope();

                        if (env.sourceId() != null) {
                            // Build TTP-formatted display
                            StringBuilder ttpDisplay = new StringBuilder();
                            ttpDisplay.append(colorCode);

                            // Message type prefix
                            if (decoded.isAck()) {
                                ttpDisplay.append("\u00A7a[OK] ");
                            } else if (decoded.isQuery()) {
                                ttpDisplay.append("\u00A7e[NEED] ");
                            } else if (decoded.isStatus()) {
                                ttpDisplay.append("\u00A7b[STATUS] ");
                            }

                            // Routing info
                            ttpDisplay.append("\u00A77T").append(env.sourceId());
                            ttpDisplay.append("\u2192");
                            ttpDisplay.append(env.isBroadcast() ? "ALL" : "T" + env.destinationId());
                            ttpDisplay.append(" ");

                            // Addressee if present
                            if (env.addressee() != null) {
                                ttpDisplay.append("\u00A7f@").append(env.addressee()).append(": ");
                            }

                            // Payload
                            ttpDisplay.append("\u00A7f").append(decoded.payload());

                            displayText = ttpDisplay.toString();

                            // Build tooltip
                            StringBuilder tipBuilder = new StringBuilder();
                            tipBuilder.append("TTP Message ID: ").append(env.messageId());
                            if (env.isMultipart()) {
                                tipBuilder.append(" (Part ").append(env.partNumber()).append("/").append(env.totalParts()).append(")");
                            }
                            tipBuilder.append("\nFrom: Tower ").append(env.sourceId());
                            tipBuilder.append("\nTo: ").append(env.isBroadcast() ? "All Towers" : "Tower " + env.destinationId());
                            if (env.addressee() != null) {
                                tipBuilder.append("\nRecipient: ").append(env.addressee());
                            }
                            tooltip = tipBuilder.toString();
                        }
                    } else if (isCarnite) {
                        String tense = xyz.nim.telegraph.client.carnite.CarniteParser.getTenseFromColor(msg.bannerType);
                        String expanded = xyz.nim.telegraph.client.carnite.CarniteVocabulary.formatWithExpansion(msg.name);
                        tooltip = tense + "\n" + expanded;

                        if (settings.isShowTranslations()) {
                            xyz.nim.telegraph.client.carnite.CarniteTranslator.TranslationResult carniteResult =
                                xyz.nim.telegraph.client.carnite.CarniteTranslator.translate(msg.name, msg.bannerType);
                            translation = carniteResult.translation();
                        }
                    }

                    messageList.addEntryToList(new MessageEntry(client, displayText, TelegraphTheme.TEXT_PRIMARY, tooltip, translation));
                }
            }
        }
    }

    private List<FormattedMapMessage> extractMapMessages(List<TelegraphMessage> messages) {
        List<FormattedMapMessage> result = new ArrayList<>();

        ChannelSettings settings = selectedMapId != -1 ? channel.getSettings(selectedMapId) : null;

        for (TelegraphMessage message : messages) {
            if (message.decoration() != null && message.decoration().type().contains("banner")) {
                if (message.type() == TelegraphMessage.ChangeType.ADDED && message.decoration().name() != null) {
                    String formattedText = message.decoration().name();
                    String bannerColor = message.decoration().type();

                    if (settings != null && settings.getProtocol() != null && settings.getChannelType() != null) {
                        formattedText = settings.getProtocol().formatMessage(message, settings.getChannelType());
                    }

                    result.add(new FormattedMapMessage(
                        formattedText,
                        bannerColor,
                        message.decoration().x(),
                        message.decoration().z(),
                        message.timestamp()
                    ));
                }
            }
        }

        result.sort(Comparator.comparing(FormattedMapMessage::timestamp).reversed());

        return result;
    }

    private String getBannerColorFormatting(String bannerType) {
        if (bannerType.contains("white")) return "\u00A7f";
        if (bannerType.contains("orange")) return "\u00A76";
        if (bannerType.contains("magenta")) return "\u00A7d";
        if (bannerType.contains("light_blue")) return "\u00A7b";
        if (bannerType.contains("yellow")) return "\u00A7e";
        if (bannerType.contains("lime")) return "\u00A7a";
        if (bannerType.contains("pink")) return "\u00A7d";
        if (bannerType.contains("gray") && !bannerType.contains("light")) return "\u00A78";
        if (bannerType.contains("light_gray")) return "\u00A77";
        if (bannerType.contains("cyan")) return "\u00A73";
        if (bannerType.contains("purple")) return "\u00A75";
        if (bannerType.contains("blue") && !bannerType.contains("light")) return "\u00A79";
        if (bannerType.contains("brown")) return "\u00A76";
        if (bannerType.contains("green")) return "\u00A72";
        if (bannerType.contains("red")) return "\u00A7c";
        if (bannerType.contains("black")) return "\u00A70";
        return "\u00A7f";
    }

    private record FormattedMapMessage(String name, String bannerType, double x, double z, java.time.Instant timestamp) {}

    private int getColorForType(TelegraphMessage.ChangeType type) {
        if (type == null) return TelegraphTheme.TEXT_SECONDARY;
        return switch (type) {
            case ADDED -> TelegraphTheme.INFO;
            case REMOVED -> TelegraphTheme.ERROR;
            case CHANGED -> TelegraphTheme.SELECTED;
        };
    }

    private String getTypePrefix(TelegraphMessage.ChangeType type) {
        if (type == null) return "";
        return switch (type) {
            case ADDED -> "[+] ";
            case REMOVED -> "[-] ";
            case CHANGED -> "[~] ";
        };
    }

    private class ChannelListWidget extends TelegraphListWidget<ChannelEntry> {
        public ChannelListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
    }

    private class ChannelEntry extends TelegraphListWidget.Entry<ChannelEntry> {
        private final net.minecraft.client.MinecraftClient client;
        private final int mapId;
        private final String displayName;
        private final boolean selected;
        private final java.util.function.Consumer<Integer> onSelect;
        private final TelegraphChannel.ChannelMetadata metadata;

        public ChannelEntry(net.minecraft.client.MinecraftClient client, int mapId, String displayName, boolean selected,
                           java.util.function.Consumer<Integer> onSelect, TelegraphChannel.ChannelMetadata metadata) {
            this.client = client;
            this.mapId = mapId;
            this.displayName = displayName;
            this.selected = selected;
            this.onSelect = onSelect;
            this.metadata = metadata;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {

            renderBackground(context, x, y, entryWidth, entryHeight, hovered, selected);

            int textColor = selected ? TelegraphTheme.SELECTED : TelegraphTheme.TEXT_PRIMARY;
            context.drawText(client.textRenderer, displayName, x + 4, y + 2, textColor, false);

            if (metadata != null) {
                int rightOffset = 8;

                if (metadata.unreadCount() > 0) {
                    String unreadText = metadata.unreadCount() > 99 ? "99+" : String.valueOf(metadata.unreadCount());
                    int unreadWidth = client.textRenderer.getWidth(unreadText) + 6;
                    context.fill(x + entryWidth - unreadWidth - rightOffset, y + 2, x + entryWidth - rightOffset, y + 12, TelegraphTheme.ERROR);
                    context.drawText(client.textRenderer, unreadText, x + entryWidth - unreadWidth - rightOffset + 3, y + 3, TelegraphTheme.TEXT_PRIMARY, false);
                    rightOffset += unreadWidth + 4;
                }

                String countText = String.valueOf(metadata.messageCount());
                int countWidth = client.textRenderer.getWidth(countText) + 4;
                context.fill(x + entryWidth - countWidth - rightOffset, y + 2, x + entryWidth - rightOffset, y + 12, 0x60555555);
                context.drawText(client.textRenderer, countText, x + entryWidth - countWidth - rightOffset + 2, y + 3, TelegraphTheme.TEXT_SECONDARY, false);

                boolean isActive = metadata.lastActivity() != null &&
                    metadata.lastActivity().isAfter(Instant.now().minus(Duration.ofHours(24)));
                int dotColor = isActive ? TelegraphTheme.SUCCESS : 0xFF555555;
                context.fill(x + entryWidth - 6, y + entryHeight - 6, x + entryWidth - 2, y + entryHeight - 2, dotColor);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                onSelect.accept(mapId);
                return true;
            }
            return false;
        }

        @Override
        public Text getNarration() {
            return Text.literal(displayName);
        }
    }

    private class MessageListWidget extends TelegraphListWidget<MessageEntry> {
        public MessageListWidget(net.minecraft.client.MinecraftClient client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
    }

    private class MessageEntry extends TelegraphListWidget.Entry<MessageEntry> {
        private final String message;
        private final int color;
        private final String tooltip;
        private final String translation;
        private final net.minecraft.client.MinecraftClient client;

        public MessageEntry(net.minecraft.client.MinecraftClient client, String message, int color, String tooltip) {
            this(client, message, color, tooltip, null);
        }

        public MessageEntry(net.minecraft.client.MinecraftClient client, String message, int color, String tooltip, String translation) {
            this.client = client;
            this.message = message;
            this.color = color;
            this.tooltip = tooltip;
            this.translation = translation;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.drawText(client.textRenderer, message, x + 4, y + 2, color, false);

            if (translation != null && !translation.isEmpty()) {
                String translationPrefix = "  -> ";
                String truncatedTranslation = translation;

                int maxWidth = entryWidth - 30;
                if (client.textRenderer.getWidth(translationPrefix + translation) > maxWidth) {
                    while (client.textRenderer.getWidth(translationPrefix + truncatedTranslation + "...") > maxWidth && truncatedTranslation.length() > 10) {
                        truncatedTranslation = truncatedTranslation.substring(0, truncatedTranslation.length() - 1);
                    }
                    truncatedTranslation += "...";
                }

                context.drawText(client.textRenderer, translationPrefix + truncatedTranslation, x + 4, y + 12, TelegraphTheme.TEXT_SECONDARY, false);
            }

            if (hovered && tooltip != null && mouseX >= x && mouseX <= x + entryWidth && mouseY >= y && mouseY <= y + entryHeight) {
                List<Text> tooltipLines = new ArrayList<>();
                for (String line : tooltip.split("\n")) {
                    tooltipLines.add(Text.literal(line));
                }

                if (translation != null && !translation.isEmpty() && client.textRenderer.getWidth("  -> " + translation) > (entryWidth - 30)) {
                    tooltipLines.add(Text.literal(""));
                    tooltipLines.add(Text.literal("Full Translation:"));
                    tooltipLines.add(Text.literal(translation));
                }

                context.drawTooltip(client.textRenderer, tooltipLines, mouseX, mouseY);
            }
        }

        @Override
        public Text getNarration() {
            return Text.literal(message);
        }
    }
}
