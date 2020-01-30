package io.quarkus.undertow.websockets.runtime.devmode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.jboss.logging.Logger;

import io.quarkus.dev.spi.HotReplacementContext;
import io.quarkus.dev.spi.HotReplacementSetup;
import io.quarkus.undertow.runtime.UndertowDeploymentRecorder;
import io.undertow.Handlers;
import io.undertow.predicate.Predicates;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;

public class WebsocketHotReloadSetup implements HotReplacementSetup {

    private static Logger logger = Logger.getLogger(WebsocketHotReloadSetup.class);

    static volatile String replacementPassword;

    @Override
    public void setupHotDeployment(HotReplacementContext hotReplacementContext) {
        // we can't use the typical ConfigProvider.getConfig() because the config infrastructure isn't setup at this point
        String password = System.getProperty(HotReplacementWebsocketEndpoint.QUARKUS_LIVE_RELOAD_PASSWORD);
        if (password == null || password.isEmpty()) {
            password = System.getenv(HotReplacementWebsocketEndpoint.QUARKUS_LIVE_RELOAD_PASSWORD_ENV);
        }

        if (password != null && !password.isEmpty()) {
            replacementPassword = password;
        } else {
            List<Path> resources = hotReplacementContext.getResourcesDir();
            if (!resources.isEmpty()) {
                //TODO: fix this
                File appConfig = resources.get(0).resolve("application.properties").toFile();
                if (appConfig.isFile()) {
                    try (InputStream is = new FileInputStream(appConfig)) {
                        try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                            try (BufferedReader br = new BufferedReader(isr)) {
                                Properties p = new Properties();
                                p.load(br);
                                replacementPassword = p
                                        .getProperty(HotReplacementWebsocketEndpoint.QUARKUS_LIVE_RELOAD_PASSWORD);
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Failed to read application.properties", e);
                    }
                }
            }
        }
        if (replacementPassword != null) {
            logger.info("Using websocket based hot deployment");
        } else {
            return;
        }
        try {
            hotReplacementContext.addPreScanStep(new Runnable() {
                @Override
                public void run() {
                    HotReplacementWebsocketEndpoint.checkForChanges(hotReplacementContext);
                }
            });
            //we need a special websocket setup
            //this will likely change
            //but we create a servlet deployment that lasts for the life of the server
            WebSocketDeploymentInfo info = new WebSocketDeploymentInfo();
            info.addEndpoint(HotReplacementWebsocketEndpoint.class);

            DeploymentInfo d = new DeploymentInfo();
            d.setDeploymentName("hot-replacement-websockets");
            d.setContextPath("/");
            d.setClassLoader(getClass().getClassLoader());
            d.addServletContextAttribute(WebSocketDeploymentInfo.ATTRIBUTE_NAME, info);
            ServletContainer servletContainer = Servlets.defaultContainer();
            DeploymentManager manager = servletContainer.addDeployment(d);
            manager.deploy();
            HttpHandler ws = manager.start();
            UndertowDeploymentRecorder.addHotDeploymentWrapper(new HandlerWrapper() {
                @Override
                public HttpHandler wrap(HttpHandler handler) {
                    return Handlers.predicate(Predicates.path(HotReplacementWebsocketEndpoint.QUARKUS_HOT_RELOAD), ws, handler);
                }
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
