# How to Access the Carnite Trade System

## In-Game Access Methods

### 1. **Keyboard Shortcut (Primary Method)**
- **Press `T` key** (default keybind)
- Opens the Trade Composer screen directly
- Can be rebound in Minecraft Controls → Telegraph category

### 2. **From Telegraph Menu**
- Press `M` to open Telegraph Menu (Map Decorations)
- Click **"Trade"** button (will be added)
- Opens Trade Composer

### 3. **Via Command** (Future)
```
/trade                    → Opens Trade Composer
/trade [civilization]     → Opens Trade Composer with pre-filled target
/trade book              → Opens Trade Book
/trade history           → Opens Trade History
```

### 4. **Context Menu** (Future)
- Right-click on **yellow banner** in world
- Select "View Trade Offer" or "Create Trade"
- Opens relevant screen

### 5. **From Notifications** (Future)
- When you receive a trade offer, a toast notification appears
- Click the notification or the **[View Trade]** link in chat
- Opens Trade Book with that offer highlighted

## Screen Navigation

```
Main Menu / Game
    │
    ├─ Press T ──────────────► Trade Composer Screen
    │                              │
    │                              ├─ Create new trade offer
    │                              ├─ Add items you offer
    │                              ├─ Add items you request
    │                              ├─ Select target civilization
    │                              ├─ Send trade
    │                              │
    │                              └─ "View Trade Book" button ───► Trade Book Screen
    │
    └─ Press M ──────────────► Telegraph Menu
                                   │
                                   └─ "Trade" button ────────► Trade Composer Screen
```

## Trade Book Navigation

```
Trade Book Screen
    │
    ├─ [All Trades] Tab
    │   Shows all trade offers (incoming + outgoing)
    │
    ├─ [Incoming] Tab
    │   Shows trades sent to you
    │   Actions: Accept / Reject / Counter
    │
    ├─ [My Offers] Tab
    │   Shows trades you've sent
    │   Can cancel pending offers
    │
    ├─ [Active] Tab
    │   Shows ongoing negotiations
    │   Countered offers
    │
    └─ [History] Tab
        Accepted/rejected/expired trades
        Export to CSV option
```

## Quick Start Tutorial

### Creating Your First Trade

1. **Press `T`** to open Trade Composer

2. **Select target civilization:**
   - Click the dropdown at top
   - Choose "Carnation" (or any civ)
   - Or check "Broadcast" to offer to everyone

3. **Add items you're offering:**
   - Click **"+ Add Item"** in left panel
   - Select "Diamond" from item list
   - Enter quantity: `64` (or use stack toggle for `.dmd`)
   - Click **"Add"**

4. **Add items you want:**
   - Click **"+ Add Item"** in right panel
   - Select "Iron"
   - Enter quantity: `32`
   - Click **"Add"**

5. **Review Carnite preview:**
   - Bottom shows: `.dmd ; 32irn: CN:`
   - English: "My civilization offers 64 diamonds for 32 iron to Carnation"

6. **Click "Send Trade"**
   - Yellow banner message is created and sent
   - Trade appears in your "My Offers" tab

### Accepting a Trade

1. You'll receive a notification:
   ```
   🟡 [TRADE] Carnation offers you 64 diamonds for 32 iron
   [View Trade]
   ```

2. Click **[View Trade]** or press `T` → "View Trade Book"

3. Find the trade in "Incoming" tab

4. Click **"Accept"** button
   - Trade status changes to ACCEPTED
   - Sender receives acceptance notification

### Making a Counter-Offer

1. Open trade in Trade Book

2. Click **"Counter"** button

3. Trade Composer opens with original offer pre-filled

4. Modify the items (e.g., change 32 iron to 16 iron)

5. Click **"Send Trade"**
   - Counter-offer is marked with `^` marker
   - Original sender receives: `^ .dmd ; 16irn: CN:`

## Current Implementation Status

### ✅ Completed
- Core data models (TradeOffer, TradeItem, TradeStatus)
- Carnite encoding/decoding (TradeEncoder)
- Keybind registration (`T` key)
- Placeholder screens (TradeComposerScreen, TradeBookScreen)
- Design specification (TRADE_SYSTEM_DESIGN.md)

### 🚧 In Progress
- Full Trade Composer UI implementation
- Trade Book UI with filtering
- Item selector widget

### 📋 To Do
- Trade Manager (business logic)
- Notification system
- Trade history persistence
- Multiplayer synchronization
- Telegraph menu integration
- Command handlers
- Context menu for banners

## Configuration

Edit `config/telegraph/trade.json` (will be created):
```json
{
  "enableTradeSystem": true,
  "enableNotifications": true,
  "enableSounds": true,
  "tradeTimeoutHours": 72,
  "maxActiveTrades": 50,
  "autoArchiveAfterDays": 30
}
```

## Keybind Settings

Go to **Options → Controls → Key Binds → Telegraph**:
- `Open Telegraph Menu` - Default: `M`
- `Open Trade Composer` - Default: `T`

Change to any key you prefer!

## Tips

1. **Use Stack Notation:** Instead of `64 diamonds`, use `.dmd` (cleaner)
2. **Broadcast for Open Offers:** Check broadcast when you don't care who accepts
3. **Use Approximate:** For flexible quantities, toggle approximate (`~32irn`)
4. **Negotiate Option:** Add `_` item to indicate "make me an offer"
5. **Check History:** Review past trades to see fair market rates
6. **Counter Instead of Reject:** Keep negotiations open by countering

## Troubleshooting

**Q: T key doesn't work**
- Check if T is bound to another mod
- Rebind in Controls → Telegraph

**Q: Trade not showing in Book**
- Ensure yellow banner was used
- Check if trade expired (72 hours default)

**Q: Can't send trade**
- Verify target civ is allied
- Check Carnite format is valid
- Ensure at least one item in offering/requesting

**Q: Trade appears twice**
- This is normal: once in "My Offers", once in "Incoming" (if you're both sender/receiver)

## Next Steps

After trades are working, see:
- [TRADE_SYSTEM_DESIGN.md](TRADE_SYSTEM_DESIGN.md) for full feature set
- [CARNITE_INTEGRATION.md](CARNITE_INTEGRATION.md) for Carnite grammar
- [QUICK_START.md](QUICK_START.md) for general Telegraph usage
