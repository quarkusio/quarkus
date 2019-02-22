package io.quarkus.jdbc.h2.runtime.graal;

import org.h2.engine.SysProperties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(org.h2.value.CompareMode.class)
public final class CompareMode {

    @Alias
    private static volatile CompareMode lastUsed;

    //    @Inject
    //    private static final org.h2.value.CompareMode SINGLE_CHOICE = org.h2.value.CompareMode.getInstance(null, 0, SysProperties.SORT_BINARY_UNSIGNED);

    @Substitute
    public static CompareMode getInstance(String name, int strength, boolean binaryUnsigned) {
        if (name != null || strength != 0 || binaryUnsigned != SysProperties.SORT_BINARY_UNSIGNED) {
            throw new UnsupportedOperationException(
                    "Only the default Collator can be currently used in SubstrateVM; see https://github.com/oracle/graal/issues/839");
        }
        CompareMode var3 = lastUsed;
        if (var3 != null) {
            return var3;
        } else {
            var3 = new CompareMode(name, strength, binaryUnsigned);
            lastUsed = var3;
            return var3;
        }
        //TODO?: Can't create a singleton via @Inject
        //return SINGLE_CHOICE;
    }

    @Alias
    protected CompareMode(String var1, int var2, boolean var3) {
        //Uses the original code
    }

}
