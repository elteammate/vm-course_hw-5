package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import space.elteammate.lama.LamaException;

@NodeInfo(shortName = "==")
public abstract class EqualsNode extends InfixNode {
    @Specialization
    public static long doNums(long lhs, long rhs) {
        return (lhs == rhs) ? 1 : 0;
    }

    @Fallback
    public static Object typeError(Object left, Object right, @Bind Node node) {
        throw LamaException.typeError(node, "==", left, right);
    }
}
