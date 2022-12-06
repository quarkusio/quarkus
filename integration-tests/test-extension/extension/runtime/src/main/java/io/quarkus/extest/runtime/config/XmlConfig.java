package io.quarkus.extest.runtime.config;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * A hypothetical configuration that needs xml parsing at build time
 */
@XmlRootElement(namespace = "https://quarkus.io")
public class XmlConfig {
    private ArrayList<XmlData> dataList;
    private String address;
    private int port;

    // XmlElement sets the name of the entities
    @XmlElement(name = "data")
    public ArrayList<XmlData> getDataList() {
        return dataList;
    }

    public void setDataList(ArrayList<XmlData> dataList) {
        this.dataList = dataList;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "XmlConfig{" +
                "dataList=" + dataList +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
