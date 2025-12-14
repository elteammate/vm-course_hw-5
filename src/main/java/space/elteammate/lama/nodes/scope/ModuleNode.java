package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;

public class ModuleNode extends LamaNode {
    FrameDescriptor frameDescriptor;

    @Child LamaNode expr;

    public ModuleNode(FrameDescriptor frameDescriptor, LamaNode expr) {
        this.frameDescriptor = frameDescriptor;
        this.expr = expr;
    }

    @Override
    public Void execute(VirtualFrame virtualFrame) {
        this.expr.execute(virtualFrame);
        return null;
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }
}
