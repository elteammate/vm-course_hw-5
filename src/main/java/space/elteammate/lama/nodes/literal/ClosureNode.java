package space.elteammate.lama.nodes.literal;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ClosureObject;
import space.elteammate.lama.types.LamaCallTarget;

public class ClosureNode extends LamaNode {
    private final int fnSlot;
    @CompilerDirectives.CompilationFinal
    private LamaCallTarget fn;
    @Node.Children
    private LamaNode[] captures;

    public ClosureNode(int fnSlot, LamaCallTarget fn, LamaNode[] captures) {
        this.fnSlot = fnSlot;
        this.fn = fn;
        this.captures = captures;
    }

    @Override
    @ExplodeLoop
    public ClosureObject execute(VirtualFrame frame) {
        Object[] captures = new Object[this.captures.length];
        for (int i = 0; i < captures.length; i++) {
            captures[i] = this.captures[i].execute(frame);
        }

        if (fn == null) {
            CompilerDirectives.transferToInterpreter();
            LamaContext ctx = LamaContext.get(this);
            fn = ctx.getFunction(fnSlot);
        }

        return new ClosureObject(fn, captures);
    }
}
