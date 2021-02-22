package org.acme.cxf;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "ping", namespace = "http://cxf.acme.org/")
@XmlType(name = "ping", namespace = "http://cxf.acme.org/")
public class Ping {
    private String text;

    public Ping() {
    }

    @XmlElement(name = "text", namespace = "")
    public String getText() {
        return this.text;
    }

    public void setText(String var1) {
        this.text = var1;
    }
}
