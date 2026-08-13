package net.hfstack.realmguard.integration.rallyguard;

import net.hfstack.rallyguard.api.RallyGuardApi;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RallyGuardContractTest {
    @Test
    void loadsRequiredRallyApiServices() {
        assertTrue(RallyGuardApi.apiVersion() >= RallyGuardIntegration.REQUIRED_API_VERSION);
        assertNotNull(RallyGuardApi.guardRecruitment());
        assertSame(RallyGuardApi.guardCommands(), RallyGuardIntegration.guardCommands());
        assertDoesNotThrow(RallyGuardIntegration::initialize);
        assertDoesNotThrow(RallyGuardIntegration::initialize);
    }
}
