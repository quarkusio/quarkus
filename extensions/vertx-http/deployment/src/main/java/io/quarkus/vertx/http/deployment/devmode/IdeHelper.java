package io.quarkus.vertx.http.deployment.devmode;

import static io.smallrye.common.os.OS.LINUX;
import static io.smallrye.common.os.OS.MAC;
import static io.smallrye.common.os.OS.OTHER;
import static io.smallrye.common.os.OS.WINDOWS;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.smallrye.common.os.OS;

public class IdeHelper {
    private static final Logger log = Logger.getLogger(IdeHelper.class);

    public static void openBrowser(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, String path, String host,
            String port) {
        IdeHelper.openBrowser(rp, np, "http", path, host, port);
    }

    public static void openBrowser(HttpRootPathBuildItem rp, NonApplicationRootPathBuildItem np, String protocol, String path,
            String host,
            String port) {
        if (path.startsWith("/q")) {
            path = np.resolvePath(path.substring(3));
        } else {
            path = rp.resolvePath(path.substring(1));
        }

        StringBuilder sb = new StringBuilder(protocol);
        sb.append("://");
        sb.append(host);
        sb.append(":");
        sb.append(port);
        sb.append(path);
        String url = sb.toString();

        Runtime rt = Runtime.getRuntime();
        OS os = OS.current();
        String[] command = null;
        try {
            switch (os) {
                case MAC -> command = new String[] { "open", url };
                case LINUX -> command = new String[] { "xdg-open", url };
                case WINDOWS -> command = new String[] { "rundll32", "url.dll,FileProtocolHandler", url };
                case OTHER -> log.error("Cannot launch browser on this operating system");
            }
            if (command != null) {
                rt.exec(command);
            }
        } catch (Exception e) {
            log.debug("Failed to launch browser", e);
            if (command != null) {
                log.warn("Unable to open browser using command: '" + String.join(" ", command) + "'. Failure is: '"
                        + e.getMessage() + "'");
            }
        }

    }
}
