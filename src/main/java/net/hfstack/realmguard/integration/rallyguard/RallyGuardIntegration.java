package net.hfstack.realmguard.integration.rallyguard;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.hfstack.rallyguard.api.RallyGuardApi;
import net.hfstack.rallyguard.api.command.GuardCommandResult;
import net.hfstack.rallyguard.api.command.GuardCommandService;
import net.hfstack.rallyguard.api.recruitment.GuardRecruitmentEvents;
import net.hfstack.rallyguard.api.recruitment.RecruitmentDecision;
import net.hfstack.realmguard.RealmOfTheGuard;
import net.hfstack.realmguard.service.LegitimacyService;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RallyGuardIntegration {
    public static final int REQUIRED_API_VERSION = 1;

    private static final GuardCommandService GUARD_COMMANDS = RallyGuardApi.guardCommands();
    private static final LegitimacyService LEGITIMACY = new LegitimacyService();
    private static boolean initialized;

    private RallyGuardIntegration() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        int apiVersion = RallyGuardApi.apiVersion();
        if (apiVersion < REQUIRED_API_VERSION) {
            throw new IllegalStateException(
                    "Realm of the Guard requires Rally API " + REQUIRED_API_VERSION
                            + " or newer, but found " + apiVersion
            );
        }

        GuardRecruitmentEvents.BEFORE.register((context, offer) -> RecruitmentDecision.allow(offer));
        GuardRecruitmentEvents.AFTER.register((context, appliedOffer) -> {
                LEGITIMACY.adjustOwnedSettlementAt(
                        context.player(),
                        context.position(),
                        2,
                        "reason.realmguard.legitimacy.guard_recruited"
                );
                RealmOfTheGuard.LOGGER.debug(
                        "Observed Rally recruitment: guard={}, player={}, payment={}x{}",
                        context.guard().getUuid(),
                        context.player().getUuid(),
                        appliedOffer.cost(),
                        appliedOffer.paymentItemId()
                );
        });

        initialized = true;
        RealmOfTheGuard.LOGGER.info("Rally of the Guard integration initialized with API {}.", apiVersion);
    }

    public static GuardCommandService guardCommands() {
        return GUARD_COMMANDS;
    }

    public static GuardCommandResult followGuard(ServerPlayerEntity commander, GuardEntity guard) {
        return GUARD_COMMANDS.follow(commander, guard);
    }
}
