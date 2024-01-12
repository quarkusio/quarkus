package io.quarkus.deployment.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.eclipse.microprofile.config.Config;
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
    private final Lock codeGenLock = CodeGenLock.lockForCodeGen();

    CodeGenWatcher(CuratedApplication curatedApplication, DevModeContext context) throws CodeGenException {
        final QuarkusClassLoader deploymentClassLoader = curatedApplication.createDeploymentClassLoader();
        final List<CodeGenData> codeGens = CodeGenerator.init(curatedApplication.getApplicationModel(),
                context.getBuildSystemProperties(),
                deploymentClassLoader, context.getAllModules());
        if (codeGens.isEmpty()) {
            fsWatchUtil = null;
            this.deploymentClassLoader = null;
            deploymentClassLoader.close();
        } else {
            final Collection<FSWatchUtil.Watcher> watchers = new ArrayList<>(codeGens.size());
            final Properties properties = new Properties();
            properties.putAll(context.getBuildSystemProperties());
            final Config config = CodeGenerator.getConfig(curatedApplication.getApplicationModel(), LaunchMode.DEVELOPMENT,
                    properties, deploymentClassLoader);
            for (CodeGenData codeGen : codeGens) {
                for (String ext : codeGen.provider.inputExtensions()) {
                    watchers.add(new FSWatchUtil.Watcher(codeGen.sourceDir, ext,
                            modifiedPaths -> {
                                codeGenLock.lock();
                                try {
                                    CodeGenerator.trigger(deploymentClassLoader,
                                            codeGen,
                                            curatedApplication.getApplicationModel(), config, false);
                                } catch (Exception any) {
                                    log.warn("Code generation failed", any);
                                } finally {
                                    codeGenLock.unlock();
                                }
                            }));
                }
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
