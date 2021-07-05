package io.quarkus.scala3.deployment;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.jackson.spi.ClassPathJacksonModuleBuildItem;

public class Scala3Processor {

    private static final String SCALA_JACKSON_MODULE = "com.fasterxml.jackson.module.scala.DefaultScalaModule";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SCALA3);
    }

    // TODO: Native support for Scala 3 in "scala-jackson-module" is under development and will be released in next version
    //   See: https://github.com/FasterXML/jackson-module-scala#scala-3
    //   Scala 3 is interopable with Scala 2.13 libraries -- if this is using the 2.13 module then it should be alright
    //   But I'm not knowledgeable enough to form a solid understanding of whether or not this should still work properly.
    //   Probably ask for a PR review here

    /*
     * Register the Scala Jackson module if that has been added to the classpath
     * Producing the BuildItem is entirely safe since if quarkus-jackson is not on the classpath
     * the BuildItem will just be ignored
     */
    @BuildStep
    void registerScalaJacksonModule(BuildProducer<ClassPathJacksonModuleBuildItem> classPathJacksonModules) {
        try {
            Class.forName(SCALA_JACKSON_MODULE, false, Thread.currentThread().getContextClassLoader());
            classPathJacksonModules.produce(new ClassPathJacksonModuleBuildItem(SCALA_JACKSON_MODULE));
        } catch (Exception ignored) {
        }
    }
}
