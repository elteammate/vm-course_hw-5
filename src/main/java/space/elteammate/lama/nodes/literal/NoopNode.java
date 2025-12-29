package space.elteammate.lama.nodes.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;

public class NoopNode extends LamaNode {
    @Override
    public long executeNum(VirtualFrame frame) {
        return 0L;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeNum(frame);
    }
}
