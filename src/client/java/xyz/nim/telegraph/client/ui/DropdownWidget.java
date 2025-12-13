package xyz.nim.telegraph.client.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DropdownWidget {
    private final MinecraftClient client;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<DropdownOption> options;
    private final Consumer<String> onSelect;
    
    private ButtonWidget button;
    private boolean expanded = false;
    private int selectedIndex = 0;
    private String selectedValue;
    private int scrollOffset = 0;
    
    private static final int OPTION_HEIGHT = 20;
    private int maxVisibleOptions = 5;
    private static final int SCROLLBAR_WIDTH = 6;
    
    public static class DropdownOption {
        public final String value;
        public final String label;
        
        public DropdownOption(String value, String label) {
            this.value = value;
            this.label = label;
        }
    }
    
    public DropdownWidget(MinecraftClient client, int x, int y, int width, int height, 
                         List<DropdownOption> options, Consumer<String> onSelect) {
        this(client, x, y, width, height, options, onSelect, 5);
    }
    
    public DropdownWidget(MinecraftClient client, int x, int y, int width, int height, 
                         List<DropdownOption> options, Consumer<String> onSelect, int maxVisibleOptions) {
        this.client = client;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.options = new ArrayList<>(options);
        this.onSelect = onSelect;
        this.maxVisibleOptions = maxVisibleOptions;
        
        if (!options.isEmpty()) {
            this.selectedValue = options.get(0).value;
        }
        
        createButton();
    }
    
    private void createButton() {
        String label = options.isEmpty() ? "Select..." : options.get(selectedIndex).label;
        this.button = ButtonWidget.builder(Text.literal(label + " ▼"), btn -> {
            expanded = !expanded;
        }).dimensions(x, y, width, height).build();
    }
    
    public void setSelected(String value) {
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value.equals(value)) {
                selectedIndex = i;
                selectedValue = value;
                updateButtonText();
                break;
            }
        }
    }
    
    public String getSelectedValue() {
        return selectedValue;
    }
    
    private void updateButtonText() {
        if (selectedIndex >= 0 && selectedIndex < options.size()) {
            String label = options.get(selectedIndex).label;
            button.setMessage(Text.literal(label + (expanded ? " ▲" : " ▼")));
        }
    }
    
    public ButtonWidget getButton() {
        return button;
    }
    
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!expanded) {
            return;
        }
        
        int visibleCount = Math.min(options.size(), maxVisibleOptions);
        int dropdownHeight = visibleCount * OPTION_HEIGHT;
        int dropdownY = y + height;
        boolean needsScroll = options.size() > maxVisibleOptions;
        
        context.fill(x, dropdownY, x + width, dropdownY + dropdownHeight, TelegraphTheme.PANEL_BG);
        context.drawBorder(x, dropdownY, width, dropdownHeight, TelegraphTheme.PANEL_BORDER);
        
        int maxScroll = Math.max(0, options.size() - maxVisibleOptions);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        for (int i = 0; i < visibleCount; i++) {
            int optionIndex = i + scrollOffset;
            if (optionIndex >= options.size()) break;
            
            int optionY = dropdownY + i * OPTION_HEIGHT;
            boolean isHovered = mouseX >= x && mouseX < x + width - (needsScroll ? SCROLLBAR_WIDTH : 0) && 
                              mouseY >= optionY && mouseY < optionY + OPTION_HEIGHT;
            
            if (isHovered) {
                context.fill(x + 1, optionY + 1, x + width - (needsScroll ? SCROLLBAR_WIDTH : 0) - 1,
                           optionY + OPTION_HEIGHT - 1, TelegraphTheme.HOVER);
            }
            
            DropdownOption option = options.get(optionIndex);
            int textColor = optionIndex == selectedIndex ? TelegraphTheme.SELECTED : TelegraphTheme.TEXT_PRIMARY;
            context.drawText(client.textRenderer, option.label, x + 5, optionY + 6, textColor, false);
        }
        
        if (needsScroll) {
            int scrollbarX = x + width - SCROLLBAR_WIDTH;
            int scrollbarHeight = Math.max(20, (visibleCount * dropdownHeight) / options.size());
            int scrollbarY = dropdownY + (int)((float)scrollOffset / maxScroll * (dropdownHeight - scrollbarHeight));
            
            context.fill(scrollbarX, dropdownY, scrollbarX + SCROLLBAR_WIDTH, dropdownY + dropdownHeight, TelegraphTheme.HEADER_BG);
            context.fill(scrollbarX, scrollbarY, scrollbarX + SCROLLBAR_WIDTH, scrollbarY + scrollbarHeight, TelegraphTheme.TEXT_MUTED);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!expanded) {
            return false;
        }
        
        int visibleCount = Math.min(options.size(), maxVisibleOptions);
        int dropdownHeight = visibleCount * OPTION_HEIGHT;
        int dropdownY = y + height;
        
        if (mouseX >= x && mouseX < x + width && mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight) {
            int clickedVisibleIndex = (int) ((mouseY - dropdownY) / OPTION_HEIGHT);
            int clickedIndex = clickedVisibleIndex + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < options.size()) {
                selectedIndex = clickedIndex;
                selectedValue = options.get(clickedIndex).value;
                expanded = false;
                scrollOffset = 0;
                updateButtonText();
                if (onSelect != null) {
                    onSelect.accept(selectedValue);
                }
                return true;
            }
        }
        
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= dropdownY + dropdownHeight) {
            expanded = false;
            scrollOffset = 0;
            updateButtonText();
            return true;
        }
        
        return false;
    }
    
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (!expanded) {
            return false;
        }
        
        int visibleCount = Math.min(options.size(), maxVisibleOptions);
        int dropdownHeight = visibleCount * OPTION_HEIGHT;
        int dropdownY = y + height;
        
        if (mouseX >= x && mouseX < x + width && mouseY >= dropdownY && mouseY < dropdownY + dropdownHeight) {
            scrollOffset -= (int) amount;
            int maxScroll = Math.max(0, options.size() - maxVisibleOptions);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        
        return false;
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        updateButtonText();
    }
}
