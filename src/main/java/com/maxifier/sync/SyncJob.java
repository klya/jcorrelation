/*
* Copyright (c) 2008-2013 Maxifier Ltd. All Rights Reserved.
*/
package com.maxifier.sync;

import com.maxifier.sync.resource.ResourceContext;

public interface SyncJob<Config> {
    void doJob(ResourceContext context, Config config) throws Exception;
}
