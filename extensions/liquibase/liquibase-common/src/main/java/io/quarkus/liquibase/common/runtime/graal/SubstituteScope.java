package io.quarkus.liquibase.common.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

import liquibase.Scope;
import liquibase.ScopeManager;
import liquibase.SingletonScopeManager;
import liquibase.util.SmartMap;

@TargetClass(value = Scope.class)
public final class SubstituteScope {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.NewInstance, declClass = SmartMap.class)
    private SmartMap values = new SmartMap();

    /**
     * All the following code is here to reset <a href=
     * "https://github.com/liquibase/liquibase/blob/51de1de4437e5b5fbcbd25cff006d1c6d5313bab/liquibase-standard/src/main/java/liquibase/Scope.java#L95-L102">this</a>
     */

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.NewInstance, declClass = CustomInheritableThreadLocal.class)
    private static InheritableThreadLocal<ScopeManager> scopeManager;

    public static final class CustomInheritableThreadLocal extends InheritableThreadLocal<ScopeManager> {
        @Override
        protected ScopeManager childValue(ScopeManager parentValue) {
            CustomScopeManager sm = new CustomScopeManager();
            sm.setCurrentScope(parentValue.getCurrentScope());
            return sm;
        }
    }

    public static class CustomScopeManager extends SingletonScopeManager {
        @Override
        public synchronized void setCurrentScope(Scope scope) {
            super.setCurrentScope(scope);
        }
    }

}
