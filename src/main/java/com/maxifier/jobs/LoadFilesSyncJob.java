package com.maxifier.jobs;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.maxifier.sync.SyncJob;
import com.maxifier.sync.resource.ResourceContext;
import com.maxifier.sync.resource.SyncResource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class LoadFilesSyncJob implements SyncJob<LoadFilesSyncJob.Config> {
    public static final int BACKFILL_PERIOD = 30;
    public static final String BASE_URI = "http://localhost:8080/";

    @Override
    public void doJob(ResourceContext context, Config config) throws Exception {
        HashMap<URI, String> etags = new HashMap<URI, String>();
        if (config.from == null && config.to == null) {
            // auto mode
            readProcessedETags(context, etags);
        }

        if (config.from == null) {
            config.from = getBackfillThreshold();
        }
        if (config.to == null) {
            config.to = new Date();
        }

        // Enumerate existing files
        URI baseURI = new URI(BASE_URI);
        CloseableHttpClient client = HttpClients.createDefault();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(config.from);
        for (; calendar.getTime().before(config.to); calendar.add(Calendar.DATE, 1)) { // loop on days
            Date date = calendar.getTime();
            String filePrefix = new SimpleDateFormat("yyyy-MM-dd_").format(date);
            for (int i = 1; ; i++) {
                URI fileURI = baseURI.resolve(filePrefix + i + ".txt");
                HttpGet get = new HttpGet(fileURI);
                String etag = etags.get(fileURI);
                if (etag != null) {
                    get.setHeader(HttpHeaders.IF_NONE_MATCH, etag);
                }

                CloseableHttpResponse response = client.execute(get);
                try {
                    int status = response.getStatusLine().getStatusCode();
                    if (status == HttpStatus.SC_NOT_FOUND) {
                        break; // Move to next date
                    }
                    if (status == HttpStatus.SC_NOT_MODIFIED) { // Not modified
                        continue;
                    }
                    if (status == HttpStatus.SC_OK) { // Modified
                        String etagNew = response.getFirstHeader(HttpHeaders.ETAG).getValue();
                        if (etagNew != null) {
                            if (config.checkSchedule) {
                                throw new IllegalStateException("Some files availaible on server");
                            }
                            readFileContent(context, date, fileURI.toString(), response.getEntity(), etagNew);
                            context.successfullyChanged(new FileResource(fileURI, etagNew));
                        } else {
                            System.out.println("ETag not found in " + fileURI);
                        }
                        continue;
                    }
                    // Error
                    System.out.println("Error while reading file " + fileURI + ": " + response.getStatusLine());
                } finally {
                    response.close();
                }
            }
        }

        context.broadcastChanges();
    }

    private void readFileContent(ResourceContext context, Date date, String uri, HttpEntity entity, String etag) throws IOException {
        EntityManager entityManager = context.getEntityManager();
        FileData data = entityManager.find(FileData.class, uri);
        if (data == null) {
            data = new FileData(uri, date);
            entityManager.persist(data);
        }
        data.lines = 0;
        data.etag = etag;

        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        try {
            while (reader.readLine() != null) {
                data.lines++;
            }
        } finally {
            reader.close();
        }
    }

    private HashMap<URI, String> readProcessedETags(ResourceContext context, HashMap<URI, String> etags) {
        Date backfillThreshold = getBackfillThreshold();

        // Enumerate processed files
        ResourceContext.Walker walker = context.getHistoryWalker()
            .stepOn(new FileResource(null, null));
        for (SyncResource resource : walker) {
            FileResource fileResource = (FileResource) resource;
            if (fileResource.date.before(backfillThreshold)) {
                break; // limit history depth
            }
            if (!etags.containsKey(fileResource.uri)) { // Skip old ETags
                etags.put(fileResource.uri, fileResource.etag);
            }
        }
        return etags;
    }

    private Date getBackfillThreshold() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DATE, -BACKFILL_PERIOD);
        return calendar.getTime();
    }

    @Parameters(commandDescription = "Load log files")
    public static class Config {
        @Parameter(names = "--from", description = "Manual reload period start (inclusive)")
        public Date from;

        @Parameter(names = "--to", description = "Manual reload period end (exclusive)")
        public Date to;

        @Parameter(names = "--checkSchedule", description = "check schedule mode")
        public boolean checkSchedule;
    }
}
