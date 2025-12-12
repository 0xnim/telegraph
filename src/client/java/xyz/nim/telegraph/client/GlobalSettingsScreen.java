package xyz.nim.telegraph.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;
import xyz.nim.telegraph.client.ui.ConfirmDialog;
import xyz.nim.telegraph.client.ui.KeyboardConstants;
import xyz.nim.telegraph.client.ui.ToastManager;

import java.util.*;

public class GlobalSettingsScreen extends Screen {
    private static final int PANEL_BORDER_COLOR = 0xFFC0C0C0;
    private static final int HEADER_COLOR = 0xFF222222;

    private final Screen parent;

    private TextFieldWidget civCodeField;
    private TextFieldWidget civNameField;
    private TextFieldWidget searchField;
    private ButtonWidget addCivButton;
    private ButtonWidget doneButton;
    private CivilizationListWidget civList;

    private final ToastManager toastManager = new ToastManager();
    private ConfirmDialog confirmDialog;
    private boolean codeFieldValid = true;
    private String searchFilter = "";

    public GlobalSettingsScreen(Screen parent) {
        super(Text.literal("Global Telegraph Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int margin = 20;
        int controlHeight = 20;
        int rowSpacing = 30;
        int headerHeight = 30;
        int panelWidth = width - (margin * 2);
        int y = 60;

        y += headerHeight + 10;

        // Input fields - responsive widths
        int codeFieldWidth = Math.min(80, width / 8);
        int nameFieldWidth = Math.min(250, width / 3);
        int addButtonWidth = Math.min(130, width / 6);

        civCodeField = new TextFieldWidget(textRenderer, margin, y, codeFieldWidth, controlHeight, Text.literal("Code"));
        civCodeField.setMaxLength(4);
        civCodeField.setPlaceholder(Text.literal("Code..."));
        civCodeField.setChangedListener(text -> {
            String upper = text.trim().toUpperCase();
            codeFieldValid = upper.isEmpty() || upper.matches("[A-Z]{2,4}");
        });
        addDrawableChild(civCodeField);

        civNameField = new TextFieldWidget(textRenderer, margin + codeFieldWidth + 5, y, nameFieldWidth, controlHeight, Text.literal("Name"));
        civNameField.setMaxLength(64);
        civNameField.setPlaceholder(Text.literal("Civilization name..."));
        addDrawableChild(civNameField);

        addCivButton = ButtonWidget.builder(Text.literal("Add Civilization"), button -> {
            String code = civCodeField.getText().trim().toUpperCase();
            String name = civNameField.getText().trim();

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
            civCodeField.setText("");
            civNameField.setText("");
            codeFieldValid = true;
            updateCivList();
            toastManager.success("Added: " + code + " = " + name);
        }).dimensions(margin + codeFieldWidth + nameFieldWidth + 10, y, addButtonWidth, controlHeight).build();
        addDrawableChild(addCivButton);

        y += rowSpacing;

        // Search field
        int searchWidth = Math.min(200, width / 4);
        searchField = new TextFieldWidget(textRenderer, margin, y, searchWidth, controlHeight, Text.literal("Search"));
        searchField.setPlaceholder(Text.literal("Filter civs..."));
        searchField.setMaxLength(32);
        searchField.setChangedListener(text -> {
            searchFilter = text.toLowerCase();
            updateCivList();
        });
        addDrawableChild(searchField);

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
            if (!searchFilter.isEmpty()) {
                if (!entry.getKey().toLowerCase().contains(searchFilter) &&
                    !entry.getValue().toLowerCase().contains(searchFilter)) {
                    continue;
                }
            }
            civList.addCivEntry(new CivilizationListWidget.CivEntry(
                client,
                entry.getKey(),
                entry.getValue(),
                this::showRemoveConfirmation,
                this::onEditCiv
            ));
        }
    }

    private void showRemoveConfirmation(String code) {
        String name = CarniteVocabulary.getAllCivilizations().get(code);
        confirmDialog = new ConfirmDialog(
            "Remove Civilization",
            "Are you sure you want to remove \"" + code + "\" (" + name + ")?",
            () -> {
                CarniteVocabulary.removeCivilization(code);
                updateCivList();
                toastManager.success("Removed: " + code);
            }
        );
        confirmDialog.show(width, height, this::addDrawableChild);
    }

    private void onEditCiv(String code) {
        String name = CarniteVocabulary.getAllCivilizations().get(code);
        civCodeField.setText(code);
        civNameField.setText(name);
        CarniteVocabulary.removeCivilization(code);
        updateCivList();
        toastManager.info("Editing: " + code + " - Update and click Add");
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int margin = 20;
        int headerHeight = 30;
        int panelWidth = width - (margin * 2);

        context.drawCenteredTextWithShadow(textRenderer, this.title, width / 2, 20, 0xFFFFFFFF);

        int y = 60;
        int civHeaderY = y;

        context.fill(margin, civHeaderY, margin + panelWidth, civHeaderY + headerHeight, HEADER_COLOR);
        context.drawBorder(margin, civHeaderY, panelWidth, headerHeight, PANEL_BORDER_COLOR);
        context.drawText(textRenderer, "§eGlobal Civilizations", margin + 8, civHeaderY + 10, 0xFFFFFFFF, false);

        if (!codeFieldValid && civCodeField != null) {
            int fx = civCodeField.getX() - 1;
            int fy = civCodeField.getY() - 1;
            int fw = civCodeField.getWidth() + 2;
            int fh = civCodeField.getHeight() + 2;
            context.drawBorder(fx, fy, fw, fh, 0xFFFF0000);

            context.drawText(textRenderer, "§c2-4 letters", fx, fy + fh + 2, 0xFFFF5555, false);
        }

        if (civNameField != null) {
            int nameLen = civNameField.getText().length();
            int nameColor = nameLen > 50 ? 0xFFFFAA00 : 0xFF888888;
            String counter = nameLen + "/64";
            int counterX = civNameField.getX() + civNameField.getWidth() - textRenderer.getWidth(counter);
            context.drawText(textRenderer, counter, counterX, civNameField.getY() + civNameField.getHeight() + 2, nameColor, false);
        }

        y += 150;
        context.drawText(textRenderer, "§7Civilizations are shared across all channels.",
            margin + 8, y, 0xFF888888, false);

        toastManager.render(context, textRenderer, width, height);

        if (confirmDialog != null && confirmDialog.isVisible()) {
            confirmDialog.render(context, textRenderer, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (confirmDialog != null && confirmDialog.isVisible()) {
            return confirmDialog.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (confirmDialog != null && confirmDialog.isVisible()) {
            return confirmDialog.keyPressed(keyCode, scanCode, modifiers);
        }

        if (keyCode == KeyboardConstants.KEY_ESCAPE) {
            close();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
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
            private final java.util.function.Consumer<String> onEdit;
            private ButtonWidget removeButton;
            private ButtonWidget editButton;

            public CivEntry(net.minecraft.client.MinecraftClient client, String code, String name,
                           java.util.function.Consumer<String> onRemove,
                           java.util.function.Consumer<String> onEdit) {
                this.client = client;
                this.code = code;
                this.name = name;
                this.onRemove = onRemove;
                this.onEdit = onEdit;
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                              int mouseX, int mouseY, boolean hovered, float tickDelta) {

                context.drawText(client.textRenderer, "§6" + code, x + 5, y + 6, 0xFFFFFFFF, false);
                context.drawText(client.textRenderer, "§f→ " + name, x + 60, y + 6, 0xFFFFFFFF, false);

                if (editButton == null) {
                    editButton = ButtonWidget.builder(Text.literal("Edit"), button -> {
                        onEdit.accept(code);
                    }).dimensions(x + entryWidth - 145, y + 2, 55, 20).build();
                } else {
                    editButton.setX(x + entryWidth - 145);
                    editButton.setY(y + 2);
                }

                if (removeButton == null) {
                    removeButton = ButtonWidget.builder(Text.literal("Remove"), button -> {
                        onRemove.accept(code);
                    }).dimensions(x + entryWidth - 85, y + 2, 75, 20).build();
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
            public Text getNarration() {
                return Text.literal("Civilization: " + code + " - " + name);
            }
        }
    }
}
