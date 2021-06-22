package io.quarkus.jdbc.postgresql.runtime.graal;

import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.postgresql.core.BaseConnection;
import org.postgresql.util.GT;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.postgresql.xml.DefaultPGXmlFactoryFactory;
import org.postgresql.xml.PGXmlFactoryFactory;

/**
 * Used by PgSQLXML: easier to keep the actual code separated from the substitutions.
 */
public final class DomHelper {

    //This is the actual code reaching to the JDK XML types; no other code except
    //the reflective call in the previous method should trigger this inclusion.
    public static String processDomResult(DOMResult domResult, BaseConnection conn) throws SQLException {
        return maybeProcessAsDomResult(domResult, conn);
    }

    // This is only ever invoked if field domResult got initialized; which in turn
    // is only possible if setResult(Class) is reacheable.
    public static String maybeProcessAsDomResult(DOMResult domResult, BaseConnection conn) throws SQLException {
        try {
            return (String) DomHelper.class.getMethod(obfuscatedMethodName(), DOMResult.class, BaseConnection.class)
                    .invoke(null, domResult, conn);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Unexpected failure in reflective call - please report", e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof SQLException) {
                //Propagate normal SQLException(s):
                throw (SQLException) cause;
            } else {
                throw new RuntimeException("Unexpected failure in reflective call - please report", e);
            }
        }
    }

    //When GraalVM can figure out a constant name for the target method to be invoked reflectively,
    //it automatically registers it for reflection. We don't want that to happen in this particular case.
    private static String obfuscatedMethodName() {
        return "reallyProcessDom" + "Result";
    }

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
            throw new PSQLException(GT.tr("Unable to convert DOMResult SQLXML data to a string."),
                    PSQLState.DATA_ERROR, te);
        }
    }

    private static PGXmlFactoryFactory getXmlFactoryFactory(BaseConnection conn) throws SQLException {
        if (conn != null) {
            return conn.getXmlFactoryFactory();
        }
        return DefaultPGXmlFactoryFactory.INSTANCE;
    }
}
