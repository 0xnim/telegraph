package xyz.nim.telegraph.client.ui;

public class ResponsiveLayout {

    public final int screenWidth;
    public final int screenHeight;
    public final TelegraphTheme.ScreenSize size;
    public final int margin;
    public final int padding;
    public final int spacing;
    public final int buttonHeight;
    public final int headerHeight;
    public final int controlHeight;
    public final int buttonWidth;
    public final int smallButtonWidth;

    public ResponsiveLayout(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        this.size = TelegraphTheme.getSize(width, height);
        this.margin = TelegraphTheme.margin(size);
        this.padding = TelegraphTheme.padding(size);
        this.spacing = TelegraphTheme.spacing(size);
        this.buttonHeight = TelegraphTheme.buttonHeight(size);
        this.headerHeight = TelegraphTheme.headerHeight(size);
        this.controlHeight = TelegraphTheme.controlHeight(size);
        this.buttonWidth = TelegraphTheme.buttonWidth(size);
        this.smallButtonWidth = TelegraphTheme.smallButtonWidth(size);
    }

    // Content area (inside margins)
    public int contentX() {
        return margin;
    }

    public int contentY() {
        return margin;
    }

    public int contentWidth() {
        return screenWidth - margin * 2;
    }

    public int contentHeight() {
        return screenHeight - margin * 2;
    }

    // Center helpers
    public int centerX(int elementWidth) {
        return (screenWidth - elementWidth) / 2;
    }

    public int centerY(int elementHeight) {
        return (screenHeight - elementHeight) / 2;
    }

    // Two-panel split (left ratio of content width)
    public SplitLayout split(float leftRatio) {
        int totalWidth = contentWidth();
        int left = (int) (totalWidth * leftRatio);
        int right = totalWidth - left - spacing;
        return new SplitLayout(contentX(), contentY(), left, right, contentHeight(), spacing);
    }

    // Centered panel with max width
    public SimpleLayout.Box centered(int maxWidth) {
        int w = Math.min(maxWidth, contentWidth());
        int x = (screenWidth - w) / 2;
        return new SimpleLayout.Box(x, contentY(), w, contentHeight());
    }

    // Centered panel with max width and max height
    public SimpleLayout.Box centered(int maxWidth, int maxHeight) {
        int w = Math.min(maxWidth, contentWidth());
        int h = Math.min(maxHeight, contentHeight());
        int x = (screenWidth - w) / 2;
        int y = (screenHeight - h) / 2;
        return new SimpleLayout.Box(x, y, w, h);
    }

    // Full content area as a box
    public SimpleLayout.Box fullContent() {
        return new SimpleLayout.Box(contentX(), contentY(), contentWidth(), contentHeight());
    }

    // Create a VStack in the content area
    public SimpleLayout.VStack vstack() {
        return new SimpleLayout.VStack(contentX(), contentY(), contentWidth(), spacing);
    }

    // Create an HStack in the content area
    public SimpleLayout.HStack hstack() {
        return new SimpleLayout.HStack(contentX(), contentY(), contentHeight(), spacing);
    }

    public record SplitLayout(int x, int y, int leftWidth, int rightWidth, int height, int gap) {

        public SimpleLayout.Box left() {
            return new SimpleLayout.Box(x, y, leftWidth, height);
        }

        public SimpleLayout.Box right() {
            return new SimpleLayout.Box(x + leftWidth + gap, y, rightWidth, height);
        }

        public int rightX() {
            return x + leftWidth + gap;
        }
    }
}
