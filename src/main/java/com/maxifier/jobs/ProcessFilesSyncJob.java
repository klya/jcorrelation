package com.maxifier.jobs;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.maxifier.sync.SyncJob;
import com.maxifier.sync.resource.ResourceContext;
import com.maxifier.sync.resource.SyncResource;

import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProcessFilesSyncJob implements SyncJob<ProcessFilesSyncJob.Config> {
    @Override
    public void doJob(ResourceContext context, Config config) throws Exception {
        Date nextDate = null;
        ResourceContext.Walker walker1 = context.getHistoryWalker()
            .stepOn(new ReportResource(null, false));
        for (SyncResource resource : walker1) {
            ReportResource r = (ReportResource) resource;
            if (!r.patched) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(r.date);
                calendar.add(Calendar.DATE, 1);
                nextDate = calendar.getTime();
                break; // next report date found
            }
        }

        EntityManager entityManager = context.getEntityManager();

        // manual mode
        if (config.from != null && config.to != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(config.from);
            while (calendar.getTime().before(config.to)) {
                Date date = calendar.getTime();
                processDate(entityManager, date);

                calendar.add(Calendar.DATE, 1);
                if (nextDate == null || date.equals(nextDate) || config.markConsistent) {
                    nextDate = calendar.getTime();
                    context.successfullyChanged(new ReportResource(date, false));
                } else {
                    context.successfullyChanged(new ReportResource(date, true));
                }
            }
            context.broadcastChanges();
            return;
        }

        // auto mode
        HashMap<Date, List<FileResource>> resources = new HashMap<Date, List<FileResource>>();

        ResourceContext.Walker walker2 = context.getHistoryWalker()
            .stepOn(new FileResource(null, null), new ReportResource(null, false));
        for (SyncResource resource : walker2) {
            FileResource r = (FileResource) resource;
            File file = new File(r.uri.getPath());
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(file.getName());
            if (!resources.containsKey(date)) {
                resources.put(date, new ArrayList<FileResource>());
            }
            resources.get(date).add(r);
        }

        if (config.checkSchedule) {
            if (!resources.isEmpty()) {
                throw new IllegalStateException("Not processed files found");
            }
            return;
        }

        Calendar calendar = Calendar.getInstance();
        ArrayList<Date> dates = new ArrayList<Date>(resources.keySet());
        Collections.sort(dates);
        for (Date date : dates) {
            processDate(entityManager, date);
            if (nextDate == null || date.equals(nextDate) || config.markConsistent) {
                calendar.setTime(date);
                calendar.add(Calendar.DATE, 1);
                nextDate = calendar.getTime();
                context.successfullyChanged(new ReportResource(date, false), resources.get(date));
            } else {
                context.successfullyChanged(new ReportResource(date, true), resources.get(date));
            }
        }
        context.broadcastChanges();
    }

    private void processDate(EntityManager entityManager, Date date) {
        Number lines = (Number) entityManager
            .createQuery("select sum(o.lines) from FileData o where o.date = ?1")
            .setParameter(1, date, TemporalType.DATE)
            .getSingleResult();
        if (lines == null) {
            return;
        }

        List<Report> reports = entityManager
            .createQuery("select o from Report o where o.date = ?1", Report.class)
            .setParameter(1, date, TemporalType.DATE)
            .getResultList();
        if (reports.isEmpty()) {
            Report report = new Report();
            report.date = date;
            report.lines = lines.intValue();
            entityManager.persist(report);
        } else {
            reports.get(0).lines = lines.intValue();
        }
    }

    @Parameters(commandDescription = "Process log files")
    public static class Config {
        @Parameter(names = "--from", description = "manually specify lower bound (inclusive)")
        public Date from;

        @Parameter(names = "--to", description = "manually specify upper bound (non inclusive)")
        public Date to;

        @Parameter(names = "--markConsistent", description = "marks report as consistent")
        public boolean markConsistent;

        @Parameter(names = "--checkSchedule", description = "check schedule mode")
        public boolean checkSchedule;
    }
}
