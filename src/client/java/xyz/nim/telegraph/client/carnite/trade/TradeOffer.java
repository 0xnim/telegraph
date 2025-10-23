package xyz.nim.telegraph.client.carnite.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a complete trade offer between civilizations.
 * 
 * Carnite format examples:
 * - "2.dmd,acft ; 32irn: CN:" = My civ offers 2 stacks diamonds + autocrafter for 32 iron to Carnation
 * - ".brd ; _: ::" = My civ offers 1 stack bread, accepting any offers (broadcast)
 * - "^ 2.gpdr ; 16irn: EG:" = Counter-offer: 2 stacks gunpowder for 16 iron to Eastguard
 */
public class TradeOffer {
    private final UUID offerId;
    private final String offeringCiv;        // Your civilization name
    private final String targetCiv;          // Target civ (or "BROADCAST" for ::)
    private final List<TradeItem> offering;  // Items you're giving
    private final List<TradeItem> requesting; // Items you want
    private TradeStatus status;              // Current status
    private final long timestamp;            // When created (Unix epoch ms)
    private final UUID respondingToOffer;    // If counter-offer, ID of original
    private String notes;                    // Optional player notes
    
    private TradeOffer(Builder builder) {
        this.offerId = builder.offerId != null ? builder.offerId : UUID.randomUUID();
        this.offeringCiv = builder.offeringCiv;
        this.targetCiv = builder.targetCiv;
        this.offering = new ArrayList<>(builder.offering);
        this.requesting = new ArrayList<>(builder.requesting);
        this.status = builder.status;
        this.timestamp = builder.timestamp != 0 ? builder.timestamp : System.currentTimeMillis();
        this.respondingToOffer = builder.respondingToOffer;
        this.notes = builder.notes;
    }
    
    // Getters
    public UUID getOfferId() { return offerId; }
    public String getOfferingCiv() { return offeringCiv; }
    public String getTargetCiv() { return targetCiv; }
    public List<TradeItem> getOffering() { return new ArrayList<>(offering); }
    public List<TradeItem> getRequesting() { return new ArrayList<>(requesting); }
    public TradeStatus getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public UUID getRespondingToOffer() { return respondingToOffer; }
    public String getNotes() { return notes; }
    
    // Setters for mutable fields
    public void setStatus(TradeStatus status) { this.status = status; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public boolean isBroadcast() {
        return "BROADCAST".equals(targetCiv);
    }
    
    public boolean isCounterOffer() {
        return respondingToOffer != null;
    }
    
    public boolean isActive() {
        return status.isActive();
    }
    
    public boolean isComplete() {
        return status.isComplete();
    }
    
    /**
     * Get age of offer in milliseconds.
     */
    public long getAgeMs() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Check if offer has expired (default 72 hours).
     */
    public boolean isExpired(long timeoutMs) {
        return isActive() && getAgeMs() > timeoutMs;
    }
    
    /**
     * Convert this trade offer to Carnite notation.
     * Format: [^] <offering> ; <requesting> : <target>:
     * 
     * Examples:
     * - "2.dmd,acft ; 32irn: CN:"
     * - "^ .gpdr ; 16irn: EG:"
     * - ".brd ; _: ::"
     */
    public String toCarnite() {
        StringBuilder carnite = new StringBuilder();
        
        // Add counter-offer marker
        if (isCounterOffer()) {
            carnite.append("^ ");
        }
        
        // Add offering items
        if (offering.isEmpty()) {
            carnite.append("_");
        } else {
            for (int i = 0; i < offering.size(); i++) {
                if (i > 0) carnite.append(",");
                carnite.append(offering.get(i).toCarnite());
            }
        }
        
        carnite.append(" ; ");
        
        // Add requesting items
        if (requesting.isEmpty()) {
            carnite.append("_");
        } else {
            for (int i = 0; i < requesting.size(); i++) {
                if (i > 0) carnite.append(",");
                carnite.append(requesting.get(i).toCarnite());
            }
        }
        
        carnite.append(" : ");
        
        // Add target civ
        if (isBroadcast()) {
            carnite.append("::");
        } else {
            carnite.append(targetCiv).append(":");
        }
        
        return carnite.toString();
    }
    
    /**
     * Convert to human-readable English description.
     */
    public String toEnglish() {
        StringBuilder english = new StringBuilder();
        
        if (isCounterOffer()) {
            english.append("In response: ");
        }
        
        english.append(offeringCiv).append(" offers ");
        
        // Offering items
        if (offering.isEmpty()) {
            english.append("items");
        } else {
            for (int i = 0; i < offering.size(); i++) {
                if (i > 0 && i == offering.size() - 1) {
                    english.append(" and ");
                } else if (i > 0) {
                    english.append(", ");
                }
                english.append(offering.get(i).toEnglish());
            }
        }
        
        english.append(" for ");
        
        // Requesting items
        if (requesting.isEmpty()) {
            english.append("open offers");
        } else {
            for (int i = 0; i < requesting.size(); i++) {
                if (i > 0 && i == requesting.size() - 1) {
                    english.append(" and ");
                } else if (i > 0) {
                    english.append(", ");
                }
                english.append(requesting.get(i).toEnglish());
            }
        }
        
        if (isBroadcast()) {
            english.append(" (to anyone)");
        } else {
            english.append(" to ").append(targetCiv);
        }
        
        return english.toString();
    }
    
    /**
     * Get a short summary suitable for notifications or lists.
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        
        // Offering summary
        int offerCount = offering.size();
        if (offerCount == 0) {
            summary.append("items");
        } else if (offerCount == 1) {
            summary.append(offering.get(0).toEnglish());
        } else {
            summary.append(offerCount).append(" items");
        }
        
        summary.append(" → ");
        
        // Requesting summary
        int requestCount = requesting.size();
        if (requestCount == 0 || requesting.stream().anyMatch(TradeItem::isNegotiable)) {
            summary.append("open offer");
        } else if (requestCount == 1) {
            summary.append(requesting.get(0).toEnglish());
        } else {
            summary.append(requestCount).append(" items");
        }
        
        return summary.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradeOffer that)) return false;
        return Objects.equals(offerId, that.offerId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(offerId);
    }
    
    @Override
    public String toString() {
        return String.format("TradeOffer{%s → %s: %s [%s]}", 
                           offeringCiv, targetCiv, getSummary(), status);
    }
    
    // Builder pattern
    public static class Builder {
        private UUID offerId;
        private String offeringCiv;
        private String targetCiv;
        private List<TradeItem> offering = new ArrayList<>();
        private List<TradeItem> requesting = new ArrayList<>();
        private TradeStatus status = TradeStatus.PENDING;
        private long timestamp;
        private UUID respondingToOffer;
        private String notes;
        
        public Builder offerId(UUID offerId) {
            this.offerId = offerId;
            return this;
        }
        
        public Builder offeringCiv(String offeringCiv) {
            this.offeringCiv = offeringCiv;
            return this;
        }
        
        public Builder targetCiv(String targetCiv) {
            this.targetCiv = targetCiv;
            return this;
        }
        
        public Builder broadcast() {
            this.targetCiv = "BROADCAST";
            return this;
        }
        
        public Builder offering(List<TradeItem> offering) {
            this.offering = new ArrayList<>(offering);
            return this;
        }
        
        public Builder addOffering(TradeItem item) {
            this.offering.add(item);
            return this;
        }
        
        public Builder requesting(List<TradeItem> requesting) {
            this.requesting = new ArrayList<>(requesting);
            return this;
        }
        
        public Builder addRequesting(TradeItem item) {
            this.requesting.add(item);
            return this;
        }
        
        public Builder status(TradeStatus status) {
            this.status = status;
            return this;
        }
        
        public Builder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder respondingTo(UUID originalOfferId) {
            this.respondingToOffer = originalOfferId;
            return this;
        }
        
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public TradeOffer build() {
            if (offeringCiv == null) {
                throw new IllegalStateException("Offering civ is required");
            }
            if (targetCiv == null) {
                throw new IllegalStateException("Target civ is required");
            }
            return new TradeOffer(this);
        }
    }
}
