package net.hfstack.realmguard.service;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.registry.RegistryKey;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RegistrationPreviewService {
    public static final Duration CONFIRMATION_WINDOW = Duration.ofSeconds(30);

    private final Map<UUID, PendingPreview> pending = new HashMap<>();

    public void remember(ServerPlayerEntity player, BlockPos anchor) {
        pending.put(player.getUuid(), new PendingPreview(
                player.getEntityWorld().getRegistryKey(),
                anchor.toImmutable(),
                System.currentTimeMillis() + CONFIRMATION_WINDOW.toMillis()
        ));
    }

    public boolean consume(ServerPlayerEntity player, BlockPos anchor) {
        PendingPreview preview = pending.remove(player.getUuid());
        return preview != null
                && preview.expiresAtMillis() >= System.currentTimeMillis()
                && preview.world().equals(player.getEntityWorld().getRegistryKey())
                && preview.anchor().equals(anchor);
    }

    private record PendingPreview(RegistryKey<World> world, BlockPos anchor, long expiresAtMillis) {
    }
}
