package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.dsl.NodeChild;
import space.elteammate.lama.nodes.LamaNode;

@NodeChild("lhs")
@NodeChild("rhs")
public abstract class InfixNode extends LamaNode {
}
