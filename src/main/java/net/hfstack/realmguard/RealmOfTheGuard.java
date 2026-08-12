package net.hfstack.realmguard;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealmOfTheGuard implements ModInitializer {
	public static final String MOD_ID = "realmguard";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Realm of the Guard initialized.");
	}

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
