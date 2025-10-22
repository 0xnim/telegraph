# Carnite Telegraphic Integration

This mod includes comprehensive support for **Carnite Telegraphic v1.0**, a constructed language designed specifically for Map Telegraph communication in Minecraft.

## Features Implemented

### 1. Carnite Message Composer (`CarniteComposerScreen`)
- **Dedicated UI** for composing Carnite messages
- **Symbol Picker** with all 11 Carnite symbols (|, :, ;, ,, &, ., _, ^, '', ~, ::, -)
- **Banner Color Selection** with visual tense indicators
- **Quick Templates** for common message patterns
- **Live Validation** with error/warning/info messages
- **Syntax Suggestions** based on message structure
- **Character Count** with readability warnings (>32 chars)
- **Copy to Clipboard** with formatted output
- **Integrated Help System** with examples and quick reference
- **🎓 LEARNING MODE** - Interactive hover-based explanations
  - Color-coded message parts (Entity, Quantity, Abbreviation, Modifier, etc.)
  - Hover over any word/symbol for detailed explanation
  - Shows message structure analysis (Od Oi S V)
  - Live legend showing what each color means
  - Highlights grammar patterns
- **📖 EXPAND MODE** - Full translation and breakdown
  - Translates Carnite → English automatically
  - Word-by-word breakdown with expansions
  - Shows full tense context from banner color
  - Perfect for learning and verification

### 2. Carnite Parser (`CarniteParser`)
- **Tokenization** of Carnite messages into semantic units
- **Grammar Analysis** detecting message types (Statement, Question, Response, Trade)
- **Tense Detection** from banner colors
- **Civ Abbreviation Extraction** (finds CAPS patterns like CN, DR, TWC)
- **Trade Offer Parsing** extracts offering/requesting items
- **Token Types**: WORD, AGENT, YOUR_CIV, MY_CIV, PROPERTY, AND, STACK, QUESTION_BLANK, RESPONSE, PLURAL, NEGATION, QUOTED

### 3. Carnite Vocabulary System (`CarniteVocabulary`)
- **50+ Common Abbreviations** with expand/abbreviate functions
  - Players: dp (diplomat), bld (builder), mn (miner), smth (blacksmith), lib (librarian)
  - Resources: dmd (diamond), irn (iron), gpdr (gunpowder), brd (bread)
  - Actions: atk (attack), rd (raid), trd (trade), ally (ally), merg (merge)
  - Time: m (minutes), h (hours), t (time)
- **Symbol Definitions** with explanations
- **Banner Color Reference** for all 10 tenses
- **Autocomplete Suggestions** for partial input
- **Message Expansion** shows full words in tooltips

### 4. Carnite Validator (`CarniteValidator`)
- **Length Checks** (optimal ≤32, max 38 characters)
- **Banner Color Consistency** (yellow for trades, red for urgent)
- **Symbol Usage Validation**
- **Grammar Hints** (Od Oi S V word order)
- **Trade Format Verification**
- **Severity Levels**: ERROR, WARNING, INFO

### 5. Carnite Explainer (`CarniteExplainer`) - NEW! 🎓
- **Interactive Message Analysis** breaks down every part of a message
- **Token Classification** identifies 10 different part types
- **Automatic Translation** converts Carnite → English
- **Expansion System** shows full meaning of abbreviations
- **Structure Analysis** explains grammar and word order
- **Stack Calculator** converts .notation to actual item counts (2.5dmd = 133 diamonds)
- **Smart Recognition** for civs, players, numbers, verbs, and objects

### 5. Enhanced Message Display
- **Hover Tooltips** showing:
  - Banner tense/mood
  - Expanded abbreviations
  - Carnite symbols explained
- **Color-Coded Banners** matching Carnite tense system
- **Smart Protocol Detection** automatically uses Carnite features when protocol is active
- **Both Raw and Messages View** support Carnite parsing

### 6. Notification Integration
- **Carnite-Aware Notifications** showing tense in toast messages
- **⚠ URGENT** marker for red banners
- **Tense Indicators** in notification text: [PRESENT], [TRADE], [QUESTION], etc.
- **Banner Color Detection** for appropriate alert levels

## Carnite Language Quick Reference

### Word Order
**Od Oi S V** = Object-direct, Object-indirect, Subject, Verb
- English: "Carnation is attacking raiders at Sunfish"
- Carnite: `~rd| SF CN atk` (raiders at Sunfish, Carnation attacks)

### Symbols
| Symbol | Meaning | Example |
|--------|---------|---------|
| `\|` | Agent/Individual | `bld\|5` = level 5 builder |
| `:` | Your civ (addressing) | `CN:` = to Carnation |
| `;` | My civ (we/us) | `;` = my civilization |
| `,` | Property of | `CN,dp\|` = Carnation's diplomat |
| `&` | And | `bld\|&mn\|` = builder and miner |
| `.` | Stack (64 items) | `2.5dmd` = 133 diamonds |
| `_` | Question blank | `_dmd` = how many diamonds? |
| `^` | Response/Because | `^ y` = response: yes |
| `''` | Quote/Reference | `'acpt'` = the term "acpt" |
| `~` | Plural/About | `~5bld\|` = about 5 builders |
| `::` | All civs on channel | `:: ; atk` = we're attacking all of you |
| `-` | Negation/Not | `-acpt` = do not accept |

### Banner Colors (Tense)
| Color | Tense | Use Case |
|-------|-------|----------|
| White | Present Statement | Currently happening |
| Light Gray | Past Statement | Already happened |
| Gray | Future Statement | Will happen |
| Pink | Conditional/Might | Uncertain/Undecided |
| **Red** | **Urgent/High Priority** | **EMERGENCY** |
| Light Blue | Request/Command | Suggestions/Orders |
| Black | Opinion/Decision | Decided matters |
| Blue | Y/N Question | Seeking confirmation |
| Yellow | Trade Offer | Trading items |
| Purple | Goal/Objective | Current objectives |

### Common Examples
```
~rd| ;              → Raiders at my civ (RED banner)
2bld|5 CN:          → Sending 2 level-5 builders to Carnation (LIGHT BLUE)
.dmd CN ; trd       → Traded 1 stack of diamonds to CN (LIGHT GRAY)
_ CN atk            → Who is attacking Carnation? (BLUE)
^ y                 → Response: Yes (WHITE)
.brd,32irn ; _:     → Offering 1 stack bread + 16 iron, what you give? (YELLOW)
~rd| SF DR; atk     → We Dwarven Republic are attacking raiders at Sunfish (RED)
```

## Usage Guide

### Composing Messages
1. Open Map Decorations screen with **M key**
2. Select a channel
3. Click **"Compose"** button
4. If using Carnite Protocol, the Carnite Composer opens automatically
5. Use **Symbol Buttons** to insert Carnite symbols
6. Select appropriate **Banner Color** for tense
7. Use **Templates** for common phrases
8. Click **"Validate"** to check for issues
9. **NEW!** Click **"Learn"** for interactive explanations - hover over each part!
10. **NEW!** Click **"Expand"** to see full English translation
11. Click **"Copy"** to copy formatted message
12. Rename banner in anvil, place on map, right-click with map

### Learning Mode Features 🎓
**Click "Learn" button to activate:**
- Message parts are **color-coded** by type:
  - 🔵 Cyan = Entities (civs, players)
  - 🟢 Green = Quantities (numbers, stacks)
  - 🟡 Yellow = Abbreviations
  - 🟣 Purple = Modifiers (~, -)
  - 🟠 Orange = Connectors (,, &)
- **Hover over any part** to see:
  - What the symbol/word means
  - Expanded full word
  - Grammatical function
- **Structure panel** shows:
  - Message type (Statement/Question/Trade)
  - Word order analysis
  - Grammar components
- **Interactive legend** on the right explains colors

### Expand Mode Features 📖
**Click "Expand" button to activate:**
- **English Translation** at the top with full context
- **Word-by-Word Breakdown** showing:
  - Original Carnite → Expanded meaning
  - `dmd` → diamond
  - `2.5dmd` → 133 diamonds (2 stacks + 5)
  - `;` → my civilization
- **Tense Context** from banner color
- Perfect for **double-checking** messages before sending

### Reading Messages
1. Messages automatically parsed when using Carnite Protocol
2. **Hover over messages** to see:
   - Full tense/mood explanation
   - Expanded abbreviations
   - Symbol meanings
3. Switch between **Raw** and **Messages** tabs
4. Color-coded by banner type

### Protocol Selection
1. Open **Channel Settings** (Advanced button)
2. Click **"Protocol"** to cycle between Telegraph/Carnite
3. Carnite Protocol enables all integration features
4. Settings persist and auto-save

## Technical Implementation

### Parser Architecture
```
Message → Tokenizer → Token Stream → Grammar Analyzer → ParsedMessage
                                                      ↓
                                            ValidationResult
```

### Integration Points
- `MapDecorationsScreen`: Auto-selects composer based on protocol
- `NotificationManager`: Carnite-aware toast notifications
- `MessageListWidget`: Tooltip support for hover expansions
- `ChannelSettings`: Protocol persistence and selection

### Extensibility
- Add new abbreviations in `CarniteVocabulary.COMMON_ABBREVIATIONS`
- Extend `CarniteValidator` for custom validation rules
- Customize `CarniteParser` for advanced grammar features
- Add new message types to `CarniteMessageType` enum

## Learning Mode Examples

### Example 1: Simple Message
**Input:** `~rd| ;`  
**Banner:** Red

**Learning Mode shows:**
- `~` = Plural/About (PURPLE)
- `rd` = raiders (YELLOW abbreviation)
- `|` = Agent/Individual marker (GRAY)
- `;` = My civilization (CYAN entity)

**Structure:** Statement, Present Urgent  
**Expand shows:** "[Urgent/High Priority] some raiders at my civilization"

### Example 2: Trade Offer
**Input:** `.brd,32irn ; _:`  
**Banner:** Yellow

**Learning Mode shows:**
- `.` = Stack of 64 (GRAY)
- `brd` = bread (YELLOW abbreviation) 
- `,` = Property connector (ORANGE)
- `32` = Number 32 (GREEN)
- `irn` = iron (YELLOW abbreviation)
- `;` = My civ (CYAN)
- `_` = Question blank (MAGENTA)
- `:` = Your civ (CYAN)

**Structure:** Trade Offer  
**Expand shows:** "[Trade Offer] my civilization offers 64 bread and 32 iron, what will you give?"

### Example 3: Complex Military
**Input:** `2bld|5 CN: ; atk`  
**Banner:** Red

**Learning Mode shows each part:**
- `2` = quantity (GREEN)
- `bld` = builder abbreviation (YELLOW)
- `|` = agent marker (GRAY)
- `5` = level 5 (GREEN)
- `CN` = Carnation civ (CYAN)
- `:` = addressing them (CYAN)
- `;` = my civ (CYAN)
- `atk` = attack (YELLOW)

**Expand shows:** "We (my civilization) are sending 2 level-5 builders to attack Carnation"

## Future Enhancements (Possible)
- [ ] Auto-complete while typing (with dropdown)
- [ ] Civ abbreviation management/dictionary
- [ ] Trade offer acceptance UI
- [ ] Message threading with `^` responses
- [ ] Custom symbol/abbreviation sets per server
- [ ] Carnite language documentation in-game book
- [ ] Export Carnite grammar guide
- [✅] **Learning Mode** - IMPLEMENTED!
- [✅] **Expand/Translation Mode** - IMPLEMENTED!

## Credits
Based on "Carnite Telegraphic v1.0" by Discord user BlueEnby with help from MapleSamara, designed for the Map Telegraph system by Adara (ilikeairships).
