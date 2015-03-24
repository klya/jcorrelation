package com.maxifier.jobs;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.maxifier.sync.SyncJob;
import com.maxifier.sync.resource.ResourceContext;
import com.maxifier.sync.resource.SyncResource;

import java.util.ArrayList;

public class StatsSyncJob implements SyncJob<StatsSyncJob.Config> {
    @Override
    public void doJob(ResourceContext context, Config config) throws Exception {
        ResourceContext.Walker walker = context.getHistoryWalker()
            .stepOn(new CampaignResource(), new StatsResource())
            .stepOn(new ReportResource(null, false), new StatsResource());
        ArrayList<SyncResource> resources = new ArrayList<SyncResource>();
        for (SyncResource resource : walker) {
            resources.add(resource);
        }

        if (config.checkSchedule && !resources.isEmpty()) {
            throw new IllegalStateException("Found dirty resources");
        } else {
            System.out.println("Found " + resources.size() + " dirty resources");
            context.successfullyChanged(new StatsResource(), resources);
            context.broadcastChanges();
        }
    }

    @Parameters(commandDescription = "Calculate log statistics")
    public static class Config {
        @Parameter(names = "--checkSchedule", description = "check schedule mode")
        public boolean checkSchedule;
    }
}
