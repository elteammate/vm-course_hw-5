package space.elteammate.lama.nodes.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.Sexp;

public final class SexpNode extends LamaNode {
    private final TruffleString tag;

    @Children
    private LamaNode[] items;

    public SexpNode(TruffleString tag, LamaNode[] items) {
        this.tag = tag;
        this.items = items;
    }

    @Override
    @ExplodeLoop
    public Sexp execute(VirtualFrame frame) {
        Object[] items = new Object[this.items.length];
        for (int i = 0; i < this.items.length; i++) {
            items[i] = this.items[i].execute(frame);
        }
        return new Sexp(tag, items);
    }
}
