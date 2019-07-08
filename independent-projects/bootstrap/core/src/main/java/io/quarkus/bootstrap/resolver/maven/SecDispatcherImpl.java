package io.quarkus.bootstrap.resolver.maven;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

@SuppressWarnings("rawtypes")
public class SecDispatcherImpl implements SecDispatcher {

    public static final String SYSTEM_PROPERTY_SEC_LOCATION = "settings.security";

    public static final String TYPE_ATTR = "type";

    public static final char ATTR_START = '[';

    public static final char ATTR_STOP  = ']';

    /**
     * DefaultHandler
     *
     * @plexus.requirement
     */
    protected PlexusCipher _cipher;

    /**
     * All available dispatchers
     *
     * @plexus.requirement role="org.sonatype.plexus.components.sec.dispatcher.PasswordDecryptor"
     */
    protected Map _decryptors;

    /**
     *
     * @plexus.configuration default-value="~/.m2/settings-security.xml"
     */
    protected String _configurationFile = "~/.m2/settings-security.xml";

    public SecDispatcherImpl() {
        try {
            this._cipher = new DefaultPlexusCipher();
        } catch (PlexusCipherException e) {
            throw new IllegalStateException("Failed to init Security Dispatcher", e);
        }
    }

    // ---------------------------------------------------------------
    public String decrypt( String str )
        throws SecDispatcherException
    {
        if( ! isEncryptedString( str ) )
            return str;

        String bare = null;

        try
        {
            bare = _cipher.unDecorate( str );
        }
        catch ( PlexusCipherException e1 )
        {
            throw new SecDispatcherException( e1 );
        }

        try
        {
            Map attr = stripAttributes( bare );

            String res = null;

            SettingsSecurity sec = getSec();

            if( attr == null || attr.get( "type" ) == null )
            {
                String master = getMaster( sec );

                res = _cipher.decrypt( bare, master );
            }
            else
            {
                String type = (String) attr.get( TYPE_ATTR );

                if( _decryptors == null )
                    throw new SecDispatcherException( "plexus container did not supply any required dispatchers - cannot lookup "+type );

                Map conf = SecUtil.getConfig( sec, type );

                PasswordDecryptor dispatcher = (PasswordDecryptor) _decryptors.get( type );

                if( dispatcher == null )
                    throw new SecDispatcherException( "no dispatcher for hint "+type );

                String pass = attr == null ? bare : strip( bare );

                return dispatcher.decrypt( pass, attr, conf );
            }
            return res;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new SecDispatcherException(e);
        }
    }

    private String strip( String str )
    {
        int pos = str.indexOf( ATTR_STOP );

        if( pos == str.length() )
            return null;

        if( pos != -1 )
            return str.substring( pos+1 );

        return str;
    }

    @SuppressWarnings("unchecked")
    private Map stripAttributes( String str )
    {
        int start = str.indexOf( ATTR_START );
        int stop = str.indexOf( ATTR_STOP );
        if ( start != -1 && stop != -1 && stop > start )
        {
            if( stop == start+1 )
                return null;

            String attrs = str.substring( start+1, stop ).trim();

            if( attrs == null || attrs.length() < 1 )
                return null;

            Map res = null;

            StringTokenizer st = new StringTokenizer( attrs, ", " );

            while( st.hasMoreTokens() )
            {
                if( res == null )
                    res = new HashMap( st.countTokens() );

                String pair = st.nextToken();

                int pos = pair.indexOf( '=' );

                if( pos == -1 )
                    continue;

                String key = pair.substring( 0, pos ).trim();

                if( pos == pair.length() )
                {
                    res.put( key, null );
                    continue;
                }

                String val = pair.substring( pos+1 );

                res.put(  key, val.trim() );
            }

            return res;
        }

        return null;
    }
    //----------------------------------------------------------------------------
    private boolean isEncryptedString( String str )
    {
        if( str == null )
            return false;

        return _cipher.isEncryptedString( str );
    }
    //----------------------------------------------------------------------------
    private SettingsSecurity getSec()
    throws SecDispatcherException
    {
        String location = System.getProperty( SYSTEM_PROPERTY_SEC_LOCATION
                                              , getConfigurationFile()
                                            );
        String realLocation = location.charAt( 0 ) == '~'
            ? System.getProperty( "user.home" ) + location.substring( 1 )
            : location
            ;

        SettingsSecurity sec = SecUtil.read( realLocation, true );

        if( sec == null )
            throw new SecDispatcherException( "cannot retrieve master password. Please check that "+realLocation+" exists and has data" );

        return sec;
    }
    //----------------------------------------------------------------------------
    private String getMaster( SettingsSecurity sec )
    throws SecDispatcherException
    {
        String master = sec.getMaster();

        if( master == null )
            throw new SecDispatcherException( "master password is not set" );

        try
        {
            return _cipher.decryptDecorated( master, SYSTEM_PROPERTY_SEC_LOCATION );
        }
        catch ( PlexusCipherException e )
        {
            throw new SecDispatcherException(e);
        }
    }
    //---------------------------------------------------------------
    public String getConfigurationFile()
    {
        return _configurationFile;
    }

    public void setConfigurationFile( String file )
    {
        _configurationFile = file;
    }
}
