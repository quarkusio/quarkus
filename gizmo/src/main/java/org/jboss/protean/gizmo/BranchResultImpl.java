package org.jboss.protean.gizmo;

class BranchResultImpl implements BranchResult {
    private final BytecodeCreator trueBranch;
    private final BytecodeCreator falseBranch;

    public BranchResultImpl(BytecodeCreator trueBranch, BytecodeCreator falseBranch) {
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
    }

    @Override
    public BytecodeCreator trueBranch() {
        return trueBranch;
    }

    @Override
    public BytecodeCreator falseBranch() {
        return falseBranch;
    }
}
