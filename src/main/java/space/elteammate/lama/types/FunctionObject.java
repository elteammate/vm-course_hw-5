package space.elteammate.lama.types;

import com.oracle.truffle.api.CallTarget;

public class FunctionObject {
    public final CallTarget callTarget;

    public FunctionObject(CallTarget callTarget) {
        this.callTarget = callTarget;
    }
}
