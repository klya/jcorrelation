/*
 * Copyright (c) 2008-2015 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.jobs;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.maxifier.sync.SyncJob;
import com.maxifier.sync.impl.ResourceRecord;
import com.maxifier.sync.resource.ResourceContext;

import javax.persistence.EntityManager;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;

/**
 * @author Konstantin Lyamshin (2015-03-23 16:59)
 */
public class ShowSyncJob implements SyncJob<ShowSyncJob.Config> {
    @Override
    public void doJob(ResourceContext context, Config config) throws Exception {
        ExclusionStrategy exclusionStrategy = new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                return f.getAnnotation(ManyToMany.class) != null || f.getAnnotation(OneToMany.class) != null;
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                return false;
            }
        };

        Gson gson = new GsonBuilder().setExclusionStrategies(exclusionStrategy).create();
        EntityManager entityManager = context.getEntityManager();

        if (config.history) {
            System.out.println("----------- Resource History -----------");
            TypedQuery<ResourceRecord> query = entityManager.createQuery("select o from ResourceRecord o order by o.id asc", ResourceRecord.class);
            for (ResourceRecord o : query.getResultList()) {
                System.out.println(gson.toJson(o));
            }
        }

        if (config.files) {
            System.out.println("-------------- Files Data --------------");
            TypedQuery<FileData> query = entityManager.createQuery("select o from FileData o order by o.date asc", FileData.class);
            for (FileData o : query.getResultList()) {
                System.out.println(gson.toJson(o));
            }
        }

        if (config.report) {
            System.out.println("-------------- Report Data -------------");
            TypedQuery<Report> query = entityManager.createQuery("select o from Report o order by o.date asc", Report.class);
            for (Report o : query.getResultList()) {
                System.out.println(gson.toJson(o));
            }
        }
    }

    @Parameters(commandDescription = "Prints various diagnostics")
    public static class Config {
        @Parameter(names = "--history", description = "Print resource history")
        boolean history;

        @Parameter(names = "--files", description = "Print files process result")
        boolean files;

        @Parameter(names = "--report", description = "Print report")
        boolean report;
    }
}
