package com.maxifier.jobs;

import com.maxifier.sync.resource.SyncResource;

public class StatsResource implements SyncResource {
    @Override
    public String getName() {
        return "Statistics";
    }
}
