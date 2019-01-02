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

package org.jboss.protean.gizmo;

/**
 * A {@code try} block.  If the flow of control exits the bottom of the {@code try} block, it will resume after
 * the point the block was declared in the enclosing scope.
 */
public interface TryBlock extends BytecodeCreator {
    /**
     * Add a {@code catch} block.  The {@code catch} block will be emitted after the {@code try} block.
     * If the flow of control exits the bottom of the {@code catch} block, it will resume after the point where
     * the {@code try} block was declared in the enclosing scope.  The parent scope of the {@code catch} block is the
     * same as the parent scope of the {@code try} block.
     *
     * @param exceptionType the exception type to catch (must not be {@code null})
     * @return the catch block (not {@code null})
     */
    CatchBlockCreator addCatch(String exceptionType);

    /**
     * Add a {@code catch} block in the same manner as {@link #addCatch(String)}.
     *
     * @param exceptionType the exception type (must not be {@code null})
     * @return the catch block (not {@code null})
     */
    default CatchBlockCreator addCatch(Class<? extends Throwable> exceptionType) {
        return addCatch(exceptionType.getName());
    }
}
