package net.hfstack.realmguard.service;

import net.hfstack.realmguard.domain.settlement.SettlementRecord;

public record LegitimacyChange(
        SettlementRecord settlement,
        int requestedDelta,
        int appliedDelta,
        int previousValue,
        int currentValue
) {
    public boolean changed() {
        return appliedDelta != 0;
    }
}
