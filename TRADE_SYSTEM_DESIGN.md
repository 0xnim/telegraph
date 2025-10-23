# Carnite Trade System Design

## Overview
The Carnite trade system uses **yellow banners** to indicate trade offers between civilizations. It provides a specialized GUI separate from normal message composition.

## Core Components

### 1. Trade Offer Model
```java
class TradeOffer {
    UUID offerId;
    String offeringCiv;      // Your civ
    String targetCiv;        // Their civ (or "BROADCAST" for open offers)
    List<TradeItem> offering; // What you're giving
    List<TradeItem> requesting; // What you want in return
    TradeStatus status;      // PENDING, ACCEPTED, REJECTED, COUNTERED, EXPIRED
    long timestamp;
    UUID respondingToOffer;  // For counter-offers (^EG: pattern)
}

class TradeItem {
    String itemType;         // "dmd", "brd", "bld", etc.
    int quantity;            // Exact count or stack notation
    boolean isStack;         // Is it in stack form (.dmd vs 32dmd)
    List<String> properties; // For blss,fd notation
    boolean isAgent;         // For bld| notation (person)
}

enum TradeStatus {
    PENDING,      // Waiting for response
    ACCEPTED,     // Trade agreed upon
    REJECTED,     // Declined
    COUNTERED,    // Counter-offer made
    EXPIRED       // Timed out
}
```

### 2. Trade Composer Screen

**Purpose:** Create new trade offers with a visual interface

**Features:**
- **Left Panel: "You Offer"**
  - Item selector (dropdown or searchable list)
  - Quantity input (with stack/count toggle)
  - Property tags (blessed, from-civ, etc.)
  - Agent toggle (for traders, builders, etc.)
  - Add/Remove item buttons
  - Real-time Carnite preview

- **Right Panel: "You Request"**
  - Same interface as left panel
  - Can use `_` for "open offer" (accept any items)
  - Can mark as "negotiate" for flexible items

- **Top Section:**
  - Target civ selector (dropdown of allied civs)
  - Broadcast toggle (:: = all civs)
  - Response toggle (^ = counter-offer to specific trade)

- **Bottom Section:**
  - Carnite code preview (editable)
  - Send button (creates yellow banner message)
  - Cancel button

**Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│ Trade Offer to: [Carnation ▼]  □ Broadcast  □ Counter-Offer │
├─────────────────────┬───────────────────────────────────────┤
│   YOU OFFER         │         YOU REQUEST                   │
├─────────────────────┼───────────────────────────────────────┤
│ + Add Item          │ + Add Item                            │
│                     │                                       │
│ ┌─────────────────┐ │ ┌─────────────────┐                  │
│ │ 🔹 2 stacks of  │ │ │ 🔹 32 iron      │                  │
│ │    diamonds     │ │ │                 │                  │
│ │    [Edit] [X]   │ │ │    [Edit] [X]   │                  │
│ └─────────────────┘ │ └─────────────────┘                  │
│                     │                                       │
│ ┌─────────────────┐ │ ┌─────────────────┐                  │
│ │ 🔹 1 autocrafter│ │ │ 🔹 Open offer   │                  │
│ │    [Edit] [X]   │ │ │    (negotiate)  │                  │
│ └─────────────────┘ │ │    [Edit] [X]   │                  │
│                     │ └─────────────────┘                  │
├─────────────────────┴───────────────────────────────────────┤
│ Carnite: 2.dmd,acft ; 32irn,_: CN:                         │
│ English: My civ offers 2 stacks of diamonds and an         │
│          autocrafter for 32 iron (negotiable) to Carnation │
├─────────────────────────────────────────────────────────────┤
│                    [Send Trade]  [Cancel]                   │
└─────────────────────────────────────────────────────────────┘
```

### 3. Trade Book / Browser Screen

**Purpose:** View and manage incoming/outgoing trade offers

**Features:**
- **Filter Tabs:**
  - All Trades
  - Incoming Offers
  - My Offers
  - Active Negotiations
  - Trade History

- **Trade List:**
  - Shows trade cards with status
  - Color-coded by status (yellow=pending, green=accepted, red=rejected)
  - Sort by date, civ, or status

- **Trade Detail View:**
  - Full offer breakdown
  - Accept button
  - Reject button
  - Counter-offer button (opens composer)
  - Chat history for this trade

**Layout:**
```
┌─────────────────────────────────────────────────────────────┐
│  Trade Book                                   [+ New Trade]  │
├─────────────────────────────────────────────────────────────┤
│ [All] [Incoming] [My Offers] [Active] [History]             │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🟡 PENDING - From Carnation           2h ago           │ │
│ │ They offer: 64 diamonds, 32 iron                       │ │
│ │ They want: 2 builders (lvl 5+), 1 diplomat             │ │
│ │                   [Accept] [Reject] [Counter]          │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🟢 ACCEPTED - To Eastguard            5h ago           │ │
│ │ You offered: 3 stacks bread, 1 autocrafter             │ │
│ │ You got: 128 gunpowder                                  │ │
│ │                              [View Details] [Archive]   │ │
│ └─────────────────────────────────────────────────────────┘ │
│                                                             │
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ 🔵 COUNTERED - From Nowy Madagaskar   1d ago           │ │
│ │ Original: 2 stacks diamonds → 5 builders               │ │
│ │ Counter: 2 stacks diamonds → 3 builders + 1 librarian  │ │
│ │                   [Accept] [Reject] [Counter]          │ │
│ └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4. Trade Notification System

**In-Game Notifications:**
- Toast notification when new trade offer received
- Sound effect (bell/chime)
- HUD indicator (yellow banner icon with count)
- Chat message with clickable link to open trade

**Example:**
```
[TRADE] Carnation offers you 64 diamonds for 2 builders [View Trade]
```

### 5. Item Selector UI

**Features:**
- Categorized item list
- Search/filter
- Quick access to common items
- Stack calculator (converts between count and stacks)
- Agent type selector (for roles)

**Categories:**
- Resources (dmd, irn, brd, etc.)
- Crafted Items (acft, swd, etc.)
- Agents (bld|, dp|, tdr|, etc.)
- Properties (blss, scrt, etc.)

**Layout:**
```
┌──────────────────────────────┐
│ Add Trade Item               │
├──────────────────────────────┤
│ Search: [________]     [🔍]  │
├──────────────────────────────┤
│ Category: [Resources ▼]      │
├──────────────────────────────┤
│ □ Resources                  │
│   • Diamond (dmd)            │
│   • Iron (irn)               │
│   • Bread (brd)              │
│   • Gunpowder (gpdr)         │
│ □ Agents                     │
│   • Builder (bld|)           │
│   • Diplomat (dp|)           │
│   • Trader (tdr|)            │
│ □ Crafted                    │
│   • Autocrafter (acft)       │
├──────────────────────────────┤
│ Selected: Diamond            │
│ Quantity: [64] [+][-]        │
│ □ Use stacks (.dmd)          │
│ □ Approximate (~)            │
│                              │
│ Properties:                  │
│ □ Blessed  □ Secret          │
│ From: [______]               │
├──────────────────────────────┤
│ Preview: .dmd                │
│ (1 stack of diamonds)        │
├──────────────────────────────┤
│        [Add]  [Cancel]       │
└──────────────────────────────┘
```

### 6. Trade Encoding/Decoding

**Encoder:**
```java
class TradeEncoder {
    String encodeOffer(TradeOffer offer) {
        // Builds Carnite string: "items_offering ; items_requesting : target_civ:"
        StringBuilder carnite = new StringBuilder();
        
        // Encode offering items
        carnite.append(encodeItemList(offer.offering));
        carnite.append(" ; ");
        
        // Encode requesting items
        carnite.append(encodeItemList(offer.requesting));
        carnite.append(" : ");
        
        // Encode target
        if (offer.targetCiv.equals("BROADCAST")) {
            carnite.append(":: ");
        } else {
            carnite.append(offer.targetCiv).append(": ");
        }
        
        // Add response marker if counter-offer
        if (offer.respondingToOffer != null) {
            carnite.insert(0, "^ ");
        }
        
        return carnite.toString();
    }
    
    String encodeItemList(List<TradeItem> items) {
        // Converts items to Carnite notation
        // Examples: ".dmd", "32irn", "2bld|5", "blss,fd"
    }
}
```

**Decoder:**
```java
class TradeDecoder {
    TradeOffer decodeMessage(String carniteMessage, String bannerColor) {
        // Parses Carnite trade message back into TradeOffer
        // Validates yellow banner
        // Splits on ; and : markers
        // Parses item notation
    }
}
```

### 7. Trade History & Analytics

**Features:**
- Track all trades (sent, received, accepted, rejected)
- Statistics per civilization
  - Total trades
  - Accept rate
  - Most traded items
  - Average response time
- Export to CSV or JSON
- Search/filter history

**Storage:**
```json
{
  "trades": [
    {
      "id": "uuid",
      "timestamp": 1234567890,
      "from": "My Civ",
      "to": "Carnation",
      "offered": ["2.dmd", "acft"],
      "requested": ["32irn", "_"],
      "status": "ACCEPTED",
      "carnite": "2.dmd,acft ; 32irn,_: CN:",
      "responseTime": 3600
    }
  ]
}
```

## Integration Points

### 1. Telegraph System Integration
- Trade messages use yellow banner color
- Stored in telegraph message log
- Can be sent via telegraph network
- Uses same message validation as other carnite messages

### 2. UI Access Points
- Telegraph screen: "Trade" button/tab
- Hotkey (default: T)
- Command: `/trade` or `/trade [civ]`
- Right-click on telegraph → "Open Trade Book"

### 3. Multiplayer Sync
- Trade offers synced across clients via telegraph messages
- Status updates broadcast to relevant parties
- Notifications sent when status changes

## Technical Implementation

### File Structure
```
/src/client/java/xyz/nim/telegraph/client/carnite/trade/
├── TradeOffer.java          // Data model
├── TradeItem.java           // Item data model
├── TradeStatus.java         // Enum
├── TradeComposerScreen.java // Main trade creation GUI
├── TradeBookScreen.java     // Trade browsing GUI
├── ItemSelectorWidget.java  // Item picker component
├── TradeCardWidget.java     // Trade display component
├── TradeEncoder.java        // Carnite encoding
├── TradeDecoder.java        // Carnite decoding
├── TradeManager.java        // Business logic
├── TradeHistory.java        // History tracking
└── TradeNotifications.java  // Toast/HUD notifications
```

### Configuration
```json
{
  "trade": {
    "enableNotifications": true,
    "enableSounds": true,
    "autoArchiveAfterDays": 30,
    "maxActiveTrades": 50,
    "tradeTimeoutHours": 72,
    "defaultStackSize": 64
  }
}
```

## User Workflow Examples

### Example 1: Simple Trade Offer
1. Player opens Trade Composer (T key or /trade command)
2. Selects target civ "Carnation" from dropdown
3. Adds to "You Offer":
   - 2 stacks diamonds
   - 1 autocrafter
4. Adds to "You Request":
   - 32 iron
5. Clicks "Send Trade"
6. Yellow banner message sent: `2.dmd,acft ; 32irn: CN:`

### Example 2: Receiving Trade
1. Player receives toast: "[TRADE] Carnation offers you..."
2. Clicks notification or opens Trade Book
3. Reviews offer details
4. Clicks "Accept" → status updates to ACCEPTED
5. Acceptance message sent back to Carnation

### Example 3: Counter-Offer
1. Player receives trade offer
2. Clicks "Counter" button
3. Trade Composer opens pre-filled with original offer
4. Player modifies requested items
5. Sends counter-offer with ^ marker: `^ 2.dmd,acft ; 16irn: CN:`
6. Original sender receives counter-offer notification

### Example 4: Broadcast Trade
1. Player opens Trade Composer
2. Checks "Broadcast" toggle
3. Creates offer
4. All allied civs receive the offer
5. First civ to accept gets the trade

## Future Enhancements

1. **Trade Templates:** Save common trade patterns
2. **Bulk Trading:** Multi-item categories (e.g., "~ench" for various enchants)
3. **Escrow System:** Items held until both parties confirm
4. **Trade Reputation:** Rating system for reliable traders
5. **Market Price Suggestions:** AI-suggested fair trades based on history
6. **Trade Chat:** Built-in messaging within trade negotiation
7. **Conditional Trades:** "If X then Y" logic
8. **Recurring Trades:** Automated regular exchanges

## Testing Requirements

- [ ] Unit tests for encoding/decoding
- [ ] GUI tests for composer screen
- [ ] Integration tests for trade flow
- [ ] Multiplayer sync tests
- [ ] Performance tests (1000+ trades in history)
- [ ] Edge cases (malformed carnite, missing items, etc.)
