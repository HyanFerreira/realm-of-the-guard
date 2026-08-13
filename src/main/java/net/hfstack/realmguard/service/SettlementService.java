package net.hfstack.realmguard.service;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.hfstack.realmguard.attachment.ModAttachments;
import net.hfstack.realmguard.domain.settlement.RealmWorldData;
import net.hfstack.realmguard.domain.settlement.SettlementRecord;
import net.hfstack.realmguard.domain.settlement.SettlementTitle;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.tag.PointOfInterestTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SettlementService {
    public static final int PREVIEW_RADIUS = 32;
    public static final int ANCHOR_SEARCH_RADIUS = 8;
    public static final int MINIMUM_SETTLEMENT_DISTANCE = 64;
    public static final int MAXIMUM_NAME_LENGTH = 48;
    public static final int INITIAL_LEGITIMACY = 25;

    public RealmWorldData data(ServerWorld world) {
        return world.getAttachedOrCreate(ModAttachments.REALM_WORLD_DATA);
    }

    public List<SettlementRecord> settlements(ServerWorld world) {
        return data(world).settlements();
    }

    public Optional<BlockPos> findNearestBell(ServerWorld world, BlockPos origin) {
        BlockPos.Mutable cursor = new BlockPos.Mutable();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int x = -ANCHOR_SEARCH_RADIUS; x <= ANCHOR_SEARCH_RADIUS; x++) {
            for (int y = -ANCHOR_SEARCH_RADIUS; y <= ANCHOR_SEARCH_RADIUS; y++) {
                for (int z = -ANCHOR_SEARCH_RADIUS; z <= ANCHOR_SEARCH_RADIUS; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!world.getBlockState(cursor).isOf(Blocks.BELL)) {
                        continue;
                    }
                    double distance = cursor.getSquaredDistance(origin);
                    if (distance < bestDistance) {
                        best = cursor.toImmutable();
                        bestDistance = distance;
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public SettlementResult register(ServerPlayerEntity player, BlockPos anchor, String requestedName) {
        ServerWorld world = player.getEntityWorld();
        String name = requestedName.strip();
        Optional<Text> issue = registrationIssue(player, anchor, name);
        if (issue.isPresent()) {
            return SettlementResult.failure(registrationOutcome(world, anchor, name), issue.get());
        }

        RealmWorldData current = data(world);
        SettlementSnapshot snapshot = inspect(world, anchor);
        SettlementRecord settlement = new SettlementRecord(
                SettlementRecord.CURRENT_SCHEMA_VERSION,
                UUID.randomUUID(),
                name,
                anchor,
                player.getUuid(),
                world.getTime(),
                snapshot.villagers(),
                snapshot.beds(),
                snapshot.workstations(),
                snapshot.guards(),
                SettlementTitle.PROTECTOR,
                INITIAL_LEGITIMACY
        );
        world.setAttached(ModAttachments.REALM_WORLD_DATA, current.add(settlement));
        return SettlementResult.success(
                Text.translatable("message.realmguard.settlement.registered", settlement.name()),
                settlement
        );
    }

    public Optional<Text> registrationIssue(ServerPlayerEntity player, BlockPos anchor, String requestedName) {
        ServerWorld world = player.getEntityWorld();
        String name = requestedName.strip();
        if (name.isEmpty() || name.length() > MAXIMUM_NAME_LENGTH) {
            return Optional.of(Text.translatable("message.realmguard.settlement.invalid_name", MAXIMUM_NAME_LENGTH));
        }
        if (!world.getBlockState(anchor).isOf(Blocks.BELL)) {
            return Optional.of(Text.translatable("message.realmguard.settlement.no_bell"));
        }

        RealmWorldData current = data(world);
        if (current.atCenter(anchor).isPresent()) {
            return Optional.of(Text.translatable("message.realmguard.settlement.already_registered"));
        }
        Optional<SettlementRecord> nearby = current.nearest(anchor, MINIMUM_SETTLEMENT_DISTANCE - 0.01);
        if (nearby.isPresent()) {
            return Optional.of(Text.translatable("message.realmguard.settlement.too_close", nearby.get().name()));
        }
        return Optional.empty();
    }

    private SettlementResult.Outcome registrationOutcome(ServerWorld world, BlockPos anchor, String name) {
        if (name.isEmpty() || name.length() > MAXIMUM_NAME_LENGTH) {
            return SettlementResult.Outcome.INVALID_NAME;
        }
        if (!world.getBlockState(anchor).isOf(Blocks.BELL)) {
            return SettlementResult.Outcome.ANCHOR_NOT_FOUND;
        }
        if (data(world).atCenter(anchor).isPresent()) {
            return SettlementResult.Outcome.ANCHOR_ALREADY_REGISTERED;
        }
        return SettlementResult.Outcome.TOO_CLOSE_TO_SETTLEMENT;
    }

    public SettlementResult rename(ServerPlayerEntity player, String requestedName) {
        ServerWorld world = player.getEntityWorld();
        String name = requestedName.strip();
        if (name.isEmpty() || name.length() > MAXIMUM_NAME_LENGTH) {
            return SettlementResult.failure(
                    SettlementResult.Outcome.INVALID_NAME,
                    Text.translatable("message.realmguard.settlement.invalid_name", MAXIMUM_NAME_LENGTH)
            );
        }

        RealmWorldData current = data(world);
        Optional<SettlementRecord> nearest = current.nearest(player.getBlockPos(), PREVIEW_RADIUS);
        if (nearest.isEmpty()) {
            return SettlementResult.failure(
                    SettlementResult.Outcome.SETTLEMENT_NOT_FOUND,
                    Text.translatable("message.realmguard.settlement.none_nearby")
            );
        }
        SettlementRecord settlement = nearest.get();
        if (!settlement.ruler().equals(player.getUuid())) {
            return SettlementResult.failure(
                    SettlementResult.Outcome.NOT_RULER,
                    Text.translatable("message.realmguard.settlement.not_ruler")
            );
        }

        SettlementRecord renamed = settlement.withName(name);
        world.setAttached(ModAttachments.REALM_WORLD_DATA, current.replace(renamed));
        return SettlementResult.success(
                Text.translatable("message.realmguard.settlement.renamed", renamed.name()),
                renamed
        );
    }

    public Optional<SettlementRecord> nearest(ServerWorld world, BlockPos position, double maximumDistance) {
        return data(world).nearest(position, maximumDistance);
    }

    public SettlementSnapshot inspect(ServerWorld world, BlockPos center) {
        Box area = new Box(center).expand(PREVIEW_RADIUS);
        int villagers = world.getEntitiesByClass(VillagerEntity.class, area, VillagerEntity::isAlive).size();
        int guards = world.getEntitiesByClass(GuardEntity.class, area, GuardEntity::isAlive).size();
        PointOfInterestStorage poi = world.getPointOfInterestStorage();
        int beds = Math.toIntExact(poi.count(
                type -> type.matchesKey(PointOfInterestTypes.HOME),
                center,
                PREVIEW_RADIUS,
                PointOfInterestStorage.OccupationStatus.ANY
        ));
        int workstations = Math.toIntExact(poi.count(
                type -> type.isIn(PointOfInterestTypeTags.ACQUIRABLE_JOB_SITE),
                center,
                PREVIEW_RADIUS,
                PointOfInterestStorage.OccupationStatus.ANY
        ));
        return new SettlementSnapshot(
                villagers,
                beds,
                workstations,
                guards,
                world.getBlockState(center).isOf(Blocks.BELL)
        );
    }

    public Optional<SettlementRecord> nearestOwned(ServerPlayerEntity player) {
        return data(player.getEntityWorld()).settlements().stream()
                .filter(settlement -> settlement.ruler().equals(player.getUuid()))
                .filter(settlement -> settlement.center().getSquaredDistance(player.getBlockPos()) <= PREVIEW_RADIUS * PREVIEW_RADIUS)
                .min(Comparator.comparingDouble(settlement -> settlement.center().getSquaredDistance(player.getBlockPos())));
    }

    public Optional<SettlementRecord> transferNearestTo(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        RealmWorldData current = data(world);
        Optional<SettlementRecord> nearest = current.nearest(player.getBlockPos(), PREVIEW_RADIUS);
        if (nearest.isEmpty()) {
            return Optional.empty();
        }
        SettlementRecord transferred = nearest.get().withRuler(player.getUuid());
        world.setAttached(ModAttachments.REALM_WORLD_DATA, current.replace(transferred));
        return Optional.of(transferred);
    }
}
