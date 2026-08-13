package net.hfstack.realmguard.domain.settlement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Uuids;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.UUID;

public record SettlementRecord(
        int schemaVersion,
        UUID id,
        String name,
        BlockPos center,
        UUID ruler,
        long createdAt,
        int registrationVillagers,
        int registrationBeds,
        int registrationWorkstations,
        int registrationGuards,
        SettlementTitle rulerTitle,
        int legitimacy
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;

    public static final Codec<SettlementRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(SettlementRecord::schemaVersion),
            Uuids.CODEC.fieldOf("id").forGetter(SettlementRecord::id),
            Codec.STRING.fieldOf("name").forGetter(SettlementRecord::name),
            BlockPos.CODEC.fieldOf("center").forGetter(SettlementRecord::center),
            Uuids.CODEC.fieldOf("ruler").forGetter(SettlementRecord::ruler),
            Codec.LONG.optionalFieldOf("created_at", 0L).forGetter(SettlementRecord::createdAt),
            Codec.INT.optionalFieldOf("registration_villagers", 0).forGetter(SettlementRecord::registrationVillagers),
            Codec.INT.optionalFieldOf("registration_beds", 0).forGetter(SettlementRecord::registrationBeds),
            Codec.INT.optionalFieldOf("registration_workstations", 0).forGetter(SettlementRecord::registrationWorkstations),
            Codec.INT.optionalFieldOf("registration_guards", 0).forGetter(SettlementRecord::registrationGuards),
            SettlementTitle.CODEC.optionalFieldOf("ruler_title", SettlementTitle.PROTECTOR).forGetter(SettlementRecord::rulerTitle),
            Codec.intRange(0, 100).optionalFieldOf("legitimacy", 25).forGetter(SettlementRecord::legitimacy)
    ).apply(instance, SettlementRecord::new));

    public SettlementRecord {
        if (schemaVersion < 1 || schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported settlement schema version: " + schemaVersion);
        }
        Objects.requireNonNull(id, "id");
        name = Objects.requireNonNull(name, "name");
        center = Objects.requireNonNull(center, "center").toImmutable();
        Objects.requireNonNull(ruler, "ruler");
        Objects.requireNonNull(rulerTitle, "rulerTitle");
        if (name.isBlank()) {
            throw new IllegalArgumentException("Settlement name cannot be blank");
        }
        if (registrationVillagers < 0 || registrationBeds < 0 || registrationWorkstations < 0 || registrationGuards < 0) {
            throw new IllegalArgumentException("Registration counts cannot be negative");
        }
        if (legitimacy < 0 || legitimacy > 100) {
            throw new IllegalArgumentException("Legitimacy must be between 0 and 100");
        }
        schemaVersion = CURRENT_SCHEMA_VERSION;
    }

    public SettlementRecord withName(String newName) {
        return new SettlementRecord(
                schemaVersion,
                id,
                newName,
                center,
                ruler,
                createdAt,
                registrationVillagers,
                registrationBeds,
                registrationWorkstations,
                registrationGuards,
                rulerTitle,
                legitimacy
        );
    }

    public SettlementRecord withLegitimacy(int newLegitimacy) {
        return new SettlementRecord(
                schemaVersion,
                id,
                name,
                center,
                ruler,
                createdAt,
                registrationVillagers,
                registrationBeds,
                registrationWorkstations,
                registrationGuards,
                rulerTitle,
                newLegitimacy
        );
    }

    public SettlementRecord withRuler(UUID newRuler) {
        return new SettlementRecord(
                schemaVersion,
                id,
                name,
                center,
                newRuler,
                createdAt,
                registrationVillagers,
                registrationBeds,
                registrationWorkstations,
                registrationGuards,
                rulerTitle,
                legitimacy
        );
    }
}
