package io.quarkus.narayana.jta.runtime.graal;

import com.arjuna.ats.arjuna.common.ObjectStoreEnvironmentBean;
import com.arjuna.ats.arjuna.exceptions.FatalError;
import com.arjuna.ats.arjuna.logging.tsLogger;
import com.arjuna.ats.arjuna.objectstore.ObjectStoreAPI;
import com.arjuna.ats.internal.arjuna.objectstore.ShadowNoFileLockStore;
import com.arjuna.common.internal.util.propertyservice.BeanPopulator;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Force the use of ShadowNoFileLockStore for now due to class loading issue.
 * <p>
 * Using another one won't work anyway in native image mode right now as there's a good chance the class won't be included in
 * the native image.
 */
@TargetClass(className = "com.arjuna.ats.arjuna.objectstore.StoreManager")
public final class StoreManagerSubstitution {

    @Substitute
    private static final ObjectStoreAPI initStore(String name) {
        ObjectStoreEnvironmentBean storeEnvBean = BeanPopulator.getNamedInstance(ObjectStoreEnvironmentBean.class, name);
        String storeType = storeEnvBean.getObjectStoreType();
        ObjectStoreAPI store;

        try {
            store = new ShadowNoFileLockStore(storeEnvBean);
        } catch (final Throwable ex) {
            throw new FatalError(tsLogger.i18NLogger.get_StoreManager_invalidtype() + " " + storeType, ex);
        }

        store.start();

        return store;
    }
}
