package net.hfstack.realmguard.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.hfstack.realmguard.domain.settlement.SettlementRecord;
import net.hfstack.realmguard.item.DomainLedgerItem;
import net.hfstack.realmguard.service.SettlementResult;
import net.hfstack.realmguard.service.SettlementService;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class RealmCommands {
    private static final SettlementService SETTLEMENTS = new SettlementService();

    private RealmCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("realm")
                        .then(literal("settlement")
                                .then(literal("register")
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(RealmCommands::registerSettlement)))
                                .then(literal("rename")
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(RealmCommands::renameSettlement)))
                                .then(literal("claim")
                                        .requires(source -> source.getPermissions().hasPermission(
                                                new Permission.Level(PermissionLevel.GAMEMASTERS)
                                        ))
                                        .executes(RealmCommands::claimSettlement))
                                .then(literal("info").executes(RealmCommands::showInfo))
                                .then(literal("list").executes(RealmCommands::listSettlements)))
        ));
    }

    private static int registerSettlement(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Optional<BlockPos> bell = SETTLEMENTS.findNearestBell(player.getEntityWorld(), player.getBlockPos());
        if (bell.isEmpty()) {
            source.sendError(Text.translatable("message.realmguard.settlement.no_bell"));
            return 0;
        }

        SettlementResult result = SETTLEMENTS.register(player, bell.get(), StringArgumentType.getString(context, "name"));
        if (!result.succeeded()) {
            source.sendError(result.feedback());
            return 0;
        }
        source.sendFeedback(() -> result.feedback(), false);
        DomainLedgerItem.showSummary(player, result.record().orElseThrow(), SETTLEMENTS.inspect(player.getEntityWorld(), bell.get()));
        return 1;
    }

    private static int renameSettlement(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        SettlementResult result = SETTLEMENTS.rename(player, StringArgumentType.getString(context, "name"));
        if (!result.succeeded()) {
            source.sendError(result.feedback());
            return 0;
        }
        source.sendFeedback(() -> result.feedback(), false);
        return 1;
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Optional<SettlementRecord> settlement = SETTLEMENTS.nearest(
                player.getEntityWorld(),
                player.getBlockPos(),
                SettlementService.PREVIEW_RADIUS
        );
        if (settlement.isEmpty()) {
            source.sendError(Text.translatable("message.realmguard.settlement.none_nearby"));
            return 0;
        }
        SettlementRecord record = settlement.get();
        DomainLedgerItem.showSummary(player, record, SETTLEMENTS.inspect(player.getEntityWorld(), record.center()));
        return 1;
    }

    private static int claimSettlement(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        Optional<SettlementRecord> transferred = SETTLEMENTS.transferNearestTo(player);
        if (transferred.isEmpty()) {
            source.sendError(Text.translatable("message.realmguard.settlement.none_nearby"));
            return 0;
        }
        SettlementRecord settlement = transferred.get();
        source.sendFeedback(() -> Text.translatable(
                "message.realmguard.settlement.claimed",
                settlement.name(),
                player.getName()
        ), true);
        DomainLedgerItem.showSummary(player, settlement, SETTLEMENTS.inspect(player.getEntityWorld(), settlement.center()));
        return 1;
    }

    private static int listSettlements(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerWorld world = source.getWorld();
        List<SettlementRecord> settlements = SETTLEMENTS.settlements(world);
        if (settlements.isEmpty()) {
            source.sendFeedback(() -> Text.translatable("message.realmguard.settlement.list_empty"), false);
            return 1;
        }

        source.sendFeedback(() -> Text.translatable("message.realmguard.settlement.list_header", settlements.size()), false);
        for (SettlementRecord settlement : settlements) {
            source.sendFeedback(() -> Text.translatable(
                    "message.realmguard.settlement.list_entry",
                    settlement.name(),
                    settlement.center().getX(),
                    settlement.center().getY(),
                    settlement.center().getZ()
            ), false);
        }
        return settlements.size();
    }
}
