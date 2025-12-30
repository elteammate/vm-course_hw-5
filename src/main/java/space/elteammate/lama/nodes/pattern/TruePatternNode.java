package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

public final class TruePatternNode extends BasePatternNode {
    public TruePatternNode() {}

    @Override
    public boolean match(VirtualFrame frame, Object scrutinee) {
        return !scrutinee.equals(0L);
    }
}
