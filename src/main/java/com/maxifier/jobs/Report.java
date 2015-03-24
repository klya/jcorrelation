/*
 * Copyright (c) 2008-2015 Maxifier Ltd. All Rights Reserved.
 */
package com.maxifier.jobs;

import javax.persistence.*;
import java.util.Date;

/**
 * @author Konstantin Lyamshin (2015-03-23 20:03)
 */
@Entity
public class Report {
    @Id
    @GeneratedValue
    public int id;

    @Temporal(TemporalType.DATE)
    public Date date;

    public int lines;
}
