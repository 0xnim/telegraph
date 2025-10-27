package xyz.nim.telegraph.client.trade;

import xyz.nim.telegraph.client.TelegraphChannel;
import xyz.nim.telegraph.client.TelegraphMessage;
import xyz.nim.telegraph.client.carnite.CarniteParser;
import xyz.nim.telegraph.client.carnite.CarniteVocabulary;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TradeManager {
    public static boolean TRADES_ENABLED = false;
    
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern STACK_PATTERN = Pattern.compile("(\\d*)\\.?(\\d*)");
    private static final int STACK_SIZE = 64;
    
    private final TelegraphChannel channel;
    private final List<TradeOffer> trades = new ArrayList<>();
    private final TradePersistence persistence;
    private Map<String, TradeStatus> savedStatuses;
    
    public TradeManager(TelegraphChannel channel) {
        this.channel = channel;
        this.persistence = new TradePersistence();
        this.savedStatuses = persistence.loadTradeStatuses();
    }
    
    public List<TradeOffer> getAllTrades() {
        return new ArrayList<>(trades);
    }
    
    public void refreshTrades() {
        trades.clear();
        
        Map<Integer, String> channels = channel.getAllChannels();
        for (Map.Entry<Integer, String> entry : channels.entrySet()) {
            int channelId = entry.getKey();
            String channelName = entry.getValue();
            
            List<TelegraphMessage> messages = channel.getMessages(channelId);
            for (TelegraphMessage msg : messages) {
                if (isTradeMessage(msg)) {
                    TradeOffer trade = parseTradeMessage(msg, channelId, channelName);
                    if (trade != null) {
                        String key = TradePersistence.getTradeKey(trade);
                        if (savedStatuses.containsKey(key)) {
                            trade.setStatus(savedStatuses.get(key));
                        }
                        trades.add(trade);
                    }
                }
            }
        }
        
        trades.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
    }
    
    public boolean isTradeMessage(TelegraphMessage msg) {
        if (msg.decoration() == null || msg.decoration().type() == null) {
            return false;
        }
        
        String bannerType = msg.decoration().type();
        String message = msg.decoration().name();
        
        return bannerType.contains("yellow") && message != null && 
               CarniteParser.isTradeMessage(message);
    }
    
    public TradeOffer parseTradeMessage(TelegraphMessage msg, int channelId, String channelName) {
        String message = msg.decoration().name();
        if (message == null) return null;
        
        CarniteParser.TradeOffer parsed = CarniteParser.parseTradeOffer(message);
        if (parsed == null) return null;
        
        List<TradeItem> offeringItems = parseItems(parsed.offering());
        List<TradeItem> requestingItems = parseItems(parsed.requesting());
        
        String fromCiv = extractCiv(message, true);
        String toCiv = extractCiv(message, false);
        
        return new TradeOffer(
            msg, channelId, channelName,
            parsed.offering(), parsed.requesting(),
            offeringItems, requestingItems,
            fromCiv, toCiv
        );
    }
    
    private List<TradeItem> parseItems(String itemString) {
        List<TradeItem> items = new ArrayList<>();
        if (itemString == null || itemString.trim().isEmpty()) {
            return items;
        }
        
        String[] parts = itemString.split("[,&]");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            TradeItem item = parseItem(part);
            if (item != null) {
                items.add(item);
            }
        }
        
        return items;
    }
    
    private TradeItem parseItem(String itemStr) {
        itemStr = itemStr.trim();
        
        int quantity = 1;
        String itemName = itemStr;
        
        if (itemStr.contains(".")) {
            Matcher stackMatcher = STACK_PATTERN.matcher(itemStr);
            if (stackMatcher.find()) {
                String stacksStr = stackMatcher.group(1);
                String remainderStr = stackMatcher.group(2);
                
                int stacks = stacksStr.isEmpty() ? 1 : Integer.parseInt(stacksStr);
                int remainder = remainderStr.isEmpty() ? 0 : Integer.parseInt(remainderStr);
                
                quantity = stacks * STACK_SIZE + remainder;
                
                itemName = itemStr.substring(stackMatcher.end()).trim();
            }
        } else {
            Matcher numMatcher = NUMBER_PATTERN.matcher(itemStr);
            if (numMatcher.find()) {
                quantity = Integer.parseInt(numMatcher.group(1));
                itemName = itemStr.substring(numMatcher.end()).trim();
            }
        }
        
        if (itemName.startsWith("~")) {
            itemName = itemName.substring(1);
            quantity = -1;
        }
        
        String expandedName = CarniteVocabulary.expand(itemName);
        
        return new TradeItem(expandedName, quantity);
    }
    
    private String extractCiv(String message, boolean fromMyCiv) {
        List<String> civs = CarniteParser.extractCivAbbreviations(message);
        
        if (fromMyCiv) {
            if (message.contains(";")) {
                return "My Civilization";
            }
        } else {
            if (!civs.isEmpty()) {
                String civCode = civs.get(0);
                return CarniteVocabulary.getCivilizationName(civCode);
            }
            
            if (message.contains(":")) {
                return "Recipient";
            }
        }
        
        return "Unknown";
    }
    
    public List<TradeOffer> filterTrades(TradeFilter filter) {
        List<TradeOffer> filtered = new ArrayList<>();
        
        for (TradeOffer trade : trades) {
            if (filter.matches(trade)) {
                filtered.add(trade);
            }
        }
        
        return filtered;
    }
    
    public void saveTradeStatuses() {
        persistence.saveTradeStatuses(trades);
    }
    
    public static class TradeFilter {
        private TradeStatus status;
        private String searchText;
        private boolean myTradesOnly;
        private boolean incomingOnly;
        
        public TradeFilter() {
        }
        
        public TradeFilter setStatus(TradeStatus status) {
            this.status = status;
            return this;
        }
        
        public TradeFilter setSearchText(String searchText) {
            this.searchText = searchText;
            return this;
        }
        
        public TradeFilter setMyTradesOnly(boolean myTradesOnly) {
            this.myTradesOnly = myTradesOnly;
            return this;
        }
        
        public TradeFilter setIncomingOnly(boolean incomingOnly) {
            this.incomingOnly = incomingOnly;
            return this;
        }
        
        public boolean matches(TradeOffer trade) {
            if (status != null && trade.getStatus() != status) {
                return false;
            }
            
            if (myTradesOnly && !trade.getFromCiv().equals("My Civilization")) {
                return false;
            }
            
            if (incomingOnly && !trade.getToCiv().contains("Recipient")) {
                return false;
            }
            
            if (searchText != null && !searchText.isEmpty()) {
                String lower = searchText.toLowerCase();
                boolean matches = 
                    trade.getOfferingRaw().toLowerCase().contains(lower) ||
                    trade.getRequestingRaw().toLowerCase().contains(lower) ||
                    trade.getFromCiv().toLowerCase().contains(lower) ||
                    trade.getToCiv().toLowerCase().contains(lower);
                
                if (!matches) {
                    return false;
                }
            }
            
            return true;
        }
    }
}
