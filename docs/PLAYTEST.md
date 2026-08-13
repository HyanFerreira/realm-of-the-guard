# Realm of the Guard — Foundation Playtest

This document covers the first playable settlement-persistence slice. It is a developer playtest, not the complete Protector progression.

## Included in this build

- Domain Ledger item and recipe.
- Settlement registration preview and confirmation anchored to a village bell.
- Persistent, versioned settlement records stored per dimension.
- Live inspection of villagers, beds, workstations and Guard Villagers guards within 32 blocks.
- Multiple settlements in one dimension, with a minimum 64-block separation.
- Ruler-only renaming.
- Persistent Protector title and legitimacy from 0 to 100.
- Legitimacy reactions to local raider kills, Rally guard recruitment and harm to residents.
- Recoverable missing-anchor state: destroying the bell does not delete the settlement.
- Dedicated-server-compatible commands.

Legitimacy now has a persistent baseline, initial event sources and the Protector title is displayed. Full standing history, protection achievements, the naming screen and settlement recovery actions are intentionally not enforced yet. Commands provide the test harness until those systems exist.

## Start the development client

From `realm-of-the-guard`:

```powershell
.\gradlew.bat runClient
```

The workspace expects `rally-of-the-guard` to be a sibling directory. Gradle builds and loads the current Rally checkout automatically.
The development client uses the stable offline identity `RealmTester`, preventing ownership from changing between runs.

## Test route

1. Create or load a world with cheats enabled for convenient setup.
2. Obtain the ledger from the Tools creative tab or run:

   ```text
   /give @s realmguard:domain_ledger
   ```

3. Find or place a bell, beds, villager workstations and villagers.
4. Right-click the bell while holding the Domain Ledger without sneaking.
5. Confirm that chat shows a preview with center, live counts, initial Protector title and legitimacy.
6. Within 30 seconds, hold Shift and right-click the same bell to confirm registration.
7. Confirm that chat reports `Settlement registered`, the provisional name and the complete summary.
   The ledger consumes both interactions, so the bell itself should not ring on these clicks.
8. Rename it while within 32 blocks:

   ```text
   /realm settlement rename Oakwatch
   ```

9. Inspect it again:

   ```text
   /realm settlement info
   ```

10. List all settlements in the current dimension:

   ```text
   /realm settlement list
   ```

11. Save and leave the world, reopen it, then repeat `info` and `list`.
12. Destroy the anchor bell and run `info`. The status should become `Bell missing — recoverable`, while the settlement remains listed.
13. Replace the bell at the same coordinates and run `info` again. The anchor should return to `Active`.
14. Travel at least 64 blocks away and register another settlement to verify independent records.
15. Try to confirm a new settlement without previewing it first. Registration must be refused.
16. Let a preview expire for more than 30 seconds. Confirmation must be refused.

## Legitimacy route

Perform these actions within 32 blocks of a settlement that you govern:

1. Spawn a pillager and defeat it. Legitimacy must increase by 1.
2. Recruit a Guard Villagers guard through Rally. Legitimacy must increase by 2.
3. Hit a resident villager without killing it. Legitimacy must decrease by 2.
4. Kill a resident villager. The fatal hit must decrease legitimacy by 15.
5. Use the ledger on the settlement bell or run `/realm settlement info` after each action to confirm the persisted total.
6. Repeat gains near 100 and losses near 0. Legitimacy must remain clamped between 0 and 100.

Actions outside the 32-block settlement radius or inside a settlement governed by another player must not change your legitimacy.

If a settlement was created before the stable development identity was configured, stand within 32 blocks and transfer it once with:

```text
/realm settlement claim
```

This is an operator-only recovery/test command. The settlement summary must then show `Governante: Você`.

Instead of using the ledger, a named settlement can be registered around the nearest bell within eight blocks:

```text
/realm settlement register Riverstead
```

## Expected limitations

- Registration uses chat preview plus Shift-click confirmation; a dedicated screen comes later.
- The ledger currently reuses Minecraft's book texture.
- Counts are a live radius snapshot, not final resident membership.
- Settlement data is stored per dimension; cross-dimensional kingdom aggregation is not implemented.
- A missing bell is reported but cannot yet be reassigned to a new coordinate.
- Trading, raid-victory and curing legitimacy sources, standing history and Protector eligibility requirements are not active yet.

## Useful verification commands

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat runServer --args nogui
```
