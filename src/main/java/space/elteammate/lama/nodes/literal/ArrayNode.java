package space.elteammate.lama.nodes.literal;


import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import space.elteammate.lama.nodes.LamaNode;

public final class ArrayNode extends LamaNode {
    @Children
    private LamaNode[] items;

    public ArrayNode(LamaNode[] items) {
        this.items = items;
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame frame) {
        Object[] result = new Object[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = items[i].execute(frame);
        }
        return result;
    }
}
