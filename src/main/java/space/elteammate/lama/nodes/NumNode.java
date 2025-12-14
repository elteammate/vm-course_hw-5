package space.elteammate.lama.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

public class NumNode extends LamaNode {
    private final long value;

    public NumNode(long value) {
        this.value = value;
    }

    @Override
    public long executeNum(VirtualFrame frame) {
        return value;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeNum(frame);
    }
}
