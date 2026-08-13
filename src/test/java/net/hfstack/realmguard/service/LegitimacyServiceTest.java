package net.hfstack.realmguard.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegitimacyServiceTest {
    @Test
    void appliesPositiveAndNegativeChanges() {
        assertEquals(27, LegitimacyService.calculateUpdatedValue(25, 2));
        assertEquals(23, LegitimacyService.calculateUpdatedValue(25, -2));
    }

    @Test
    void clampsChangesToSupportedRangeWithoutOverflow() {
        assertEquals(100, LegitimacyService.calculateUpdatedValue(99, 10));
        assertEquals(0, LegitimacyService.calculateUpdatedValue(1, -15));
        assertEquals(100, LegitimacyService.calculateUpdatedValue(50, Integer.MAX_VALUE));
        assertEquals(0, LegitimacyService.calculateUpdatedValue(50, Integer.MIN_VALUE));
    }
}
