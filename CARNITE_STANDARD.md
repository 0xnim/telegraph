# Carnite Telegraphic Standard v1.0

A constructed language using banner colors for tense and symbols for grammar, designed for efficient communication in Minecraft via map decorations.

## Core Principles

**Word Order:** Od Oi S V (Object-direct, Object-indirect, Subject, Verb)
- **What** - Object direct (Od)
- **Where/To** - Object indirect (Oi) 
- **Who** - Subject (S)
- **Action** - Verb (V)

**Example:** `dmd CN ; take` = "CN takes diamonds from my civ"

## Banner Colors (Tense Markers)

| Color | Tense/Mode | Description |
|-------|-----------|-------------|
| White | Present Statement | Present tense statements happening right now |
| Light Grey | Past Statement | Past tense statements about completed actions |
| Dark Grey | Future Statement | Future tense statements about planned actions |
| Pink | Conditional/Might | Conditional statements expressing uncertainty or possibilities |
| Red | Urgent/High Priority | Urgent/high priority present statements requiring immediate attention |
| Light Blue | Request/Command | Requests, commands, or suggestions for action |
| Black | Opinion/Decision | Opinions or decisions that have been made |
| Blue | Y/N Question | Yes/No questions seeking confirmation or correction |
| Yellow | Trade Offer | Trade offers and negotiations (special grammar) |
| Purple | Goal/Objective | Current objectives or goals being pursued |

## Core Symbols

| Symbol | Meaning | Usage |
|--------|---------|-------|
| `\|` | agent/player | Marks a player or agent role |
| `:` | your civ | Refers to the recipient's civilization |
| `;` | my civ | Refers to the sender's civilization |
| `,` | property of | Links properties/attributes |
| `&` | and | Conjunction |
| `.` | stack (64) | Indicates a stack of items |
| `_` | question blank | Placeholder for requested information |
| `^` | response | Marks a response to a previous message |
| `~` | plural/about | Indicates plural or approximate quantity |
| `::` | all civs | Broadcasts to all civilizations on channel |
| `-` | not/negate | Negates the following term |

## Numbers and Quantities

### Basic Numbers
Simple digit sequences represent exact quantities:
- `2dmd` = 2 diamonds
- `16blss,fd` = 16 blessed food

### Stacks
The `.` symbol indicates stacks (64 items):
- `2.5dmd` = 2 stacks + 5 diamonds (133 total)
- `1.` = 1 stack (64 items)

### Approximate Quantities
The `~` prefix indicates approximate or plural:
- `~16blss,fd` = Around 16 blessed food
- `~rd|` = Some raiders

## Agent Markers

The `|` symbol marks agents/players:
- `|` alone = single agent
- `bld|5` = level 5 builder
- `2bld|5` = 2 level 5 builders
- `~rd|` = some raiders

### Civilization-Specific Agents
Use comma notation for civ-owned agents:
- `NM,smth|5` = Nowy Madagaskar's level 5 blacksmith
- `~NM,rd|` = the Nowy Madagaskar raiders

## Property Notation

The `,` symbol links properties:
- `blss,fd` = blessed food
- `NM,rd|` = Nowy Madagaskar raider

## Message Types

### 1. Statements
Follow Od Oi S V structure:
- `dmd CN ; take` = "My civ takes diamonds from Carnation"
- `~rd| ;` = "Some raiders at my civilization"

### 2. Questions (with `_`)
The `_` indicates what information is requested:
- `_ CN ~rd| take` = "What did raiders take from Carnation?"
- `~dmd _| take` = "Who stole some diamonds?"
- `_dmd` = "How many diamonds?"

### 3. Yes/No Questions (Blue Banner)
Blue banner without `_` indicates yes/no question:
- `mtgm FTN` on blue = "Is Fortun metagaming?"
- `CN :: ; atk` on blue = "Is my civilization attacking Carnation?"

### 4. Trade Offers (Yellow Banner)
Format: `[offering]; _:`
- `2.5dmd ; _:` = "My civilization offers 2 stacks + 5 diamonds. What will you give in return?"
- `^CN: 3.irn ; _:` = "In response to Carnation's previous trade offer: My civilization offers 3 stacks of iron. What will you give in return?"

### 5. Responses (with `^`)
The `^` marks responses to previous messages:
- `^yes` = "Yes (in response to previous message)"
- `^CN: 3.irn` = Response to Carnation with counter-offer

### 6. Broadcast (with `::`)
Double colon broadcasts to all civs on channel:
- `dmd :: ; give` = "My civ gives diamonds to all civs on channel"

## Negation

The `-` prefix negates terms:
- `-acpt` = "do not accept"
- `-` before verb = negates action

## Character Limits

- **Optimal:** 32 characters or less for readability
- **Warning:** 32-38 characters (text will be small)
- **Max readable:** 38 characters (text becomes very small on map)

## Mass Nouns

These nouns are never pluralized:
- bread, iron, gold, gunpowder, food, wood, stone, enchant

## Common Verbs

attack, raid, steal, take, give, trade, receive, move, merge, elect, die, kill, surrender, accept, ally, build, mine, call, send, metagaming, gear, get

## Stack Size Constant

**STACK_SIZE = 64**

## Patterns and Examples

### Regular Expressions
- **Civ Abbreviation:** `\b[A-Z]{2,4}\b`
- **Number:** `\d+`
- **Number + Abbreviation:** `(\d+)([a-z,]+)`
- **Stack Pattern:** `(\d+)\.(\d*)([a-z,]+)`

### Example Messages

| Carnite | English |
|---------|---------|
| `dmd CN ; take` | "My civ takes diamonds from Carnation" |
| `~rd| ;` | "Some raiders at my civilization" |
| `_ CN ~rd| take` | "What did raiders take from Carnation?" |
| `2.5dmd ; _:` | "My civilization offers 2 stacks + 5 diamonds. What will you give in return?" |
| `NM,smth|5 die` | "Nowy Madagaskar's level 5 blacksmith is dying" |
| `16blss,fd CN ; give` | "My civ gives 16 blessed food to Carnation" |

## Grammar Rules

1. **Word Order:** Always Od Oi S V (What Where Who Action)
2. **Banner Color:** Determines tense/mode of entire message
3. **Trade Messages:** Should use YELLOW banners
4. **Yes/No Questions:** Use BLUE banners (no `_` needed)
5. **Subject Marker:** `;` indicates "my civ" as subject
6. **Recipient Marker:** `:` indicates "your civ" or recipient

## Validation Guidelines

1. Messages should not exceed 38 characters
2. Trade messages should use yellow banners
3. Symbols must be used correctly (e.g., `::` for broadcast)
4. Word order should follow Od Oi S V structure
5. Civ abbreviations must be 2-4 uppercase letters
6. Agent markers require proper format (role + `|` + optional level)

## Quick Reference

```
Word Order: [What] [Where/To] [Who] [Action]
Example: "dmd CN ; take" = "CN takes diamonds from my civ"

Common Symbols:
| = agent/player   : = your civ   ; = my civ
, = property of    & = and        . = stack (64)
_ = question       ^ = response   ~ = plural/about
:: = all civs      - = not/negate

Numbers: 2.5dmd = 2 stacks + 5 diamonds (133 total)
Levels: bld|5 = level 5 builder
```

---

**Version:** 1.0  
**Implementation:** Telegraph Mod (Fabric Minecraft Mod)  
**Package:** `xyz.nim.telegraph.client.carnite`
