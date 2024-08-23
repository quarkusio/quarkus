package io.quarkus.mongodb.runtime.graal;

import static java.util.Collections.unmodifiableList;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientException;
import com.mongodb.MongoCompressor;
import com.mongodb.ServerAddress;
import com.mongodb.UnixServerAddress;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
import com.mongodb.internal.connection.BufferProvider;
import com.mongodb.internal.connection.InternalStreamConnection;
import com.mongodb.internal.connection.ServerAddressHelper;
import com.mongodb.internal.connection.SocketStream;
import com.mongodb.internal.connection.SocketStreamFactory;
import com.mongodb.internal.connection.Stream;
import com.mongodb.internal.connection.UnixSocketChannelStream;
import com.mongodb.lang.Nullable;
import com.mongodb.spi.dns.InetAddressResolver;
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
    private InetAddressResolver inetAddressResolver;
    @Alias
    private SocketSettings settings;
    @Alias
    private SslSettings sslSettings;
    @Alias
    private BufferProvider bufferProvider;

    @Substitute
    public Stream create(final ServerAddress serverAddress) {
        Stream stream;
        if (sslSettings.isEnabled()) {
            stream = new SocketStream(serverAddress, inetAddressResolver, settings, sslSettings,
                    getSslContext().getSocketFactory(), bufferProvider);
        } else {
            stream = new SocketStream(serverAddress, inetAddressResolver, settings, sslSettings,
                    SocketFactory.getDefault(), bufferProvider);
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
final class InternalStreamConnectionSubstitution {
    @Substitute
    private CompressorSubstitute createCompressor(final MongoCompressor mongoCompressor) {
        throw new UnsupportedOperationException("Unsupported compressor in native mode");
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

//TODO: move to a dedicated jna extension that will simply collect JNA substitutions
@TargetClass(com.sun.jna.Native.class)
final class JnaNativeSubstitutions {

    // This method can trick GraalVM into thinking that Classloader#findLibrary is getting called
    @Substitute
    public static String getWebStartLibraryPath(final String libName) {
        return null;
    }
}
