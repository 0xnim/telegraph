# Telegraph v1.0 - Initial Release

## 🎉 Welcome to Telegraph!

Telegraph is a Minecraft mod that brings **Carnite Telegraphic v1.0** - a condensed language designed for inter-settlement communication - to life with real-time translation, interactive learning tools, and intelligent message composition.

## 🌟 What is Carnite Telegraphic?

Carnite Telegraphic is a specialized language created by Discord users BlueEnby and MapleSamara for the telegraph communication system proposed by ilikeairships. It's designed to be:

- **Concise**: Maximum information in minimal characters
- **Learnable**: Systematic rules that are easy to remember
- **Minecraft-optimized**: Built for banner-based communication
- **Balanced**: Different enough from English to require skill, similar enough to learn quickly

## ✨ Key Features

### 🔤 Complete Carnite Translation Engine
- **Real-time translation** from Carnite to English as you type
- **10 tense colors** via banner selection:
  - White (Present) - "is building"
  - Light Gray (Past) - "built"
  - Gray (Future) - "will build"
  - Pink (Conditional) - "might build"
  - Red (Urgent) - "⚠ URGENT: IS BUILDING!"
  - Light Blue (Request) - "should build"
  - Black (Decision) - "decided to build"
  - Blue (Question) - "Is [civ] building?"
  - Yellow (Trade) - "offers X for Y"
  - Purple (Goal) - "goal is to build"

### 📝 Interactive Message Composer
- **Instant feedback** - Click banner color, see translation update immediately
- **3 view modes:**
  - **Info**: Validation, civs, quick reference
  - **Learn**: Part-by-part breakdown with color-coded explanations
  - **Expand**: Full English translation
- **Quick-insert buttons** for all symbols (|, ;, :, &, ~, _, ^, -)
- **Message templates** for common phrases
- **Validation** with helpful error messages

### 🎓 Learning System
- **Interactive explanations** hover over message parts to understand each component
- **Grammar breakdown** see how word order (Od Oi S V) works in real-time
- **Color-coded parts** (entities, modifiers, questions, verbs)
- **Example messages** with descriptions

### 📊 Supported Grammar

#### Quantity System
- Singular: `blss,fd` = "blessed food"
- Plural: `~dmd` = "some diamonds"
- Specific: `32irn` = "32 iron"
- Stacks: `.dmd` = "64 diamonds (1 stack)"
- Multiple stacks: `2.32brd` = "160 bread (2 stacks + 32)"
- Estimates: `~16fd` = "around 16 food"

#### Symbols
- `|` Agent/Individual - `tdr|` = "a trader", `2bld|5` = "2 level-5 builders"
- `;` My civilization - `; mov` = "my civ might move"
- `:` Your civilization - `CN:` = "to Carnation"
- `~` Plural/About - `~rd|` = "some raiders"
- `_` Question blank - `_ atk` = "Who is attacking?"
- `^` Response - `^ y` = "In response: Yes"
- `-` Negation - `-acpt` = "do not accept"
- `&` And - `CN&EG` = "Carnation and Eastguard"
- `::` All civs - `::` = "to everyone"
- `.` Stack (64) - `.dmd` = "1 stack of diamonds"
- `,` Property - `blss,fd` = "blessed food"

#### Word Order (Od Oi S V)
Carnite uses Object-direct, Object-indirect, Subject, Verb order:
- `~rd SF DR atk` = "Dwarven Republic is attacking some raiders at Sunfish"
- `frm NT CN bld` = "Carnation is building a farm for Nautilus"
- `CN ; atk` = "My civilization is attacking Carnation"

#### Questions
- Who (person): `~dmd CV _| take` = "Who stole diamonds from Cannabis Village?"
- What: `_ CV ~rd| take` = "What did raiders steal from Cannabis Village?"
- When: `_t attack happen` = "When did the attack happen?"
- Where: `~dmd ~rd| _ take` = "Where did raiders steal diamonds from?"
- Why: `attack _rsn` = "Why attack?"
- How: `_ method` = "How?"
- How many: `_dmd take` = "How many diamonds stolen?"

### 🏛️ Civilization Support
50+ pre-configured civilizations including:
- Carnation (CN), Dwarven Republic (DR), Nautilus (NT)
- Cannabis Village (CV), The Crusaders (CRS)
- And many more...

**Configurable:** Add your server's civilizations via config

## 🎮 Usage

1. **Press M** to open the Menu


### Quick Examples

Try these to get started:

```carnite
~rd| ;              (red)   → "⚠ URGENT: Some raiders are at my civilization!"
frm NT CN bld       (white) → "Carnation is building a farm for Nautilus"
; mov               (pink)  → "My civilization might move. Undecided."
_ CV atk            (blue)  → "Who is attacking Cannabis Village?"
170| TWC; elct      (black) → "The Twin Cities decided to elect player 170"
```


## 🔧 Configuration

Located at: `config/telegraph/messages.json`

Configure:
- Custom civilizations
- Abbreviation overrides
- UI preferences
- Channel settings

## ✅ What Works in v1.0

- ✅ All 10 tense colors with proper conjugation
- ✅ Quantities, stacks, and number system
- ✅ Basic to intermediate word order (Od Oi S V)
- ✅ All content questions (Who, What, When, Where, Why, How)
- ✅ Agent phrases with levels
- ✅ Property notation (blessed, secret, etc.)
- ✅ Responses and replies (^ marker)
- ✅ Plural and approximate quantities
- ✅ Real-time translation with instant banner color updates
- ✅ Interactive learning mode
- ✅ Message validation

## ⚠️ Known Limitations (v1.0)

These edge cases are not yet fully supported:

1. **Trade system UI** - Yellow banner trade messages parse correctly, but dedicated trade screen not yet implemented (coming in v1.1)
2. **Complex multi-civ scenarios** - Some advanced patterns with 3+ civilizations
3. **& operator in isolation** - Works in sentences but not standalone
4. **Implied verbs** - Some possession cases ("has librarian" vs "is at")
5. **Standalone negation** - Negation works in sentences but not alone

**Workaround:** Use slightly more explicit phrasing for these cases.

**Impact:** ~25% of advanced/edge-case grammar patterns. Normal gameplay messages work fine!

## 🗺️ Roadmap

### v1.1 (Planned)
- Full trade system GUI with visual offer composer
- Fix remaining grammar edge cases
- & operator standalone support
- Additional civilizations
- Performance optimizations

### v1.2 (Future)
- Message history and search
- Favorite messages/templates
- Advanced filtering
- Dashboard


## 📋 Changelog

### [1.0] - 2025-10-23

#### Added
- Initial release of Telegraph mod
- Complete Carnite Telegraphic v1.0 translation engine
- Message composer with real-time translation
- 10 tense color support via banner selection
- Interactive learning mode with part-by-part explanations
- Content question support (Who, What, When, Where, Why, How, How many)
- Quantity and stack notation system
- Agent phrase support with levels
- 50+ pre-configured civilizations
- Message validation and error reporting
- Symbol quick-insert buttons
- Message templates for common phrases
- 3 view modes (Info, Learn, Expand)
- Instant banner color updates in UI
- Property notation (blessed, secret, etc.)
- Response marker (^) support
- Plural and approximate quantities (~)

#### Known Issues
- Trade system GUI deferred to v1.1 (design complete)
- 14 edge-case grammar patterns not yet supported
- Some complex multi-civilization scenarios
- Standalone & operator not yet implemented

---

**Thank you for using Telegraph! May your messages be swift and your raiders be detected early! ⚠️**
