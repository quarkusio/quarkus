package io.quarkus.qute.generator.hierarchy;

public class Level3 extends Level2 {

    public int overridenLevel = 3;

    public int getLevel3() {
        return 3;
    }

    // This method should be overridden
    public int getLevel4() {
        return 34;
    }
}
