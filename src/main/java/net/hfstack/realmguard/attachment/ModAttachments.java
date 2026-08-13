package net.hfstack.realmguard.attachment;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.hfstack.realmguard.RealmOfTheGuard;
import net.hfstack.realmguard.domain.settlement.RealmWorldData;

public final class ModAttachments {
    public static final AttachmentType<RealmWorldData> REALM_WORLD_DATA = AttachmentRegistry.create(
            RealmOfTheGuard.id("realm_world_data"),
            builder -> builder
                    .initializer(RealmWorldData::empty)
                    .persistent(RealmWorldData.CODEC)
    );

    private ModAttachments() {
    }

    public static void initialize() {
        // Class loading registers attachment types.
    }
}
