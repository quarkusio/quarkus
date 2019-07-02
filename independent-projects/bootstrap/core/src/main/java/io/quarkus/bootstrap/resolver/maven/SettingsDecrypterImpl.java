package io.quarkus.bootstrap.resolver.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.building.DefaultSettingsProblem;
import org.apache.maven.settings.building.SettingsProblem;
import org.apache.maven.settings.building.SettingsProblem.Severity;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;

public class SettingsDecrypterImpl implements SettingsDecrypter {

    private SecDispatcher securityDispatcher;

    public SettingsDecrypterImpl() {
        securityDispatcher = new SecDispatcherImpl();
    }

    @Override
    public SettingsDecryptionResult decrypt( SettingsDecryptionRequest request )
    {
        List<SettingsProblem> problems = new ArrayList<>();

        List<Server> servers = new ArrayList<>();

        for ( Server server : request.getServers() )
        {
            server = server.clone();

            servers.add( server );

            try
            {
                server.setPassword( decrypt( server.getPassword() ) );
            }
            catch ( SecDispatcherException e )
            {
                problems.add( new DefaultSettingsProblem( "Failed to decrypt password for server " + server.getId()
                    + ": " + e.getMessage(), Severity.ERROR, "server: " + server.getId(), -1, -1, e ) );
            }

            try
            {
                server.setPassphrase( decrypt( server.getPassphrase() ) );
            }
            catch ( SecDispatcherException e )
            {
                problems.add( new DefaultSettingsProblem( "Failed to decrypt passphrase for server " + server.getId()
                    + ": " + e.getMessage(), Severity.ERROR, "server: " + server.getId(), -1, -1, e ) );
            }
        }

        List<Proxy> proxies = new ArrayList<>();

        for ( Proxy proxy : request.getProxies() )
        {
            proxy = proxy.clone();

            proxies.add( proxy );

            try
            {
                proxy.setPassword( decrypt( proxy.getPassword() ) );
            }
            catch ( SecDispatcherException e )
            {
                problems.add( new DefaultSettingsProblem( "Failed to decrypt password for proxy " + proxy.getId()
                    + ": " + e.getMessage(), Severity.ERROR, "proxy: " + proxy.getId(), -1, -1, e ) );
            }
        }

        return new SettingsDecryptionResultImpl( servers, proxies, problems );
    }

    private String decrypt( String str )
        throws SecDispatcherException
    {
        return ( str == null ) ? null : securityDispatcher.decrypt( str );
    }
}
