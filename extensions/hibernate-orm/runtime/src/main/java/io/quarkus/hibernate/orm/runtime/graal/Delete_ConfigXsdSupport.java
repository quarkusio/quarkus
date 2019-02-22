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

package io.quarkus.hibernate.orm.runtime.graal;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Class org.hibernate.boot.xsd.ConfigXsdSupport is used only to validate
 * configuration schema, which should never happen at runtime. It also keeps
 * hold of references to parsed schemas in static fields, which is good for
 * bootstrap performance when running in the JVM - so we need to remove this
 * cache.
 *
 * WARNING: This single removal is worth almost 5MB of size in the final image.
 */
@TargetClass(className = "org.hibernate.boot.xsd.ConfigXsdSupport")
@Delete
public final class Delete_ConfigXsdSupport {
}
