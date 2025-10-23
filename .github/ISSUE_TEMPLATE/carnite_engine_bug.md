---
name: Carnite Engine Bug Report
about: Report incorrect Carnite translations or parsing errors
title: '[CARNITE] '
labels: bug, carnite-engine
assignees: ''
---

## Carnite Translation Issue

### Input Message
**Carnite message:** `[your carnite message here]`  
**Banner color:** [e.g., white, pink, blue, light_gray, etc.]

### Expected Translation
What should the message translate to?

```
[Expected English translation here]
```

### Actual Translation
What does it currently translate to?

```
[Actual English translation here]
```

### Pattern Information
**Message structure:** [e.g., Od Oi S V, Od V, S V, etc.]  
**Components:**
- Direct Object (Od): 
- Indirect Object (Oi): 
- Subject (S): 
- Verb (V): 

### Additional Context
- [ ] This is a regression (it used to work correctly)
- [ ] This involves special patterns (agents, properties, levels, etc.)
- [ ] Multiple similar messages have this issue

**Special patterns involved:**
- [ ] Agent marker (|)
- [ ] Level indicator (e.g., |5)
- [ ] Property notation (e.g., NM,smth)
- [ ] Addressing marker (:: or :)
- [ ] Stack notation (.)
- [ ] Plural marker (~)
- [ ] Question blank (_)
- [ ] Response marker (^)
- [ ] Other: ___________

### Screenshots or Examples
If applicable, add screenshots showing the translation in-game or provide additional examples of similar incorrect translations.

### Carnite Specification Reference
If this relates to a specific part of the Carnite specification document, please reference the section or provide the relevant rule.

---

**For Developers:**
- [ ] Test case added: `src/test/java/xyz/nim/telegraph/carnite/CarniteTranslatorTest.java`
- [ ] Issue reproduced locally
- [ ] Root cause identified
- [ ] Fix implemented and tested
