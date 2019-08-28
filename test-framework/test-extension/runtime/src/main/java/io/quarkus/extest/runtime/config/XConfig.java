package io.quarkus.extest.runtime.config;

import java.util.ArrayList;

/**
 * An alternate to XlmConfig that has no JAXB annotations
 */
public class XConfig {
    private ArrayList<XData> dataList;
    private String address;
    private int port;

    public ArrayList<XData> getDataList() {
        return dataList;
    }

    public void setDataList(ArrayList<XData> dataList) {
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
        return "XConfig{" +
                "dataList=" + dataList +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
