package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ClosureObject;

public final class ClosureCallNode extends LamaNode {
    @Child
    private LamaNode closure;

    @Children
    private LamaNode[] callArguments;

    public ClosureCallNode(LamaNode closure, LamaNode[] callArguments) {
        this.closure = closure;
        this.callArguments = callArguments;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object rawClosure = this.closure.execute(frame);
        if (!(rawClosure instanceof ClosureObject closure)) {
            throw LamaException.create("Object is not callable", this);
        }
        Object[] argumentValues = new Object[callArguments.length + 1];
        for (int i = 0; i < callArguments.length; i++) {
            argumentValues[i] = callArguments[i].execute(frame);
        }
        argumentValues[callArguments.length] = closure;

        if (closure.callTarget().getNumArgs() != callArguments.length) {
            throw LamaException.create(
                    "Mismatched number of arguments calling closure. Given " +
                            callArguments.length + ", expected " +
                            closure.callTarget().getNumArgs(),
                    this
            );
        }

        return closure.callTarget().call(argumentValues);
    }
}
