package net.hfstack.realmguard.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementSnapshotTest {
    @Test
    void acceptsEmptySettlementAndRejectsNegativeCounts() {
        assertDoesNotThrow(() -> new SettlementSnapshot(0, 0, 0, 0, false));
        assertThrows(IllegalArgumentException.class, () -> new SettlementSnapshot(-1, 0, 0, 0, true));
    }
}
