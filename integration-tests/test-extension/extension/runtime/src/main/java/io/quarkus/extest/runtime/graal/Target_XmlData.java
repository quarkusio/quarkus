package io.quarkus.extest.runtime.graal;

import java.util.Date;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.extest.runtime.config.XmlData;

@TargetClass(XmlData.class)
@Substitute
final public class Target_XmlData {
    @Substitute
    private String name;
    @Substitute
    private String model;
    @Substitute
    private Date date;

    @Substitute
    public String getName() {
        return name;
    }

    @Substitute
    public String getModel() {
        return model;
    }

    @Substitute
    public Date getDate() {
        return date;
    }

    @Substitute
    @Override
    public String toString() {
        return "Target_XmlData{" +
                "name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", date='" + date + '\'' +
                '}';
    }

}
