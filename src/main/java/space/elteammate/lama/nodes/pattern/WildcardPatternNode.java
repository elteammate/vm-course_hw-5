package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class WildcardPatternNode extends BasePatternNode {
    @Override
    public boolean match(VirtualFrame frame, Object scrutinee) {
        return true;
    }
}
