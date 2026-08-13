package net.hfstack.realmguard.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.hfstack.realmguard.item.DomainLedgerItem;
import net.hfstack.realmguard.registry.ModItems;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

public final class DomainLedgerInteraction {
    private DomainLedgerInteraction() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!player.getStackInHand(hand).isOf(ModItems.DOMAIN_LEDGER)
                    || !world.getBlockState(hitResult.getBlockPos()).isOf(Blocks.BELL)) {
                return ActionResult.PASS;
            }

            if (world.isClient()) {
                return ActionResult.SUCCESS;
            }
            if (player instanceof ServerPlayerEntity serverPlayer
                    && ModItems.DOMAIN_LEDGER instanceof DomainLedgerItem ledger) {
                ledger.useOnBell(serverPlayer, hitResult.getBlockPos());
                return ActionResult.SUCCESS_SERVER;
            }
            return ActionResult.PASS;
        });
    }
}
