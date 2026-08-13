package net.hfstack.realmguard.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.hfstack.realmguard.RealmOfTheGuard;
import net.hfstack.realmguard.item.DomainLedgerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModItems {
    private static final RegistryKey<Item> DOMAIN_LEDGER_KEY = RegistryKey.of(
            RegistryKeys.ITEM,
            RealmOfTheGuard.id("domain_ledger")
    );

    public static final Item DOMAIN_LEDGER = Registry.register(
            Registries.ITEM,
            DOMAIN_LEDGER_KEY,
            new DomainLedgerItem(new Item.Settings().registryKey(DOMAIN_LEDGER_KEY).maxCount(1))
    );

    private ModItems() {
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(DOMAIN_LEDGER));
    }
}
