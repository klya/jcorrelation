package com.maxifier.jobs;

import com.maxifier.sync.resource.SyncResource;

import java.util.Date;

public class ReportResource implements SyncResource {
    public final Date date;
    public final boolean patched;

    public ReportResource(Date date, boolean patched) {
        this.date = date;
        this.patched = patched;
    }

    @Override
    public String getName() {
        return "Report";
    }
}
