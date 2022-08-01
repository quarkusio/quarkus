package io.quarkus.arc.test.clientproxy.constructor.some;

public abstract class Resource {

    Resource() {
    }

    public static Resource from(int ping) {
        return new Resource() {

            @Override
            public int ping() {
                return ping;
            }
        };
    }

    public abstract int ping();

}
