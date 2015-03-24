package com.maxifier.sync.impl;

import com.google.gson.Gson;
import com.maxifier.sync.resource.SyncResource;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
public class ResourceRecord {
    @Id
    @GeneratedValue
    public int id;

    @Basic(optional = false)
    public Date date;

    @Basic(optional = false)
    public String resourceName;

    @Basic(optional = false)
    public String className;

    @Lob
    public String data;

    @ManyToMany
    public Set<ResourceRecord> sources;

    @ManyToMany(mappedBy = "sources")
    public Set<ResourceRecord> derivatives;

    public ResourceRecord() {
    }

    public ResourceRecord(SyncResource resource, Gson gson) {
        this.date = new Date();
        this.resourceName = resource.getName() + "/";
        this.className = resource.getClass().getName();
        this.data = gson.toJson(resource);
        this.sources = new HashSet<ResourceRecord>();
    }
}
