package io.quarkus.extest.runtime.config;

import java.util.Date;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "data")
public class XmlData {
    private String name;
    private String model;
    private Date date;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "XmlData{" +
                "name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", date='" + date + '\'' +
                '}';
    }
}
