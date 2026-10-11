# Kadma Story Engine

**Kadma Story Engine** (`kadma`) — a Forge mod for **Minecraft 1.20.1 / Forge 47.3.x / Java 17** that turns a
server into a running narrative: a vertical medieval city called **Кадма**, story chapters,
quests, NPC dialogues, world state, catch-up mechanics, rumors, daily duties, weekly rites,
a lobby bridge ("Порог") and a minimal HUD.

The engine is **server-authoritative**. All gameplay outcomes (quest completion, flags,
rewards, chapter progression, world state, dialogue effects) are computed and written on
the server. The client only renders the HUD and plays local cosmetic effects the server
explicitly requested.

---

## 1. Requirements

| Requirement | Version |
|---|---|
| Minecraft | 1.20.1 |
| MinecraftForge | 47.3.x (see also 47.2.x) |
| Java | 17 (toolchain, no Java 8/11) |
| Gradle | wrapper included (8.8) |
| Mappings | official (`mapping_channel=official`, `mapping_version=1.20.1`) |

No Fabric. No NeoForge. No Spigot/Paper/Bukkit plugins.

---

## 2. Build

```bat
git clone <this repo> kadma
cd kadma
gradlew.bat build
```

On Linux/macOS:

```bash
./gradlew build
```

The build produces:

```
build/libs/kadma-1.0.0.jar               <- ship this
build/libs/kadma-1.0.0-sources.jar       <- sources
```

`build.gradle` uses ForgeGradle 6.0.54 with `net.minecraftforge:forge:1.20.1-47.3.0`
and official Mojang mappings. The `jar` task is finalized by `reobfJar`, so the jar is
directly runnable in a production Forge installation.

Useful tasks:

```bat
gradlew.bat build          :: compile + jar + javadoc-free checks
gradlew.bat runServer      :: dev server on run/ (needs eula=true)
gradlew.bat runClient      :: dev client
gradlew.bat runData        :: data generation (not used by this mod)
```

---

## 3. Installation

1. Build the mod (`gradlew.bat build`), or take the provided `kadma-1.0.0.jar`.
2. Copy `kadma-1.0.0.jar` into your server's `mods/` folder.
3. Start the server once. It creates:
   * `config/kadma/` — content and settings
   * `config/kadma/players/<uuid>.json` — one file per player
   * `config/kadma/global.json` — world state
   * `config/kadma/state.json` — last-known server snapshot (autosave summary)
4. Restart the server after editing content, or use `/kadma reload` for a live reload.

A resource pack is **not** required. The mod functions with vanilla assets only.

---

## 4. First-run / first-test checklist

```
[ ] gradlew.bat build succeeds and build/libs/kadma-1.0.0.jar exists
[ ] server boots, log contains "[kadma] Kadma Story Engine 1.0.0 initialising"
[ ] config/kadma/ created with all 11 JSON files
[ ] /kadma reload prints "Конфиги перезагружены" with a content count
[ ] /kadma npc spawn nima creates an ArmorStand with a floating name tag
[ ] right-click the Nima body prints her lines in chat
[ ] clicking "Осмотреть портал" runs /kadma dialogue choose nima_awakening root 0
[ ] /kadma quest start <player> lower_01_awakening starts the chapter 1 opener
[ ] collecting 3 portal shards (amethyst_shard with kadma.portal_shard=1)
     progresses lower_02_shards
[ ] /kadma status shows chapter, layer, Прозрение, active quests
[ ] /kadma hud off hides the HUD, /kadma hud on restores it
[ ] kill the server, start it again — player data and world state survive
[ ] /kadma flag global set test 1 then /kadma reload keeps the flag
```

Portal shards are created by the mod itself (dialogue effects and quest rewards). To make
them by hand in a creative/command-block context, give an amethyst shard with the NBT:

```
/give <player> minecraft:amethyst_shard{display:{Name:'{"text":"Осколок портала"}'},kadma:{portal_shard:1b}} 1
```

---

## 5. Commands

Admin permission level defaults to **2** and is configurable in `settings.json`
(`adminPermissionLevel`). The whole `admin` branch is gated on it.

### Player commands (no permission needed)

| Command | Effect |
|---|---|
| `/kadma` | usage / help |
| `/kadma status` | your chapter, stage, layer, zone, Прозрение, quests, flags, rumors |
| `/kadma status <player>` | the same for another player |
| `/kadma journal [page]` | your journal as paginated chat with clickable next/prev |
| `/kadma player world` | the world journal |
| `/kadma hud on \| off \| toggle` | HUD visibility |
| `/kadma lobby` / `/kadma threshold` | return to the lobby "Порог" |
| `/kadma enter city` / `/kadma player city` | move from the lobby into the city |
| `/kadma tp <layerId>` | teleport within your current depth band |
| `/kadma zone goto <zoneId>` | teleport to a zone centre |
| `/kadma cycle complete` | finish an arc, get the cycle reward, return to the Threshold |
| `/kadma dialogue choose <dialogueId> <nodeId> <index>` | pick a dialogue option (shell of the chat click) |

### Admin commands

| Command | Effect |
|---|---|
| `/kadma admin reload` | reload all JSON, re-resolve content problems |
| `/kadma admin flag player set <key> <value>` | set a player flag |
| `/kadma admin flag player remove <key>` | delete a player flag |
| `/kadma admin flag global set <key> <value>` | set a global flag |
| `/kadma admin flag global remove <key>` | delete a global flag |
| `/kadma admin quest start <player> <questId>` | start a quest |
| `/kadma admin quest complete <player> <questId>` | force-complete a quest |
| `/kadma admin quest reset <player> <questId>` | wipe a quest's progress |
| `/kadma admin chapter set player <player> <chapterId>` | move a player's chapter |
| `/kadma admin chapter set global <chapterId>` | unlock a world chapter |
| `/kadma admin prozrenie add <player> <n>` | grant Прозрение |
| `/kadma admin prozrenie set <player> <n>` | set Прозрение |
| `/kadma admin npc spawn <npcId>` | spawn an NPC from config |
| `/kadma admin npc spawnall` | spawn every configured NPC |
| `/kadma admin npc clear` | remove every mod-spawned NPC |
| `/kadma admin marker clear` | remove every quest marker |
| `/kadma admin tp <layerId>` | teleport any layer (admin alias) |
| `/kadma admin zone goto <zoneId>` | teleport to a zone (admin alias) |
| `/kadma admin event start <eventId>` | start a weekly rite |
| `/kadma admin event stop` | stop the running rite |
| `/kadma admin rumor add <rumorId>` | feed a rumor to a player (or broadcast) |
| `/kadma admin debug on \| off` | verbose logging |
| `/kadma admin reset daily <player>` | reset daily counters and Прозрение regen |
| `/kadma admin cycle complete <player>` | close another player's cycle |
| `/kadma admin cycle reset <player>` | reset the cycle for a player, keeping history |
| `/kadma admin save` | flush all pending data immediately |

### Headless verification

The mod ships a command smoke test that runs against the real Brigadier dispatcher
with no client attached:

```
gradlew.bat runServer -Pkadma.selftest=1
```

It boots the dev server, executes the whole command tree, prints a
`[kadma] selftest ... 25/25 ...` line and halts the server. Use this after editing
content to confirm nothing broke.

---

## 6. Configuration

All content lives under `config/kadma/`. The mod ships bundles of these files inside the
jar (`/default-configs/...`) and copies each one into `config/kadma/` when it is missing,
so a broken or hand-edited file can always be restored by deleting it.

| File | Purpose |
|---|---|
| `settings.json` | reset hour, timezone, Прозрение rules, duty limits, permission level, HUD cadence, autosave, marker TTL, lobby/city coordinates, feature switches |
| `layers.json` | vertical layers (`lobby`, `lower`, `craft`, `trade`, `temple`) with Y ranges, optional X/Z boxes, ambience, particles and access rules |
| `zones.json` | named boxes inside layers, used by goto/survive/kill/interact/event objectives |
| `items.json` | logical story items mapped onto vanilla bases |
| `npcs.json` | NPC definitions, spawn anchors, dialogue roots |
| `dialogues.json` | dialogue trees with conditions, lines, choices and effects |
| `quests.json` | quest definitions with objectives and rewards |
| `chapters.json` | chapter windows, thresholds, entry/late/aftermath quests |
| `rumors.json` | rumor definitions with triggers, contradictions, cooldowns |
| `events.json` | weekly rites with phases, boss bars, rewards and aftermath |
| `world_states.json` | initial global stats |

`config/kadma/players/<uuid>.json` and `config/kadma/global.json` are written on change,
flushed on the autosave interval (default 300s) and on server stop.

All writes are atomic: content goes to a `.tmp` sibling first and is then moved over the
final path, so a crash mid-save cannot corrupt a file. A corrupt player file is moved
aside as `.broken` and a fresh record is started instead of crashing the boot.

---

## 7. How the systems work

### Chapters and windows
Each chapter has `openDays`, `requiredGlobalPoints`, `requiredPlayerCount`, `entryQuests`,
`lateEntryQuests`, `aftermathQuests` and a `nextChapter`. On the daily reset the engine
recomputes "global points" from completed main stages, shard totals, side work, rite
participation and flags that many players hold. When the thresholds are met (or the
window closes and the chapter is configured to extend), the world chapter advances.
If the window closes while the server is behind, the chapter is **extended** rather than
locked, so the story never walls players out.

### Прозрение ("Insight")
The personal main-story resource.

* `maxProzrenie` (default **3**) caps it;
* it regenerates **1 per configured day**;
* unused units **carry over** and are tracked in `prozrenieCarried`, never dropped on the spot;
* main story stages spend it (`requiresProzrenie`);
* side content, catch-up and rite participation grant small amounts back.

Only the server grants or spends it. There is no client packet that can change it.

### Catch-up
A player who enters a chapter late gets `lateEntryQuests` instead of the missable content.
Catch-up quests are flagged `catchUp: true`, grant the **same essential flags** as the
content they replace (`flagsOnComplete`), and use present-tense text that describes what
the city looks like now. `CatchUpService` offers a catch-up quest automatically on layer
change, city entry and the daily reset.

### Daily duties
`DailyService` runs at `dailyResetHour` in `timezoneOffsetMinutes`. On reset it
regenerates Прозрение (capped for players who were offline a long time), clears daily
counters, processes the chapter window, refreshes rumors and writes everything to disk.
A player who was offline across many resets is *not* punished: they receive the carried
resource and are routed through catch-up.

### Weekly rites
`WeeklyEventService` starts rites from `schedule` (weekday + hour) or via
`/kadma admin event start`. Phases run in order (`announce` → `gather` → `defend` →
`conclusion`), show a boss bar per player, can modify world stats, give participation
rewards and leave rumors behind. `aftermathQuest` is what a player who missed the rite
can investigate later, with the same critical flags.

### Rumors
Rumors are *signals*, not truth. They fire from world-stat thresholds, quest completions,
global flags, rite outcomes or contradictions between other rumors, and they are stored
per player (`knownRumors`) with an unread count on the HUD. They can unlock side quests
(`unlocks`) and gate dialogue (`rumor_known` / `rumor_unknown` conditions).

---

## 8. Adding content

### Add a quest

```json
{
  "id": "daily_lower_water_run",
  "title": "Ходка за водой",
  "type": "daily",
  "chapter": "act1_lower",
  "stage": 1,
  "layer": "lower",
  "zone": "channel",
  "dailyLimit": 3,
  "prerequisites": ["lower_01_awakening"],
  "objectives": [
    { "id": "goto_channel", "type": "goto", "zone": "channel",
      "description": "Дойти до канала" },
    { "id": "carry", "type": "survive", "zone": "channel",
      "ticks": 600, "persistProgress": true,
      "description": "Донести воду" }
  ],
  "rewards": {
    "items": [{"itemId": "portal_shard", "amount": 1}],
    "experience": 15,
    "stats": {"corruption": -1}
  },
  "flagsOnComplete": ["player.carried_water"]
}
```

Then `/kadma admin reload`.

Supported objective types:

`goto`, `talk`, `collect`, `kill`, `interact`, `choose`, `deliver`, `survive`, `timer`,
`use_item`, `enter_layer`, `reach_flag`.

### Add a dialogue

```json
{
  "id": "mira_ledger",
  "npc": "mira",
  "root": "root",
  "nodes": [
    {
      "id": "root",
      "text": ["Полки реагируют на шаги."],
      "conditions": [{"type": "player_flag", "key": "player.met_mira", "value": "true"}],
      "choices": [
        { "index": 0, "text": "Показать свиток",
          "next": "handover",
          "conditions": [{"type": "has_item", "itemId": "salted_ledger", "count": 1}] }
      ]
    },
    {
      "id": "handover",
      "text": ["Это моя запись. Имя совпадает."],
      "effects": [
        {"type": "complete_quest", "questId": "lower_04_mira"},
        {"type": "add_journal", "journal": "Мира нашла себя в расходе."},
        {"type": "change_stat", "stat": "schism", "amount": 2}
      ]
    }
  ]
}
```

Conditions available: `player_flag`, `global_flag`, `has_item`, `quest_completed`,
`quest_active`, `quest_not_completed`, `layer`, `zone`, `chapter`, `stage_min`,
`stage_max`, `prozrenie_min`, `stat_min`, `stat_max`, `rumor_known`, `rumor_unknown`,
`npc_met`, `dialogue_unlocked`, `spark`, `entered_city`, `always`.

Effects available: `set_player_flag`, `set_global_flag`, `remove_player_flag`,
`add_journal`, `add_rumor`, `start_quest`, `complete_quest`, `give_item`,
`change_stat`, `set_stage`, `unlock_dialogue`, `prozrenie`, `met_npc`, `title`,
`enter_city`, `to_lobby`.

### Add an NPC

```json
{
  "id": "dusa",
  "displayName": "Дуса",
  "title": "Смотрительница стока",
  "layer": "lower",
  "zone": "channel",
  "dialogueRoot": "aftermath_missing_event",
  "spawn": { "world": "minecraft:the_overworld", "x": -8.5, "y": 32.0, "z": 22.5, "yaw": 0 }
}
```

`/kadma admin npc spawn dusa` spawns an ArmorStand body (invulnerable, persistent, silent,
no gravity, no base plate) with `kadma.npc="dusa"` in its persistent data and a
`TextDisplay` label above it holding the title.

### Add a chapter

```json
{
  "id": "act6_surface_placeholder",
  "title": "Акт 6: Поверхность",
  "order": 6,
  "openDays": 14,
  "requiredGlobalPoints": 160,
  "requiredPlayerCount": 1,
  "entryQuests": [],
  "lateEntryQuests": [],
  "aftermathQuests": [],
  "nextChapter": "",
  "layer": "temple",
  "journalOnOpen": ["Выход наверх открыт."],
  "rumorsOnOpen": ["priest_carries_water"],
  "globalEffects": { "stats": {"faith": -2}, "flags": ["surface.open"] }
}
```

---

## 9. Narrative design rules used by this mod

The story is told in the **present** and through **consequences**, never through exposition:

* **no flashbacks** — a player never sees a scene from another time;
* **no prophecies** — nothing predicts the future;
* **no "long ago"** phrasing, no past or future tense;
* what the player learns comes from rumors (which contradict each other), documents,
  NPC contradictions, environmental detail (wet stone, black cracks, salt on walls,
  children drawing doors without handles) and their own observation;
* world states have weight: faith, corruption, schism and sparks change what NPCs say and
  what the city looks like;
* story "progress" is visible as physical state (a door without a handle, a wall sewn
  shut, sweet water in the well), not as a cutscene.

The sample content follows these rules; keep them when adding more.

---

## 10. Known limitations

* The vertical city is **position-based**: `layers.json` uses Y ranges plus optional X/Z
  boxes. If you build a city with overlapping Y bands in the same XZ, the most specific
  (X/Z-bounded) layer wins, and a second fallback layer is applied per Y.
* NPC bodies are vanilla ArmorStands, so they cannot hold custom animations and their
  skin is the default player model. `marker: true` keeps them invisible with only the
  floating `TextDisplay` label.
* `ArmorStand.setMarker()` and `setSmall()` are not reachable from the official mappings
  without an access transformer, so `npc.marker` currently renders as an invisible
  ArmorStand with a `TextDisplay`.
* Distance-based objective ordering ("walk to this exact block") is approximated by a
  zone or a relative anchor, not by pathfinding.
* Only one weekly rite can run at a time; the runner is single-instance per event id.
* Parchment parameter-name mappings are not used, so generated javadoc uses Mojang names.
* The HUD is a fixed corner block. The corner can be changed with the JVM property
  `-Dkadma.hud.corner=top-right|bottom-left|bottom-right`.
* Dialogue choice handling is chat-command based by design; it does not draw GUI screens.
* Java 17 toolchain is enforced. Building with Java 8 or 11 will fail.

---

## 11. Roadmap

* [ ] Parchment mappings for readable parameter names.
* [ ] Access transformer for `ArmorStand.setMarker` / `setSmall`.
* [ ] Data generation (`runData`) for lang and default config deployment.
* [ ] Per-layer custom boss bars and per-layer music discs.
* [ ] Multiple simultaneous weekly rites with cross-event consequences.
* [ ] Editor commands (`/kadma admin editor zone`) to create zones by walking a box.
* [ ] Optional resource pack that adds custom models for the portal and the seal door.
* [ ] Quest chains with branching outcomes tracked as world state.
