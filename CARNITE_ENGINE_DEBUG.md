# Carnite Engine Debugging Guide

This guide helps with debugging and fixing Carnite translation issues.

## Quick Issue Reporting

When reporting a Carnite translation bug, please provide:

1. **Carnite input**: The exact message (e.g., `CN :: ; atk`)
2. **Banner color**: white, pink, blue, red, light_gray, dark_gray, light_blue, black, yellow, purple
3. **Expected output**: What it should translate to
4. **Actual output**: What it currently translates to
5. **Pattern type**: Statement, Question, Trade, Response, etc.

## Common Issue Types

### 1. Word Order Issues (Od Oi S V)
**Symptoms:** Subject and object are swapped, or wrong entity is performing the action

**Debug checklist:**
- [ ] Is the civ abbreviation being handled correctly?
- [ ] Is the ; (MY_CIV) marker setting the right subject?
- [ ] Are directObjects vs indirectObjects being populated correctly?
- [ ] Check `translateStatement()` parsing logic

**Example fix:** See commit for "CN :: ; atk" - CN should be Od, not S

### 2. Agent Phrases (| marker)
**Symptoms:** Agent roles not recognized, levels missing, possessive forms incorrect

**Debug checklist:**
- [ ] Is the property phrase built BEFORE checking for agent marker?
- [ ] Is the level number captured after the agent marker?
- [ ] Does `parseAgentPhrase()` handle civ properties correctly?
- [ ] Are agent nouns in the `toAgentNoun()` whitelist?

**Example:** `NM,smth|5` should parse as "Nowy Madagaskar's level 5 blacksmith"

### 3. Verb Recognition
**Symptoms:** Verbs not captured, sentences incomplete, verbs confused with nouns

**Debug checklist:**
- [ ] Is `shouldBeVerb` condition checking directObjects?
- [ ] Is the verb after all property/agent phrase building?
- [ ] Check if word is in `CarniteConstants.isVerb()`
- [ ] For agent words (rd|), is willBeAgent checked BEFORE verb check?

### 4. Verb Conjugation
**Symptoms:** Wrong tense, "is dieing" instead of "is dying", irregular verbs wrong

**Debug checklist:**
- [ ] Check `conjugateVerb()` for the tense mode
- [ ] Special cases: "die"→"dying", "move"→"moving", "see"→"seeing"
- [ ] Irregular past tense in `getIrregularPast()`

### 5. Banner Colors / Tense Modes
**Symptoms:** Wrong tense interpretation, question vs statement confusion

**Debug checklist:**
- [ ] Check `getTenseModeFromColor()` for color matching
- [ ] Order matters: check light_gray before gray, light_blue before blue
- [ ] Blue banner without `_` should be YES_NO_QUESTION pattern

### 6. Special Symbols
**Symptoms:** ::, ;, :, |, ~, _, ^, & not handled correctly

**Debug checklist:**
- [ ] Check token type in `CarniteParser`
- [ ] Verify symbol handler in appropriate case statement
- [ ] Test with and without symbol to isolate issue

## Testing Workflow

### 1. Add a Test Case
```java
@Test
@DisplayName("Description of what should happen")
void testYourCase() {
    var result = CarniteTranslator.translate("carnite msg", "banner_color");
    assertTrue(result.translation().contains("expected phrase"),
        "Should contain X, but was: " + result.translation());
}
```

### 2. Run the Test
```bash
./gradlew test --tests "*testYourCase"
```

### 3. Add Debug Output
```java
System.out.println("DEBUG: Od=" + directObjects + ", S=" + subject + ", V=" + verb);
```

### 4. Trace Execution
- Check which token case is handling your input
- Verify fullWord after property building
- Check shouldBeVerb conditions
- Verify sentence construction case matched

### 5. Fix and Verify
```bash
./gradlew test --tests "CarniteTranslatorTest"
```

## Architecture Overview

```
CarniteTranslator.translate()
├── CarniteParser.parse() - Tokenize input
├── getTenseModeFromColor() - Determine tense from banner
├── detectPattern() - Statement/Question/Trade/Response
└── Pattern-specific translator:
    ├── translateStatement() - Main Od Oi S V parser
    │   ├── Parse tokens into Od, Oi, S, V components
    │   ├── Handle special cases (agents, properties, civs)
    │   └── constructSentence() - Build English output
    ├── translateQuestion()
    ├── translateYesNoQuestion()
    ├── translateTradeOffer()
    └── translateResponse()
```

## Key Files

- `CarniteTranslator.java` - Main translation engine
- `CarniteParser.java` - Tokenizer
- `CarniteVocabulary.java` - Word expansions
- `CarniteConstants.java` - Verbs, patterns, civ names
- `CarniteTranslatorTest.java` - Test suite

## Common Fixes

### Fix: Civ property phrases not recognized
**Before:** "NM" processed as civ, "smth" processed separately  
**After:** Build "NM,smth" property phrase first, then check for agent marker

### Fix: Level numbers not captured
**Before:** Agent marker consumed, level number skipped  
**After:** Check for level after agent marker, capture and skip both

### Fix: Intransitive verbs missing
**Before:** Only set verb if subject/indirectObjects present  
**After:** Also set verb if directObjects present (Od V pattern)

### Fix: DirectObject becomes subject for intransitive verbs
**Before:** "agent die" → "agent."  
**After:** "agent die" → "agent is dying."

## Performance Notes

- Parser caches are not implemented yet
- Each translation parses from scratch
- Consider adding memoization for repeated messages

## Contributing

When fixing a Carnite engine bug:
1. ✅ Add test case demonstrating the issue
2. ✅ Run existing tests to verify no regressions
3. ✅ Update this debug guide if you discover new patterns
4. ✅ Document the fix in git commit message
5. ✅ Update relevant specification sections if grammar interpretation changes
