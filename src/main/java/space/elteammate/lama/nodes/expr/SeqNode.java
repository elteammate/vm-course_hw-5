package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;

import java.util.List;


@NodeChild("prev")
@NodeChild("expr")
public abstract class SeqNode extends LamaNode {
    @Specialization
    public Object doSequence(Object ignoredPrev, Object expr) {
        return expr;
    }

    public static LamaNode fold(List<LamaNode> exprs, LamaNode last) {
        for (int i = exprs.size() - 1; i >= 0; i--) {
            last = SeqNodeGen.create(exprs.get(i), last);
        }
        return last;
    }
}
