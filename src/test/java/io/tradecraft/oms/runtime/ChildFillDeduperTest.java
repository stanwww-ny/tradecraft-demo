package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ChildId;
import io.tradecraft.common.id.ExecId;
import io.tradecraft.common.id.IdFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChildFillDeduperTest {

    IdFactory ids;
    ChildId childIdA, childIdB, childIdC;
    ExecId execId1, execId2,  execId3;
    ChildFillDeduper deduper;

    @BeforeEach
    void setUp() {
        ids = IdFactory.testIds(42L);
        childIdA = ids.child().allocate();
        childIdB = ids.child().allocate();
        childIdC = ids.child().allocate();
        execId1 = ids.exec().next();
        execId2 = ids.exec().next();
        execId3 = ids.exec().next();
        deduper = new ChildFillDeduper(64);
    }
    @Test
    void firstAndSecondFill() {
        assertFalse(deduper.isDuplicate(childIdA, execId1), "First time seeing an ExecId should NOT be duplicate");
        assertTrue(deduper.isDuplicate(childIdA, execId1), "Second time seeing same ExecId should be duplicate");
    }

    @Test
    void differentExecIdsAreNew() {
        // add maximum entries
        assertFalse(deduper.isDuplicate(childIdA, execId1));
        assertFalse(deduper.isDuplicate(childIdA, execId2));
        assertFalse(deduper.isDuplicate(childIdA, execId3));
        for (int i=3; i<64; ++i) {
            assertFalse(deduper.isDuplicate(childIdA, ids.exec().next()));
        }
        // 11 should still be counted as duplicate within the window
        assertTrue(deduper.isDuplicate(childIdA, execId1));

        // ExecId 0 falls out of window; treated as new again
        assertFalse(deduper.isDuplicate(childIdA, ids.exec().next()));
        assertTrue(deduper.isDuplicate(childIdA, execId1), "ExecId should be considered new again after falling out of window");
    }

    @Test
    void windowDoesNotMixDifferentChildren() {
        // Both children should treat this ExecId as new independently
        assertFalse(deduper.isDuplicate(childIdA, execId1));
        assertFalse(deduper.isDuplicate(childIdB, execId1));
    }

}
