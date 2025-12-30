package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class BindingPatternNode extends BasePatternNode {
    private final int slot;

    public BindingPatternNode(int slot) {
        this.slot = slot;
    }

    @Override
    public boolean match(VirtualFrame frame, Object scrutinee) {
        frame.setObject(slot, scrutinee);
        return true;
    }
}
