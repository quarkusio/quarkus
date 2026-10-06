package io.quarkus.hibernate.accessor.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class HibernateAccessorGenerationUtil implements Opcodes {

    // Split large dispatch methods to stay below the JVM's 64 KiB method bytecode limit.
    // String cases need more bytecode than integer cases because they also perform equality checks.
    static final int SWITCH_CHUNK_SIZE = 1000;
    static final int STRING_SWITCH_CHUNK_SIZE = 500;

    private HibernateAccessorGenerationUtil() {
    }

    static String fqcnToName(String fqcn) {
        return fqcn.replace('.', '/');
    }

    static String nameToFqcn(String fqcn) {
        return fqcn.replace('/', '.');
    }

    static void pushIntConst(MethodVisitor mv, int value) {
        if (value >= -1 && value <= 5) {
            mv.visitInsn(ICONST_0 + value);
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            mv.visitIntInsn(BIPUSH, value);
        } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
            mv.visitIntInsn(SIPUSH, value);
        } else {
            mv.visitLdcInsn(value);
        }
    }

    @FunctionalInterface
    interface CaseBodyEmitter {
        void emit(MethodVisitor mv, int caseIndex);
    }

    /**
     * Emits the javac-style two-phase string switch pattern:
     * Phase 1: LOOKUPSWITCH on hashCode -> equals checks -> set temp variable
     * Phase 2: TABLESWITCH on temp variable -> case bodies
     *
     * <pre>
     * int caseIndex = -1;
     * switch (key.hashCode()) {
     *     case 2236: // "Ea" and "FB" have the same hash
     *         if (key.equals("Ea"))
     *             caseIndex = 0;
     *         else if (key.equals("FB"))
     *             caseIndex = 1;
     *         break;
     * }
     * switch (caseIndex) {
     *     case 0: // bodyEmitter.emit(mv, 0)
     *     case 1: // bodyEmitter.emit(mv, 1)
     *     default: // jump to defaultLabel
     * }
     * </pre>
     *
     * The case bodies above are placeholders: the emitter supplies their instructions and control flow.
     */
    static void emitStringSwitch(MethodVisitor mv, int stringSlot, int tempSlot,
            List<String> cases, Label defaultLabel, CaseBodyEmitter bodyEmitter) {
        if (cases.isEmpty()) {
            mv.visitJumpInsn(GOTO, defaultLabel);
            return;
        }

        // LOOKUPSWITCH requires sorted keys. Keep all cases for each hash: distinct member names can
        // collide, so the hash selects a bucket and equals checks select the actual case.
        TreeMap<Integer, List<Integer>> hashToCaseIndices = new TreeMap<>();
        for (int i = 0; i < cases.size(); i++) {
            int hash = cases.get(i).hashCode();
            hashToCaseIndices.computeIfAbsent(hash, k -> new ArrayList<>()).add(i);
        }

        int[] hashes = hashToCaseIndices.keySet().stream().mapToInt(Integer::intValue).toArray();
        Label[] hashLabels = new Label[hashes.length];
        for (int i = 0; i < hashes.length; i++) {
            hashLabels[i] = new Label();
        }

        Label secondSwitch = new Label();

        pushIntConst(mv, -1);
        mv.visitVarInsn(ISTORE, tempSlot);

        mv.visitVarInsn(ALOAD, stringSlot);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitLookupSwitchInsn(secondSwitch, hashes, hashLabels);

        for (int h = 0; h < hashes.length; h++) {
            mv.visitLabel(hashLabels[h]);
            mv.visitFrame(F_SAME, 0, null, 0, null);

            List<Integer> caseIndices = hashToCaseIndices.get(hashes[h]);
            for (int c = 0; c < caseIndices.size(); c++) {
                int ci = caseIndices.get(c);
                boolean last = (c == caseIndices.size() - 1);

                mv.visitVarInsn(ALOAD, stringSlot);
                mv.visitLdcInsn(cases.get(ci));
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals",
                        "(Ljava/lang/Object;)Z", false);

                if (last) {
                    Label afterSet = new Label();
                    mv.visitJumpInsn(IFEQ, afterSet);
                    pushIntConst(mv, ci);
                    mv.visitVarInsn(ISTORE, tempSlot);
                    mv.visitLabel(afterSet);
                    mv.visitFrame(F_SAME, 0, null, 0, null);
                } else {
                    Label nextCheck = new Label();
                    mv.visitJumpInsn(IFEQ, nextCheck);
                    pushIntConst(mv, ci);
                    mv.visitVarInsn(ISTORE, tempSlot);
                    mv.visitJumpInsn(GOTO, secondSwitch);
                    mv.visitLabel(nextCheck);
                    mv.visitFrame(F_SAME, 0, null, 0, null);
                }
            }
            mv.visitJumpInsn(GOTO, secondSwitch);
        }

        mv.visitLabel(secondSwitch);
        mv.visitFrame(F_SAME, 0, null, 0, null);

        Label[] bodyLabels = new Label[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            bodyLabels[i] = new Label();
        }

        mv.visitVarInsn(ILOAD, tempSlot);
        mv.visitTableSwitchInsn(0, cases.size() - 1, defaultLabel, bodyLabels);

        for (int i = 0; i < cases.size(); i++) {
            mv.visitLabel(bodyLabels[i]);
            mv.visitFrame(F_SAME, 0, null, 0, null);
            bodyEmitter.emit(mv, i);
        }
    }

}
