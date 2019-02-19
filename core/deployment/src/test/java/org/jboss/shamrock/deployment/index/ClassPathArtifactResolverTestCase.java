/*
 * Copyright 2018 Red Hat, Inc.
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

package io.quarkus.deployment.index;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class ClassPathArtifactResolverTestCase {

    private static final ClassPathArtifactResolver RESOLVER = new ClassPathArtifactResolver(ClassPathArtifactResolverTestCase.class.getClassLoader());

    @Test
    public void testSingleGroupArtifact() throws Exception {
    	assertNotNull(RESOLVER.getArtifact("junit", "junit", null));
    }
    
    @Test
    public void testMultipleGroupArtifact() throws Exception {
    	assertNotNull(RESOLVER.getArtifact("javax.annotation", "javax.annotation-api", null));
    }

    @Test(expected=RuntimeException.class)
    public void testClassifierNotFound() throws Exception {
    	assertNotNull(RESOLVER.getArtifact("junit", "junit", "unknow-classifier"));
    }

}
