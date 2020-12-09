package io.quarkus.funqy.test;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventBuilder;
import io.quarkus.funqy.knative.events.CloudEventMapping;

public class ExposedCloudEvents {

    @Funq
    @CloudEventMapping(trigger = "test-type")
    public CloudEvent<TestBean> doubleIt(CloudEvent<TestBean> event) {

        if (event == null)
            throw new RuntimeException("Event is null!");
        if (!event.id().equals("test-id"))
            throw new RuntimeException("Bad id!");
        if (!event.source().equals("/OfTest"))
            throw new RuntimeException("Bad source!");
        if (!event.type().equals("test-type"))
            throw new RuntimeException("Bad type!");
        if (!event.subject().equals("test-subj"))
            throw new RuntimeException("Bad subject!");
        if (!event.dataSchema().equals("test-dataschema-client"))
            throw new RuntimeException("Bad dataschema!");
        if (!event.extensions().equals(Collections.singletonMap("extclient", "ext-client-val")))
            throw new RuntimeException("Bad extensions!");
        if (event.time() == null)
            throw new RuntimeException("Bad time!");

        TestBean inBean = event.data();
        return CloudEventBuilder.create()
                .specVersion(event.specVersion())
                .id("double-it-id")
                .type("double-it-type")
                .source("/OfDoubleIt")
                .extensions(Collections.singletonMap("extserver", "ext-server-val"))
                .dataSchema("dataschema-server")
                .build(new TestBean(inBean.getI() * 2, inBean.getS() + inBean.getS()));
    }

    @Funq
    @CloudEventMapping(trigger = "test-defaults", responseSource = "default-source", responseType = "default-type")
    public CloudEvent<Void> withDefaults(CloudEvent<Void> event) {
        return CloudEventBuilder.create().build();
    }

    @Funq
    @CloudEventMapping(trigger = "test-generics")
    public CloudEvent<Integer> sum(CloudEvent<List<TestBean>> event) {
        Integer data = event.data().stream()
                .map(TestBean::getI)
                .reduce((a, b) -> a + b)
                .orElse(0);

        return CloudEventBuilder.create()
                .specVersion(event.specVersion())
                .id("test-sum-id")
                .type("test-sum-type")
                .build(data);
    }

    public static class TestBean implements Serializable {
        private int i;
        private String s;

        public TestBean() {
        }

        public TestBean(int i, String s) {
            this.i = i;
            this.s = s;
        }

        public int getI() {
            return i;
        }

        public void setI(int i) {
            this.i = i;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestBean testBean = (TestBean) o;
            return i == testBean.i && Objects.equals(s, testBean.s);
        }

        @Override
        public int hashCode() {
            return Objects.hash(s, i);
        }

        @Override
        public String toString() {
            return "TestBean{" +
                    "i=" + i +
                    ", s='" + s + '\'' +
                    '}';
        }
    }
}
