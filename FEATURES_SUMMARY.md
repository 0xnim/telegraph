# Telegraph Mod - Features Summary

## Overview
A Minecraft Fabric mod that implements the **Map Telegraph** system for inter-settlement communication using map decorations (banners) as message carriers. Includes comprehensive **Carnite Telegraphic v1.0** language support.

## Core Features

### 📡 Map Telegraph System
- **BannerTracker** - Monitors hotbar for filled maps and tracks banner changes
- **MapDecorationTracker** - Tracks all map decorations with full lifecycle
- **Multi-Channel Support** - Each map = separate channel
- **Real-time Updates** - Instant detection when banners are added/removed/changed
- **Message History** - Stores up to 100 messages per channel
- **Auto-Save** - Persists settings and messages every 60 seconds

### 🖥️ User Interface
- **MapDecorationsScreen** - Main hub (press M key)
  - Channel list with selection
  - Message list with Raw/Messages views
  - Protocol selection per channel
  - Compose button for new messages
  - Settings access
- **ChannelSettingsScreen** - Advanced configuration
  - Notification toggles (On/Off, All/Important/None)
  - Protocol switching (Telegraph/Carnite)
  - Channel type selection
  - Tags system
  - Archive functionality
  - Export channel data
- **MessageComposerScreen** - Standard composer
  - Quick templates by channel type
  - Message preview
- **CarniteComposerScreen** - Specialized Carnite UI
  - Symbol picker
  - Color selector
  - Validation
  - **Learning Mode** 🎓
  - **Expand Mode** 📖

### 🔔 Notification System
- **Toast Notifications** with custom icons (📬, 🗑, ✏)
- **Sound Alerts** - Different sounds for urgent vs normal
- **Carnite-Aware** - Shows tense indicators
- **Per-Channel Control** - Enable/disable per map
- **Urgency Detection** - Red banners = bell sound + urgent icon
- **Respects Settings** - ALL/IMPORTANT_ONLY/NONE levels

### 🗣️ Protocol Support

#### Map Telegraph Protocol
- **3 Channel Types:**
  - KOS/Wanted - Red (danger)
  - Military - Orange (urgent)
  - Civilian - Green (normal)
- **Formatted Messages** with prefixes
- **Color-coded** by importance

#### Carnite Telegraphic Protocol
- **10 Tenses** via banner colors
  - White (Present), Gray (Future), Light Gray (Past)
  - Pink (Conditional), Red (Urgent), Light Blue (Request)
  - Black (Decision), Blue (Question), Yellow (Trade), Purple (Goal)
- **50+ Abbreviations** (dp, bld, mn, dmd, irn, atk, rd, etc.)
- **11 Symbols** (|, :, ;, ,, &, ., _, ^, '', ~, ::, -)
- **Od Oi S V** word order (Object-direct, Object-indirect, Subject, Verb)
- **Full Language Support**

## 🎓 Learning Features (NEW!)

### Interactive Learning Mode
**What it does:**
- **Color-codes** every part of your message
- **Hover tooltips** on every word/symbol
- **Explains** what each part means
- **Shows** message structure (grammar)
- **Displays** live legend

**10 Part Types Recognized:**
1. **Entity** (Cyan) - Civs, players, agents
2. **Quantity** (Green) - Numbers, stacks
3. **Abbreviation** (Yellow) - Shortened words
4. **Modifier** (Purple) - ~, -
5. **Connector** (Orange) - ,, &
6. **Question** (Magenta) - _
7. **Response** (Pink) - ^
8. **Quote** (Yellow) - ''
9. **Word** (White) - Regular text
10. **Symbol** (Gray) - Other symbols

**Hover Examples:**
- Hover `bld` → "Abbreviation for: builder"
- Hover `|` → "Individual/Player marker - indicates this is a specific person"
- Hover `;` → "My civilization - refers to speaker's civ (we/us)"
- Hover `2.5` → "2 stacks + 5 items"

### Expand/Translation Mode
**What it does:**
- **Translates** Carnite → English automatically
- **Shows** full tense context from banner color
- **Lists** word-by-word expansions
- **Calculates** stack notation (2.5dmd = 133 diamonds)

**Example:**
```
Input:  2bld|5 CN: ; atk
Banner: Red (Urgent)

Expand Shows:
┌──────────────────────────────────────────────┐
│ EXPANDED TRANSLATION                         │
├──────────────────────────────────────────────┤
│ English:                                     │
│ [Urgent/High Priority] my civilization is   │
│ sending 2 level-5 builders to attack        │
│ Carnation                                    │
│                                              │
│ Word-by-Word Breakdown:                      │
│ 2 → 2                                        │
│ bld → 2 builders                             │
│ 5 → 5                                        │
│ CN → CN                                      │
│ ; → my civilization                          │
│ atk → attack                                 │
└──────────────────────────────────────────────┘
```

### Validation System
- **Length warnings** (>32 = warning, >38 = error)
- **Banner color checks** (yellow for trade, red for urgent)
- **Symbol validation** with helpful hints
- **Grammar suggestions**
- **3 Severity Levels**: ERROR, WARNING, INFO

## 🛠️ Technical Features

### CarniteParser
- **Tokenization** into 12 token types
- **Grammar analysis** (Statement/Question/Response/Trade)
- **Tense detection** from banner colors
- **Civ extraction** (finds CAPS abbreviations)
- **Trade parsing**

### CarniteExplainer (NEW!)
- **Token classification** into 10 part types
- **Automatic translation** to English
- **Expansion system** for abbreviations
- **Stack calculator** (handles .notation)
- **Structure analyzer** (Od Oi S V detection)
- **Smart recognition** for verbs, civs, numbers

### CarniteVocabulary
- **50+ abbreviations** with expand/abbreviate functions
- **Symbol definitions** with explanations
- **Autocomplete suggestions** (partial word → full list)
- **Banner color reference**
- **Message expansion** for tooltips

### CarniteValidator
- **Real-time validation** as you type
- **Context-aware** (checks banner color match)
- **Helpful suggestions** not just errors
- **Quick syntax help** command

### Data Persistence
- **PersistenceManager** saves:
  - Channel settings (protocol, type, notifications)
  - Message history
  - Custom names
  - Tags
  - Archive status
- **Auto-save** every 60 seconds
- **Export** to JSON

## 📚 Documentation

### Included Files
1. **CARNITE_INTEGRATION.md** - Full Carnite feature documentation
2. **LEARNING_MODE_GUIDE.md** - Step-by-step learning guide
3. **FEATURES_SUMMARY.md** - This file
4. Original research documents:
   - "The Map Telegraph" by Adara
   - "Long-Distance Yappage" by Phantom_Boi
   - "Carnite Telegraphic v1.0" by BlueEnby

## 🎮 Usage Workflow

### Basic Usage
1. Get filled maps in hotbar (slots 0-8)
2. Press **M** to open interface
3. Select channel (map)
4. View messages in Raw or Messages tab
5. Compose new messages with templates

### Carnite Usage
1. Switch protocol to Carnite in settings
2. Click "Compose" to open Carnite composer
3. Use symbol buttons to build message
4. Select banner color for tense
5. Click **"Learn"** to see color-coded breakdown
6. **Hover** over any part for explanation
7. Click **"Expand"** to see full translation
8. Click "Validate" to check for issues
9. Copy formatted message
10. Rename banner in anvil with message
11. Place banner on map area
12. Right-click banner with map to add

### Reading Messages
1. Messages auto-appear when banners detected
2. Hover for tooltips (if Carnite)
3. Messages sorted newest first
4. Color-coded by type/urgency

## 🔥 Key Innovations

### What Makes This Special

1. **First-Ever Interactive Carnite Learning System**
   - No other tool teaches Carnite interactively
   - Hover-based education is intuitive
   - Color-coding makes patterns visible

2. **Real-time Translation**
   - Instant Carnite → English conversion
   - Handles complex grammar
   - Stack notation calculator built-in

3. **Smart Validation**
   - Context-aware error checking
   - Helpful suggestions not just errors
   - Teaches while validating

4. **Comprehensive Protocol Support**
   - Supports both standard Telegraph and Carnite
   - Per-channel protocol selection
   - Protocol-aware notifications

5. **Professional UI/UX**
   - Clean interface design
   - Color-coded everything
   - Toast notifications
   - Sound feedback
   - Keyboard shortcuts

## 📊 Statistics

- **4 Protocols/Systems** - Carnite, MapTelegraph, Parser, Validator
- **50+ Abbreviations** in vocabulary
- **11 Symbols** in Carnite grammar
- **10 Banner Colors** for tenses
- **10 Part Types** recognized by explainer
- **12 Token Types** in parser
- **3 Message Views** - Raw, Messages, Expanded
- **100 Messages** stored per channel
- **Unlimited Channels** (one per map)

## 🎯 Perfect For

- **Learning Carnite** from scratch
- **Teaching others** Carnite language
- **Verifying messages** before sending
- **Understanding** received messages
- **Military coordination** with allies
- **Trading** between civilizations
- **News distribution** across settlements
- **Diplomatic communication**
- **KOS list management**

## 🚀 Quick Start

```
1. Load mod in Fabric Minecraft
2. Get a filled map in hotbar
3. Press M
4. Click any message composer
5. If Carnite: Click "Learn" button
6. Hover over words to learn!
```

## Credits

**Mod Development:**
- Amp AI Assistant

**Carnite Language:**
- BlueEnby (creator)
- MapleSamara (contributor)

**Map Telegraph Concept:**
- Adara/ilikeairships

**Research:**
- Phantom_Boi ("Long-Distance Yappage")

**Inspiration:**
- Civlabs community
- Realciv experiments

---

**Version:** 1.0  
**Minecraft Version:** 1.21+  
**Mod Loader:** Fabric  
**License:** See LICENSE.txt
