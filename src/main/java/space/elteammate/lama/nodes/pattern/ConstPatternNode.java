package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class ConstPatternNode extends BasePatternNode {
    private final Object value;

    public ConstPatternNode(Object value) {
        this.value = value;
    }

    @Override
    @ExplodeLoop
    public boolean match(VirtualFrame frame, Object scrutinee) {
        return scrutinee.equals(value);
    }
}
