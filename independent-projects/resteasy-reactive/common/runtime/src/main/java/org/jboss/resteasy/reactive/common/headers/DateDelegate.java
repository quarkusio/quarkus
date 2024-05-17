package org.jboss.resteasy.reactive.common.headers;

import java.util.Date;

import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.reactive.common.util.DateUtil;

public class DateDelegate implements RuntimeDelegate.HeaderDelegate<Date> {
    public static final DateDelegate INSTANCE = new DateDelegate();

    @Override
    public Date fromString(String value) {
        if (value == null)
            throw new IllegalArgumentException("Param was null");
        return DateUtil.parseDate(value);
    }

    @Override
    public String toString(Date value) {
        if (value == null)
            throw new IllegalArgumentException("Param was null");
        return DateUtil.formatDate(value);
    }
}
