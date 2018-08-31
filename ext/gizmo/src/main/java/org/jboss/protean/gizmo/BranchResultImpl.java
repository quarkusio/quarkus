package org.jboss.protean.gizmo;

class BranchResultImpl implements BranchResult {
    private final BytecodeCreatorImpl owner;

    private final BytecodeCreator trueBranch;
    private final BytecodeCreator falseBranch;
    private final BytecodeCreatorImpl underlyingTrueBranch;
    private final BytecodeCreatorImpl underlyingFalseBranch;

    public BranchResultImpl(BytecodeCreatorImpl owner, BytecodeCreator trueBranch, BytecodeCreator falseBranch, BytecodeCreatorImpl underlyingTrueBranch, BytecodeCreatorImpl underlyingFalseBranch) {
        this.owner = owner;
        this.trueBranch = trueBranch;
        this.falseBranch = falseBranch;
        this.underlyingTrueBranch = underlyingTrueBranch;
        this.underlyingFalseBranch = underlyingFalseBranch;
    }

    @Override
    public BytecodeCreator trueBranch() {
        return trueBranch;
    }

    @Override
    public BytecodeCreator falseBranch() {
        return falseBranch;
    }

    @Override
    public ResultHandle mergeBranches(ResultHandle trueResult, ResultHandle falseResult) {
            if(!trueResult.getType().equals(falseResult.getType())) {
                throw new RuntimeException("Result handles must be of the same type " + trueResult.getType() + " " + falseResult.getType());
            }
            ResultHandle rs = new ResultHandle(trueResult.getType(), owner);
            underlyingTrueBranch.assign(rs, trueResult);
            underlyingFalseBranch.assign(rs, falseResult);
            return rs;
    }
}
