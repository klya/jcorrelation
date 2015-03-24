package com.maxifier.jobs;

import com.maxifier.sync.resource.SyncResource;

import java.net.URI;
import java.util.Date;

public class FileResource implements SyncResource {
    public final Date date;
    public final URI uri;
    public final String etag;

    public FileResource(URI uri, String etag) {
        this.date = new Date();
        this.uri = uri;
        this.etag = etag;
    }

    @Override
    public String getName() {
        return "remote/files";
    }
}
