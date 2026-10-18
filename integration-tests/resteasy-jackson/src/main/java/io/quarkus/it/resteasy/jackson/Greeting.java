package io.quarkus.it.resteasy.jackson;

import java.sql.Date;
import java.time.LocalDate;

import javax.xml.datatype.XMLGregorianCalendar;

public class Greeting {

    private final String message;
    private final LocalDate date;
    private final Date sqlDate;
    private final XMLGregorianCalendar xmlGregorianCalendar;

    public Greeting(String message, LocalDate date, Date sqlDate, XMLGregorianCalendar xmlGregorianCalendar) {
        this.message = message;
        this.date = date;
        this.sqlDate = sqlDate;
        this.xmlGregorianCalendar = xmlGregorianCalendar;
    }

    public String getMessage() {
        return message;
    }

    public LocalDate getDate() {
        return date;
    }

    public Date getSqlDate() {
        return sqlDate;
    }

    public XMLGregorianCalendar getXmlGregorianCalendar() {
        return xmlGregorianCalendar;
    }
}
