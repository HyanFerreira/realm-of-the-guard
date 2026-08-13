package net.hfstack.realmguard.domain.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record RealmWorldData(int schemaVersion, List<SettlementRecord> settlements) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public static final Codec<RealmWorldData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(RealmWorldData::schemaVersion),
            SettlementRecord.CODEC.listOf().optionalFieldOf("settlements", List.of()).forGetter(RealmWorldData::settlements)
    ).apply(instance, RealmWorldData::new));

    public RealmWorldData {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported realm data schema version: " + schemaVersion);
        }
        settlements = List.copyOf(Objects.requireNonNull(settlements, "settlements"));
        long uniqueIds = settlements.stream().map(SettlementRecord::id).distinct().count();
        long uniqueCenters = settlements.stream().map(SettlementRecord::center).distinct().count();
        if (uniqueIds != settlements.size() || uniqueCenters != settlements.size()) {
            throw new IllegalArgumentException("Settlement IDs and centers must be unique");
        }
    }

    public static RealmWorldData empty() {
        return new RealmWorldData(CURRENT_SCHEMA_VERSION, List.of());
    }

    public Optional<SettlementRecord> byId(UUID id) {
        return settlements.stream().filter(settlement -> settlement.id().equals(id)).findFirst();
    }

    public Optional<SettlementRecord> atCenter(BlockPos center) {
        return settlements.stream().filter(settlement -> settlement.center().equals(center)).findFirst();
    }

    public Optional<SettlementRecord> nearest(BlockPos position, double maximumDistance) {
        double maximumSquared = maximumDistance * maximumDistance;
        return settlements.stream()
                .filter(settlement -> settlement.center().getSquaredDistance(position) <= maximumSquared)
                .min((left, right) -> Double.compare(
                        left.center().getSquaredDistance(position),
                        right.center().getSquaredDistance(position)
                ));
    }

    public RealmWorldData add(SettlementRecord settlement) {
        Objects.requireNonNull(settlement, "settlement");
        if (byId(settlement.id()).isPresent() || atCenter(settlement.center()).isPresent()) {
            throw new IllegalArgumentException("Settlement already exists");
        }
        List<SettlementRecord> updated = new ArrayList<>(settlements);
        updated.add(settlement);
        return new RealmWorldData(schemaVersion, updated);
    }

    public RealmWorldData replace(SettlementRecord settlement) {
        Objects.requireNonNull(settlement, "settlement");
        List<SettlementRecord> updated = new ArrayList<>(settlements.size());
        boolean replaced = false;
        for (SettlementRecord current : settlements) {
            if (current.id().equals(settlement.id())) {
                updated.add(settlement);
                replaced = true;
            } else {
                updated.add(current);
            }
        }
        if (!replaced) {
            throw new IllegalArgumentException("Unknown settlement: " + settlement.id());
        }
        return new RealmWorldData(schemaVersion, updated);
    }
}
