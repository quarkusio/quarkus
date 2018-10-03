/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.shamrock.runtime.wfcommon;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.StackValue;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CCharPointerPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.UnsignedWord;
import org.graalvm.word.WordFactory;

@TargetClass(className = "com.oracle.svm.core.JavaMainWrapper")
final class Target_com_oracle_svm_core_JavaMainWrapper {
    @Alias
    static int argc;
    @Alias
    static CCharPointerPointer argv;
}

@TargetClass(className = "org.wildfly.common.os.GetProcessInfoAction")
final class Target_org_wildfly_common_os_GetProcessInfoAction {

    @Substitute
    public Object[] run() {
        return new Object[] { Long.valueOf(NativeInfo.getpid() & 0xffff_ffffL), ProcessUtils.getProcessName() };
    }
}

@TargetClass(className = "org.wildfly.common.net.HostName")
final class Target_org_wildfly_common_net_HostName {

    @Alias
    static native InetAddress getLocalHost() throws UnknownHostException;
}

@TargetClass(className = "org.wildfly.common.net.GetHostInfoAction")
final class Target_org_wildfly_common_net_GetHostInfoAction {

    @Substitute
    public String[] run() {
        // still allow host name to be overridden
        String qualifiedHostName = System.getProperty("jboss.qualified.host.name");
        String providedHostName = System.getProperty("jboss.host.name");
        String providedNodeName = System.getProperty("jboss.node.name");
        if (qualifiedHostName == null) {
            // if host name is specified, don't pick a qualified host name that isn't related to it
            qualifiedHostName = providedHostName;
            if (qualifiedHostName == null) {
                // query the operating system
                CCharPointer nameBuf = StackValue.get(ProcessSubstitutions.SIZE); // should be more than enough
                int res = NativeInfo.gethostname(nameBuf, WordFactory.unsigned(ProcessSubstitutions.SIZE));
                if (res != -1 && res > 0) {
                    if (res == ProcessSubstitutions.SIZE) {
                        // null-terminate a really long name
                        nameBuf.write(ProcessSubstitutions.SIZE - 1, (byte) 0);
                    }
                    qualifiedHostName = CTypeConversion.toJavaString(nameBuf);
                }
            }
            if (qualifiedHostName == null) {
                // POSIX-like OSes including Mac should have this set
                qualifiedHostName = System.getenv("HOSTNAME");
            }
            if (qualifiedHostName == null) {
                // Certain versions of Windows
                qualifiedHostName = System.getenv("COMPUTERNAME");
            }
            if (qualifiedHostName == null) {
                try {
                    qualifiedHostName = Target_org_wildfly_common_net_HostName.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    qualifiedHostName = null;
                }
            }
            if (qualifiedHostName != null && Pattern.compile("^\\d+\\.\\d+\\.\\d+\\.\\d+$|:").matcher(qualifiedHostName).find()) {
                // IP address is not acceptable
                qualifiedHostName = null;
            }
            if (qualifiedHostName == null) {
                // Give up
                qualifiedHostName = "unknown-host.unknown-domain";
            } else {
                qualifiedHostName = qualifiedHostName.trim().toLowerCase();
            }
        }
        if (providedHostName == null) {
            // Use the host part of the qualified host name
            final int idx = qualifiedHostName.indexOf('.');
            providedHostName = idx == - 1 ? qualifiedHostName : qualifiedHostName.substring(0, idx);
        }
        if (providedNodeName == null) {
            providedNodeName = providedHostName;
        }
        return new String[] {
            providedHostName,
            qualifiedHostName,
            providedNodeName
        };

    }
}

@CContext(NativeInfoDirectives.class)
final class NativeInfo {
    @CFunction
    static native int getpid();

    @CFunction
    static native int gethostname(CCharPointer nameBuf, UnsignedWord /* size_t */ len);
}

final class NativeInfoDirectives implements CContext.Directives {

    public List<String> getHeaderFiles() {
        return Collections.singletonList("<unistd.h>");
    }
}

final class ProcessUtils {
    static String getProcessName() {
        String name = System.getProperty("jboss.process.name");
        if (name == null) {
            // todo: they promised there would be an API for this in the near future
            if (Target_com_oracle_svm_core_JavaMainWrapper.argc > 0 && Target_com_oracle_svm_core_JavaMainWrapper.argv.isNonNull()) {
                name = CTypeConversion.toJavaString(Target_com_oracle_svm_core_JavaMainWrapper.argv.read(0));
                final int idx = name.lastIndexOf(File.separatorChar);
                if (idx != -1) {
                    name = name.substring(idx + 1);
                }
            }
        }
        if (name == null) {
            name = "<unknown>";
        }
        return name;
    }
}

final class ProcessSubstitutions {
    static final int SIZE = 512;
}
