package xyz.nim.telegraph.client.trade;

import xyz.nim.telegraph.client.TelegraphMessage;

import java.time.LocalDateTime;
import java.util.List;

public class TradeOffer {
    private final TelegraphMessage sourceMessage;
    private final int channelId;
    private final String channelName;
    private final String offeringRaw;
    private final String requestingRaw;
    private final List<TradeItem> offeringItems;
    private final List<TradeItem> requestingItems;
    private final String fromCiv;
    private final String toCiv;
    private final LocalDateTime timestamp;
    private TradeStatus status;
    
    public TradeOffer(TelegraphMessage sourceMessage, int channelId, String channelName,
                      String offeringRaw, String requestingRaw,
                      List<TradeItem> offeringItems, List<TradeItem> requestingItems,
                      String fromCiv, String toCiv) {
        this.sourceMessage = sourceMessage;
        this.channelId = channelId;
        this.channelName = channelName;
        this.offeringRaw = offeringRaw;
        this.requestingRaw = requestingRaw;
        this.offeringItems = offeringItems;
        this.requestingItems = requestingItems;
        this.fromCiv = fromCiv;
        this.toCiv = toCiv;
        this.timestamp = LocalDateTime.now();
        this.status = TradeStatus.OPEN;
    }
    
    public TelegraphMessage getSourceMessage() {
        return sourceMessage;
    }
    
    public int getChannelId() {
        return channelId;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public String getOfferingRaw() {
        return offeringRaw;
    }
    
    public String getRequestingRaw() {
        return requestingRaw;
    }
    
    public List<TradeItem> getOfferingItems() {
        return offeringItems;
    }
    
    public List<TradeItem> getRequestingItems() {
        return requestingItems;
    }
    
    public String getFromCiv() {
        return fromCiv;
    }
    
    public String getToCiv() {
        return toCiv;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public TradeStatus getStatus() {
        return status;
    }
    
    public void setStatus(TradeStatus status) {
        this.status = status;
    }
    
    public String getOriginalMessage() {
        if (sourceMessage.decoration() != null && sourceMessage.decoration().name() != null) {
            return sourceMessage.decoration().name();
        }
        return "";
    }
}
