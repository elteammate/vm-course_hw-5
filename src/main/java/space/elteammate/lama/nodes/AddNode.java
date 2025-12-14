package space.elteammate.lama.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import space.elteammate.lama.LamaException;

@NodeInfo(shortName = "+")
@NodeChild("lhs")
@NodeChild("rhs")
public abstract class AddNode extends LamaNode {
    @Specialization
    public static long doNums(long lhs, long rhs) {
        return lhs + rhs;
    }

    @Fallback
    public static Object typeError(Object left, Object right, @Bind Node node) {
        throw LamaException.typeError(node, "+", left, right);
    }
}
