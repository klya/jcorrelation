/*
 * Copyright (c) 2008-2015 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.jobs;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * @author Konstantin Lyamshin (2015-03-23 19:54)
 */
@Entity
public class FileData {
    @Id
    public String uri;

    @Temporal(TemporalType.DATE)
    public Date date;

    public int lines;

    public String etag;

    public FileData() {
    }

    public FileData(String uri, Date date) {
        this.uri = uri;
        this.date = date;
    }
}
