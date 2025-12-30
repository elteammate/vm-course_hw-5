package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.types.LamaList;

@NodeInfo(shortName = ":")
public abstract class ConsNode extends InfixNode {
    @Specialization
    public static LamaList addToEmpty(
            Object lhs,
            long rhs,
            @Bind Node node
    ) {
        if (rhs != 0) {
            throw LamaException.create("List construction must have either list, or 0 as tail", node);
        }
        return new LamaList(lhs, null);
    }

    @Specialization
    public static LamaList addToNotEmpty(
            Object lhs,
            LamaList rhs
    ) {
        return new LamaList(lhs, rhs);
    }

    @Fallback
    public static Object typeError(Object left, Object right, @Bind Node node) {
        throw LamaException.typeError(node, ":", left, right);
    }
}
