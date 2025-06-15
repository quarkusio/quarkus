package io.quarkus.jdbc.postgresql.runtime.graal;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.function.BiFunction;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.graalvm.nativeimage.ImageSingletons;
import org.postgresql.core.BaseConnection;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.xml.DefaultPGXmlFactoryFactory;
import org.postgresql.xml.PGXmlFactoryFactory;

/**
 * Used by PgSQLXML: easier to keep the actual code separated from the substitutions.
 */
final class DomHelper {

    // Stays null unless XML result processing becomes reachable.
    private BiFunction<DOMResult, BaseConnection, String> processDomResult;

    // This is only ever invoked if field domResult got initialized; which in turn
    // is only possible if setResult(Class) is reachable.
    public static String processDomResult(DOMResult domResult, BaseConnection conn) throws SQLException {
        BiFunction<DOMResult, BaseConnection, String> func = ImageSingletons.lookup(DomHelper.class).processDomResult;
        if (func == null) {
            return null;
        } else {
            try {
                return func.apply(domResult, conn);
            } catch (UncheckedSQLException e) {
                throw (SQLException) e.getCause();
            }
        }
    }

    // Called by SQLXMLFeature when setResult(Class) becomes reachable
    static void enableXmlProcessing() {
        ImageSingletons.lookup(DomHelper.class).processDomResult = DomHelper::doProcessDomResult;
    }

    public static String doProcessDomResult(DOMResult domResult, BaseConnection conn) {
        try {
            return reallyProcessDomResult(domResult, conn);
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }
    }

    // This XML processing code should only be reachable (via processDomResult function) iff it's
    // actually used. I.e. setResult(Class) is reachable.
    public static String reallyProcessDomResult(DOMResult domResult, BaseConnection conn) throws SQLException {
        TransformerFactory factory = getXmlFactoryFactory(conn).newTransformerFactory();
        try {
            Transformer transformer = factory.newTransformer();
            DOMSource domSource = new DOMSource(domResult.getNode());
            StringWriter stringWriter = new StringWriter();
            StreamResult streamResult = new StreamResult(stringWriter);
            transformer.transform(domSource, streamResult);
            return stringWriter.toString();
        } catch (TransformerException te) {
            throw new PSQLException(GT.tr("Unable to convert DOMResult SQLXML data to a string."), PSQLState.DATA_ERROR,
                    te);
        }
    }

    private static PGXmlFactoryFactory getXmlFactoryFactory(BaseConnection conn) throws SQLException {
        if (conn != null) {
            return conn.getXmlFactoryFactory();
        }
        return DefaultPGXmlFactoryFactory.INSTANCE;
    }
}

@SuppressWarnings("serial")
final class UncheckedSQLException extends RuntimeException {

    UncheckedSQLException(SQLException cause) {
        super(cause);
    }
}
