package space.elteammate.lama.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;

import java.io.IOException;
import java.util.List;

@NodeInfo(shortName = "write")
@NodeChild(value = "arg", type = LamaNode.class)
public abstract class WriteBuiltinNode extends AbstractBuiltinNode {
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public long write(
            long arg,
            @Bind LamaContext context
    ) {
        try {
            context.getOutput().write(arg + "\n");
            context.getOutput().flush();
        } catch (IOException e) {
            throw LamaException.create("Failed to write to environment", this);
        } catch (NumberFormatException e) {
            throw LamaException.create("Write value is not a number", this);
        }
        return 0;
    }

    public static WriteBuiltinNode build(List<LamaNode> args) {
        if (args.size() != 1) {
            throw LamaException.typeError(null, "write", args);
        } else {
            return WriteBuiltinNodeGen.create(args.getFirst());
        }
    }
}
