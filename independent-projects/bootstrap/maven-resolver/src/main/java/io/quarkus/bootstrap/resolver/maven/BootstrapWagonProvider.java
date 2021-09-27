package io.quarkus.bootstrap.resolver.maven;

import org.apache.maven.wagon.Wagon;
import org.eclipse.aether.transport.wagon.WagonProvider;

public class BootstrapWagonProvider implements WagonProvider {

    @Override
    public Wagon lookup(String roleHint) throws Exception {
        String impl = null;
        switch (roleHint) {
            case "https":
            case "http":
                impl = "org.apache.maven.wagon.providers.http.HttpWagon";
                break;
            case "file":
                impl = "org.apache.maven.wagon.providers.file.FileWagon";
                break;
            case "ftp":
                impl = "org.apache.maven.wagon.providers.ftp.FtpWagon";
                break;
            case "ftps":
                impl = "org.apache.maven.wagon.providers.ftp.FtpsWagon";
                break;
            case "ftph":
                impl = "org.apache.maven.wagon.providers.ftp.FtpHttpWagon";
                break;
            case "scm":
                impl = "org.apache.maven.wagon.providers.scm.ScmWagon";
                break;
            default:
                throw new IllegalStateException("Not supported Wagon implementation hint " + roleHint);
        }
        final Class<?> cls = loadClass(impl, roleHint);
        final Object wagon;
        try {
            wagon = cls.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to instantiate Wagon impl " + impl, t);
        }
        return Wagon.class.cast(wagon);
    }

    @Override
    public void release(Wagon wagon) {
    }

    private static Class<?> loadClass(String name, String protocol) {
        try {
            return BootstrapWagonProvider.class.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            final StringBuilder buf = new StringBuilder();
            buf.append("Failed to locate Wagon implementation ")
                    .append(name)
                    .append(" on the classpath for protocol ")
                    .append(protocol)
                    .append(", please add the ");
            final String defaultWagonGA = getDefaultWagonGA(protocol);
            if (defaultWagonGA == null) {
                buf.append("corresponding classpath dependency to your project");
            } else {
                buf.append("desired version of ").append(defaultWagonGA).append(" as a classpath dependency to your project");
            }
            throw new IllegalStateException(buf.toString());
        }
    }

    private static String getDefaultWagonGA(String protocol) {
        switch (protocol) {
            case "https":
            case "http":
                return "org.apache.maven.wagon:wagon-http or org.apache.maven.wagon:wagon-http-lightweight";
            case "file":
                return "org.apache.maven.wagon:wagon-file";
            case "ftp":
            case "ftps":
            case "ftph":
                return "org.apache.maven.wagon:wagon-ftp";
            case "scm":
                return "org.apache.maven.wagon:wagon-scm";
        }
        return null;
    }
}
