# Telegraph Transfer Protocol (TTP)

**Version:** 1.0
**Date:** 2025-12-13
**Status:** Release Standard

---

## 1. Introduction

The **Telegraph Transfer Protocol (TTP)** is a human-readable messaging standard for long-distance communication in Minecraft. It is designed to facilitate communication between distant settlements (Civilizations) where travel is difficult and chat range is limited.

### 1.1 Design Philosophy

TTP is **designed for humans, not software**. Every message can be composed, transmitted, and decoded by hand using only the rules in this document. While automation tools may exist, they are optional conveniences—the protocol assumes a human operator at each tower.

This means:
- **No memorization required.** Keep this reference card at your station.
- **No calculations.** All fields are simple character counts.
- **No dependencies.** Works the same whether you're the only tower or one of sixteen.

### 1.2 The Mechanism

TTP uses the **Map** mechanic. When a banner is placed within a mapped area, the map updates **instantly and globally** for all copies. A message placed at Tower 3 appears on Tower 7's wall the same moment—regardless of distance.

### 1.3 Constraints

* **Message Size:** 50 Characters (maximum length of a renamed banner).
* **Characters:** A-Z, 0-9, and basic punctuation.
* **Network:** Up to 16 Towers (identified by Hex IDs `0-F`).

---

## 2. Physical Infrastructure

### 2.1 The Tower
Every Civilization should maintain a Telegraph Tower with the following setup:
1. **Outbound Map:** A map covering the tower's immediate area (Scale 1:1 or 1:2 recommended).
2. **Inbound Array:** A wall of Item Frames containing copies of the Outbound Maps from every other tower.
3. **Encoding Station:** An Anvil and a supply of White Banners.
4. **Experience Supply:** Bottles o' Enchanting or a local farm (renaming requires 1 XP level).
5. **Message Board:** A public area where the operator posts incoming messages for local citizens.

### 2.2 The Message Board

The Message Board is where the operator posts public messages for local citizens to read. This serves as the "last mile" for public communications.

**Board Options:**
- **Map Board:** A dedicated map where the operator places named banners—same mechanic as the telegraph, but for local display.
- **Sign Wall:** Operator transcribes decoded messages onto signs.
- **Lectern Library:** Decoded messages written into Books & Quills on lecterns.

Organize by date, sender, or topic as suits your civilization.

**Message Types:**
- **Public:** No personal address in payload → Post to Message Board.
- **Private:** Has personal address (e.g., `BLUWOO:`) → Deliver to recipient's mailbox.

---

## 3. Envelope Specification

TTP uses a **Fixed-Width Positional Header** designed to be easy to read and write by hand.

### 3.1 Packet Types

| Type            | Header Format | Header Len | Payload Space | Usage                 |
| :-------------- | :------------ | :--------- | :------------ | :-------------------- |
| **Single-Part** | `SDI `        | 4 chars    | **46 chars**  | Short messages & ACKs |
| **Multi-Part**  | `SDIpT `      | 6 chars    | **44 chars**  | Long messages         |

### 3.2 Field Definitions

* **S (Source):** Hex ID (`0-F`) of the sending tower.
* **D (Destination):** Hex ID (`0-F`) of the target (`*` for Broadcast).
* **I (Message ID):** Base-36 ID (`0-Z`). Unique to the message sequence.
* **p (Part ID):** Base-36 ID (`1-Z`). Sequential counter for multipart messages.
* **T (Total Parts):** Base-36 ID (`1-Z`). Total number of banners in message.
* **` ` (Space):** Mandatory delimiter.

---

## 4. Addressing (Routing)

To deliver a message to a specific player ("Last Mile Delivery"), use the **3x3 Hash** at the very start of the payload.

### 4.1 The 3x3 Hash Rules
1. Take the **First 3 Letters** of the Username (First Name).
2. Take the **First 3 Letters** of the Surname (Last Name).
3. Convert to **UPPERCASE**.
4. Append a colon `:`.

**Handling Edge Cases:**
* **Short Names:** Pad with `X` if a name is shorter than 3 letters.
* **Placement:** The address is only required on the **first banner** of a message.

### 4.2 Examples
* `Iron_Golem` → `IROGOL:`
* `Red_Bed` → `REDBED:`
* `Jo_Yu` → `JOXYUX:`

---

## 5. Protocol Examples

### 5.1 Standard Direct Message
**Context:** Tower 3 sends to Tower 7.  
**Recipient:** `Blue_Wool` (`BLUWOO`).  
**Message:** "Gates open".

~~~text
37A BLUWOO:Gates open
~~~
*(Header: 37A | Payload: BLUWOO:Gates open)*

---

### 5.2 Multi-Part Message
**Context:** Tower 3 sends a long report to Tower 7.  
**Recipient:** `King_Steve` (`KINSTE`).  
**Message:** "The northern border has been secured. Patrols report no movement in the valley."  
**Total Parts:** 2

**Banner 1 (ID: B, Part: 1 of 2):**
~~~text
37B12 KINSTE:The northern border has been secur
~~~

**Banner 2 (ID: B, Part: 2 of 2):**
~~~text
37B22 ed. Patrols report no movement in valley.
~~~

---

### 5.3 Acknowledgement (ACK)
**Context:** Tower 7 confirms they received Message ID `B` from Tower 3.
**Format:** `OK ID`

~~~text
73C OK B
~~~
*(Tower 7 uses local ID 'C' to carry the confirmation payload)*

---

### 5.4 Retransmit Request
**Context:** Tower 7 received parts 1 and 2 of `B` but is missing part 3.
**Format:** `NEED IDp`

~~~text
73D NEED B3
~~~

---

### 5.5 Status Announcement
**Context:** Tower 3 announces it will be offline for maintenance.
**Format:** `STATUS message`

~~~text
3*A STATUS OFFLINE 3 DAYS
~~~
*(Broadcast to all towers using `*` destination)*

~~~text
3*B STATUS ONLINE
~~~
*(Tower back in service)*

---

## 6. Operational Manual

### 6.1 Sending a Message
1. **Draft:** Write the message on a sign or book first to verify spelling.
2. **Address:** Calculate the 3x3 Hash for the recipient.
3. **Anvil:** Rename the banner(s) using the TTP format.
4. **Transmit:** Place the banner(s) on your Outbound Map.
5. **Wait:** Do not remove banners until you receive an ACK from the destination.

---

### 6.2 Receiving a Message
1. **Scan:** Check your Inbound Wall for new banners.
2. **Verify:**
    * Read the header.
    * If Multi-Part (`SDIpT`), use `T` to determine the total count.
        * e.g. `37B13` means **Part 1 of 3**.
    * Only decode after **all parts are present**.
3. **Missing parts:** If incomplete, send a retransmit request (`NEED Bp`).
4. **Deliver:** Post public messages to the Message Board. Deliver private messages (with personal address) to the recipient's mailbox.
5. **Confirm:** Send an ACK banner back to the source tower.

---

### 6.3 Cleanup
* **Sender:** Upon receiving an ACK, break your banners. This clears the map for new messages.
* **Receiver:** Upon seeing the sender’s banners disappear, remove your ACK banner.

---

## 7. Operator Reference Card (In-Game Book)

~~~text
[ TTP v1.0 REFERENCE ]

MAX LEN: 50 Chars

HEADER FORMATS:
Single: S D I [Space] Payload
Multi : S D I p T [Space] Payload

S = Source Tower (You)
D = Destination (* = All)
I = Message ID (0-9, A-Z)
p = Current Part
T = Total Parts

ADDRESSING (3x3):
First3 + Last3 (UPPERCASE) + :
Ex: Stone_Brick -> STOBRI:

CONTROL CODES:
ACK    : 73X OK A      (Recv Msg A)
NEED   : 73Y NEED A2   (Resend A Part 2)
STATUS : 3*X STATUS OFFLINE 3 DAYS

MULTIPART EXAMPLE:
37B13 (Msg B, Part 1 of 3)
37B23 (Msg B, Part 2 of 3)
37B33 (Msg B, Part 3 of 3)
~~~