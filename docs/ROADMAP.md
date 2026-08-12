# Realm of the Guard Roadmap

This roadmap captures the intended player fantasy, scope and implementation order. It is deliberately incremental: every milestone should produce a playable loop without requiring MineColonies-scale simulation.

## Vision

Realm of the Guard transforms vanilla villages into player-governed medieval realms.

The player begins as an outsider, earns trust through trade and protection, registers a settlement, develops freely built structures, organizes a professional guard, governs through a council, unites villages and completes a public coronation to become king.

The mod does not automatically construct a city for the player. It recognizes and gives purpose to what the player builds.

## Product boundaries

### Core promises

- Vanilla villagers become residents and political subjects without being replaced.
- Player-built structures are recognized through a Domain Ledger.
- Authority is earned through legitimacy, population, buildings and service.
- Military power depends on settlement infrastructure and resources.
- Rally of the Guard remains the tactical foundation.
- Progression culminates in a kingdom with a capital, vassal settlements and royal guard.

### Non-goals for the first public release

- Automatic block-by-block construction by NPCs.
- Full production-chain simulation.
- Hundreds of custom human NPC models.
- Procedurally generated rival kingdoms.
- Siege engines and large-scale destruction.
- PvP conquest and grief-protection framework.
- Dynastic marriage, children and hereditary succession.

These may become later expansions, but they must not block the first complete kingdom loop.

## Player progression

```text
Outsider
   ↓
Ally
   ↓
Protector
   ↓
Steward
   ↓
Lord
   ↓
High Lord
   ↓
King
```

Suggested meaning:

- **Outsider:** no authority; normal vanilla interaction.
- **Ally:** recognized positive relationship and access to local requests.
- **Protector:** may register the settlement after proving military service.
- **Steward:** manages storage, projects and limited decrees.
- **Lord:** appoints councilors, recruits through settlement resources and commands the local military.
- **High Lord:** controls multiple settlements and appoints local administrators.
- **King:** owns a capital, issues realm-wide decrees and commands the royal hierarchy.

A crown represents completed legitimacy. Crafting or finding one never grants authority by itself.

## Phase 0 — Foundation and integration contract

Goal: establish a clean technical base before gameplay data becomes expensive to migrate.

### Realm work

- Keep common and client source sets separated.
- Establish registries, networking bootstrap and configuration conventions.
- Select the persistence mechanism after a small Data Attachment prototype.
- Define codecs with explicit schema versions.
- Establish unit-test and GameTest source sets.
- Add development dependencies for Guard Villagers and Rally through reproducible repositories or published artifacts.
- Change both integrations from `suggests` to versioned `depends` once used.

### Rally work

- Fix reproducible build and publish a consumable development artifact.
- Extract tactical operations from networking handlers into `GuardCommandService`.
- Add recruitment `BEFORE` and `AFTER` events.
- Add tactical-order events.
- Add structured eligibility callbacks.
- Add a read-only presentation provider for rank and settlement labels.
- Publish API version documentation.

### Acceptance criteria

- Both projects build from clean clones on Java 21.
- Realm can run a dedicated development server with required dependencies.
- Realm calls one Rally command service in a proof-of-concept integration test.
- Rally works unchanged when Realm is absent.
- No Realm class is referenced from Rally.

## Phase 1 — Settlement charter and Protector loop

Goal: let the player turn a vanilla village into a named, persistent settlement.

### Gameplay

- Add the Domain Ledger.
- Use a village bell as the initial settlement anchor.
- Detect nearby beds, workstations and villagers for a registration preview.
- Require positive standing and a protection achievement.
- Allow the player to name the settlement.
- Grant the Protector title.
- Display population, guards, center, current title and basic legitimacy.

### Legitimacy sources

- Trading with residents.
- Defeating a raid affecting the settlement.
- Curing a resident zombie villager.
- Completing settlement requests.
- Donating food or essential resources.
- Maintaining hired guards assigned to the settlement.

### Legitimacy losses

- Damaging or killing residents.
- Stealing protected treasury resources.
- Excessive taxation in later phases.
- Failing major requests or abandoning an active crisis.

### Acceptance criteria

- Settlement survives save/reload and server restart.
- Two settlements can exist in one dimension without sharing data.
- Membership and management are validated on the server.
- Destroyed or moved anchors produce a recoverable state instead of data loss.

## Phase 2 — Domain Ledger and recognized buildings

Goal: allow freeform construction to drive settlement progression.

### Scanner MVP

- Add automatic enclosed-space flood fill.
- Enforce scan radius, volume, height, block-budget and timeout limits.
- Produce a server result with client-side outline preview.
- Add confirm and cancel flows.
- Reject overlap with registered buildings.
- Add manual two-corner selection fallback.
- Persist and display registered bounds on the settlement map.

### Data-driven catalog

- Load building definitions from JSON.
- Support exact blocks and block tags.
- Report fulfilled and missing requirements.
- Add schema validation and useful data-pack error logging.
- Generate built-in definitions and tags through datagen.

### First buildings

1. **Town center:** bell and administrative anchor.
2. **Warehouse:** four storage chests and one lectern.
3. **Barracks:** beds, armor stands, storage and grindstone.

### Validation

- Track buildings by chunk.
- Mark affected buildings dirty on relevant block changes.
- Revalidate through a bounded queue.
- Change broken buildings to inoperable instead of deleting them.
- Reactivate repaired buildings automatically.

### Acceptance criteria

- A normal closed house can be discovered automatically.
- An open structure can be registered manually.
- An oversized or leaking scan cannot stall the server.
- Removing a required block suspends the building effect.
- Repair restores the effect without re-registering.

## Phase 3 — Treasury, projects and institutional recruitment

Goal: make buildings affect gameplay and make royal recruitment feel powerful without becoming free power.

### Warehouse and treasury

- Designate protected inventories within the warehouse.
- Define deposit and withdrawal permissions.
- Record transactions for administrative feedback.
- Add settlement projects that reserve real items.
- Prevent duplication through atomic server transactions.

### Military capacity

- Barracks provide a configurable number of military slots.
- Population limits maximum sustainable military size.
- Armory supplies define available equipment tiers.
- Food and treasury resources contribute to recruitment and upkeep.

### Rally integration

- Intercept Rally recruitment through its public policy event.
- Let authorized rulers pay from settlement resources.
- Keep ordinary personal recruitment available according to Rally configuration.
- Record settlement and starting rank after successful recruitment.
- Roll back the full transaction if any step fails.

### Acceptance criteria

- A ruler with valid infrastructure recruits without personal emerald payment.
- The settlement still pays configured resources.
- Missing capacity, equipment or funds produces a clear denial.
- A non-ruler follows normal Rally recruitment rules.
- Disconnects or duplicate clicks cannot charge twice or create partial state.

## Phase 4 — Government, council and meaningful decrees

Goal: transform settlement management from a status screen into decision-making.

### Profession-based council

- Farmer or butcher: provisions.
- Armorer or weaponsmith: military equipment.
- Librarian: records, research and reports.
- Cleric: legitimacy and ceremony.
- Cartographer: expansion and regional information.
- Mason: public projects and fortification requirements.

Council appointments require resident profession, loyalty and an available office in the council hall.

### Initial decrees

- Reduced taxes: loyalty benefit, lower income.
- War tribute: temporary income, loyalty cost.
- Military service: more capacity, prosperity or loyalty cost.
- Reinforced rations: faster military recovery, higher food consumption.
- Curfew: greater safety during threats, reduced productivity.
- Festival: treasury and food cost, legitimacy and loyalty gain.

### Resident interaction

- Ask about settlement condition.
- View a profession-specific need.
- Ask opinion of the ruler.
- Invite eligible resident to council.
- Receive rumors or warnings from unemployed villagers and nitwits.

### Acceptance criteria

- Council roles unlock specific information or actions.
- Decrees have visible benefits and costs.
- Opinion is based on Realm loyalty plus selected vanilla reputation signals.
- No decree generates resources without an explainable source.

## Phase 5 — Military hierarchy and squads

Goal: evolve hired guards into an organized royal army.

### Ranks

- Recruit.
- Soldier or Archer specialization.
- Veteran.
- Sergeant.
- Captain.
- Royal Guard.
- Marshal.

### Promotion rules

- Minimum service or combat experience.
- Loyalty threshold.
- Required equipment.
- Available position in settlement hierarchy.
- Required building or title.
- Explicit appointment by an authorized player.

### Functional responsibilities

- Sergeants lead persistent squads.
- Captains own settlement patrol groups and defense plans.
- Royal guards prioritize the ruler and throne hall.
- Marshals coordinate captains across settlements.

### Rally communication

- Realm contributes rank, settlement and squad labels to the Rally ledger.
- Rally remains responsible for executing follow, wait, patrol, route and rally.
- Realm filters eligibility through public callbacks.
- Realm listens to order events for reports and progression.
- Captains may propagate a high-level Realm order into individual Rally service calls, with bounded group size.

### Acceptance criteria

- Rank persists through unload and restart.
- Promotion cannot bypass infrastructure or permission rules.
- Removing Realm leaves Rally tactical ownership intact.
- Rally screens remain usable without Realm presentation providers.

## Phase 6 — Lordship, multiple settlements and coronation

Goal: complete the journey from village protector to king.

### Settlement ranks

- Settlement.
- Protected village.
- Lordship.
- Town.
- Capital.

Progression considers population, valid buildings, prosperity, loyalty, military capacity and completed events.

### Expansion

- Discover another vanilla village.
- Build legitimacy independently there.
- Offer protection or complete a rescue event.
- Integrate it as an allied or vassal settlement.
- Appoint a steward or captain.
- Define the original or selected settlement as capital.

### Coronation requirements

- Minimum number of governed settlements.
- Capital rank achieved.
- Valid throne hall, treasury, council hall and military infrastructure.
- Required legitimacy and loyalty.
- Captain or marshal appointed.
- Major defensive achievement.
- Crafted ceremonial crown.
- Public ceremony at the throne or town center.

### Royal unlocks

- Realm-wide overview.
- Royal guard appointments.
- Kingdom banner and color.
- Realm-wide decrees.
- Settlement administrators.
- Treasury transfers between settlements.

### Acceptance criteria

- Crown item alone cannot grant the King title.
- Losing a requirement creates political consequences but does not silently delete the kingdom.
- Each settlement retains local state while belonging to one kingdom.
- Permissions support a ruler and delegated officers.

## Phase 7 — Prosperity, requests and realm events

Goal: make the realm react to player decisions and produce stories.

### Local events

- Food shortage.
- Broken tools or depleted armory.
- Visiting merchant.
- Missing resident.
- Illness or zombie outbreak.
- Guard misconduct.
- Dispute between residents.
- Festival request.
- Abundant harvest.

### Military events

- Reinforced pillager raid.
- Bandit threat.
- Desertion.
- Patrol disappearance.
- Attack on a vassal settlement.
- Capital siege as a late milestone.

### Political events

- Council disagreement.
- Unpopular tax response.
- Request for autonomy.
- Rival claimant.
- Loyalty crisis.
- Petition from an ungoverned village.

Events begin as server-authored objectives and choices. Custom mobs and elaborate scenes should only be added where they materially improve the event.

## Phase 8 — Multiplayer government and diplomacy

Goal: support cooperative kingdoms before competitive conquest.

### Cooperative roles

- Ruler.
- Regent.
- Marshal.
- Steward.
- Treasurer.
- Councilor.
- Citizen.

Every action uses server-side permission checks. Role changes and treasury operations are audited.

### Diplomacy

- Neutral, allied and hostile relationships.
- Trade agreements.
- Mutual defense requests.
- Peaceful vassalization.
- Shared map markers where permitted.

PvP territorial conquest remains out of scope until claims, offline protection, surrender and anti-grief rules have a complete design.

## Future possibilities

- Roads and caravan routes between settlements.
- Custom bandit factions.
- Heraldry editor and kingdom banners.
- Structured quests from council members.
- Castle sections and fortification scoring.
- Succession and regency.
- Rebellions driven by sustained low loyalty.
- Seasonal production and famine.
- Optional compatibility with economy, map and decoration mods.

## Release outline

| Release | Target |
|---|---|
| `0.1.x` | Foundation, integration contract and settlement persistence. |
| `0.2.x` | Domain Ledger and building scanner prototype. |
| `0.3.x` | Warehouse, barracks, treasury and institutional recruitment. |
| `0.4.x` | Government, council, requests and decrees. |
| `0.5.x` | Military hierarchy, squads and Rally presentation integration. |
| `0.6.x` | Multiple settlements, capital and coronation. |
| `0.7.x` | Events, prosperity and balance. |
| `1.0.0` | Complete single-player and cooperative kingdom progression. |

## Definition of the first complete release

Version 1.0 is ready when a player can:

1. Earn the trust of a vanilla village.
2. Register and name it through the Domain Ledger.
3. Build and validate freeform settlement buildings.
4. Establish warehouse, barracks, armory and council infrastructure.
5. Recruit guards through settlement resources.
6. Promote guards and organize squads.
7. Issue meaningful decrees through a council.
8. Govern multiple settlements.
9. Establish a capital and complete a coronation.
10. Continue managing a stable kingdom after server restart and in cooperative multiplayer.
