/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.creator.config;

import java.nio.file.Paths;
import java.util.Arrays;

import org.jboss.shamrock.creator.AppCreationPhase;
import org.jboss.shamrock.creator.AppCreator;
import org.jboss.shamrock.creator.config.reader.MappedPropertiesHandler;
import org.jboss.shamrock.creator.config.reader.PropertiesConfigReaderException;
import org.jboss.shamrock.creator.config.reader.PropertiesHandler;
import org.jboss.shamrock.creator.config.reader.PropertyContext;
import org.jboss.shamrock.creator.phase.augment.AugmentPhase;
import org.jboss.shamrock.creator.phase.nativeimage.NativeImagePhase;

/**
 *
 * @author Alexey Loubyansky
 */
public class AppCreatorPropertiesHandler implements PropertiesHandler<AppCreator> {

    private final PropertiesHandler<AugmentPhase> augmentHandler = new MappedPropertiesHandler<AugmentPhase>() {
        @Override
        public AugmentPhase newInstance() {
            return new AugmentPhase();
        }
    }
    .map("output", (AugmentPhase t, String value) -> t.setOutputDir(Paths.get(value)))
    .map("classes", (AugmentPhase t, String value) -> t.setAppClassesDir(Paths.get(value)))
    .map("wiring-classes", (AugmentPhase t, String value) -> t.setWiringClassesDir(Paths.get(value)))
    .map("lib", (AugmentPhase t, String value) -> t.setLibDir(Paths.get(value)))
    .map("final-name", AugmentPhase::setFinalName)
    .map("main-class", AugmentPhase::setMainClass)
    .map("uber-jar", (AugmentPhase t, String value) -> t.setUberJar(Boolean.parseBoolean(value)));

    private final PropertiesHandler<NativeImagePhase> nativeImageHandler = new PropertiesHandler<NativeImagePhase>() {
        @Override
        public NativeImagePhase newInstance() {
            return new NativeImagePhase();
        }

        @Override
        public boolean set(NativeImagePhase t, PropertyContext ctx) {
            //System.out.println("native-image.set " + ctx.getRelativeName() + "=" + ctx.getValue());
            final String value = ctx.getValue();
            switch(ctx.getRelativeName()) {
                case "output":
                    t.setOutputDir(Paths.get(value));
                    break;
                case "report-errors-at-runtime":
                    t.setReportErrorsAtRuntime(Boolean.parseBoolean(value));
                    break;
                case "debug-symbols":
                    t.setDebugSymbols(Boolean.parseBoolean(value));
                    break;
                case "debug-build-process":
                    t.setDebugBuildProcess(Boolean.parseBoolean(value));
                    break;
                case "cleanup-server":
                    t.setCleanupServer(Boolean.parseBoolean(value));
                    break;
                case "enable-http-url-handler":
                    t.setEnableHttpUrlHandler(Boolean.parseBoolean(value));
                    break;
                case "enable-https-url-handler":
                    t.setEnableHttpsUrlHandler(Boolean.parseBoolean(value));
                    break;
                case "enable-all-security-services":
                    t.setEnableAllSecurityServices(Boolean.parseBoolean(value));
                    break;
                case "enable-retained-heap-reporting":
                    t.setEnableRetainedHeapReporting(Boolean.parseBoolean(value));
                    break;
                case "enable-code-size-reporting":
                    t.setEnableCodeSizeReporting(Boolean.parseBoolean(value));
                    break;
                case "enable-isolates":
                    t.setEnableIsolates(Boolean.parseBoolean(value));
                    break;
                case "graalvm-home":
                    t.setGraalvmHome(value);
                    break;
                case "enable-server":
                    t.setEnableServer(Boolean.parseBoolean(value));
                    break;
                case "enable-jni":
                    t.setEnableJni(Boolean.parseBoolean(value));
                    break;
                case "auto-service-loader-registration":
                    t.setAutoServiceLoaderRegistration(Boolean.parseBoolean(value));
                    break;
                case "dump-proxies":
                    t.setDumpProxies(Boolean.parseBoolean(value));
                    break;
                case "native-image-xmx":
                    t.setNativeImageXmx(value);
                    break;
                case "docker-build":
                    t.setDockerBuild(Boolean.parseBoolean(value));
                    break;
                case "enable-vm-inspection":
                    t.setEnableVMInspection(Boolean.parseBoolean(value));
                    break;
                case "full-stack-traces":
                    t.setFullStackTraces(Boolean.parseBoolean(value));
                    break;
                case "disable-reports":
                    t.setDisableReports(Boolean.parseBoolean(value));
                    break;
                case "additional-build-args":
                    t.setAdditionalBuildArgs(Arrays.asList(value.split(",")));
                    break;
                default:
                    return false;
            }
            return true;
        }
    };

    @Override
    public AppCreator newInstance() {
        return new AppCreator();
    }

    @Override
    public boolean set(AppCreator appCreator, PropertyContext line) {
        switch(line.getRelativeName()) {
            case "augment":
                if(line.getValue().equals("true")) {
                    appCreator.addPhase(new AugmentPhase());
                }
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public PropertiesHandler<?> getNestedHandler(String name) {
        //System.out.println("getNestedHandler for " + name);
        switch (name) {
            case "augment":
                return augmentHandler;
            case "native-image":
                return nativeImageHandler;
        }
        return null;
    }

    @Override
    public void setNested(AppCreator creator, String name, Object child) {
        creator.addPhase((AppCreationPhase) child);
    }
}
