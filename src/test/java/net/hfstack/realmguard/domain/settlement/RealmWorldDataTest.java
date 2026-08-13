package net.hfstack.realmguard.domain.settlement;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmWorldDataTest {
    private static final UUID SETTLEMENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RULER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void codecRoundTripPreservesVersionedSettlementData() {
        SettlementRecord settlement = settlement(SETTLEMENT_ID, "Oakwatch", new BlockPos(10, 64, -20));
        RealmWorldData original = RealmWorldData.empty().add(settlement);

        JsonElement encoded = RealmWorldData.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        RealmWorldData decoded = RealmWorldData.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();

        assertEquals(original, decoded);
        assertEquals(RealmWorldData.CURRENT_SCHEMA_VERSION, decoded.schemaVersion());
        assertEquals(SettlementRecord.CURRENT_SCHEMA_VERSION, decoded.settlements().getFirst().schemaVersion());
    }

    @Test
    void addAndRenameReturnNewImmutableSnapshots() {
        RealmWorldData empty = RealmWorldData.empty();
        SettlementRecord settlement = settlement(SETTLEMENT_ID, "Oakwatch", new BlockPos(0, 64, 0));
        RealmWorldData added = empty.add(settlement);
        SettlementRecord renamed = settlement.withName("New Oakwatch");
        RealmWorldData updated = added.replace(renamed);

        assertTrue(empty.settlements().isEmpty());
        assertEquals("Oakwatch", added.settlements().getFirst().name());
        assertEquals("New Oakwatch", updated.settlements().getFirst().name());
        assertNotSame(added, updated);
        assertThrows(UnsupportedOperationException.class, () -> updated.settlements().clear());
    }

    @Test
    void rejectsDuplicateIdentityAndAnchor() {
        SettlementRecord first = settlement(SETTLEMENT_ID, "Oakwatch", new BlockPos(0, 64, 0));
        SettlementRecord duplicateId = settlement(SETTLEMENT_ID, "Riverstead", new BlockPos(100, 64, 0));
        SettlementRecord duplicateCenter = settlement(UUID.randomUUID(), "Riverstead", first.center());
        RealmWorldData data = RealmWorldData.empty().add(first);

        assertThrows(IllegalArgumentException.class, () -> data.add(duplicateId));
        assertThrows(IllegalArgumentException.class, () -> data.add(duplicateCenter));
        assertThrows(IllegalArgumentException.class, () -> new RealmWorldData(1, List.of(first, duplicateCenter)));
    }

    @Test
    void nearestHonorsMaximumDistance() {
        SettlementRecord near = settlement(SETTLEMENT_ID, "Oakwatch", new BlockPos(10, 64, 0));
        SettlementRecord far = settlement(UUID.randomUUID(), "Riverstead", new BlockPos(200, 64, 0));
        RealmWorldData data = RealmWorldData.empty().add(near).add(far);

        BlockPos query = new BlockPos(0, 64, 0);
        assertEquals(near, data.nearest(query, 16).orElseThrow());
        assertTrue(data.nearest(query, 5).isEmpty());
    }

    @Test
    void migratesSchemaOneSettlementWithProtectorDefaults() {
        String legacyJson = """
                {
                  "schema_version": 1,
                  "id": [286331153, 286326784, 286331153, 286331153],
                  "name": "Oakwatch",
                  "center": [10, 64, -20],
                  "ruler": [572662306, 572653568, 572662306, 572662306],
                  "created_at": 1200,
                  "registration_villagers": 5,
                  "registration_beds": 7,
                  "registration_workstations": 3,
                  "registration_guards": 2
                }
                """;

        SettlementRecord migrated = SettlementRecord.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(legacyJson))
                .getOrThrow();

        assertEquals(SettlementRecord.CURRENT_SCHEMA_VERSION, migrated.schemaVersion());
        assertEquals(SettlementTitle.PROTECTOR, migrated.rulerTitle());
        assertEquals(25, migrated.legitimacy());
        assertEquals("Oakwatch", migrated.name());
        assertEquals(new BlockPos(10, 64, -20), migrated.center());
    }

    @Test
    void rejectsLegitimacyOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> new SettlementRecord(
                2,
                SETTLEMENT_ID,
                "Oakwatch",
                new BlockPos(0, 64, 0),
                RULER_ID,
                1200L,
                5,
                7,
                3,
                2,
                SettlementTitle.PROTECTOR,
                101
        ));
    }

    @Test
    void transferringRulerPreservesSettlementProgress() {
        SettlementRecord original = settlement(SETTLEMENT_ID, "Oakwatch", new BlockPos(0, 64, 0));
        UUID successor = UUID.fromString("33333333-3333-3333-3333-333333333333");

        SettlementRecord transferred = original.withRuler(successor);

        assertEquals(successor, transferred.ruler());
        assertEquals(original.id(), transferred.id());
        assertEquals(original.legitimacy(), transferred.legitimacy());
        assertEquals(original.center(), transferred.center());
    }

    private static SettlementRecord settlement(UUID id, String name, BlockPos center) {
        return new SettlementRecord(1, id, name, center, RULER_ID, 1200L, 5, 7, 3, 2, SettlementTitle.PROTECTOR, 25);
    }
}
