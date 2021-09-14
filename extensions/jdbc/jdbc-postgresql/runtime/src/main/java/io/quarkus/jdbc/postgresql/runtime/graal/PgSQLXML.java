package io.quarkus.jdbc.postgresql.runtime.graal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLXML;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.postgresql.core.BaseConnection;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.xml.DefaultPGXmlFactoryFactory;
import org.postgresql.xml.PGXmlFactoryFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The PgSQLXML implementation in the PostgreSQL JDBC
 * driver has a dependency on the JDK's XML parsers;
 * while the use of this type is a strong hint that the
 * application's nature is relying on XML processing via
 * the JDK standard libraries, we've observed that this type
 * is also often included in the analysis graph being
 * triggered via {@see java.sql.PreparedStatement#setObject},
 * which is widely used for other purposes as well.
 *
 * Considering that the efficiency costs of including all of
 * resources and reflective registrations necessary to support the
 * XML parsers is non trivial, we apply here a trick to help the
 * GraalVM native image compiler avoid this inclusion unless this
 * feature is actually is reachable, by isolating the construction of
 * the parsers and their supporting other types via a reflective call.
 *
 * This reflective invocation exists merely to give us a control knob
 * to toggle the inclusion of this code; it so happens that we can
 * trigger the toggle automatically so to not manifest as semantic changes
 * for the Quarkus users who don't need to know about this.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
@TargetClass(className = "org.postgresql.jdbc.PgSQLXML")
@Substitute
public final class PgSQLXML implements SQLXML {

    private final BaseConnection conn;
    private String data;
    private boolean initialized;
    private boolean active;
    private boolean freed;

    private ByteArrayOutputStream byteArrayOutputStream;
    private StringWriter stringWriter;
    private DOMResult domResult;

    @Substitute
    public PgSQLXML(BaseConnection conn) {
        this(conn, null, false);
    }

    @Substitute
    public PgSQLXML(BaseConnection conn, String data) {
        this(conn, data, true);
    }

    @Substitute
    private PgSQLXML(BaseConnection conn, String data, boolean initialized) {
        this.conn = conn;
        this.data = data;
        this.initialized = initialized;
        this.active = false;
        this.freed = false;
    }

    @Substitute
    private PGXmlFactoryFactory getXmlFactoryFactory() throws SQLException {
        if (conn != null) {
            return conn.getXmlFactoryFactory();
        }
        return DefaultPGXmlFactoryFactory.INSTANCE;
    }

    @Substitute
    @Override
    public synchronized void free() {
        freed = true;
        data = null;
    }

    @Substitute
    @Override
    public synchronized InputStream getBinaryStream() throws SQLException {
        checkFreed();
        ensureInitialized();

        if (data == null) {
            return null;
        }

        try {
            return new ByteArrayInputStream(conn.getEncoding().encode(data));
        } catch (IOException ioe) {
            // This should be a can't happen exception. We just
            // decoded this data, so it would be surprising that
            // we couldn't encode it.
            // For this reason don't make it translatable.
            throw new PSQLException("Failed to re-encode xml data.", PSQLState.DATA_ERROR, ioe);
        }
    }

    @Substitute
    @Override
    public synchronized Reader getCharacterStream() throws SQLException {
        checkFreed();
        ensureInitialized();

        if (data == null) {
            return null;
        }

        return new StringReader(data);
    }

    // We must implement this unsafely because that's what the
    // interface requires. Because it says we're returning T
    // which is unknown, none of the return values can satisfy it
    // as Java isn't going to understand the if statements that
    // ensure they are the same.
    //
    @Substitute
    @Override
    public synchronized <T extends Source> T getSource(Class<T> sourceClass)
            throws SQLException {
        checkFreed();
        ensureInitialized();

        String data = this.data;
        if (data == null) {
            return null;
        }

        try {
            if (sourceClass == null || DOMSource.class.equals(sourceClass)) {
                DocumentBuilder builder = getXmlFactoryFactory().newDocumentBuilder();
                InputSource input = new InputSource(new StringReader(data));
                DOMSource domSource = new DOMSource(builder.parse(input));
                //noinspection unchecked
                return (T) domSource;
            } else if (SAXSource.class.equals(sourceClass)) {
                XMLReader reader = getXmlFactoryFactory().createXMLReader();
                InputSource is = new InputSource(new StringReader(data));
                return sourceClass.cast(new SAXSource(reader, is));
            } else if (StreamSource.class.equals(sourceClass)) {
                return sourceClass.cast(new StreamSource(new StringReader(data)));
            } else if (StAXSource.class.equals(sourceClass)) {
                XMLInputFactory xif = getXmlFactoryFactory().newXMLInputFactory();
                XMLStreamReader xsr = xif.createXMLStreamReader(new StringReader(data));
                return sourceClass.cast(new StAXSource(xsr));
            }
        } catch (Exception e) {
            throw new PSQLException(GT.tr("Unable to decode xml data."), PSQLState.DATA_ERROR, e);
        }

        throw new PSQLException(GT.tr("Unknown XML Source class: {0}", sourceClass),
                PSQLState.INVALID_PARAMETER_TYPE);
    }

    @Substitute
    @Override
    public synchronized String getString() throws SQLException {
        checkFreed();
        ensureInitialized();
        return data;
    }

    @Substitute
    @Override
    public synchronized OutputStream setBinaryStream() throws SQLException {
        checkFreed();
        initialize();
        active = true;
        byteArrayOutputStream = new ByteArrayOutputStream();
        return byteArrayOutputStream;
    }

    @Substitute
    @Override
    public synchronized Writer setCharacterStream() throws SQLException {
        checkFreed();
        initialize();
        active = true;
        stringWriter = new StringWriter();
        return stringWriter;
    }

    @Substitute
    @Override
    public synchronized <T extends Result> T setResult(Class<T> resultClass) throws SQLException {
        checkFreed();
        initialize();

        if (resultClass == null || DOMResult.class.equals(resultClass)) {
            domResult = new DOMResult();
            active = true;
            //noinspection unchecked
            return (T) domResult;
        } else if (SAXResult.class.equals(resultClass)) {
            try {
                SAXTransformerFactory transformerFactory = getXmlFactoryFactory().newSAXTransformerFactory();
                TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
                stringWriter = new StringWriter();
                transformerHandler.setResult(new StreamResult(stringWriter));
                active = true;
                return resultClass.cast(new SAXResult(transformerHandler));
            } catch (TransformerException te) {
                throw new PSQLException(GT.tr("Unable to create SAXResult for SQLXML."),
                        PSQLState.UNEXPECTED_ERROR, te);
            }
        } else if (StreamResult.class.equals(resultClass)) {
            stringWriter = new StringWriter();
            active = true;
            return resultClass.cast(new StreamResult(stringWriter));
        } else if (StAXResult.class.equals(resultClass)) {
            StringWriter stringWriter = new StringWriter();
            this.stringWriter = stringWriter;
            try {
                XMLOutputFactory xof = getXmlFactoryFactory().newXMLOutputFactory();
                XMLStreamWriter xsw = xof.createXMLStreamWriter(stringWriter);
                active = true;
                return resultClass.cast(new StAXResult(xsw));
            } catch (XMLStreamException xse) {
                throw new PSQLException(GT.tr("Unable to create StAXResult for SQLXML"),
                        PSQLState.UNEXPECTED_ERROR, xse);
            }
        }

        throw new PSQLException(GT.tr("Unknown XML Result class: {0}", resultClass),
                PSQLState.INVALID_PARAMETER_TYPE);
    }

    @Substitute
    @Override
    public synchronized void setString(String value) throws SQLException {
        checkFreed();
        initialize();
        data = value;
    }

    @Substitute
    private void checkFreed() throws SQLException {
        if (freed) {
            throw new PSQLException(GT.tr("This SQLXML object has already been freed."),
                    PSQLState.OBJECT_NOT_IN_STATE);
        }
    }

    @Substitute
    private void ensureInitialized() throws SQLException {
        if (!initialized) {
            throw new PSQLException(
                    GT.tr(
                            "This SQLXML object has not been initialized, so you cannot retrieve data from it."),
                    PSQLState.OBJECT_NOT_IN_STATE);
        }

        // Is anyone loading data into us at the moment?
        if (!active) {
            return;
        }

        if (byteArrayOutputStream != null) {
            try {
                data = conn.getEncoding().decode(byteArrayOutputStream.toByteArray());
            } catch (IOException ioe) {
                throw new PSQLException(GT.tr("Failed to convert binary xml data to encoding: {0}.",
                        conn.getEncoding().name()), PSQLState.DATA_ERROR, ioe);
            } finally {
                byteArrayOutputStream = null;
                active = false;
            }
        } else if (stringWriter != null) {
            // This is also handling the work for Stream, SAX, and StAX Results
            // as they will use the same underlying stringwriter variable.
            //
            data = stringWriter.toString();
            stringWriter = null;
            active = false;
        } else if (domResult != null) {
            DOMResult domResult = this.domResult;
            try {
                data = DomHelper.processDomResult(domResult, conn);
            } finally {
                domResult = null;
                active = false;
            }
        }
    }

    @Substitute
    private void initialize() throws SQLException {
        if (initialized) {
            throw new PSQLException(
                    GT.tr(
                            "This SQLXML object has already been initialized, so you cannot manipulate it further."),
                    PSQLState.OBJECT_NOT_IN_STATE);
        }
        initialized = true;
    }

}
