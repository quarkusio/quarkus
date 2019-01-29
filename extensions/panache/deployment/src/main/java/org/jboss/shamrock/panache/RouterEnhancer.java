package org.jboss.shamrock.panache;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.jboss.panache.router.Router;
import org.jboss.panache.router._Router;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class RouterEnhancer implements BiFunction<String, ClassVisitor, ClassVisitor> {

    public final static String ROUTER_NAME = Router.class.getName();
    public final static String ROUTER_BINARY_NAME = ROUTER_NAME.replace('.', '/');
    public final static String _ROUTER_NAME = _Router.class.getName();
    public final static String _ROUTER_BINARY_NAME = _ROUTER_NAME.replace('.', '/');

    public final static String ROUTER_METHOD = "getURI";
    
    public static class PointerValue extends BasicValue {

        private Handle handle;

        public PointerValue(Type type, Handle handle) {
            super(type);
            this.handle = handle;
        }

    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new RouterEnhancingClassVisitor(className, outputClassVisitor);
    }

    static class RouterEnhancingClassVisitor extends ClassVisitor {

        public RouterEnhancingClassVisitor(String className, ClassVisitor outputClassVisitor) {
            super(Opcodes.ASM6, outputClassVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor omv = super.visitMethod(access, name, descriptor, signature, exceptions);
            
            return new MethodNode(Opcodes.ASM6, access, name, descriptor, signature, exceptions) {
                boolean shouldInstrument;
                
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    if(opcode == Opcodes.INVOKESTATIC 
                            && owner.equals(ROUTER_BINARY_NAME)
                            && name.equals(ROUTER_METHOD)) {
                        shouldInstrument = true;
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }

                public void visitEnd() {
                    if (shouldInstrument && instructions.size() > 0) {
                        try {
                            // collect info about the INDY values
                            Analyzer<BasicValue> a = new Analyzer<>(new BasicInterpreter(Opcodes.ASM6) {
                                public BasicValue naryOperation(AbstractInsnNode insn, List<? extends BasicValue> values) throws AnalyzerException {
                                    BasicValue type = super.naryOperation(insn, values);
                                    if(insn.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                                        Handle bsm = ((InvokeDynamicInsnNode)insn).bsm;
                                        if(bsm.getTag() == Opcodes.H_INVOKESTATIC
                                                && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                                                && bsm.getName().equals("altMetafactory")) {
                                            Object[] args = ((InvokeDynamicInsnNode)insn).bsmArgs;
                                            if(args.length > 2 && args[1] instanceof Handle) {
                                                Handle pointerTo = (Handle) args[1];
                                                if(pointerTo.getTag() == Opcodes.H_INVOKEVIRTUAL) {
                                                    return new PointerValue(type.getType(), pointerTo);
                                                }
                                            }
                                        }
                                    }
                                    return type;
                                };
                            });
                            
                            // analyze the bytecode to get argument frames
                            a.analyze(name, this);
                            Map<MethodInsnNode, Handle> calls = new HashMap<>();
                            final ListIterator<AbstractInsnNode> it = instructions.iterator();
                            
                            // find and collect the Router.getURI method calls
                            while (it.hasNext()) {
                                final AbstractInsnNode insnNode = it.next();
                                if (insnNode instanceof MethodInsnNode) {
                                    final MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                                    if (methodInsnNode.getOpcode() == Opcodes.INVOKESTATIC) {
                                        if (methodInsnNode.name.equals(ROUTER_METHOD) 
                                                && methodInsnNode.owner.equals(ROUTER_BINARY_NAME)) {
                                            final Frame<BasicValue> frame = a.getFrames()[it.previousIndex()];
                                            final int stackSize = frame.getStackSize();
                                            int classArg = stackSize - 2;
                                            if (classArg >= 0) {
                                                final BasicValue value = frame.getStack(classArg);
                                                if(value instanceof PointerValue) {
                                                    // we need to collect them rather than modify them in the same pass,
                                                    // otherwise we modify the indices and we can't get the proper frames
                                                    calls.put(methodInsnNode, ((PointerValue)value).handle);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            // now insert extra arguments
                            for (Entry<MethodInsnNode, Handle> entry : calls.entrySet()) {
                                // insert args before this call
                                MethodInsnNode node = entry.getKey();
                                Handle handle = entry.getValue();
                                instructions.insertBefore(node, new LdcInsnNode(Type.getType("L"+handle.getOwner()+";")));
                                instructions.insertBefore(node, new LdcInsnNode(handle.getName()));

                                Type[] argumentTypes = Type.getArgumentTypes(handle.getDesc());
                                instructions.insertBefore(node, count(argumentTypes.length));
                                instructions.insertBefore(node, new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"));
                                for (int i = 0; i < argumentTypes.length; i++) {
                                    Type type = argumentTypes[i];
                                    instructions.insertBefore(node, new InsnNode(Opcodes.DUP));
                                    instructions.insertBefore(node, count(i));
                                    instructions.insertBefore(node, type(type));
                                    instructions.insertBefore(node, new InsnNode(Opcodes.AASTORE));
                                }
                                // change the descriptor of what we're calling
                                String oldDesc = node.desc;
                                int lastParen = oldDesc.indexOf(')');
                                // new extra params
                                node.desc = oldDesc.substring(0, lastParen) 
                                        + "Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;"
                                        + oldDesc.substring(lastParen);
                                // switch owner too
                                node.owner = _ROUTER_BINARY_NAME;
                            }
                        }catch(AnalyzerException x) {
                            x.printStackTrace();
                        }
                    }
                    if (omv != null) accept(omv);
                }
            };
        }

        protected AbstractInsnNode type(Type type) {
            type.getSort();
            switch(type.getSort()) {
            case Type.VOID:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
            case Type.BOOLEAN:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
            case Type.CHAR:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
            case Type.BYTE:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
            case Type.SHORT:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
            case Type.INT:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
            case Type.LONG:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
            case Type.FLOAT:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
            case Type.DOUBLE:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
            }
            // the rest is handled by this (arrays and objects)
            return new LdcInsnNode(type);
        }

        protected AbstractInsnNode count(int length) {
            switch(length) {
            case 0:
                return new InsnNode(Opcodes.ICONST_0);
            case 1:
                return new InsnNode(Opcodes.ICONST_1);
            case 2:
                return new InsnNode(Opcodes.ICONST_2);
            case 3:
                return new InsnNode(Opcodes.ICONST_3);
            case 4:
                return new InsnNode(Opcodes.ICONST_4);
            case 5:
                return new InsnNode(Opcodes.ICONST_5);
            default:
                return new LdcInsnNode(length);    
            }
        }
    }
}
