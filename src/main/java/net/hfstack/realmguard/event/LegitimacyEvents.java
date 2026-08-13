package net.hfstack.realmguard.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.hfstack.realmguard.service.LegitimacyService;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class LegitimacyEvents {
    public static final int RAIDER_KILLED_GAIN = 1;
    public static final int RESIDENT_DAMAGED_LOSS = -2;
    public static final int RESIDENT_KILLED_LOSS = -15;

    private static final LegitimacyService LEGITIMACY = new LegitimacyService();

    private LegitimacyEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damage, blocked) -> {
            if (!(entity instanceof VillagerEntity villager) || blocked || damage <= 0) {
                return;
            }
            ServerPlayerEntity attacker = serverPlayer(source.getAttacker());
            if (attacker != null) {
                LEGITIMACY.adjustOwnedSettlementAt(
                        attacker,
                        villager.getBlockPos(),
                        RESIDENT_DAMAGED_LOSS,
                        "reason.realmguard.legitimacy.resident_damaged"
                );
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed, source) -> {
            ServerPlayerEntity player = serverPlayer(killer);
            if (player == null) {
                return;
            }
            if (killed instanceof VillagerEntity) {
                LEGITIMACY.adjustOwnedSettlementAt(
                        player,
                        killed.getBlockPos(),
                        RESIDENT_KILLED_LOSS,
                        "reason.realmguard.legitimacy.resident_killed"
                );
            } else if (killed instanceof RaiderEntity) {
                LEGITIMACY.adjustOwnedSettlementAt(
                        player,
                        killed.getBlockPos(),
                        RAIDER_KILLED_GAIN,
                        "reason.realmguard.legitimacy.raider_killed"
                );
            }
        });
    }

    private static ServerPlayerEntity serverPlayer(Entity entity) {
        return entity instanceof ServerPlayerEntity player ? player : null;
    }
}
