package io.quarkus.mongo.runtime.graal;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import com.mongodb.*;
import com.mongodb.connection.*;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.mongodb.internal.connection.ServerAddressHelper;
import com.mongodb.internal.connection.SocketStream;
import com.mongodb.internal.connection.UnixSocketChannelStream;
import com.mongodb.internal.dns.DefaultDnsResolver;
import com.mongodb.lang.Nullable;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

public final class MongoClientSubstitutions {
}

@TargetClass(MongoCompressor.class)
final class MongoCompressorSubstitution {

    @Substitute
    public static MongoCompressor createSnappyCompressor() {
        throw new UnsupportedOperationException("Unsupported operation - snappy");
    }

}

@TargetClass(ConnectionString.class)
final class ConnectionStringSubstitution {

    @Substitute
    private List<MongoCompressor> buildCompressors(final String compressors, @Nullable final Integer zlibCompressionLevel) {
        List<MongoCompressor> compressorsList = new ArrayList<>();

        for (String cur : compressors.split(",")) {
            if (cur.equals("zlib")) {
                MongoCompressor zlibCompressor = MongoCompressor.createZlibCompressor();
                zlibCompressor = zlibCompressor.withProperty(MongoCompressor.LEVEL, zlibCompressionLevel);
                compressorsList.add(zlibCompressor);
            } else if (cur.equals("snappy")) {
                // DO NOTHING
            } else if (!cur.isEmpty()) {
                throw new IllegalArgumentException("Unsupported compressor '" + cur + "'");
            }
        }

        return unmodifiableList(compressorsList);
    }

}

@TargetClass(UnixServerAddress.class)
final class UnixServerAddressSubstitution {

}

@TargetClass(SocketStreamFactory.class)
final class SocketStreamFactorySubstitution {

    @Alias
    private SocketSettings settings;
    @Alias
    private SslSettings sslSettings;
    @Alias
    private SocketFactory socketFactory;
    @Alias
    private BufferProvider bufferProvider;

    @Substitute
    public Stream create(final ServerAddress serverAddress) {
        Stream stream;
        if (socketFactory != null) {
            stream = new SocketStream(serverAddress, settings, sslSettings, socketFactory, bufferProvider);
        } else if (sslSettings.isEnabled()) {
            stream = new SocketStream(serverAddress, settings, sslSettings, getSslContext().getSocketFactory(),
                    bufferProvider);
        } else {
            stream = new SocketStream(serverAddress, settings, sslSettings, SocketFactory.getDefault(), bufferProvider);
        }
        return stream;
    }

    @Alias
    private SSLContext getSslContext() {
        try {
            return (sslSettings.getContext() == null) ? SSLContext.getDefault() : sslSettings.getContext();
        } catch (NoSuchAlgorithmException e) {
            throw new MongoClientException("Unable to create default SSLContext", e);
        }
    }
}

@TargetClass(className = "com.mongodb.internal.connection.Compressor")
final class CompressorSubstitute {

}

@TargetClass(InternalStreamConnection.class)
final class InternalStreamConnectionSubtitution {
    @Substitute
    private CompressorSubstitute createCompressor(final MongoCompressor mongoCompressor) {
        throw new UnsupportedOperationException("Unsupported compressor in native mode");
    }
}

@TargetClass(MongoClientOptions.class)
final class MongoClientOptionsSubstitution {

    @Alias
    private SocketFactory socketFactory;

    @Alias
    private static SocketFactory DEFAULT_SOCKET_FACTORY;

    @Substitute
    public SocketFactory getSocketFactory() {
        if (this.socketFactory != null) {
            return this.socketFactory;
        } else {
            return DEFAULT_SOCKET_FACTORY;
        }
    }
}

@TargetClass(UnixSocketChannelStream.class)
@Delete
final class UnixSocketChannelStreamSubstitution {

}

@TargetClass(ServerAddressHelper.class)
final class ServerAddressHelperSubstitution {

    @Substitute
    public static ServerAddress createServerAddress(final String host, final int port) {
        if (host != null && host.endsWith(".sock")) {
            throw new UnsupportedOperationException("Unix socket not supported");
        } else {
            return new ServerAddress(host, port);
        }
    }

}

@TargetClass(DefaultDnsResolver.class)
final class DefaultDnsResolverSubstitution {
    /*
     * The format of SRV record is
     * priority weight port target.
     * e.g.
     * 0 5 5060 example.com.
     * The priority and weight are ignored, and we just concatenate the host (after removing the ending '.') and port with a
     * ':' in between, as expected by ServerAddress.
     * It's required that the srvHost has at least three parts (e.g. foo.bar.baz) and that all of the resolved hosts have a
     * parent
     * domain equal to the domain of the srvHost.
     */
    @Substitute
    public List<String> resolveHostFromSrvRecords(final String srvHost) {
        throw new UnsupportedOperationException("mongo+srv:// not supported in native mode");
    }

    @Substitute
    private static boolean sameParentDomain(final List<String> srvHostDomainParts, final String resolvedHostDomain) {
        List<String> resolvedHostDomainParts = asList(resolvedHostDomain.split("\\."));
        if (srvHostDomainParts.size() > resolvedHostDomainParts.size()) {
            return false;
        }
        return resolvedHostDomainParts
                .subList(resolvedHostDomainParts.size() - srvHostDomainParts.size(), resolvedHostDomainParts.size())
                .equals(srvHostDomainParts);
    }

    /*
     * A TXT record is just a string
     * We require each to be one or more query parameters for a MongoDB connection string.
     * Here we concatenate TXT records together with a '&' separator as required by connection strings
     */
    @Substitute
    public String resolveAdditionalQueryParametersFromTxtRecords(final String host) {
        throw new UnsupportedOperationException("mongo+srv:// not supported in native mode");
    }
}
