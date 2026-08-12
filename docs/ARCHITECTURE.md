# Architecture and Integration

This document defines the intended technical boundaries of Realm of the Guard and its communication with Rally of the Guard and Guard Villagers. Names shown for future APIs are design contracts, not implemented classes yet.

## 1. Dependency direction

The dependency graph must remain one-way:

```text
Guard Villagers
      ↑
Rally of the Guard
      ↑
Realm of the Guard
```

Realm may call public Rally APIs. Rally must never import Realm classes or contain concepts such as kingdoms, buildings, taxes or royal titles. This prevents a circular dependency and keeps Rally usable as a lightweight standalone mod.

During foundation work, `fabric.mod.json` lists Rally and Guard Villagers as suggested integrations so the empty project can build and start independently. When Phase 0 delivers the Rally API and Realm begins importing it, both entries must move to required dependencies with explicit minimum versions.

## 2. Responsibility boundaries

| System | Owner | Notes |
|---|---|---|
| Guard entity and native combat AI | Guard Villagers | Realm should not replace its brain unless no supported extension exists. |
| Guard owner and basic hiring | Rally | The canonical tactical ownership relationship. |
| Follow, wait, patrol, route and rally | Rally | Exposed through common-side services instead of packet handlers. |
| Settlement and kingdom membership | Realm | Political ownership is separate from tactical ownership. |
| Registered buildings and territory | Realm | Includes scanning, validation, effects and spatial index. |
| Legitimacy, loyalty and titles | Realm | Player and settlement progression. |
| Treasury, taxes and upkeep | Realm | Uses real inventories and server-authoritative transactions. |
| Military rank and squad | Realm | Rank augments a guard but does not replace Rally orders. |
| Tactical command UI | Rally | Realm contributes additional presentation through an extension API. |
| Domain and kingdom UI | Realm | Never implemented inside Rally. |

## 3. Integration strategy

Cross-mod behavior must use a small public API in Rally. Realm must not invoke Rally networking payloads, access private screen handlers or mix into Rally implementation classes.

The public package should be stable and intentionally small, for example:

```text
net.hfstack.rallyguard.api
├── RallyGuardApi
├── command
│   ├── GuardCommandService
│   ├── GuardCommandType
│   └── GuardCommandResult
├── recruitment
│   ├── GuardRecruitmentEvents
│   ├── RecruitmentContext
│   └── RecruitmentOffer
├── event
│   └── GuardOrderEvents
└── presentation
    └── GuardPresentationProvider
```

The concrete names may change during implementation, but the boundaries below should remain.

### 3.1 Guard command service

Rally currently executes gameplay inside its networking receiver. Those operations should be extracted into common-side services:

```java
GuardCommandResult follow(ServerPlayerEntity commander, GuardEntity guard);
GuardCommandResult wait(ServerPlayerEntity commander, GuardEntity guard);
GuardCommandResult patrol(ServerPlayerEntity commander, GuardEntity guard, BlockPos position);
GuardCommandResult summon(ServerPlayerEntity commander, GuardEntity guard);
GuardCommandResult rally(ServerPlayerEntity commander, Collection<GuardEntity> guards);
```

Rally's own packet receivers call these services. Realm also calls the same services directly on the server. This guarantees that both mods use the same ownership checks, state transitions and cleanup behavior.

No server-side integration should simulate a client packet.

### 3.2 Recruitment policy

Rally should expose a `BEFORE` recruitment event containing:

- player;
- guard;
- default payment item;
- default cost;
- current world and position;
- mutable or replaceable offer;
- allow/deny result with a translatable reason.

Realm uses the event to apply kingdom rules. A recognized ruler might pay from the settlement treasury, consume barracks capacity and require equipment from an armory. Rally remains responsible for completing ownership transfer after every policy accepts the offer.

A separate `AFTER` event informs Realm that recruitment succeeded so it can assign the guard's settlement, starting rank and military record.

The intended flow is:

```text
Player requests recruitment
        ↓
Rally builds default offer
        ↓
Realm evaluates title, settlement, barracks and treasury
        ↓
Realm returns unchanged, modified or denied offer
        ↓
Rally validates and assigns ownership
        ↓
Realm records rank and settlement through AFTER event
```

### 3.3 Order events

Rally should emit `BEFORE` and `AFTER` events for tactical orders.

Realm can use `BEFORE` to enforce political rules, such as preventing a guard assigned to another ruler from joining a squad. It can use `AFTER` for progression, reports and captain behavior.

Listeners must not duplicate the order. Rally remains the single authority that changes tactical state.

### 3.4 Presentation extension

Realm should be able to contribute read-only information to Rally screens without Rally depending on Realm:

- military rank;
- settlement name;
- squad;
- loyalty;
- royal-guard badge;
- optional additional tooltip lines.

Rally defines a provider interface and renders zero or more registered contributions. Realm registers a provider during initialization.

The first API should avoid arbitrary widgets. Text, icons and tooltips form a safer compatibility surface.

### 3.5 Eligibility callbacks

Rally should provide predicates for operations where Realm rules matter:

- may this guard be recruited by this player;
- may this guard join this rally;
- may this commander issue this order;
- should this guard appear in this command list.

Callbacks return structured results rather than booleans so denial messages can be localized.

### 3.6 Compatibility handshake

Realm should declare a minimum compatible Rally version in `fabric.mod.json` when integration becomes required. The Java API should expose an integer contract version in addition to the mod version:

```java
int apiVersion();
```

Patch releases may add API without breaking existing consumers. Breaking contract changes require a major API increment and coordinated minimum-version updates.

## 4. Data ownership

Each fact must have exactly one authoritative owner.

### Rally-owned guard data

- commander UUID;
- current tactical posture;
- rally membership;
- patrol and tactical route state;
- immediate order execution state.

### Realm-owned guard data

- settlement UUID;
- military rank;
- squad UUID;
- service experience;
- loyalty;
- salary or upkeep class;
- royal-guard and command appointments.

### Realm-owned player data

- legitimacy by settlement or kingdom;
- current title;
- kingdom membership and permissions;
- unlocked decrees;
- historical achievements relevant to succession.

### Realm-owned world data

- settlements;
- kingdoms;
- buildings;
- claims and spatial indexes;
- treasuries;
- laws;
- council positions;
- relationships between settlements.

Persistent Fabric Data Attachments are preferred for small records tied directly to players, guards or worlds. Large aggregate indexes may use a dedicated persistent state if attachments prove unsuitable. All serialized records must have a schema version and migration path.

Immutable records and codecs should be preferred so writes are explicit and persistence remains predictable.

## 5. Core domain model

```text
KingdomRecord
├── UUID id
├── String name
├── UUID ruler
├── SettlementId capital
├── Set<SettlementId> settlements
├── Map<UUID, KingdomRole> members
├── Set<DecreeId> decrees
├── DiplomacyState diplomacy
└── int schemaVersion

SettlementRecord
├── UUID id
├── String name
├── RegistryKey<World> dimension
├── BlockPos center
├── UUID ruler
├── Optional<KingdomId> kingdom
├── SettlementRank rank
├── int legitimacy
├── int loyalty
├── int prosperity
├── Set<BuildingId> buildings
├── Set<UUID> residents
├── Set<UUID> guards
└── int schemaVersion

BuildingRecord
├── UUID id
├── SettlementId settlement
├── BuildingTypeId type
├── RegistryKey<World> dimension
├── BlockBox bounds
├── BlockPos anchor
├── Set<BlockPos> interior
├── BuildingStatus status
├── RequirementSnapshot requirements
└── int schemaVersion
```

Entity UUIDs are persistent identity. Runtime entity IDs must only be used in short-lived network interactions.

## 6. Domain Ledger and building recognition

The Domain Ledger is the central interface for settlements. Its building workflow should support automatic discovery and deterministic manual selection.

### 6.1 Automatic scan

1. Start at the player's feet and eye position.
2. Flood-fill connected passable interior cells.
3. Treat solid boundaries, doors, trapdoors and configured blocks according to scanner rules.
4. Stop and fail safely when volume, distance, time or dimension limits are exceeded.
5. Build an interior set and bounding box.
6. Count required blocks within the accepted building area.
7. Check settlement territory and overlap.
8. Return candidates and validation messages.
9. Render a client-side preview before confirmation.

The scan must be processed with a strict block budget. Large scans should be split across ticks or performed as a bounded server task to prevent tick stalls.

### 6.2 Manual fallback

The player selects two corners when automatic detection is unsuitable. This supports farms, courtyards, walls, training grounds, stables and open markets.

Manual selection follows the same size, territory, overlap and requirement validation as automatic scans.

### 6.3 Building definitions

Building types should be data-driven JSON resources. A definition contains:

- identifier and translation key;
- category;
- minimum and maximum volume;
- automatic, manual or either scan mode;
- required exact blocks or block tags;
- minimum counts;
- optional mutually exclusive requirements;
- effects activated while valid;
- map icon and display order.

Block tags must be preferred for concepts such as storage containers so compatible modded blocks can participate.

### 6.4 Validation lifecycle

- Full validation occurs on registration.
- Invalid buildings remain registered but become inoperable.
- Repairs reactivate a building without forcing re-registration.
- Buildings are indexed by chunk so relevant block changes can mark only nearby records dirty.
- Dirty buildings are revalidated through a bounded queue.
- A slow periodic audit handles changes that do not emit a convenient event.
- Unloaded buildings preserve their last known state and are audited after their chunks load.

### 6.5 Initial building catalog

| Building | Example requirements | Primary effect |
|---|---|---|
| Town center | Bell and Domain Ledger anchor | Creates and administers a settlement. |
| Warehouse | Four chests and one lectern | Holds taxes, provisions and project resources. |
| Barracks | Four beds, armor stands, chest and grindstone | Adds military capacity. |
| Armory | Smithing table, anvil, grindstone and weapon storage | Equips ranks and squads. |
| Council hall | Administrative table, lectern and seats | Unlocks council appointments and decrees. |
| Infirmary | Beds, brewing stand, cauldron and medical storage | Improves recovery. |
| Prison | Iron bars, iron door and bed | Supports justice events and decrees. |
| Market | Profession blocks, containers and stalls | Improves trade and prosperity. |
| Throne hall | Throne, banners and council capacity | Required for capital and coronation. |

Exact requirements must be balanced during playtesting and remain configurable through data.

## 7. Server authority and networking

Every mutation is server-authoritative. Client screens submit intent, never trusted results.

Examples:

- Client requests automatic scan; server computes bounds and requirements.
- Client selects two corners; server validates distance, permissions and coordinates.
- Client requests a decree; server validates title, cost and cooldown.
- Client requests recruitment; Rally and Realm policies validate it on the server.

Payload codecs must enforce hard limits before allocating collections. Coordinates must be in the correct dimension and within configured distance or territory rules. All handlers must verify membership, role and ownership again even if the UI hid an unavailable action.

Suggested channels:

```text
realmguard:open_domain_ledger
realmguard:domain_snapshot
realmguard:request_building_scan
realmguard:building_scan_result
realmguard:confirm_building
realmguard:update_building
realmguard:issue_decree
realmguard:appoint_councilor
realmguard:promote_guard
realmguard:kingdom_snapshot
```

Snapshots should be purpose-specific and bounded instead of sending the complete kingdom state to every screen.

## 8. Recruitment and economy

Royal recruitment should replace personal payment with institutional cost, not create free soldiers without consequence.

An example policy requires:

- player is recognized ruler or authorized officer;
- guard belongs to the same settlement context;
- barracks has free capacity;
- armory has required equipment;
- warehouse has provisions;
- treasury can pay initial cost;
- population-to-military ratio remains valid.

The player may see a zero personal emerald cost while the settlement pays resources and accepts ongoing upkeep. This preserves the fantasy that a king mobilizes subjects rather than hires mercenaries.

All inventory operations must be atomic: validate the complete transaction, consume resources, assign ownership and then record Realm military data. Partial failure must not lose items or create an unregistered guard.

## 9. Military hierarchy

Initial ranks:

```text
Recruit → Soldier/Archer → Veteran → Sergeant → Captain → Royal Guard → Marshal
```

Ranks provide responsibilities rather than only attribute bonuses:

- recruits learn and cannot command;
- soldiers receive ordinary tactical orders;
- veterans qualify for leadership;
- sergeants lead squads;
- captains manage settlement patrol groups;
- royal guards prioritize ruler protection;
- marshals coordinate multiple captains and settlements.

Promotion considers service, combat experience, loyalty, equipment, available posts and an explicit player decision. Realm stores rank and progression; Rally executes the tactical order.

## 10. Lifecycle and recovery

The design must handle:

- server restart;
- player death and respawn;
- logout during a scan or ceremony;
- entity unload and reload;
- guard death;
- villager conversion;
- settlement bell destruction;
- building chunk unload;
- ruler changing dimension;
- mod version migration;
- removal of Rally or Guard Villagers from an existing save.

Missing integrations should produce a clear startup error once they become required. During optional foundation development, integration code must be isolated behind availability checks and never load third-party classes when the mod is absent.

## 11. Package layout

Planned source layout:

```text
net.hfstack.realmguard
├── api                 Public Realm extension points, only when needed
├── attachment          Persistent attachment registration
├── building            Scan, definitions, validation and spatial index
├── command             Administrative commands
├── config              Gameplay and scanner limits
├── domain
│   ├── kingdom
│   ├── settlement
│   └── military
├── event               Fabric event registration
├── integration
│   ├── guardvillagers
│   └── rallyguard
├── item                Domain Ledger and royal items
├── network             Payload registration and handlers
├── registry            Central vanilla/Fabric registrations
├── screen              Common screen handlers
└── service             Server-authoritative application services
```

Client-only screens, rendering and previews remain under `src/client` with matching feature packages.

## 12. Testing strategy

Unit tests should cover codecs, migrations, building requirements, overlap, rank rules, recruitment policies and progression thresholds.

GameTests should cover settlement creation, automatic and manual registration, invalidation after block removal, treasury transactions, dedicated-server screen handling and Rally integration.

Payload fuzz tests should cover negative sizes, oversized collections, invalid enums, distant coordinates and unauthorized players.
