package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ModuleResultObject;

public class ModuleNode extends LamaNode {
    private final int numGlobals;
    @Child LamaNode expr;

    public ModuleNode(int numGlobals, LamaNode expr) {
        this.numGlobals = numGlobals;
        this.expr = expr;
    }

    @Override
    public ModuleResultObject execute(VirtualFrame virtualFrame) {
        LamaContext context = LamaContext.get(this);
        context.initializeGlobals(numGlobals);
        this.expr.execute(virtualFrame);
        return ModuleResultObject.INSTANCE;
    }
}
