package xyz.nim.telegraph.client.ui;

public class UILayoutUtil {
    
    public static int margin(int screenSize) {
        if (screenSize < 400) return 10;
        if (screenSize < 600) return 15;
        return 20;
    }
    
    public static int horizontalMargin(int screenWidth) {
        return margin(screenWidth);
    }
    
    public static int verticalMargin(int screenHeight) {
        return margin(screenHeight);
    }
    
    public static int panelWidth(int screenWidth) {
        return screenWidth - (horizontalMargin(screenWidth) * 2);
    }
    
    public static int rightPanelWidth(int screenWidth) {
        if (screenWidth < 600) return 150;
        if (screenWidth < 800) return 180;
        return 200;
    }
    
    public static int contentWidth(int screenWidth) {
        return screenWidth - horizontalMargin(screenWidth) - rightPanelWidth(screenWidth) - 10;
    }
    
    public static int controlHeight(int screenHeight) {
        if (screenHeight < 400) return 18;
        return 20;
    }
    
    public static int rowSpacing(int screenHeight) {
        if (screenHeight < 400) return 22;
        if (screenHeight < 600) return 26;
        return 30;
    }
    
    public static int buttonWidth(int screenWidth) {
        if (screenWidth < 500) return 100;
        if (screenWidth < 700) return 120;
        return 150;
    }
    
    public static int smallButtonWidth(int screenWidth) {
        if (screenWidth < 500) return 60;
        if (screenWidth < 700) return 70;
        return 80;
    }
    
    public static int headerHeight(int screenHeight) {
        if (screenHeight < 400) return 25;
        return 30;
    }
    
    public static int fontSize(int screenWidth, int screenHeight) {
        int minDim = Math.min(screenWidth, screenHeight);
        if (minDim < 400) return 8;
        if (minDim < 600) return 9;
        return 10;
    }
    
    public static class Layout {
        public final int screenWidth;
        public final int screenHeight;
        public final int marginLeft;
        public final int marginRight;
        public final int marginTop;
        public final int marginBottom;
        public final int panelWidth;
        public final int contentWidth;
        public final int rightPanelWidth;
        public final int controlHeight;
        public final int rowSpacing;
        public final int buttonWidth;
        public final int smallButtonWidth;
        public final int headerHeight;
        
        public Layout(int screenWidth, int screenHeight) {
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.marginLeft = horizontalMargin(screenWidth);
            this.marginRight = horizontalMargin(screenWidth);
            this.marginTop = verticalMargin(screenHeight);
            this.marginBottom = verticalMargin(screenHeight);
            this.panelWidth = panelWidth(screenWidth);
            this.rightPanelWidth = rightPanelWidth(screenWidth);
            this.contentWidth = contentWidth(screenWidth);
            this.controlHeight = controlHeight(screenHeight);
            this.rowSpacing = rowSpacing(screenHeight);
            this.buttonWidth = buttonWidth(screenWidth);
            this.smallButtonWidth = smallButtonWidth(screenWidth);
            this.headerHeight = headerHeight(screenHeight);
        }
        
        public int centerX(int elementWidth) {
            return (screenWidth - elementWidth) / 2;
        }
        
        public int centerY(int elementHeight) {
            return (screenHeight - elementHeight) / 2;
        }
        
        public int rightPanelX() {
            return screenWidth - marginRight - rightPanelWidth;
        }
        
        public int contentEndX() {
            return rightPanelX() - 10;
        }
        
        public int bottomY(int elementHeight) {
            return screenHeight - marginBottom - elementHeight;
        }
    }
    
    public static Layout forScreen(int width, int height) {
        return new Layout(width, height);
    }
}
