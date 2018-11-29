/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.renov8.test;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.renov8.PackLocation;
import org.jboss.renov8.test.util.xml.TestPackSpecXmlWriter;
import org.jboss.renov8.utils.IoUtils;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Alexey Loubyansky
 */
public class Renov8TestBase {

    public static PackLocation location(String producer) {
        return location(producer, "1");
    }

    public static PackLocation location(String producer, String version) {
        return PackLocation.create(producer, new StrVersion(version));
    }

    private Path workDir;
    private Path packsDir;
    protected TestPackResolver packResolver;

    @Before
    public void init() throws Exception {
        workDir = IoUtils.createRandomTmpDir();
        packsDir = workDir.resolve("pack-specs");
        Files.createDirectories(packsDir);
        packResolver = new TestPackResolver(packsDir);
        createPacks();
    }

    protected void createPacks() throws Exception {
    }

    @After
    public void cleanup() {
        IoUtils.recursiveDelete(workDir);
    }

    protected TestPack createPack(TestPack pack) throws Exception {
        Path p = TestPackResolver.resolvePackPath(packsDir, pack.location);
        TestPackSpecXmlWriter.getInstance().write(pack, p);
        return pack;
    }
}
