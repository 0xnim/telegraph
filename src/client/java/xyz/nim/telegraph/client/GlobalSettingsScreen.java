package xyz.nim.telegraph.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;
import xyz.nim.telegraph.client.ui.ResponsiveLayout;
import xyz.nim.telegraph.client.ui.TelegraphScreen;
import xyz.nim.telegraph.client.ui.TelegraphTheme;
import xyz.nim.telegraph.client.ui.components.Buttons;
import xyz.nim.telegraph.client.ui.components.TelegraphListWidget;
import xyz.nim.telegraph.client.ui.components.TextFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GlobalSettingsScreen extends TelegraphScreen {

    private final Screen parent;

    private EditBox civCodeField;
    private EditBox civNameField;
    private EditBox searchField;
    private Button addCivButton;
    private Button doneButton;
    private CivilizationListWidget civList;

    private boolean codeFieldValid = true;
    private String searchFilter = "";

    public GlobalSettingsScreen(Screen parent) {
        super(Component.literal("Global Telegraph Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int headerY = layout.margin + layout.spacing + 8;
        int y = headerY + layout.headerHeight + layout.spacing;

        // Input fields - responsive widths based on screen size
        int codeFieldWidth = Math.max(60, Math.min(80, layout.contentWidth() / 8));
        int nameFieldWidth = Math.max(150, Math.min(250, layout.contentWidth() / 3));
        int addButtonWidth = Math.max(100, Math.min(130, layout.contentWidth() / 6));

        civCodeField = TextFields.code(font, layout.margin, y, codeFieldWidth, layout, "Code...", 4);
        civCodeField.setResponder(text -> {
            String upper = text.trim().toUpperCase();
            codeFieldValid = upper.isEmpty() || upper.matches("[A-Z]{2,4}");
        });
        addRenderableWidget(civCodeField);

        civNameField = TextFields.input(font, layout.margin + codeFieldWidth + layout.spacing, y,
                nameFieldWidth, layout, "Civilization name...", 64);
        addRenderableWidget(civNameField);

        addCivButton = Buttons.create(Component.literal("Add Civilization"),
                layout.margin + codeFieldWidth + nameFieldWidth + layout.spacing * 2, y,
                addButtonWidth, layout, button -> addCivilization());
        addRenderableWidget(addCivButton);

        y += layout.controlHeight + layout.spacing * 2;

        // Search field
        int searchWidth = Math.max(120, Math.min(200, layout.contentWidth() / 4));
        searchField = TextFields.search(font, layout.margin, y, searchWidth, layout);
        searchField.setHint(Component.literal("Filter civs..."));
        searchField.setResponder(text -> {
            searchFilter = text.toLowerCase();
            updateCivList();
        });
        addRenderableWidget(searchField);

        y += layout.controlHeight + layout.spacing * 2;

        // Civilization list - fills remaining space
        int listHeight = height - y - layout.margin - layout.buttonHeight - layout.spacing * 2;
        civList = new CivilizationListWidget(minecraft, layout.contentWidth(), listHeight, y, 24);
        civList.setX(layout.margin);
        addRenderableWidget(civList);

        updateCivList();

        // Done button - centered at bottom
        doneButton = Buttons.create(Component.literal("Done"),
                layout.centerX(layout.buttonWidth), height - layout.margin - layout.buttonHeight,
                layout.buttonWidth, layout, button -> onClose());
        addRenderableWidget(doneButton);
    }

    private void addCivilization() {
        String code = civCodeField.getValue().trim().toUpperCase();
        String name = civNameField.getValue().trim();

        if (code.isEmpty() || name.isEmpty()) {
            toastManager.warning("Please enter both code and name");
            return;
        }

        if (!code.matches("[A-Z]{2,4}")) {
            toastManager.error("Code must be 2-4 uppercase letters");
            codeFieldValid = false;
            return;
        }

        if (CarniteVocabulary.getAllCivilizations().containsKey(code)) {
            toastManager.warning("Civilization '" + code + "' already exists");
            return;
        }

        CarniteVocabulary.registerCivilization(code, name);
        civCodeField.setValue("");
        civNameField.setValue("");
        codeFieldValid = true;
        updateCivList();
        toastManager.success("Added: " + code + " = " + name);
    }

    private void updateCivList() {
        if (civList == null) return;

        civList.clearEntries();

        Map<String, String> allCivs = CarniteVocabulary.getAllCivilizations();
        List<Map.Entry<String, String>> sortedCivs = new ArrayList<>(allCivs.entrySet());
        sortedCivs.sort(Map.Entry.comparingByKey());

        for (Map.Entry<String, String> entry : sortedCivs) {
            if (!searchFilter.isEmpty()) {
                if (!entry.getKey().toLowerCase().contains(searchFilter) &&
                    !entry.getValue().toLowerCase().contains(searchFilter)) {
                    continue;
                }
            }
            civList.addEntryToList(new CivEntry(minecraft, entry.getKey(), entry.getValue(),
                    this::showRemoveConfirmation, this::onEditCiv));
        }
    }

    private void showRemoveConfirmation(String code) {
        String name = CarniteVocabulary.getAllCivilizations().get(code);
        confirm("Remove Civilization",
                "Are you sure you want to remove \"" + code + "\" (" + name + ")?",
                () -> {
                    CarniteVocabulary.removeCivilization(code);
                    updateCivList();
                    toastManager.success("Removed: " + code);
                });
    }

    private void onEditCiv(String code) {
        String name = CarniteVocabulary.getAllCivilizations().get(code);
        civCodeField.setValue(code);
        civNameField.setValue(name);
        CarniteVocabulary.removeCivilization(code);
        updateCivList();
        toastManager.info("Editing: " + code + " - Update and click Add");
    }

    @Override
    protected void renderPanels(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Title
        context.drawCenteredString(font, this.title, width / 2, layout.margin, TelegraphTheme.TEXT_PRIMARY);

        // Header panel
        int headerY = layout.margin + layout.spacing + 8;
        context.fill(layout.margin, headerY, layout.margin + layout.contentWidth(),
                headerY + layout.headerHeight, TelegraphTheme.HEADER_BG);
        drawBorder(context, layout.margin, headerY, layout.contentWidth(), layout.headerHeight, TelegraphTheme.PANEL_BORDER);
        context.drawString(font, "\u00A7eGlobal Civilizations", layout.margin + layout.padding,
                headerY + (layout.headerHeight - 8) / 2, TelegraphTheme.TEXT_PRIMARY, false);
    }

    @Override
    protected void renderOverlays(GuiGraphics context, int mouseX, int mouseY, float delta) {
        // Code field validation error
        if (!codeFieldValid && civCodeField != null) {
            int fx = civCodeField.getX() - 1;
            int fy = civCodeField.getY() - 1;
            int fw = civCodeField.getWidth() + 2;
            int fh = civCodeField.getHeight() + 2;
            drawBorder(context, fx, fy, fw, fh, TelegraphTheme.ERROR);
            context.drawString(font, "\u00A7c2-4 letters", fx, fy + fh + 2, TelegraphTheme.ERROR, false);
        }

        // Name field character counter
        if (civNameField != null) {
            int nameLen = civNameField.getValue().length();
            int nameColor = nameLen > 50 ? TelegraphTheme.WARNING : TelegraphTheme.TEXT_MUTED;
            String counter = nameLen + "/64";
            int counterX = civNameField.getX() + civNameField.getWidth() - font.width(counter);
            context.drawString(font, counter, counterX, civNameField.getY() + civNameField.getHeight() + 2, nameColor, false);
        }

        // Info text
        int infoY = civList != null ? civList.getBottom() + layout.spacing : height - 80;
        context.drawString(font, "\u00A77Civilizations are shared across all channels.",
                layout.margin + layout.padding, infoY, TelegraphTheme.TEXT_MUTED, false);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            MapDecorationTracker tracker = TelegraphClient.getMapDecorationTracker();
            if (tracker != null) {
                tracker.getPersistenceManager().saveCivilizations();
            }
            minecraft.setScreen(parent);
        }
    }

    private static class CivilizationListWidget extends TelegraphListWidget<CivEntry> {
        public CivilizationListWidget(Minecraft client, int width, int height, int y, int itemHeight) {
            super(client, width, height, y, itemHeight);
        }
    }

    private static class CivEntry extends TelegraphListWidget.Entry<CivEntry> {
        private final Minecraft client;
        private final String code;
        private final String name;
        private final java.util.function.Consumer<String> onRemove;
        private final java.util.function.Consumer<String> onEdit;
        private Button removeButton;
        private Button editButton;

        public CivEntry(Minecraft client, String code, String name,
                        java.util.function.Consumer<String> onRemove,
                        java.util.function.Consumer<String> onEdit) {
            this.client = client;
            this.code = code;
            this.name = name;
            this.onRemove = onRemove;
            this.onEdit = onEdit;
        }

        @Override
        public void render(GuiGraphics context, int index, int y, int x, int entryWidth, int entryHeight,
                          int mouseX, int mouseY, boolean hovered, float tickDelta) {

            renderBackground(context, x, y, entryWidth, entryHeight, hovered, false);

            context.drawString(client.font, "\u00A76" + code, x + 5, y + 6, TelegraphTheme.TEXT_PRIMARY, false);
            context.drawString(client.font, "\u00A7f\u2192 " + name, x + 60, y + 6, TelegraphTheme.TEXT_PRIMARY, false);

            if (editButton == null) {
                editButton = Button.builder(Component.literal("Edit"), button -> onEdit.accept(code))
                        .bounds(x + entryWidth - 145, y + 2, 55, 20).build();
            } else {
                editButton.setX(x + entryWidth - 145);
                editButton.setY(y + 2);
            }

            if (removeButton == null) {
                removeButton = Button.builder(Component.literal("Remove"), button -> onRemove.accept(code))
                        .bounds(x + entryWidth - 85, y + 2, 75, 20).build();
            } else {
                removeButton.setX(x + entryWidth - 85);
                removeButton.setY(y + 2);
            }

            editButton.render(context, mouseX, mouseY, tickDelta);
            removeButton.render(context, mouseX, mouseY, tickDelta);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (editButton != null && editButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (removeButton != null && removeButton.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Component getNarration() {
            return Component.literal("Civilization: " + code + " - " + name);
        }
    }
}
