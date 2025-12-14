package io.tradecraft.oms.runtime;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.oms.core.DefaultParentOrderFsm;
import io.tradecraft.oms.core.ParentOrderFsm;
import io.tradecraft.oms.repo.ParentFsmRepository;

import java.util.HashMap;
import java.util.Map;

public class DefaultParentFsmRepository implements ParentFsmRepository {
    Map<ParentId, DefaultParentOrderFsm> map = new HashMap<>();

    @Override
    public ParentOrderFsm get(ParentId pid) {
        ParentOrderFsm fsm = map.get(pid);
        if (fsm == null) {
            create(pid);      // safe even if ev is not EvNew; your factory can decide
        }
        return map.get(pid);
    }

    public void create(ParentId pid) {
        map.computeIfAbsent(pid, id -> new DefaultParentOrderFsm());
    }

    @Override
    public void removeIfTerminal(ParentId pid) {

    }
}
