package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;

public final class AsPatternNode extends BasePatternNode {
    private final int slot;

    @Child
    private BasePatternNode patt;

    public AsPatternNode(int slot, BasePatternNode patt) {
        this.slot = slot;
        this.patt = patt;
    }

    @Override
    public boolean match(VirtualFrame frame, Object scrutinee) {
        frame.setObject(slot, scrutinee);
        return patt.match(frame, scrutinee);
    }
}
