package io.tradecraft.venue.store;

import io.tradecraft.common.domain.time.DualTimeSource;
import io.tradecraft.common.testing.TestClocks;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryVenueOrderRepositoryIdempotencyTest {
    private final DualTimeSource dualTimeSource = TestClocks.msTicker();

    @Test
    @DisplayName("seenCmd returns true the first time, false subsequently (idempotency guard)")
    void seenCmd_idempotency() {
        InMemoryVenueOrderRepository repo = new InMemoryVenueOrderRepository(dualTimeSource);

        String cmdId = "CMD-123";

        assertTrue(repo.seenCmd(cmdId), "first time should be true");
        assertFalse(repo.seenCmd(cmdId), "second time should be false");
        assertFalse(repo.seenCmd(cmdId), "third time should still be false");

        // Different id should be independent
        assertTrue(repo.seenCmd("CMD-124"), "new id should be true again");
    }

    @Test
    @DisplayName("seenExec returns true the first time, false subsequently (idempotency guard)")
    void seenExec_idempotency() {
        InMemoryVenueOrderRepository repo = new InMemoryVenueOrderRepository(dualTimeSource);

        String execId = "EXEC-999";

        assertTrue(repo.seenExec(execId), "first time should be true");
        assertFalse(repo.seenExec(execId), "second time should be false");
        assertFalse(repo.seenExec(execId), "third time should still be false");

        // Different id should be independent
        assertTrue(repo.seenExec("EXEC-1000"), "new id should be true again");
    }
}
