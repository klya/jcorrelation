package com.maxifier.jobs;

import com.beust.jcommander.Parameters;
import com.maxifier.sync.SyncJob;
import com.maxifier.sync.resource.ResourceContext;

/**
 * Created with IntelliJ IDEA.
 * User: xrcat
 * Date: 19.03.15
 * Time: 13:47
 * To change this template use File | Settings | File Templates.
 */
public class CampaignSyncJob implements SyncJob<CampaignSyncJob.Config> {
    @Override
    public void doJob(ResourceContext context, Config config) throws Exception {
        context.successfullyChanged(new CampaignResource());
        context.broadcastChanges();
        System.out.println("Campaign marked as changed");
    }

    @Parameters(commandDescription = "Update campaigns")
    public static class Config {
    }
}
