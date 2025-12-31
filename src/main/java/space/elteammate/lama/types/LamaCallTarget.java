package space.elteammate.lama.types;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.nodes.Node;

public class LamaCallTarget implements CallTarget {
    private final int numArgs;
    private final CallTarget inner;

    public LamaCallTarget(int numArgs, CallTarget inner) {
        this.numArgs = numArgs;
        this.inner = inner;
    }

    @Override
    public Object call(Object... arguments) {
        return inner.call(arguments);
    }

    @Override
    public Object call(Node location, Object... arguments) {
        return inner.call(location, arguments);
    }

    public int getNumArgs() {
        return numArgs;
    }
}
