package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.LamaCallTarget;

public final class DirectCallNode extends LamaNode {
    @CompilerDirectives.CompilationFinal
    private LamaCallTarget fn;

    private final int fnSlot;

    @Children
    private final LamaNode[] callArguments;

    public DirectCallNode(int fnSlot, LamaCallTarget fn, LamaNode[] callArguments) {
        this.callArguments = callArguments;
        this.fnSlot = fnSlot;
        this.fn = fn;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object[] argumentValues = new Object[this.callArguments.length];
        for (int i = 0; i < this.callArguments.length; i++) {
            argumentValues[i] = this.callArguments[i].execute(frame);
        }

        if (fn == null) {
            CompilerDirectives.transferToInterpreter();
            LamaContext ctx = LamaContext.get(this);
            fn = ctx.getFunction(fnSlot);
        }

        if (fn.getNumArgs() != argumentValues.length) {
            CompilerDirectives.transferToInterpreter();
            throw LamaException.create(
                    "Mismatched number of arguments. Given " +
                            argumentValues.length + ", expected " +
                            fn.getNumArgs(),
                    this
            );
        }
        return fn.call(argumentValues);
    }
}
