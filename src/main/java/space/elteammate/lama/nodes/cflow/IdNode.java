package space.elteammate.lama.nodes.cflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import space.elteammate.lama.nodes.LamaNode;

public final class IdNode extends LamaNode {
    @Node.Child
    public LamaNode node;

    public IdNode(LamaNode node) {
        this.node = node;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        return node.execute(virtualFrame);
    }
}
