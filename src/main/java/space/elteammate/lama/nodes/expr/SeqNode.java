package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.literal.NoopNode;

import java.util.List;


public class SeqNode extends LamaNode {
    @Child
    private LamaNode fst;
    @Child
    private LamaNode snd;

    SeqNode(LamaNode fst, LamaNode snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public static LamaNode create(LamaNode fst, LamaNode snd) {
        if (fst instanceof NoopNode) {
            return snd;
        } else if (fst instanceof SeqNode seqFst && seqFst.snd instanceof NoopNode) {
            return new SeqNode(seqFst.fst, snd);
        } else {
            return new SeqNode(fst, snd);
        }
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        this.fst.execute(virtualFrame);
        return this.snd.execute(virtualFrame);
    }

    public static LamaNode fold(List<LamaNode> exprs, LamaNode last) {
        for (int i = exprs.size() - 1; i >= 0; i--) {
            last = SeqNode.create(exprs.get(i), last);
        }
        return last;
    }
}
