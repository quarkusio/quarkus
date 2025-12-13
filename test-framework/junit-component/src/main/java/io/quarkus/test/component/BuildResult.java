package io.quarkus.test.component;

import java.util.Map;
import java.util.Set;

record BuildResult(Map<String, byte[]> generatedClasses,
        byte[] componentsProvider,
        // prefix -> config mapping FQCN
        Map<String, Set<String>> configMappings,
        // key -> [testClass, methodName, paramType1, paramType2]
        Map<String, String[]> interceptorMethods,
        Throwable failure) {

}