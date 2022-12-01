package io.quarkus.arquillian;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.jboss.arquillian.container.spi.context.annotation.DeploymentScoped;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestResult;
import org.jboss.arquillian.test.spi.event.suite.Test;

public class ClassLoaderExceptionTransformer {

    @Inject
    @DeploymentScoped
    Instance<ClassLoader> classLoaderInstance;

    @Inject
    Instance<TestResult> testResultInstance;

    public void transform(@Observes(precedence = -1000) Test event) {
        TestResult testResult = testResultInstance.get();
        if (testResult != null) {
            Throwable res = testResult.getThrowable();
            if (res != null) {
                try {
                    if (res.getClass().getClassLoader() != null
                            && res.getClass().getClassLoader() != getClass().getClassLoader()) {
                        if (res.getClass() == classLoaderInstance.get().loadClass(res.getClass().getName())) {
                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                            ObjectOutputStream oo = new ObjectOutputStream(out);
                            oo.writeObject(res);
                            res = (Throwable) new ObjectInputStream(new ByteArrayInputStream(out.toByteArray())).readObject();
                            testResult.setThrowable(res);
                        }
                    }
                } catch (Exception ignored) {

                }
            }
        }
    }
}
