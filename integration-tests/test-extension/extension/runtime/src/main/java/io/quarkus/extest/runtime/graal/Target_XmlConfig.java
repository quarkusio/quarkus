package io.quarkus.extest.runtime.graal;

import java.util.ArrayList;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.extest.runtime.config.XData;
import io.quarkus.extest.runtime.config.XmlConfig;

@TargetClass(XmlConfig.class)
@Substitute
final public class Target_XmlConfig {
    @Substitute
    private String address;
    @Substitute
    private int port;
    @Substitute
    private ArrayList<XData> dataList;

    @Substitute
    public String getAddress() {
        return address;
    }

    @Substitute
    public int getPort() {
        return port;
    }

    @Substitute
    public ArrayList<XData> getDataList() {
        return dataList;
    }

    @Substitute
    @Override
    public String toString() {
        return "Target_XmlConfig{" +
                "address='" + address + '\'' +
                ", port=" + port +
                ", dataList=" + dataList +
                '}';
    }
}
