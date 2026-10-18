package io.quarkus.it.arc;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;

@Dependent
public class UnusedBean {

    @Inject
    InjectionPoint injectionPoint;

    public InjectionPoint getInjectionPoint() {
        return injectionPoint;
    }

    public DummyResult dummy(DummyInput dummyInput) {
        return new DummyResult(dummyInput.getName() + "/"
                + dummyInput.getNestedDummyInput().getNums().stream().mapToInt(Integer::intValue).sum());
    }

    public static class DummyResult {
        private final String result;

        public DummyResult(String result) {
            this.result = result;
        }

        public String getResult() {
            return result;
        }
    }

    public static class DummyInput {
        private final String name;
        private final NestedDummyInput nestedDummyInput;

        public DummyInput(String name, NestedDummyInput nestedDummyInput) {
            this.name = name;
            this.nestedDummyInput = nestedDummyInput;
        }

        public String getName() {
            return name;
        }

        public NestedDummyInput getNestedDummyInput() {
            return nestedDummyInput;
        }
    }

    public static class NestedDummyInput {
        private final List<Integer> nums;

        public NestedDummyInput(List<Integer> nums) {
            this.nums = nums;
        }

        public List<Integer> getNums() {
            return nums;
        }
    }

}
