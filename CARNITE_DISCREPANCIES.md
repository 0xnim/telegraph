# Carnite Telegraphic: Original Spec vs Code Implementation

This document compares the original Carnite specification (by BlueEnby & MapleSamara) with the actual code implementation in the Telegraph mod.

## ✅ Matches (Correctly Implemented)

### Word Order
- **Both agree:** Od Oi S V (Object-direct, Object-indirect, Subject, Verb)
- **Examples match:** "dmd CN ; take" structure

### Core Symbols
- `|` = agent/player ✅
- `:` = your civ ✅
- `;` = my civ ✅
- `,` = property of ✅
- `&` = and ✅
- `.` = stack (64) ✅
- `_` = question blank ✅
- `^` = response ✅
- `~` = plural/about ✅
- `::` = all civs ✅
- `-` = not/negate ✅

### Stack System
- Both use base 64 (STACK_SIZE = 64) ✅
- Stack notation: `2.5dmd` = 2 stacks + 5 diamonds ✅

## ⚠️ Discrepancies Found

### 1. Banner Color Naming
**Original Doc** vs **Code Implementation**

| Color | Original Name | Code Name |
|-------|---------------|-----------|
| White | "Present Statement" | "Present Statement" ✅ |
| Light Grey | "Past Statement" | "Past Statement" ✅ |
| Dark Grey | "Future Statement" | "Future Statement" ✅ |
| Pink | "Conditional/Might" | "Conditional/Might" ✅ |
| Red | "Present Urgent/High Prio." | "Urgent/High Priority" ⚠️ (wording) |
| Light Blue | "Request/Command/Suggestion" | "Request/Command" ⚠️ (missing "Suggestion") |
| Black | "Opinion/Decision" | "Opinion/Decision" ✅ |
| Blue | "Y/N Question" | "Y/N Question" ✅ |
| Yellow | "Trade" | "Trade Offer" ⚠️ (added "Offer") |
| Purple | "Goal/Current Objective" | "Goal/Objective" ⚠️ (removed "Current") |

**Impact:** Minor wording differences, no functional impact.

### 2. Grammar Detection

**Original Doc Example:** `frm NT CN bld`
- Translation: "Carnation is building a farm right now for Nautilus"

**Code Implementation:**
- This specific example not found in tests
- Code may handle this differently with Od Oi S V structure

**Original Doc Example:** `NT CN,dp|`
- Translation: "There is a diplomat at Nautilus from Carnation"

**Code Implementation:**
- Property notation `CN,dp|` works as specified ✅
- Agent marker handling matches ✅

### 3. Question Types

**Original Doc** has detailed question type table (Table 3) with examples like:
- `~dmd CV _| take` = "Who stole diamonds from Cannabis Village?"
- `_ CV ~rd| take` = "What did raiders steal from Cannabis Village?"

**Code Implementation:**
- `CarniteParser` detects question via `_` character ✅
- `CarniteTranslator.translateQuestion()` implements question logic ✅
- Question position determines type (Who/What/When/Where/Why/How) ✅

**Verified in code:** The question detection logic matches the original spec.

### 4. Trade Grammar

**Original Doc:**
- States yellow banner has "Nonstandard grammar"
- Format: offering items `;` then requesting items `:`
- Example: `2.brd,32irn,.bndg; _::` = "My civ offers 2 stacks of bread, half a stack of iron, and 1 stack of bandages. What does another civ want to trade to us for that?"

**Code Implementation:**
- `CarniteParser.isTradeMessage()`: checks for both `;` and `:` ✅
- Trade detection: `message.contains(";") && message.contains(":")` ✅
- `CarniteTranslator.translateTradeOffer()` implements special grammar ✅

**Match:** Implementation follows original spec ✅

### 5. Specific Translation Examples

**Original Doc Example:** `; 2crt|`
- Translation: "Two cartographers were at my civ"

**Code Implementation Issue:**
- The code would translate this differently based on banner color
- Without banner color context, translation behavior unclear

**Original Doc Example:** `~rd| ;`
- Translation: "Some raiders at my civilization" (or similar)

**Code Implementation:**
- Tests show: `~rd| ;` translates correctly ✅

### 6. Missing from Original Doc but in Code

**CarniteVocabulary System:**
- Code has dynamic vocabulary with abbreviations (e.g., `dmd` → "diamond")
- Code has civilization registry (`registerCivilization()`, `getAllCivilizations()`)
- Original doc doesn't specify vocabulary expansion rules in detail

**CarniteValidator:**
- Code has comprehensive validation system
- Checks message length limits (32-38 chars)
- Validates symbol usage
- Original doc mentions character limits but less formally

**CarniteExplainer:**
- Code has explanation system to help users understand messages
- Not mentioned in original doc

### 7. Verb Conjugation Differences

**Original Doc Examples show tense changes:**
- Present: "is building"
- Past: "built"
- Future: "will build"

**Code Implementation (`CarniteTranslator.conjugateVerb()`):**
- ✅ PRESENT → "is building" (progressive)
- ✅ PAST → "built"
- ✅ FUTURE → "will build"
- ✅ CONDITIONAL → "might build"
- ✅ REQUEST → "should build"
- ✅ DECISION → "decided to build"
- ✅ GOAL → "goal is to build"

**Match:** Implementation matches original spec ✅

### 8. Pluralization

**Original Doc Example (Table 4):**
```
blss,fd → A piece of blessed food
~blss,fd → Some blessed food
2blss,fd → 2 blessed food
```

**Code Implementation:**
- `parseNounPhrase()` handles quantity + abbreviation ✅
- Mass nouns (food, iron, gold, etc.) not pluralized ✅
- `~` prefix adds "some" or "around" ✅

**Match:** Implementation correct ✅

### 9. Agent Level Notation

**Original Doc:** `2mn|5` = "2 level 5 miners"

**Code Implementation:**
- `parseAgentPhrase()` handles count, role, and level ✅
- Format: `[count][role]|[level]` ✅
- Example tested: `bld|5` → "level 5 builder" ✅

**Match:** Implementation correct ✅

### 10. Response Marker (`^`)

**Original Doc Example:**
```
^ -acpt ; trd → "We don't accept the stated trade"
```

**Code Implementation:**
- `CarniteTokenType.RESPONSE` for `^` character ✅
- `translateResponse()` handles response translation ✅
- Can parse `^CIV:` format for responses to specific civs ✅

**Match:** Implementation correct ✅

## 🔍 Potential Problems in Original Doc

### 1. **Inconsistent Examples**
Original doc example: `mtgm FTN` appears twice:
- As Y/N Question (blue banner): "Is Fortun metagaming?"
- As Present Statement (white banner): "Fortun is metagaming."

**Problem:** Same message, different meaning based only on banner color. This is actually correct behavior, but could confuse learners.

### 2. **Character Limit Not Enforced**
Original doc has long examples like:
```
2.brd,32irn,.bndg; _::
```
This is 19 characters (fine), but some examples in Table 6 are much longer when written out.

**Code Implementation:** Validator warns at 32+ characters, which is more practical.

### 3. **Double Stack Notation Ambiguity**
Original doc: `.bndg` = "1 stack of bandages"

But also: `2.` = "2 stacks"

**Problem:** Is `.item` always 1 stack, or could it be confused with decimal notation?

**Code Implementation:** Handles this correctly with regex pattern `(\d+)\.(\d*)([a-z,]+)`

### 4. **Agent Notation Ambiguity**
Original doc shows: `CN,dp|` = "Carnation diplomat"

But also: `~NM,rd|` = "some Nowy Madagaskar raiders"

**Problem:** When is the civ name possessive vs descriptive?

**Code Implementation:** Handles this with plural logic:
- Without `~`: Possessive ("Carnation's diplomat")
- With `~`: Descriptive ("the Carnation raiders")

### 5. **Word Order Violations in Examples**
Original doc example: `; 2crt|`
- Translation: "Two cartographers were at my civ"

**Problem:** This doesn't follow Od Oi S V structure. It's just S (subject).

**Code handles this:** Allows incomplete sentences (no verb = "is at" implied).

### 6. **Quoted Terms (`''`) Not Implemented**
Original doc shows `'acpt'` for quoting terms.

**Code:** Has `CarniteTokenType.QUOTED` for single quotes, but unclear if fully implemented for this use case.

### 7. **Time Notation**
Original doc example: `5m PH: ~CSD|` = "THE CRUSADERS WILL BE AT PROMISED HOME IN 5 MINUTES!!!"

**Problem:** Time units like `5m` (5 minutes), `2h` (2 hours) not formally defined in original doc.

**Code:** May not handle time notation specially.

## 📋 Summary

### Critical Issues: None
The code implementation is generally faithful to the original specification.

### Minor Discrepancies:
1. ⚠️ Wording differences in tense names (no functional impact)
2. ⚠️ Some advanced features from examples not fully specified
3. ⚠️ Vocabulary expansion system added in code but not in original spec
4. ⚠️ Validation system more robust in code than original spec suggests

### Improvements in Code:
1. ✅ Character limit validation (32-38 chars)
2. ✅ Dynamic civilization registry
3. ✅ Vocabulary abbreviation system
4. ✅ Message explanation system
5. ✅ Comprehensive validation with suggestions

### Original Doc Issues:
1. ❌ Time notation used but not defined
2. ❌ Some examples don't strictly follow Od Oi S V
3. ❌ Quoted terms notation not fully explained
4. ❌ Some ambiguity in agent/civ property notation (resolved in code)

## Recommendation

The code implementation should be considered the **authoritative specification** as it:
1. Resolves ambiguities from the original doc
2. Adds practical features (validation, explanations)
3. Implements the grammar more consistently
4. Handles edge cases better

The original document is excellent for **learning and understanding** the language design philosophy, but the code is the **definitive reference** for actual usage.
