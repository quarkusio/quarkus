package org.acme.substitution;

public class InnerHolder {

    public static class Used {

        public String hello() {
            return "InnerHolder.Used";
        }
    }

    public static class Unused extends UnusedBase {

        public String hello() {
            return UnusedHelper.hello();
        }
    }
}
