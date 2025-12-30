package space.elteammate.lama.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;

import java.io.IOException;
import java.util.List;

@NodeInfo(shortName = "length")
@NodeChild(value = "arg", type = LamaNode.class)
public abstract class LengthBuiltinNode extends AbstractBuiltinNode {
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public long length(MutableTruffleString arg) {
        return arg.byteLength(TruffleString.Encoding.BYTES);
    }

    @Fallback
    public static Object typeError(Object collection, @Bind Node node) {
        throw LamaException.create("Cannot calculate length", node);
    }

    public static LengthBuiltinNode build(List<LamaNode> args) {
        if (args.size() != 1) {
            throw LamaException.typeError(null, "length", args);
        } else {
            return LengthBuiltinNodeGen.create(args.getFirst());
        }
    }
}
