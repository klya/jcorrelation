/*
* Copyright (c) 2008-2013 Maxifier Ltd. All Rights Reserved.
*/
package com.maxifier.sync.resource;

import javax.persistence.EntityManager;
import java.util.Collection;

public interface ResourceContext {
    void lockResources(Collection<SyncResource> resource);

    void unlockResources(Collection<SyncResource> resource);

    void successfullyChanged(SyncResource resource);

    void successfullyChanged(SyncResource resource, Collection<? extends SyncResource> sources);

    void broadcastChanges();

    Walker getHistoryWalker();
    EntityManager getEntityManager();


    interface Walker extends Iterable<SyncResource> {
        Walker stepOn(SyncResource resource);

        Walker stepOn(SyncResource resource, SyncResource mark);
    }
}
