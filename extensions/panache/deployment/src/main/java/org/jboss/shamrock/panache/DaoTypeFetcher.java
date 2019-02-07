package org.jboss.shamrock.panache;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

class DaoTypeFetcher extends SignatureVisitor {

    String foundType;
    private String daoBinaryName;

    public DaoTypeFetcher(String daoBinaryName) {
        super(Opcodes.ASM6);
        this.daoBinaryName = daoBinaryName;
    }

    @Override
    public SignatureVisitor visitInterface() {
        return new SignatureVisitor(Opcodes.ASM6) {
            private boolean recordNextType;

            @Override
            public void visitClassType(String name) {
                if(recordNextType) {
                    foundType = name;
                }else if(name.equals(daoBinaryName)) {
                    recordNextType = true;
                }
                super.visitClassType(name);
            }
        };
    }
}