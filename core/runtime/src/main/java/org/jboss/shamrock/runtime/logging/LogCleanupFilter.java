package org.jboss.shamrock.runtime.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogCleanupFilter implements Filter {

    private static class Downgrade {
        private String owner;
        private String start;
        public Downgrade(String owner, String start) {
            this.owner = owner;
            this.start = start;
        }
    }

    private final static Map<String, Downgrade> downgrades = new HashMap<>();
    static {
        downgrade("org.jboss.threads", "JBoss Threads version");
        downgrade("org.hibernate.Version", "HHH000412");
        downgrade("org.hibernate.cfg.Environment", "HHH000206");
        downgrade("org.hibernate.bytecode.enhance.spi.Enhancer", "Enhancing [%s] as");
        downgrade("org.hibernate.bytecode.enhance.internal.bytebuddy.BiDirectionalAssociationHandler", "Could not find");
        downgrade("com.arjuna.ats.arjuna", "ARJUNA012170");
        downgrade("org.hibernate.jpa.internal.util.LogHelper", "HHH000204");
        downgrade("org.hibernate.annotations.common.Version", "HCANN000001");
        downgrade("org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl", "HHH000422");
        downgrade("org.hibernate.dialect.Dialect", "HHH000400");
        downgrade("org.hibernate.type.BasicTypeRegistry", "HHH000270");
        downgrade("org.jboss.resteasy.resteasy_jaxrs.i18n", "RESTEASY002225");
        downgrade("org.hibernate.orm.beans", "HHH10005002");
        downgrade("org.hibernate.tuple.PojoInstantiator", "HHH000182");
        downgrade("org.hibernate.tuple.entity.EntityMetamodel", "HHH000157");
        downgrade("org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator", "HHH000490");
        downgrade("org.hibernate.tool.schema.internal.SchemaCreatorImpl", "HHH000476");
        downgrade("org.xnio", "XNIO version");
        downgrade("org.xnio.nio", "XNIO NIO Implementation Version");
        downgrade("org.hibernate.hql.internal.QueryTranslatorFactoryInitiator", "HHH000397");
    }

    private static void downgrade(String owner, String start) {
        downgrades.put(owner, new Downgrade(owner, start));
    }
    
    @Override
    public boolean isLoggable(LogRecord record) {
        if(record.getLevel() != Level.INFO
                && record.getLevel() != org.jboss.logmanager.Level.INFO)
            return true;
        Downgrade downgrade = downgrades.get(record.getLoggerName());
        boolean untouched = true;
        if(downgrade != null) {
            if(record.getMessage().startsWith(downgrade.start)) {
                record.setLevel(org.jboss.logmanager.Level.DEBUG);
                untouched = false;
            }
        }
        if(untouched) {
//            System.err.println("isLoggable: "+record.getLoggerName());
//            System.err.println("isLoggable: "+record.getMessage());
            return true;
        }
        // temporary because ajusting the level is not enough
        return false;
    }


}
