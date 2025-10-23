package xyz.nim.telegraph.client.carnite.trade;

import xyz.nim.telegraph.client.carnite.CarniteConstants;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encodes and decodes trade offers to/from Carnite notation.
 */
public class TradeEncoder {
    
    // Patterns for parsing Carnite trade items
    private static final Pattern STACK_PATTERN = Pattern.compile("(\\d+)\\.(\\d*)([a-z,|]+\\d*)");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(\\d+)([a-z,|]+\\d*)");
    private static final Pattern AGENT_PATTERN = Pattern.compile("([a-z,]+)\\|(\\d*)");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile("([a-z]+,)+([a-z]+)");
    
    /**
     * Encode a trade offer into Carnite message format.
     * 
     * @param offer The trade offer to encode
     * @return Carnite string (e.g., "2.dmd,acft ; 32irn: CN:")
     */
    public static String encode(TradeOffer offer) {
        return offer.toCarnite();
    }
    
    /**
     * Decode a Carnite trade message into a TradeOffer.
     * 
     * @param carniteMessage The Carnite message (e.g., "2.dmd ; 32irn: CN:")
     * @param senderCiv The civilization sending the offer
     * @param bannerColor Banner color (should be "yellow" for trades)
     * @return TradeOffer object, or null if invalid
     */
    public static TradeOffer decode(String carniteMessage, String senderCiv, String bannerColor) {
        if (carniteMessage == null || carniteMessage.isEmpty()) {
            return null;
        }
        
        // Verify yellow banner
        if (!bannerColor.contains("yellow")) {
            throw new IllegalArgumentException("Trade messages must use yellow banner, got: " + bannerColor);
        }
        
        // Check for counter-offer marker (^)
        boolean isCounterOffer = carniteMessage.trim().startsWith("^");
        if (isCounterOffer) {
            carniteMessage = carniteMessage.substring(1).trim();
        }
        
        // Split on ; and : markers
        // Format: <offering> ; <requesting> : <target>:
        String[] parts = carniteMessage.split(";");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid trade format: missing semicolon separator");
        }
        
        String offeringPart = parts[0].trim();
        String[] secondParts = parts[1].split(":");
        if (secondParts.length < 2) {
            throw new IllegalArgumentException("Invalid trade format: missing colon separator");
        }
        
        String requestingPart = secondParts[0].trim();
        String targetPart = secondParts[1].trim();
        
        // Parse offering items
        List<TradeItem> offering = parseItemList(offeringPart);
        
        // Parse requesting items
        List<TradeItem> requesting = parseItemList(requestingPart);
        
        // Parse target civ
        String targetCiv;
        if (targetPart.isEmpty() || targetPart.equals(":")) {
            // :: pattern = broadcast
            targetCiv = "BROADCAST";
        } else {
            targetCiv = targetPart;
        }
        
        // Build trade offer
        TradeOffer.Builder builder = new TradeOffer.Builder()
                .offeringCiv(senderCiv)
                .targetCiv(targetCiv)
                .offering(offering)
                .requesting(requesting)
                .status(TradeStatus.PENDING);
        
        // Note: respondingToOffer UUID would need to be tracked separately
        // in a real implementation via context/message threading
        
        return builder.build();
    }
    
    /**
     * Parse a comma-separated list of trade items in Carnite notation.
     * 
     * @param itemListStr String like "2.dmd,32irn,.bndg" or "_"
     * @return List of TradeItem objects
     */
    private static List<TradeItem> parseItemList(String itemListStr) {
        List<TradeItem> items = new ArrayList<>();
        
        if (itemListStr == null || itemListStr.isEmpty()) {
            return items;
        }
        
        // Handle single underscore = negotiable
        if (itemListStr.equals("_")) {
            items.add(TradeItem.negotiable());
            return items;
        }
        
        // Split on commas, but be careful with property notation (blss,fd)
        // We need to handle: "2.dmd,32irn" (2 items) vs "blss,fd" (1 item with properties)
        String[] tokens = splitSmartComma(itemListStr);
        
        for (String token : tokens) {
            TradeItem item = parseItem(token.trim());
            if (item != null) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    /**
     * Smart comma splitting that understands property notation.
     * "2.dmd,32irn" → ["2.dmd", "32irn"]
     * "blss,fd" → ["blss,fd"]
     */
    private static String[] splitSmartComma(String str) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        
        boolean lastWasAlpha = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == ',') {
                // Check if this comma is part of property notation
                boolean nextIsAlpha = (i + 1 < str.length() && Character.isLetter(str.charAt(i + 1)));
                
                if (lastWasAlpha && nextIsAlpha && current.length() > 0 && 
                    !current.toString().contains(".") && !current.toString().matches("\\d+.*")) {
                    // This is property notation (blss,fd)
                    current.append(c);
                } else {
                    // This is an item separator
                    if (current.length() > 0) {
                        parts.add(current.toString());
                        current = new StringBuilder();
                    }
                }
            } else {
                current.append(c);
            }
            
            lastWasAlpha = Character.isLetter(c);
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Parse a single trade item token.
     * 
     * Examples:
     * - ".dmd" → 1 stack diamonds
     * - "2.32dmd" → 2 stacks + 32 diamonds
     * - "32irn" → 32 iron
     * - "2bld|5" → 2 level-5 builders
     * - "blss,fd" → blessed food
     * - "~16brd" → approximately 16 bread
     * - "_" → negotiable
     */
    private static TradeItem parseItem(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        // Handle negotiable
        if (token.equals("_")) {
            return TradeItem.negotiable();
        }
        
        TradeItem.Builder builder = new TradeItem.Builder();
        
        // Check for approximate marker (~)
        boolean isApproximate = token.startsWith("~");
        if (isApproximate) {
            token = token.substring(1);
            builder.approximate(true);
        }
        
        // Check for agent marker (|)
        Matcher agentMatcher = AGENT_PATTERN.matcher(token);
        if (agentMatcher.find()) {
            String baseItem = agentMatcher.group(1);
            String levelStr = agentMatcher.group(2);
            Integer level = levelStr.isEmpty() ? null : Integer.parseInt(levelStr);
            
            builder.agent(true).agentLevel(level);
            token = baseItem; // Remove |5 suffix for further parsing
        }
        
        // Check for stack notation (.dmd or 2.32dmd)
        Matcher stackMatcher = STACK_PATTERN.matcher(token);
        if (stackMatcher.matches()) {
            int stacks = Integer.parseInt(stackMatcher.group(1));
            String remainderStr = stackMatcher.group(2);
            int remainder = remainderStr.isEmpty() ? 0 : Integer.parseInt(remainderStr);
            String itemType = stackMatcher.group(3);
            
            if (itemType.contains(",")) {
                // Property notation
                String[] props = itemType.split(",");
                for (String prop : props) {
                    builder.addProperty(prop);
                }
            } else {
                builder.itemType(itemType);
            }
            
            return builder.stacks(stacks, remainder).build();
        }
        
        // Check for quantity notation (32irn or 2bld)
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(token);
        if (quantityMatcher.matches()) {
            int quantity = Integer.parseInt(quantityMatcher.group(1));
            String itemType = quantityMatcher.group(2);
            
            if (itemType.contains(",")) {
                String[] props = itemType.split(",");
                for (String prop : props) {
                    builder.addProperty(prop);
                }
            } else {
                builder.itemType(itemType);
            }
            
            return builder.quantity(quantity).build();
        }
        
        // Check for property notation without quantity (blss,fd)
        if (token.contains(",")) {
            String[] props = token.split(",");
            for (String prop : props) {
                builder.addProperty(prop);
            }
            return builder.build();
        }
        
        // Simple item (dmd, brd, etc.)
        return builder.itemType(token).quantity(1).build();
    }
    
    /**
     * Validate a Carnite trade message.
     * 
     * @param carniteMessage The message to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidTradeMessage(String carniteMessage) {
        try {
            // Must contain ; and :
            if (!carniteMessage.contains(";") || !carniteMessage.contains(":")) {
                return false;
            }
            
            // Try to decode (will throw if invalid)
            decode(carniteMessage, "TEST", "yellow");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get a user-friendly error message for an invalid trade message.
     */
    public static String getValidationError(String carniteMessage) {
        try {
            decode(carniteMessage, "TEST", "yellow");
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }
}
