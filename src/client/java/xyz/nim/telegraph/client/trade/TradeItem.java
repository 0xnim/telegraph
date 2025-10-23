package xyz.nim.telegraph.client.trade;

public class TradeItem {
    private final String itemName;
    private final int quantity;
    private final boolean isStack;
    private final int stacks;
    private final int remainder;
    
    public TradeItem(String itemName, int quantity) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.isStack = quantity >= 64;
        this.stacks = quantity / 64;
        this.remainder = quantity % 64;
    }
    
    public String getItemName() {
        return itemName;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public boolean isStack() {
        return isStack;
    }
    
    public int getStacks() {
        return stacks;
    }
    
    public int getRemainder() {
        return remainder;
    }
    
    public String getFormattedQuantity() {
        if (quantity == 1) {
            return "1x";
        } else if (isStack) {
            if (remainder == 0) {
                return stacks + " stack" + (stacks > 1 ? "s" : "");
            } else {
                return stacks + "." + remainder + " stacks";
            }
        } else {
            return quantity + "x";
        }
    }
    
    @Override
    public String toString() {
        return getFormattedQuantity() + " " + itemName;
    }
}
