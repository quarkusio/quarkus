package org.jboss.protean.gizmo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

class BytecodeCreatorImpl implements BytecodeCreator {

    public static final String DEBUG_HELPERS_PROPERTY = "org.jboss.protean.gizmo.DEBUG_HELPERS_ON";

    private static final AtomicInteger functionCount = new AtomicInteger();

    private static final String FUNCTION = "$$function$$";

    protected final List<Operation> operations = new ArrayList<>();

    private final MethodCreatorImpl method;

    private final BytecodeCreatorImpl owner;

    private final Label top = new Label();
    private final Label bottom = new Label();

    private static final Map<String, String> boxingMap;
    private static final Map<String, String> boxingMethodMap;

    static {
        Map<String, String> b = new HashMap<>();
        b.put("Z", Type.getInternalName(Boolean.class));
        b.put("B", Type.getInternalName(Byte.class));
        b.put("C", Type.getInternalName(Character.class));
        b.put("S", Type.getInternalName(Short.class));
        b.put("I", Type.getInternalName(Integer.class));
        b.put("J", Type.getInternalName(Long.class));
        b.put("F", Type.getInternalName(Float.class));
        b.put("D", Type.getInternalName(Double.class));
        boxingMap = Collections.unmodifiableMap(b);

        b = new HashMap<>();
        b.put("Z", "booleanValue");
        b.put("B", "byteValue");
        b.put("C", "charValue");
        b.put("S", "shortValue");
        b.put("I", "intValue");
        b.put("J", "longValue");
        b.put("F", "floatValue");
        b.put("D", "doubleValue");
        boxingMethodMap = Collections.unmodifiableMap(b);
    }

    Label getTop() {
        return top;
    }

    Label getBottom() {
        return bottom;
    }

    BytecodeCreatorImpl() {
        this.method = (MethodCreatorImpl) this;
        this.owner = null;
    }

    BytecodeCreatorImpl(BytecodeCreatorImpl enclosing) {
        this.method = enclosing.getMethod();
        this.owner = enclosing;
    }

    @Override
    public ResultHandle getThis() {
        ResultHandle resultHandle = new ResultHandle("L" + getMethod().getDeclaringClassName().replace('.', '/') + ";", this);
        resultHandle.setNo(0);
        return resultHandle;
    }

    @Override
    public ResultHandle invokeVirtualMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, resolve(object), resolve(args), false, false));
        return ret;
    }

    @Override
    public ResultHandle invokeInterfaceMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, resolve(object), resolve(args), true, false));
        return ret;
    }

    @Override
    public ResultHandle invokeStaticMethod(MethodDescriptor descriptor, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, resolve(args)));
        return ret;
    }


    @Override
    public ResultHandle invokeSpecialMethod(MethodDescriptor descriptor, ResultHandle object, ResultHandle... args) {
        ResultHandle ret = allocateResult(descriptor.getReturnType());
        operations.add(new InvokeOperation(ret, descriptor, resolve(object), resolve(args), false, true));
        return ret;
    }


    @Override
    public ResultHandle newInstance(MethodDescriptor descriptor, ResultHandle... args) {
        ResultHandle ret = allocateResult("L" + descriptor.getDeclaringClass() + ";");
        operations.add(new NewInstanceOperation(ret, descriptor, resolve(args)));
        return ret;
    }

    @Override
    public ResultHandle newArray(String type, ResultHandle length) {
        String resultType;
        if (!type.startsWith("[")) {
            //assume a single dimension array
            resultType = "[" + DescriptorUtils.objectToDescriptor(type);
        } else {
            resultType = DescriptorUtils.objectToDescriptor(type);
        }
        if (resultType.startsWith("[[")) {
            throw new RuntimeException("Multidimensional arrays not supported yet");
        }
        final ResultHandle resolvedLength = resolve(length);
        char typeChar = resultType.charAt(1);
        if (typeChar != 'L') {
            //primitive arrays
            int opcode;
            switch (typeChar) {
                case 'Z':
                    opcode = Opcodes.T_BOOLEAN;
                    break;
                case 'B':
                    opcode = Opcodes.T_BYTE;
                    break;
                case 'C':
                    opcode = Opcodes.T_CHAR;
                    break;
                case 'S':
                    opcode = Opcodes.T_SHORT;
                    break;
                case 'I':
                    opcode = Opcodes.T_INT;
                    break;
                case 'J':
                    opcode = Opcodes.T_LONG;
                    break;
                case 'F':
                    opcode = Opcodes.T_FLOAT;
                    break;
                case 'D':
                    opcode = Opcodes.T_DOUBLE;
                    break;
                default:
                    throw new RuntimeException("Unknown type " + type);
            }
            ResultHandle ret = allocateResult(resultType);
            operations.add(new Operation() {
                @Override
                public void writeBytecode(MethodVisitor methodVisitor) {
                    loadResultHandle(methodVisitor, resolvedLength, BytecodeCreatorImpl.this, "I");
                    methodVisitor.visitIntInsn(Opcodes.NEWARRAY, opcode);
                    storeResultHandle(methodVisitor, ret);
                }

                @Override
                Set<ResultHandle> getInputResultHandles() {
                    return Collections.singleton(resolvedLength);
                }

                @Override
                ResultHandle getTopResultHandle() {
                    return resolvedLength;
                }

                @Override
                ResultHandle getOutgoingResultHandle() {
                    return ret;
                }
            });
            return ret;
        } else {
            //object arrays
            String arrayType = resultType.substring(2, resultType.length() - 1);
            ResultHandle ret = allocateResult(resultType);
            operations.add(new Operation() {
                @Override
                public void writeBytecode(MethodVisitor methodVisitor) {
                    loadResultHandle(methodVisitor, resolvedLength, BytecodeCreatorImpl.this, "I");
                    methodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, arrayType);
                    storeResultHandle(methodVisitor, ret);
                }

                @Override
                Set<ResultHandle> getInputResultHandles() {
                    return Collections.singleton(resolvedLength);
                }

                @Override
                ResultHandle getTopResultHandle() {
                    return resolvedLength;
                }

                @Override
                ResultHandle getOutgoingResultHandle() {
                    return ret;
                }
            });
            return ret;
        }

    }

    @Override
    public ResultHandle load(String val) {
        return new ResultHandle("Ljava/lang/String;", this, val);
    }

    @Override
    public ResultHandle load(byte val) {
        return new ResultHandle("B", this, val);
    }

    @Override
    public ResultHandle load(short val) {
        return new ResultHandle("S", this, val);
    }

    @Override
    public ResultHandle load(char val) {
        return new ResultHandle("C", this, val);
    }

    @Override
    public ResultHandle load(int val) {
        return new ResultHandle("I", this, val);
    }

    @Override
    public ResultHandle load(long val) {
        return new ResultHandle("J", this, val);
    }

    @Override
    public ResultHandle load(float val) {
        return new ResultHandle("F", this, val);
    }

    @Override
    public ResultHandle load(double val) {
        return new ResultHandle("D", this, val);
    }

    @Override
    public ResultHandle load(boolean val) {
        return new ResultHandle("Z", this, val);
    }

    @Override
    public ResultHandle loadClass(String className) {
        Class primtiveType = null;
        if (className.equals("boolean")) {
            primtiveType = Boolean.class;
        } else if (className.equals("byte")) {
            primtiveType = Byte.class;
        } else if (className.equals("char")) {
            primtiveType = Character.class;
        } else if (className.equals("short")) {
            primtiveType = Short.class;
        } else if (className.equals("int")) {
            primtiveType = Integer.class;
        } else if (className.equals("long")) {
            primtiveType = Long.class;
        } else if (className.equals("float")) {
            primtiveType = Float.class;
        } else if (className.equals("double")) {
            primtiveType = Double.class;
        }
        if (primtiveType == null) {
            return new ResultHandle("Ljava/lang/Class;", this, Type.getObjectType(className.replace('.', '/')));
        } else {
            Class pt = primtiveType;
            ResultHandle ret = new ResultHandle("Ljava/lang/Class;", this);
            operations.add(new Operation() {
                @Override
                void writeBytecode(MethodVisitor methodVisitor) {
                    methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, Type.getInternalName(pt), "TYPE", "Ljava/lang/Class;");
                    storeResultHandle(methodVisitor, ret);
                }

                @Override
                Set<ResultHandle> getInputResultHandles() {
                    return Collections.emptySet();
                }

                @Override
                ResultHandle getTopResultHandle() {
                    return null;
                }

                @Override
                ResultHandle getOutgoingResultHandle() {
                    return ret;
                }
            });
            return ret;
        }
    }

    @Override
    public ResultHandle loadNull() {
        return ResultHandle.NULL;
    }

    @Override
    public void writeInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance, ResultHandle value) {
        final ResultHandle resolvedInstance = resolve(instance);
        final ResultHandle resolvedValue = resolve(value);
        operations.add(new Operation() {
            @Override
            void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedInstance, BytecodeCreatorImpl.this, "L" + fieldDescriptor.getDeclaringClass() + ";");
                loadResultHandle(methodVisitor, resolvedValue, BytecodeCreatorImpl.this, fieldDescriptor.getType());
                methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return new HashSet<>(Arrays.asList(resolvedInstance, resolvedValue));
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedInstance;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }
        });
    }

    @Override
    public ResultHandle readInstanceField(FieldDescriptor fieldDescriptor, ResultHandle instance) {
        ResultHandle resultHandle = allocateResult(fieldDescriptor.getType());
        ResultHandle resolvedInstance = resolve(instance);
        operations.add(new Operation() {
            @Override
            void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedInstance, BytecodeCreatorImpl.this, "L" + fieldDescriptor.getDeclaringClass() + ";");
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
                storeResultHandle(methodVisitor, resultHandle);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.singleton(resolvedInstance);
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedInstance;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return resultHandle;
            }
        });
        return resultHandle;
    }

    @Override
    public void writeStaticField(FieldDescriptor fieldDescriptor, ResultHandle value) {
        ResultHandle resolvedValue = resolve(value);
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedValue, BytecodeCreatorImpl.this, fieldDescriptor.getType());
                methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.singleton(resolvedValue);
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedValue;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }
        });
    }

    @Override
    public ResultHandle readStaticField(FieldDescriptor fieldDescriptor) {
        ResultHandle result = allocateResult(fieldDescriptor.getType());
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, fieldDescriptor.getDeclaringClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
                storeResultHandle(methodVisitor, result);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.emptySet();
            }

            @Override
            ResultHandle getTopResultHandle() {
                return null;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return result;
            }
        });
        return result;
    }

    @Override
    public ResultHandle readArrayValue(ResultHandle array, ResultHandle index) {
        ResultHandle result = allocateResult(array.getType().substring(1));
        ResultHandle resolvedArray = resolve(array);
        ResultHandle resolvedIndex = resolve(index);
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedArray, BytecodeCreatorImpl.this, resolvedArray.getType());
                loadResultHandle(methodVisitor, resolvedIndex, BytecodeCreatorImpl.this, "I");
                methodVisitor.visitInsn(Opcodes.AALOAD);
                storeResultHandle(methodVisitor, result);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return new HashSet<>(Arrays.asList(resolvedArray, resolvedIndex));
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedArray;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return result;
            }
        });
        return result;
    }

    @Override
    public void writeArrayValue(ResultHandle array, ResultHandle index, ResultHandle value) {
        ResultHandle resolvedArray = resolve(array);
        ResultHandle resolvedIndex = resolve(index);
        ResultHandle resolvedValue = resolve(value);
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedArray, BytecodeCreatorImpl.this, resolvedArray.getType());
                loadResultHandle(methodVisitor, resolvedIndex, BytecodeCreatorImpl.this, "I");
                String arrayType = resolvedArray.getType().substring(1);
                loadResultHandle(methodVisitor, resolvedValue, BytecodeCreatorImpl.this, arrayType);
                if (arrayType.equals("Z") || arrayType.equals("B")) {
                    methodVisitor.visitInsn(Opcodes.BASTORE);
                } else if (arrayType.equals("S")) {
                    methodVisitor.visitInsn(Opcodes.SASTORE);
                } else if (arrayType.equals("I")) {
                    methodVisitor.visitInsn(Opcodes.IASTORE);
                } else if (arrayType.equals("C")) {
                    methodVisitor.visitInsn(Opcodes.CASTORE);
                } else if (arrayType.equals("L")) {
                    methodVisitor.visitInsn(Opcodes.LASTORE);
                } else if (arrayType.equals("F")) {
                    methodVisitor.visitInsn(Opcodes.FASTORE);
                } else if (arrayType.equals("D")) {
                    methodVisitor.visitInsn(Opcodes.DASTORE);
                } else {
                    methodVisitor.visitInsn(Opcodes.AASTORE);
                }
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return new HashSet<>(Arrays.asList(resolvedArray, resolvedIndex, resolvedValue));
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedArray;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }
        });
    }

    @Override
    public ResultHandle checkCast(final ResultHandle resultHandle, final String castTarget) {
        final String intName = castTarget.replace('.', '/');
        // seems like a waste of local vars but it's the safest approach since result type can't be mutated
        final ResultHandle result = allocateResult("L" + intName + ";");
        final ResultHandle resolvedResultHandle = resolve(resultHandle);
        assert result != null;
        operations.add(new Operation() {
            void writeBytecode(final MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedResultHandle, BytecodeCreatorImpl.this, result.getType());
                storeResultHandle(methodVisitor, result);
            }

            Set<ResultHandle> getInputResultHandles() {
                return Collections.singleton(resolvedResultHandle);
            }

            ResultHandle getTopResultHandle() {
                return resolvedResultHandle;
            }

            ResultHandle getOutgoingResultHandle() {
                return result;
            }
        });
        return result;
    }

    public boolean isScopedWithin(final BytecodeCreator other) {
        return other == this || owner.isScopedWithin(other);
    }

    public void continueScope(final BytecodeCreator scope) {
        if (! isScopedWithin(scope)) {
            throw new IllegalArgumentException("Cannot continue non-enclosing scope");
        }
        operations.add(new JumpOperation(((BytecodeCreatorImpl) scope).top));
    }

    public void breakScope(final BytecodeCreator scope) {
        if (! isScopedWithin(scope)) {
            throw new IllegalArgumentException("Cannot break non-enclosing scope");
        }
        operations.add(new JumpOperation(((BytecodeCreatorImpl) scope).bottom));
    }

    public BytecodeCreator createScope() {
        final BytecodeCreatorImpl enclosed = new BytecodeCreatorImpl(this);
        operations.add(new BlockOperation(enclosed));
        return enclosed;
    }

    static void storeResultHandle(MethodVisitor methodVisitor, ResultHandle handle) {
        if (handle.getResultType() == ResultHandle.ResultType.UNUSED) {
            if (handle.getType().equals("J") || handle.getType().equals("D")) {
                methodVisitor.visitInsn(Opcodes.POP2);
            } else {
                methodVisitor.visitInsn(Opcodes.POP);
            }
        } else if (handle.getResultType() == ResultHandle.ResultType.LOCAL_VARIABLE) {
            if (handle.getType().equals("S") || handle.getType().equals("Z") || handle.getType().equals("I") || handle.getType().equals("B") || handle.getType().equals("C")) {
                methodVisitor.visitVarInsn(Opcodes.ISTORE, handle.getNo());
            } else if (handle.getType().equals("J")) {
                methodVisitor.visitVarInsn(Opcodes.LSTORE, handle.getNo());
            } else if (handle.getType().equals("F")) {
                methodVisitor.visitVarInsn(Opcodes.FSTORE, handle.getNo());
            } else if (handle.getType().equals("D")) {
                methodVisitor.visitVarInsn(Opcodes.DSTORE, handle.getNo());
            } else {
                methodVisitor.visitVarInsn(Opcodes.ASTORE, handle.getNo());
            }
        }
    }

    void loadResultHandle(MethodVisitor methodVisitor, ResultHandle handle, BytecodeCreatorImpl bc, String expectedType) {
        loadResultHandle(methodVisitor, handle, bc, expectedType, false);
    }

    void loadResultHandle(MethodVisitor methodVisitor, ResultHandle handle, BytecodeCreatorImpl bc, String expectedType, boolean dontCast) {
        if (handle.getResultType() == ResultHandle.ResultType.CONSTANT) {
            if (handle.getConstant() == null) {
                methodVisitor.visitInsn(Opcodes.ACONST_NULL);
            } else {
                methodVisitor.visitLdcInsn(handle.getConstant());
            }
            return;
        }
        if (handle.getOwner() != bc && (bc.owner == null || handle.getOwner() != bc.owner)) {
            //throw new IllegalArgumentException("Wrong owner for ResultHandle " + handle);
        }
        if (handle.getResultType() != ResultHandle.ResultType.SINGLE_USE) {
            if (handle.getType().equals("S") || handle.getType().equals("Z") || handle.getType().equals("I") || handle.getType().equals("B") || handle.getType().equals("C")) {
                methodVisitor.visitVarInsn(Opcodes.ILOAD, handle.getNo());
            } else if (handle.getType().equals("J")) {
                methodVisitor.visitVarInsn(Opcodes.LLOAD, handle.getNo());
            } else if (handle.getType().equals("F")) {
                methodVisitor.visitVarInsn(Opcodes.FLOAD, handle.getNo());
            } else if (handle.getType().equals("D")) {
                methodVisitor.visitVarInsn(Opcodes.DLOAD, handle.getNo());
            } else {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, handle.getNo());
            }
        }
        if (!dontCast && !expectedType.equals(handle.getType())) {
            //both objects, we just do a checkcast
            if (expectedType.length() > 1 && handle.getType().length() > 1) {
                if (!expectedType.equals("Ljava/lang/Object;")) {
                    methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, DescriptorUtils.getTypeStringFromDescriptorFormat(expectedType));
                }
            } else if (expectedType.length() == 1 && handle.getType().length() == 1) {
                //ignore
            } else if (expectedType.length() == 1) {
                //autounboxing support
                String type = boxingMap.get(expectedType);
                if (type == null) {
                    throw new RuntimeException("Unknown primitive type " + expectedType);
                }
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, type);
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, type, boxingMethodMap.get(expectedType), "()" + expectedType, false);
            } else {
                //autoboxing support
                String type = boxingMap.get(handle.getType());
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, type, "valueOf", "(" + handle.getType() + ")L" + type + ";", false);
            }
        }
    }

    public TryBlock tryBlock() {
        final TryBlockImpl tryBlock = new TryBlockImpl(this);
        operations.add(new BlockOperation(tryBlock));
        return tryBlock;
    }

    @Override
    public BranchResult ifNonZero(ResultHandle resultHandle) {
        ResultHandle resolvedResultHandle = resolve(resultHandle);
        BytecodeCreatorImpl trueBranch = new BytecodeCreatorImpl(this);
        BytecodeCreatorImpl falseBranch = new BytecodeCreatorImpl(this);
        operations.add(new IfOperation(Opcodes.IFNE, "I", resolvedResultHandle, trueBranch, falseBranch));
        return new BranchResultImpl(owner == null ? this : owner, trueBranch, falseBranch, trueBranch, falseBranch);
    }

    @Override
    public BranchResult ifNull(ResultHandle resultHandle) {
        ResultHandle resolvedResultHandle = resolve(resultHandle);
        BytecodeCreatorImpl trueBranch = new BytecodeCreatorImpl(this);
        BytecodeCreatorImpl falseBranch = new BytecodeCreatorImpl(this);
        operations.add(new IfOperation(Opcodes.IFNULL, "Ljava/lang/Object;", resolvedResultHandle, trueBranch, falseBranch));
        return new BranchResultImpl(owner == null ? this : owner, trueBranch, falseBranch, trueBranch, falseBranch);
    }


    @Override
    public ResultHandle getMethodParam(int methodNo) {
        int count = 1;
        for (int i = 0; i < methodNo; ++i) {
            String s = getMethod().getMethodDescriptor().getParameterTypes()[i];
            if (s.equals("J") || s.equals("D")) {
                count += 2;
            } else {
                count++;
            }
        }
        ResultHandle resultHandle = new ResultHandle(getMethod().getMethodDescriptor().getParameterTypes()[methodNo], this);
        resultHandle.setNo(count);
        return resultHandle;
    }

    @Override
    public FunctionCreator createFunction(Class<?> functionalInterface) {
        if (!functionalInterface.isInterface()) {
            throw new IllegalArgumentException("Not an interface " + functionalInterface);
        }
        Method functionMethod = null;
        for (Method m : functionalInterface.getMethods()) {
            if (m.isDefault() || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            if (functionMethod != null) {
                throw new IllegalArgumentException("Not a functional interface " + functionalInterface);
            } else {
                functionMethod = m;
            }
        }
        if (functionMethod == null) {
            throw new IllegalArgumentException("Could not find function method " + functionalInterface);
        }
        final String functionName = getMethod().getDeclaringClassName() + FUNCTION + functionCount.incrementAndGet();
        ResultHandle ret = new ResultHandle("L" + functionName.replace('.', '/') + ";", this);
        ClassCreator cc = ClassCreator.builder().classOutput(getMethod().getClassOutput()).className(functionName).interfaces(functionalInterface).build();
        MethodCreatorImpl mc = (MethodCreatorImpl) cc.getMethodCreator(functionMethod.getName(), functionMethod.getReturnType(), functionMethod.getParameterTypes());

        FunctionCreatorImpl fc = mc.addFunctionBody(ret, cc, mc, this);
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                fc.writeCreateInstance(methodVisitor);
                cc.close();
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.emptySet();
            }

            @Override
            ResultHandle getTopResultHandle() {
                return null;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return fc.getInstance();
            }

            @Override
            public void findResultHandles(Set<ResultHandle> vc) {
                vc.addAll(fc.getCapturedResultHandles());
            }
        });
        return fc;
    }

    @Override
    public void returnValue(ResultHandle returnValue) {
        ResultHandle resolvedReturnValue = resolve(returnValue);
        final MethodDescriptor methodDescriptor = getMethod().getMethodDescriptor();
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                if (resolvedReturnValue == null
                        || methodDescriptor.getReturnType().equals("V")) { //ignore value for void methods, makes client code simpler
                    methodVisitor.visitInsn(Opcodes.RETURN);
                } else {
                    loadResultHandle(methodVisitor, resolvedReturnValue, BytecodeCreatorImpl.this, methodDescriptor.getReturnType());
                    if (resolvedReturnValue.getType().equals("S") || resolvedReturnValue.getType().equals("Z") || resolvedReturnValue.getType().equals("I") || resolvedReturnValue.getType().equals("B")) {
                        methodVisitor.visitInsn(Opcodes.IRETURN);
                    } else if (resolvedReturnValue.getType().equals("J")) {
                        methodVisitor.visitInsn(Opcodes.LRETURN);
                    } else if (resolvedReturnValue.getType().equals("F")) {
                        methodVisitor.visitInsn(Opcodes.FRETURN);
                    } else if (resolvedReturnValue.getType().equals("D")) {
                        methodVisitor.visitInsn(Opcodes.DRETURN);
                    } else {
                        methodVisitor.visitInsn(Opcodes.ARETURN);
                    }
                }
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                if (resolvedReturnValue == null) {
                    return Collections.emptySet();
                }
                return Collections.singleton(resolvedReturnValue);
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedReturnValue;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }
        });
    }

    @Override
    public void throwException(ResultHandle exception) {
        ResultHandle resolvedException = resolve(exception);
        operations.add(new Operation() {
            @Override
            public void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedException, BytecodeCreatorImpl.this, "Ljava/lang/Throwable;");
                methodVisitor.visitInsn(Opcodes.ATHROW);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.singleton(resolvedException);
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedException;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return null;
            }
        });
    }

    private ResultHandle allocateResult(String returnType) {
        if (returnType.equals("V")) {
            return null;
        }
        return new ResultHandle(returnType, this);
    }

    protected int allocateLocalVariables(int localVarCount) {

        Set<ResultHandle> handlesToAllocate = new LinkedHashSet<>();
        findActiveResultHandles(handlesToAllocate);
        int vc = localVarCount;
        for (ResultHandle handle : handlesToAllocate) {
            if (handle.getResultType() == ResultHandle.ResultType.CONSTANT || handle.getResultType() == ResultHandle.ResultType.LOCAL_VARIABLE) {
                continue;
            }
            handle.setNo(vc);
            if (handle.getType().equals("J") || handle.getType().equals("D")) {
                vc += 2;
            } else {
                vc++;
            }
        }
        return vc;
    }

    void findActiveResultHandles(Set<ResultHandle> handlesToAllocate) {
        Operation prev = null;
        for (int i = 0; i < operations.size(); ++i) {
            Operation op = operations.get(i);
            Set<ResultHandle> toAdd = new HashSet<>(op.getInputResultHandles());
            if (prev != null &&
                    prev.getOutgoingResultHandle() != null &&
                    prev.getOutgoingResultHandle() == op.getTopResultHandle()) {
                toAdd.remove(op.getTopResultHandle());
                if (op.getTopResultHandle().getResultType() == ResultHandle.ResultType.UNUSED) {
                    op.getTopResultHandle().markSingleUse();
                }
            }
            handlesToAllocate.addAll(toAdd);
            op.findResultHandles(handlesToAllocate);
            prev = op;
        }
    }

    protected void writeOperations(MethodVisitor visitor) {
        visitor.visitLabel(top);
        writeInteriorOperations(visitor);
        visitor.visitLabel(bottom);
    }

    protected void writeInteriorOperations(MethodVisitor visitor) {
        for (Operation op : operations) {
            op.doProcess(visitor);
        }
    }

    ResultHandle resolve(ResultHandle handle) {
        return owner.resolve(handle);
    }

    ResultHandle[] resolve(ResultHandle... handles) {
        return owner.resolve(handles);
    }

    /**
     * Assigns the value in the second result handle to the first result handle. The first result handle must not be a constant.
     * <p>
     * This is used to merge the results of if statements back into a single result.
     *
     * @param target The target result handle
     * @param value  The value
     */
    void assign(ResultHandle target, ResultHandle value) {
        ResultHandle resolvedTarget = resolve(target);
        ResultHandle resolvedValue = resolve(value);
        operations.add(new Operation() {
            @Override
            void writeBytecode(MethodVisitor methodVisitor) {
                loadResultHandle(methodVisitor, resolvedValue, BytecodeCreatorImpl.this, resolvedTarget.getType());
                storeResultHandle(methodVisitor, resolvedTarget);
            }

            @Override
            Set<ResultHandle> getInputResultHandles() {
                return Collections.singleton(resolvedValue);
            }

            @Override
            ResultHandle getTopResultHandle() {
                return resolvedValue;
            }

            @Override
            ResultHandle getOutgoingResultHandle() {
                return resolvedTarget;
            }
        });
    }

    MethodCreatorImpl getMethod() {
        return method;
    }

    BytecodeCreatorImpl getOwner() {
        return owner;
    }

    static abstract class Operation {


        private final Throwable errorPoint;

        Operation() {
            if(Boolean.getBoolean("arc.debug")) {
                errorPoint= new RuntimeException("Error location");
            } else {
                errorPoint = null;
            }
        }


        public void doProcess(MethodVisitor visitor) {
            try {
                writeBytecode(visitor);
            } catch (Throwable e) {
                if(errorPoint == null) {
                    throw new RuntimeException(e);
                }
                RuntimeException ex = new RuntimeException("Exception generating bytecode", errorPoint);
                ex.addSuppressed(e);
                throw ex;
            }
        }

        abstract void writeBytecode(MethodVisitor methodVisitor);

        /**
         * Gets all result handles that are used as input to this operation
         *
         * @return The result handles
         */
        abstract Set<ResultHandle> getInputResultHandles();

        /**
         * @return The incoming result handle that is first loaded into the stack, or null if this is not applicable
         */
        abstract ResultHandle getTopResultHandle();

        /**
         * @return The result handle that is created as a result of this operation
         */
        abstract ResultHandle getOutgoingResultHandle();

        public void findResultHandles(Set<ResultHandle> vc) {
        }
    }

    static class JumpOperation extends Operation {
        private final Label target;

        JumpOperation(final Label target) {
            this.target = target;
        }

        void writeBytecode(final MethodVisitor methodVisitor) {
            methodVisitor.visitJumpInsn(Opcodes.GOTO, target);
        }

        Set<ResultHandle> getInputResultHandles() {
            return Collections.emptySet();
        }

        ResultHandle getTopResultHandle() {
            return null;
        }

        ResultHandle getOutgoingResultHandle() {
            return null;
        }
    }

    class InvokeOperation extends Operation {
        final ResultHandle resultHandle;
        final MethodDescriptor descriptor;
        final ResultHandle object;
        final ResultHandle[] args;
        final boolean staticMethod;
        final boolean interfaceMethod;
        final boolean specialMethod;

        InvokeOperation(ResultHandle resultHandle, MethodDescriptor descriptor, ResultHandle object, ResultHandle[] args, boolean interfaceMethod, boolean specialMethod) {
            if (args.length != descriptor.getParameterTypes().length) {
                throw new RuntimeException("Wrong number of params " + Arrays.toString(descriptor.getParameterTypes()) + " vs " + Arrays.toString(args));
            }
            this.resultHandle = resultHandle;
            this.descriptor = descriptor;
            this.object = object;
            this.args = args.clone();
            this.interfaceMethod = interfaceMethod;
            this.specialMethod = specialMethod;
            this.staticMethod = false;
        }

        InvokeOperation(ResultHandle resultHandle, MethodDescriptor descriptor, ResultHandle[] args) {
            if (args.length != descriptor.getParameterTypes().length) {
                throw new RuntimeException("Wrong number of params " + Arrays.toString(descriptor.getParameterTypes()) + " vs " + Arrays.toString(args));
            }
            this.resultHandle = resultHandle;
            this.descriptor = descriptor;
            this.object = null;
            this.args = args.clone();
            this.staticMethod = true;
            this.interfaceMethod = false;
            this.specialMethod = false;
        }

        @Override
        public void writeBytecode(MethodVisitor methodVisitor) {
            if (object != null) {
                loadResultHandle(methodVisitor, object, BytecodeCreatorImpl.this, "L" + descriptor.getDeclaringClass() + ";", specialMethod);
            }
            for (int i = 0; i < args.length; ++i) {
                ResultHandle arg = args[i];
                loadResultHandle(methodVisitor, arg, BytecodeCreatorImpl.this, descriptor.getParameterTypes()[i]);
            }
            if (staticMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            } else if (interfaceMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), true);
            } else if (specialMethod) {
                methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            } else {
                methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            }
            if (resultHandle != null) {
                storeResultHandle(methodVisitor, resultHandle);
            }
        }

        @Override
        Set<ResultHandle> getInputResultHandles() {
            Set<ResultHandle> ret = new HashSet<>();
            if (object != null) {
                ret.add(object);
            }
            ret.addAll(Arrays.asList(args));
            return ret;
        }

        @Override
        ResultHandle getTopResultHandle() {
            if (object != null) {
                return object;
            }
            if (args.length > 0) {
                return args[0];
            }
            return null;
        }

        @Override
        ResultHandle getOutgoingResultHandle() {
            return resultHandle;
        }
    }

    Operation createNewInstanceOp(ResultHandle handle, MethodDescriptor descriptor, ResultHandle[] args) {
        return new NewInstanceOperation(handle, descriptor, args);
    }

    class NewInstanceOperation extends Operation {
        final ResultHandle resultHandle;
        final MethodDescriptor descriptor;
        final ResultHandle[] args;

        NewInstanceOperation(ResultHandle resultHandle, MethodDescriptor descriptor, ResultHandle[] args) {
            if (args.length != descriptor.getParameterTypes().length) {
                throw new RuntimeException("Wrong number of params " + Arrays.toString(descriptor.getParameterTypes()) + " vs " + Arrays.toString(args));
            }
            this.resultHandle = resultHandle;
            this.descriptor = descriptor;
            this.args = new ResultHandle[args.length];
            System.arraycopy(args, 0, this.args, 0, args.length);
        }

        @Override
        public void writeBytecode(MethodVisitor methodVisitor) {

            methodVisitor.visitTypeInsn(Opcodes.NEW, descriptor.getDeclaringClass());
            methodVisitor.visitInsn(Opcodes.DUP);
            for (int i = 0; i < args.length; ++i) {
                ResultHandle arg = args[i];
                loadResultHandle(methodVisitor, arg, BytecodeCreatorImpl.this, descriptor.getParameterTypes()[i]);
            }
            methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, descriptor.getDeclaringClass(), descriptor.getName(), descriptor.getDescriptor(), false);
            storeResultHandle(methodVisitor, resultHandle);
        }

        @Override
        Set<ResultHandle> getInputResultHandles() {
            return new HashSet<>(Arrays.asList(args));
        }

        @Override
        ResultHandle getTopResultHandle() {
            return null;
        }

        @Override
        ResultHandle getOutgoingResultHandle() {
            return resultHandle;
        }
    }

    class IfOperation extends Operation {
        private final int opcode;
        private final String opType;
        private final ResultHandle resultHandle;
        private final BytecodeCreatorImpl trueBranch;
        private final BytecodeCreatorImpl falseBranch;

        IfOperation(final int opcode, final String opType, final ResultHandle resultHandle, final BytecodeCreatorImpl trueBranch, final BytecodeCreatorImpl falseBranch) {
            this.opcode = opcode;
            this.opType = opType;
            this.resultHandle = resultHandle;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
        }

        @Override
        public void writeBytecode(MethodVisitor methodVisitor) {
            loadResultHandle(methodVisitor, resultHandle, BytecodeCreatorImpl.this, opType);
            methodVisitor.visitJumpInsn(opcode, trueBranch.getTop());
            falseBranch.writeOperations(methodVisitor);
            methodVisitor.visitJumpInsn(Opcodes.GOTO, trueBranch.getBottom());
            trueBranch.writeOperations(methodVisitor);
        }

        @Override
        Set<ResultHandle> getInputResultHandles() {
            return Collections.singleton(resultHandle);
        }

        @Override
        ResultHandle getTopResultHandle() {
            return resultHandle;
        }

        @Override
        ResultHandle getOutgoingResultHandle() {
            return null;
        }

        @Override
        public void findResultHandles(Set<ResultHandle> vc) {
            trueBranch.findActiveResultHandles(vc);
            falseBranch.findActiveResultHandles(vc);
        }
    }

    static class BlockOperation extends Operation {
        private final BytecodeCreatorImpl block;

        BlockOperation(final BytecodeCreatorImpl block) {
            this.block = block;
        }

        void writeBytecode(final MethodVisitor methodVisitor) {
            block.writeOperations(methodVisitor);
        }

        Set<ResultHandle> getInputResultHandles() {
            return Collections.emptySet();
        }

        ResultHandle getTopResultHandle() {
            return null;
        }

        ResultHandle getOutgoingResultHandle() {
            return null;
        }

        public void findResultHandles(final Set<ResultHandle> vc) {
            block.findActiveResultHandles(vc);
        }
    }
}
