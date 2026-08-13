package net.hfstack.realmguard.service;

import net.hfstack.realmguard.attachment.ModAttachments;
import net.hfstack.realmguard.domain.settlement.RealmWorldData;
import net.hfstack.realmguard.domain.settlement.SettlementRecord;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Optional;

public final class LegitimacyService {
    public static final int MINIMUM = 0;
    public static final int MAXIMUM = 100;

    private final SettlementService settlements = new SettlementService();

    public Optional<SettlementRecord> ownedSettlementAt(ServerPlayerEntity ruler, BlockPos position) {
        return settlements.nearest(ruler.getEntityWorld(), position, SettlementService.PREVIEW_RADIUS)
                .filter(settlement -> settlement.ruler().equals(ruler.getUuid()));
    }

    public Optional<LegitimacyChange> adjustOwnedSettlementAt(
            ServerPlayerEntity ruler,
            BlockPos position,
            int delta,
            String reasonTranslationKey
    ) {
        Optional<SettlementRecord> owned = ownedSettlementAt(ruler, position);
        if (owned.isEmpty()) {
            return Optional.empty();
        }

        ServerWorld world = ruler.getEntityWorld();
        RealmWorldData currentData = settlements.data(world);
        SettlementRecord current = owned.get();
        int updatedValue = calculateUpdatedValue(current.legitimacy(), delta);
        int appliedDelta = updatedValue - current.legitimacy();
        SettlementRecord updated = current.withLegitimacy(updatedValue);
        if (appliedDelta != 0) {
            world.setAttached(ModAttachments.REALM_WORLD_DATA, currentData.replace(updated));
            ruler.sendMessage(Text.translatable(
                    "message.realmguard.legitimacy.changed",
                    appliedDelta > 0 ? "+" + appliedDelta : Integer.toString(appliedDelta),
                    Text.translatable(reasonTranslationKey),
                    updatedValue
            ), false);
        }

        return Optional.of(new LegitimacyChange(
                updated,
                delta,
                appliedDelta,
                current.legitimacy(),
                updatedValue
        ));
    }

    static int calculateUpdatedValue(int current, int delta) {
        return (int) Math.clamp((long) current + delta, MINIMUM, MAXIMUM);
    }
}
