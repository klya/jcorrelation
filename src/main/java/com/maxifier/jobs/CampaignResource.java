package com.maxifier.jobs;

import com.maxifier.sync.resource.SyncResource;

public class CampaignResource implements SyncResource {
    @Override
    public String getName() {
        return "Entity/Campaign";
    }
}
