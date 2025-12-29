package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ModuleResultObject;

public class ModuleNode extends LamaNode {
    private final LamaContext.Descriptor descriptor;
    @Child LamaNode expr;

    public ModuleNode(LamaContext.Descriptor descriptor, LamaNode expr) {
        this.descriptor = descriptor;
        this.expr = expr;
    }

    @Override
    public ModuleResultObject execute(VirtualFrame virtualFrame) {
        LamaContext context = LamaContext.get(this);
        context.initialize(descriptor);
        this.expr.execute(virtualFrame);
        return ModuleResultObject.INSTANCE;
    }
}
