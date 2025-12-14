package io.tradecraft.oms.repo;

import io.tradecraft.common.id.ParentId;
import io.tradecraft.oms.core.ParentOrderFsm;

public interface ParentFsmRepository {
    ParentOrderFsm get(ParentId pid);

    void create(ParentId pid);

    void removeIfTerminal(ParentId pid);
}
