package io.quarkus.jdbc.mysql.runtime.graal;

import com.mysql.cj.MysqlConnection;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import com.mysql.cj.protocol.NetworkResources;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * This is to disable connection clean up thread {@link AbandonedConnectionCleanupThread} which launches a thread on a static
 * block.
 * GraalVM is not happy about that. The issue might have been fixed with https://github.com/oracle/graal/pull/1542 but we have
 * to wait for a proper GraalVM release, so we substitute the whole class for now.
 *
 * There is a PR @see <a href="https://github.com/mysql/mysql-connector-j/pull/41">on the mysql repo</a> to enable
 * disabling of this class completely by using a system property. Let's see if we can get rid of this once the PR is merged.
 */
@Substitute
@TargetClass(AbandonedConnectionCleanupThread.class)
final public class AbandonedConnectionCleanupThread_disable {

    @Substitute
    protected static void trackConnection(MysqlConnection conn, NetworkResources io) {
        // do nothing
    }
}
