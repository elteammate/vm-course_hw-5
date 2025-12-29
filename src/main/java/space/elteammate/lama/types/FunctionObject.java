package space.elteammate.lama.types;

import com.oracle.truffle.api.CallTarget;

public class FunctionObject {
    private final CallTarget callTarget;
    private final int numParams;

    public FunctionObject(CallTarget callTarget, int numParams) {
        this.callTarget = callTarget;
        this.numParams = numParams;
    }

    public CallTarget getCallTarget() {
        return callTarget;
    }

    public int getNumParams() {
        return numParams;
    }
}
