package xyz.nim.telegraph.client.ui;

import java.util.ArrayList;
import java.util.List;

public class SimpleLayout {
    
    public static class Box {
        public final int x;
        public final int y;
        public final int width;
        public final int height;
        
        public Box(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public int centerX() { return x + width / 2; }
        public int centerY() { return y + height / 2; }
    }
    
    public static class VStack {
        private int x;
        private int y;
        private int width;
        private int spacing;
        private int currentY;
        private List<Box> items = new ArrayList<>();
        
        public VStack(int x, int y, int width, int spacing) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.spacing = spacing;
            this.currentY = y;
        }
        
        public Box add(int height) {
            Box box = new Box(x, currentY, width, height);
            items.add(box);
            currentY += height + spacing;
            return box;
        }
        
        public Box add(int width, int height) {
            Box box = new Box(x, currentY, width, height);
            items.add(box);
            currentY += height + spacing;
            return box;
        }
        
        public VStack addGap(int gap) {
            currentY += gap;
            return this;
        }
        
        public int getHeight() {
            if (items.isEmpty()) return 0;
            Box last = items.get(items.size() - 1);
            return last.bottom() - y;
        }
        
        public int getCurrentY() {
            return currentY - spacing;
        }
    }
    
    public static class HStack {
        private int x;
        private int y;
        private int height;
        private int spacing;
        private int currentX;
        private List<Box> items = new ArrayList<>();
        
        public HStack(int x, int y, int height, int spacing) {
            this.x = x;
            this.y = y;
            this.height = height;
            this.spacing = spacing;
            this.currentX = x;
        }
        
        public Box add(int width) {
            Box box = new Box(currentX, y, width, height);
            items.add(box);
            currentX += width + spacing;
            return box;
        }
        
        public Box add(int width, int height) {
            Box box = new Box(currentX, y, width, height);
            items.add(box);
            currentX += width + spacing;
            return box;
        }
        
        public HStack addGap(int gap) {
            currentX += gap;
            return this;
        }
        
        public int getWidth() {
            if (items.isEmpty()) return 0;
            Box last = items.get(items.size() - 1);
            return last.right() - x;
        }
        
        public int getCurrentX() {
            return currentX - spacing;
        }
    }
    
    public static class Grid {
        private int x;
        private int y;
        private int columns;
        private int itemWidth;
        private int itemHeight;
        private int spacingX;
        private int spacingY;
        private int count = 0;
        
        public Grid(int x, int y, int columns, int itemWidth, int itemHeight, int spacingX, int spacingY) {
            this.x = x;
            this.y = y;
            this.columns = columns;
            this.itemWidth = itemWidth;
            this.itemHeight = itemHeight;
            this.spacingX = spacingX;
            this.spacingY = spacingY;
        }
        
        public Box next() {
            int col = count % columns;
            int row = count / columns;
            int itemX = x + col * (itemWidth + spacingX);
            int itemY = y + row * (itemHeight + spacingY);
            count++;
            return new Box(itemX, itemY, itemWidth, itemHeight);
        }
        
        public int getHeight() {
            int rows = (count + columns - 1) / columns;
            return rows * itemHeight + (rows - 1) * spacingY;
        }
    }
    
    public static class Panel {
        public final Box content;
        public final Box total;
        private final int padding;
        
        public Panel(int x, int y, int width, int height, int padding) {
            this.total = new Box(x, y, width, height);
            this.content = new Box(x + padding, y + padding, width - padding * 2, height - padding * 2);
            this.padding = padding;
        }
        
        public VStack vstack(int spacing) {
            return new VStack(content.x, content.y, content.width, spacing);
        }
        
        public HStack hstack(int spacing) {
            return new HStack(content.x, content.y, content.height, spacing);
        }
    }
    
    public static VStack vstack(int x, int y, int width, int spacing) {
        return new VStack(x, y, width, spacing);
    }
    
    public static HStack hstack(int x, int y, int height, int spacing) {
        return new HStack(x, y, height, spacing);
    }
    
    public static Grid grid(int x, int y, int columns, int itemWidth, int itemHeight, int spacingX, int spacingY) {
        return new Grid(x, y, columns, itemWidth, itemHeight, spacingX, spacingY);
    }
    
    public static Panel panel(int x, int y, int width, int height, int padding) {
        return new Panel(x, y, width, height, padding);
    }
}
