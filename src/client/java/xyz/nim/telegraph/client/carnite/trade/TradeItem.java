package xyz.nim.telegraph.client.carnite.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single item in a Carnite trade offer.
 * Examples:
 * - ".dmd" = 1 stack (64) diamonds
 * - "32irn" = 32 iron
 * - "2bld|5" = 2 level-5 builders
 * - "blss,fd" = blessed food
 */
public class TradeItem {
    private final String itemType;           // Core abbreviation: "dmd", "brd", "bld", etc.
    private final int quantity;              // Exact count (converted from stacks if needed)
    private final int stacks;                // Number of stacks (if using stack notation)
    private final int remainder;             // Remainder after stacks
    private final boolean isStack;           // Whether using stack notation (.dmd vs 32dmd)
    private final boolean isApproximate;     // Whether quantity is approximate (~)
    private final List<String> properties;   // Properties like "blss" in "blss,fd"
    private final boolean isAgent;           // Whether it's an agent (has | marker)
    private final Integer agentLevel;        // For agents: level (e.g., 5 in "bld|5")
    private final boolean isNegotiable;      // Whether this is a "_" (open/negotiate)
    
    private TradeItem(Builder builder) {
        this.itemType = builder.itemType;
        this.quantity = builder.quantity;
        this.stacks = builder.stacks;
        this.remainder = builder.remainder;
        this.isStack = builder.isStack;
        this.isApproximate = builder.isApproximate;
        this.properties = new ArrayList<>(builder.properties);
        this.isAgent = builder.isAgent;
        this.agentLevel = builder.agentLevel;
        this.isNegotiable = builder.isNegotiable;
    }
    
    // Getters
    public String getItemType() { return itemType; }
    public int getQuantity() { return quantity; }
    public int getStacks() { return stacks; }
    public int getRemainder() { return remainder; }
    public boolean isStack() { return isStack; }
    public boolean isApproximate() { return isApproximate; }
    public List<String> getProperties() { return new ArrayList<>(properties); }
    public boolean isAgent() { return isAgent; }
    public Integer getAgentLevel() { return agentLevel; }
    public boolean isNegotiable() { return isNegotiable; }
    
    /**
     * Convert this item to Carnite notation.
     * Examples: ".dmd", "32irn", "2bld|5", "blss,fd", "_"
     */
    public String toCarnite() {
        if (isNegotiable) {
            return "_";
        }
        
        StringBuilder result = new StringBuilder();
        
        // Add approximate marker
        if (isApproximate) {
            result.append("~");
        }
        
        // Add quantity/stacks
        if (isStack) {
            if (stacks > 1) {
                result.append(stacks);
            }
            result.append(".");
            if (remainder > 0) {
                result.append(remainder);
            }
        } else if (quantity > 1) {
            result.append(quantity);
        }
        
        // Add properties (if any)
        if (!properties.isEmpty()) {
            for (int i = 0; i < properties.size(); i++) {
                if (i > 0) result.append(",");
                result.append(properties.get(i));
            }
        } else {
            result.append(itemType);
        }
        
        // Add agent marker
        if (isAgent) {
            result.append("|");
            if (agentLevel != null) {
                result.append(agentLevel);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Get human-readable description.
     * Examples: "64 diamonds (1 stack)", "32 iron", "2 level-5 builders"
     */
    public String toEnglish() {
        if (isNegotiable) {
            return "Open offer (negotiable)";
        }
        
        StringBuilder result = new StringBuilder();
        
        if (isApproximate) {
            result.append("Around ");
        }
        
        if (isStack) {
            int total = stacks * 64 + remainder;
            result.append(total).append(" ");
        } else if (quantity > 0) {
            result.append(quantity).append(" ");
        }
        
        // Add item name
        if (!properties.isEmpty()) {
            for (int i = 0; i < properties.size(); i++) {
                if (i > 0) result.append(" ");
                result.append(expandProperty(properties.get(i)));
            }
        } else {
            result.append(expandItemType(itemType));
        }
        
        // Add stack notation if applicable
        if (isStack) {
            result.append(" (");
            if (stacks > 0) {
                result.append(stacks).append(" stack");
                if (stacks > 1) result.append("s");
            }
            if (remainder > 0) {
                if (stacks > 0) result.append(" + ");
                result.append(remainder);
            }
            result.append(")");
        }
        
        // Add agent level
        if (isAgent && agentLevel != null) {
            result.append(" (level ").append(agentLevel).append(")");
        }
        
        return result.toString();
    }
    
    private String expandItemType(String abbr) {
        // This would call CarniteVocabulary.expand() in practice
        return switch (abbr) {
            case "dmd" -> "diamonds";
            case "irn" -> "iron";
            case "brd" -> "bread";
            case "bld" -> "builders";
            case "dp" -> "diplomats";
            case "acft" -> "autocrafter";
            case "gpdr" -> "gunpowder";
            default -> abbr;
        };
    }
    
    private String expandProperty(String prop) {
        return switch (prop) {
            case "blss" -> "blessed";
            case "fd" -> "food";
            case "scrt" -> "secret";
            default -> prop;
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeItem that)) return false;
        return quantity == that.quantity && 
               stacks == that.stacks && 
               remainder == that.remainder && 
               isStack == that.isStack &&
               isApproximate == that.isApproximate && 
               isAgent == that.isAgent && 
               isNegotiable == that.isNegotiable &&
               Objects.equals(itemType, that.itemType) && 
               Objects.equals(properties, that.properties) && 
               Objects.equals(agentLevel, that.agentLevel);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(itemType, quantity, stacks, remainder, isStack, 
                           isApproximate, properties, isAgent, agentLevel, isNegotiable);
    }
    
    @Override
    public String toString() {
        return toCarnite() + " (" + toEnglish() + ")";
    }
    
    // Builder pattern
    public static class Builder {
        private String itemType;
        private int quantity = 1;
        private int stacks = 0;
        private int remainder = 0;
        private boolean isStack = false;
        private boolean isApproximate = false;
        private List<String> properties = new ArrayList<>();
        private boolean isAgent = false;
        private Integer agentLevel = null;
        private boolean isNegotiable = false;
        
        public Builder itemType(String itemType) {
            this.itemType = itemType;
            return this;
        }
        
        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }
        
        public Builder stacks(int stacks, int remainder) {
            this.isStack = true;
            this.stacks = stacks;
            this.remainder = remainder;
            this.quantity = stacks * 64 + remainder;
            return this;
        }
        
        public Builder approximate(boolean approximate) {
            this.isApproximate = approximate;
            return this;
        }
        
        public Builder properties(List<String> properties) {
            this.properties = new ArrayList<>(properties);
            return this;
        }
        
        public Builder addProperty(String property) {
            this.properties.add(property);
            return this;
        }
        
        public Builder agent(boolean isAgent) {
            this.isAgent = isAgent;
            return this;
        }
        
        public Builder agentLevel(Integer level) {
            this.agentLevel = level;
            this.isAgent = true;
            return this;
        }
        
        public Builder negotiable(boolean negotiable) {
            this.isNegotiable = negotiable;
            return this;
        }
        
        public TradeItem build() {
            if (itemType == null && !isNegotiable && properties.isEmpty()) {
                throw new IllegalStateException("Item must have type, properties, or be negotiable");
            }
            return new TradeItem(this);
        }
    }
    
    // Static factory methods for common patterns
    public static TradeItem simple(String itemType, int quantity) {
        return new Builder().itemType(itemType).quantity(quantity).build();
    }
    
    public static TradeItem stack(String itemType, int stacks) {
        return new Builder().itemType(itemType).stacks(stacks, 0).build();
    }
    
    public static TradeItem stackWithRemainder(String itemType, int stacks, int remainder) {
        return new Builder().itemType(itemType).stacks(stacks, remainder).build();
    }
    
    public static TradeItem agent(String agentType, int count, Integer level) {
        return new Builder()
                .itemType(agentType)
                .quantity(count)
                .agent(true)
                .agentLevel(level)
                .build();
    }
    
    public static TradeItem negotiable() {
        return new Builder().negotiable(true).build();
    }
}
