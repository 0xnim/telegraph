# Telegraph - Map Communication Helper

A client-side utility mod that makes map-based communication easier by helping you read, compose, and track messages sent through banner markers on maps.

## What is a Map Telegraph?

A Map Telegraph is a vanilla-compatible communication method where players place named banners on map areas to send messages. When another player right-clicks that banner with the same map, the banner marker appears on their map with the message. This mod makes working with this system much simpler.

## Features

### 📡 Message Tracking
- Automatically detects when banners are added, changed, or removed from your maps
- Tracks all maps in your hotbar as separate communication channels
- Stores message history (up to 100 messages per channel)
- Press **M** to view all your channels and messages in one interface

### 🗣️ Carnite Telegraphic Support
Full implementation of the **Carnite Telegraphic v1.0** protocol from CivLabs specialization experiments:
- 50+ common abbreviations (dmd=diamond, atk=attack, bld=builder, etc.)
- 10 banner colors representing different tenses and urgency levels
- Symbol system for efficient communication (|, :, ;, ,, &, ., _, ^, ~, -)
- **Interactive Learning Mode** with color-coded tooltips that teach you Carnite as you compose
- **Auto-translation** to convert Carnite messages to plain English
- Real-time validation to catch errors before sending

### 🔔 Smart Notifications
- Toast notifications when new messages arrive
- Sound alerts for urgent messages (red banners)
- Per-channel notification settings (All/Important/None)
- Shows message preview and sender info

### ⚙️ Channel Management
- Custom channel names
- Protocol selection (Standard Telegraph or Carnite)
- Channel types (Military, Civilian, KOS/Wanted)
- Archive old channels
- Export channel data to JSON

## How to Use

1. Get filled maps in your hotbar (slots 0-8)
2. Press **M** to open the interface
3. Select a channel to view its messages
4. Click **Compose** to write a new message
5. Copy the formatted message and rename a banner in an anvil
6. Place the banner on the map area and right-click with the map

The mod handles all the tracking automatically - you'll see notifications when others add banners to your maps!

## Perfect For

- Civilization experiment servers
- Faction coordination and diplomacy
- Long-distance trading networks
- Military coordination
- Inter-settlement communication
- Learning and using the Carnite language

## Requirements

- Fabric Loader
- Minecraft 1.21.8+
- Client-side only (works on any server, no server-side installation needed)

---

**Credits:**
- Carnite Telegraphic v1.0 by BlueEnby & MapleSamara
- Map Telegraph concept by Adara/ilikeairships
- Inspired by CivLabs community experiments
