package io.quarkus.rest.server.test.simple;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

@Provider
public class IllegalClassExceptionMapper implements ExceptionMapper<IncompatibleClassChangeError> {

    public class MethodFindingClassVisitor extends ClassVisitor {

        private String method;
        private Textifier textifier;
        private PrintWriter writer;

        public MethodFindingClassVisitor(String method, PrintWriter writer) {
            super(Opcodes.ASM8);
            this.method = method;
            this.writer = writer;
            textifier = new Textifier();
        }

        @Override
        public void visitEnd() {
            textifier.visitClassEnd();
            textifier.print(writer);
            writer.flush();
            super.visitEnd();
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (method.equals(name)) {
                System.err.println("Match for method " + name + " " + descriptor);
                Printer methodPrinter = textifier.visitMethod(access, name, descriptor, signature, exceptions);
                return new TraceMethodVisitor(mv, methodPrinter);
            }
            return null;
        }
    }

    @Override
    public Response toResponse(IncompatibleClassChangeError exception) {
        String message = exception.getMessage();
        Pattern pattern = Pattern.compile("Method (([a-zA-Z0-9_]+\\.)+)[a-zA-Z0-9_]+\\(.*");
        Matcher matcher = pattern.matcher(message);
        exception.printStackTrace();
        if (matcher.matches()) {
            String classname = matcher.group(1);
            classname = classname.substring(0, classname.length() - 1);
            System.err.println("IncompatibleClassChangeError for: " + classname + " dumping its bytecode:");
            dumpClass(classname, null);
            StackTraceElement stackTraceElement = exception.getStackTrace()[0];
            System.err.println("The call that triggered the error is at: " + stackTraceElement + " so dumping its bytecode:");
            dumpClass(stackTraceElement.getClassName(), stackTraceElement.getMethodName());
        }
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        return Response.serverError().entity(writer.toString()).build();
    }

    private void dumpClass(String classname, String method) {
        InputStream bytes = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(classname.replace('.', '/') + ".class");
        try {
            ClassReader cr = new ClassReader(bytes);
            PrintWriter writer = new PrintWriter(System.err);
            if (method == null)
                cr.accept(new TraceClassVisitor(writer), 0);
            else
                cr.accept(new MethodFindingClassVisitor(method, writer), 0);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
