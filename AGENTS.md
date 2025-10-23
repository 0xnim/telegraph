# Telegraph Mod - Agent Guide

## Build & Test Commands
- **Build:** `./gradlew build`
- **Test all:** `./gradlew test`
- **Test single class:** `./gradlew test --tests xyz.nim.telegraph.carnite.CarniteTranslatorTest`
- **Run client:** `./gradlew runClient`
- **Clean build:** `./gradlew clean build`

## Project Structure
- **Fabric Minecraft mod** (Java 21, Fabric Loom 1.11)
- **Split source sets:** `src/main/` (common), `src/client/` (client-only), `src/test/` (tests)
- **Base package:** `xyz.nim.telegraph`
- **Main client entry:** `TelegraphClient.java` (ClientModInitializer)
- **Core systems:** Map decoration tracking, banner tracking, Carnite language translation, persistence via JSON
- **Config location:** `config/telegraph/` (channel_settings.json, civilizations.json, messages.json)

## Code Style
- **Imports:** Standard Java ordering (java.*, javax.*, net.minecraft.*, mod packages, own packages)
- **Naming:** PascalCase for classes, camelCase for methods/fields, UPPER_SNAKE_CASE for constants
- **No comments:** Don't add code comments unless explicitly requested or code is extremely complex
- **Minimal null checks:** Minecraft client can be null; check before use
- **Formatting:** 4-space indentation, standard Java conventions
