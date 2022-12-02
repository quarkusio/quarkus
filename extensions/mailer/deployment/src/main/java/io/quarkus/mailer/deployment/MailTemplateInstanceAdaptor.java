package io.quarkus.mailer.deployment;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkus.mailer.MailTemplate.MailTemplateInstance;
import io.quarkus.mailer.runtime.MailTemplateProducer;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.deployment.CheckedTemplateAdapter;

public class MailTemplateInstanceAdaptor implements CheckedTemplateAdapter {

    @Override
    public String templateInstanceBinaryName() {
        return MailTemplateInstance.class.getName().replace('.', '/');
    }

    @Override
    public void convertTemplateInstance(MethodVisitor mv) {
        // we have a TemplateInstance on the stack, we need to turn it into a MailTemplateInstance
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, MailTemplateProducer.class.getName().replace('.', '/'),
                "getMailTemplateInstance",
                "(L" + TemplateInstance.class.getName().replace('.', '/') + ";)L"
                        + MailTemplateInstance.class.getName().replace('.', '/') + ";",
                false);
    }

}
