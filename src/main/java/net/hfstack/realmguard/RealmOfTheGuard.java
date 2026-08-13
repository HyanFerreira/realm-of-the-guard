package net.hfstack.realmguard;

import net.fabricmc.api.ModInitializer;
import net.hfstack.realmguard.attachment.ModAttachments;
import net.hfstack.realmguard.command.RealmCommands;
import net.hfstack.realmguard.event.DomainLedgerInteraction;
import net.hfstack.realmguard.event.LegitimacyEvents;
import net.hfstack.realmguard.integration.rallyguard.RallyGuardIntegration;
import net.hfstack.realmguard.registry.ModItems;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmOfTheGuard implements ModInitializer {
	public static final String MOD_ID = "realmguard";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModAttachments.initialize();
		ModItems.initialize();
		DomainLedgerInteraction.register();
		LegitimacyEvents.register();
		RealmCommands.register();
		RallyGuardIntegration.initialize();
		LOGGER.info("Realm of the Guard initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
