package io.quarkus.deployment.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.bootstrap.prebuild.CodeGenException;
import io.quarkus.deployment.CodeGenerator;
import io.quarkus.deployment.codegen.CodeGenData;
import io.quarkus.deployment.util.FSWatchUtil;
import io.quarkus.runtime.LaunchMode;

class CodeGenWatcher {

    private static final Logger log = Logger.getLogger(CodeGenWatcher.class);

    private final QuarkusClassLoader deploymentClassLoader;
    private final FSWatchUtil fsWatchUtil;

    CodeGenWatcher(CuratedApplication curatedApplication, DevModeContext context) throws CodeGenException {
        final QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
        final List<CodeGenData> codeGens = CodeGenerator.init(deploymentClassLoader, context.getAllModules());
        if (codeGens.isEmpty()) {
            fsWatchUtil = null;
            this.deploymentClassLoader = null;
            deploymentClassLoader.close();
        } else {
            final Collection<FSWatchUtil.Watcher> watchers = new ArrayList<>(codeGens.size());
            final Properties properties = new Properties();
            properties.putAll(context.getBuildSystemProperties());
            for (CodeGenData codeGen : codeGens) {
                watchers.add(new FSWatchUtil.Watcher(codeGen.sourceDir, codeGen.provider.inputExtension(),
                        modifiedPaths -> {
                            try {
                                CodeGenerator.trigger(deploymentClassLoader,
                                        codeGen,
                                        curatedApplication.getApplicationModel(), properties, LaunchMode.DEVELOPMENT, false);
                            } catch (Exception any) {
                                log.warn("Code generation failed", any);
                            }
                        }));
            }
            fsWatchUtil = new FSWatchUtil();
            fsWatchUtil.observe(watchers, 500);
            this.deploymentClassLoader = deploymentClassLoader;
        }
    }

    void shutdown() {
        if (fsWatchUtil != null) {
            fsWatchUtil.shutdown();
        }
        if (deploymentClassLoader != null) {
            deploymentClassLoader.close();
        }
    }
}
