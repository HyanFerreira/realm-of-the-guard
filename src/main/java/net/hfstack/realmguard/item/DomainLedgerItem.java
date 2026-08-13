package net.hfstack.realmguard.item;

import net.hfstack.realmguard.domain.settlement.SettlementRecord;
import net.hfstack.realmguard.service.SettlementResult;
import net.hfstack.realmguard.service.SettlementService;
import net.hfstack.realmguard.service.SettlementSnapshot;
import net.hfstack.realmguard.service.RegistrationPreviewService;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

public final class DomainLedgerItem extends Item {
    private static final SettlementService SETTLEMENTS = new SettlementService();
    private static final RegistrationPreviewService PREVIEWS = new RegistrationPreviewService();

    public DomainLedgerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().getBlockState(context.getBlockPos()).isOf(Blocks.BELL)) {
            return ActionResult.PASS;
        }
        if (context.getWorld().isClient()) {
            return ActionResult.SUCCESS;
        }

        PlayerEntity player = context.getPlayer();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        useOnBell(serverPlayer, context.getBlockPos());
        return ActionResult.SUCCESS_SERVER;
    }

    public void useOnBell(ServerPlayerEntity serverPlayer, BlockPos anchor) {
        ServerWorld world = serverPlayer.getEntityWorld();
        var existing = SETTLEMENTS.data(world).atCenter(anchor);
        if (existing.isPresent()) {
            showSummary(serverPlayer, existing.get(), SETTLEMENTS.inspect(world, anchor));
            return;
        }

        String provisionalName = "Settlement " + anchor.getX() + ", " + anchor.getZ();
        if (!serverPlayer.isSneaking()) {
            var issue = SETTLEMENTS.registrationIssue(serverPlayer, anchor, provisionalName);
            if (issue.isPresent()) {
                serverPlayer.sendMessage(issue.get(), false);
                return;
            }
            SettlementSnapshot snapshot = SETTLEMENTS.inspect(world, anchor);
            serverPlayer.sendMessage(Text.translatable("message.realmguard.ledger.preview_header", provisionalName), false);
            showPreview(serverPlayer, anchor, snapshot);
            PREVIEWS.remember(serverPlayer, anchor);
            serverPlayer.sendMessage(Text.translatable(
                    "message.realmguard.ledger.confirm_hint",
                    RegistrationPreviewService.CONFIRMATION_WINDOW.toSeconds()
            ), false);
            return;
        }
        if (!PREVIEWS.consume(serverPlayer, anchor)) {
            serverPlayer.sendMessage(Text.translatable("message.realmguard.ledger.preview_required"), false);
            return;
        }

        SettlementResult result = SETTLEMENTS.register(serverPlayer, anchor, provisionalName);
        serverPlayer.sendMessage(result.feedback(), false);
        if (result.succeeded()) {
            SettlementRecord settlement = result.record().orElseThrow();
            showSummary(serverPlayer, settlement, SETTLEMENTS.inspect(world, anchor));
            serverPlayer.sendMessage(Text.translatable("message.realmguard.ledger.rename_hint"), false);
        }
    }

    public static void showSummary(ServerPlayerEntity player, SettlementRecord settlement, SettlementSnapshot snapshot) {
        player.sendMessage(Text.translatable(
                "message.realmguard.settlement.summary",
                settlement.name(),
                snapshot.anchorPresent()
                        ? Text.translatable("message.realmguard.settlement.anchor_active")
                        : Text.translatable("message.realmguard.settlement.anchor_missing"),
                snapshot.villagers(),
                snapshot.beds(),
                snapshot.workstations(),
                snapshot.guards(),
                settlement.center().getX(),
                settlement.center().getY(),
                settlement.center().getZ(),
                Text.translatable(settlement.rulerTitle().translationKey()),
                settlement.legitimacy(),
                settlement.ruler().equals(player.getUuid())
                        ? Text.translatable("message.realmguard.settlement.ruler_you")
                        : Text.translatable("message.realmguard.settlement.ruler_other", settlement.ruler().toString())
        ), false);
    }

    private static void showPreview(ServerPlayerEntity player, BlockPos anchor, SettlementSnapshot snapshot) {
        player.sendMessage(Text.translatable(
                "message.realmguard.settlement.preview",
                anchor.getX(),
                anchor.getY(),
                anchor.getZ(),
                snapshot.villagers(),
                snapshot.beds(),
                snapshot.workstations(),
                snapshot.guards(),
                Text.translatable("title.realmguard.protector"),
                SettlementService.INITIAL_LEGITIMACY
        ), false);
    }
}
