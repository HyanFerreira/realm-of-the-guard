package net.hfstack.realmguard.service;

public record SettlementSnapshot(
        int villagers,
        int beds,
        int workstations,
        int guards,
        boolean anchorPresent
) {
    public SettlementSnapshot {
        if (villagers < 0 || beds < 0 || workstations < 0 || guards < 0) {
            throw new IllegalArgumentException("Settlement snapshot counts cannot be negative");
        }
    }
}
