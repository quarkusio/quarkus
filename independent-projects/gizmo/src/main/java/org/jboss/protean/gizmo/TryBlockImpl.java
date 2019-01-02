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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 */
class TryBlockImpl extends BytecodeCreatorImpl implements TryBlock {
    private final LinkedHashMap<String, CatchBlockCreatorImpl> catchBlocks = new LinkedHashMap<>();

    TryBlockImpl(final BytecodeCreatorImpl enclosing) {
        super(enclosing);
    }

    @Override
    public CatchBlockCreator addCatch(final String exceptionType) {
        String name = exceptionType.replace('.', '/');
        if (catchBlocks.containsKey(name)) {
            throw new IllegalStateException("Catch block for " + name + " already exists");
        }
        final CatchBlockCreatorImpl catchBlock = new CatchBlockCreatorImpl(name, getOwner());
        catchBlocks.put(name, catchBlock);
        return catchBlock;
    }

    @Override
    protected void writeOperations(final MethodVisitor visitor) {
        // this is everything between top & bottom labels
        super.writeOperations(visitor);
        // this is outside of the "try"
        if (getTop().getOffset() != getBottom().getOffset()) {
            // only generate catch blocks if the try body is non-empty
            final Label foot = new Label();
            visitor.visitJumpInsn(Opcodes.GOTO, foot);
            for (Map.Entry<String, CatchBlockCreatorImpl> entry : catchBlocks.entrySet()) {
                final CatchBlockCreatorImpl value = entry.getValue();
                value.writeOperations(visitor);
                visitor.visitJumpInsn(Opcodes.GOTO, foot);
                visitor.visitTryCatchBlock(getTop(), getBottom(), value.getTop(), entry.getKey());
            }
            visitor.visitLabel(foot);
        }
    }

    @Override
    void findActiveResultHandles(final Set<ResultHandle> handlesToAllocate) {
        super.findActiveResultHandles(handlesToAllocate);
        for (CatchBlockCreatorImpl value : catchBlocks.values()) {
            value.findActiveResultHandles(handlesToAllocate);
        }
    }
}
