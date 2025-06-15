package io.quarkus.devui.spi.page;

public abstract class BuildTimeDataPageBuilder<T> extends PageBuilder<T> {
    private static final String BUILD_TIME_DATA_KEY = "buildTimeDataKey";

    protected BuildTimeDataPageBuilder(String title) {
        super();
        super.title = title;
        super.internalComponent = true;// As external page runs on "internal" namespace
    }

    @SuppressWarnings("unchecked")
    public T buildTimeDataKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new RuntimeException("Invalid build time data key, can not be empty");
        }
        super.metadata.put(BUILD_TIME_DATA_KEY, key);
        return (T) this;
    }

}
