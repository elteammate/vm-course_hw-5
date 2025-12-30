package space.elteammate.lama.nodes.builtin;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;

import java.io.IOException;
import java.util.List;

@NodeInfo(shortName = "read")
public abstract class ReadBuiltinNode extends AbstractBuiltinNode {
    @Specialization
    @CompilerDirectives.TruffleBoundary
    public long read(
            @Bind LamaContext context
    ) {
        try {
            context.getOutput().write(" > ");
            context.getOutput().flush();
            String line = context.getInput().readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            throw LamaException.create("Failed to read from environment", this);
        } catch (NumberFormatException e) {
            throw LamaException.create("Read value is not a number", this);
        }
    }

    public static ReadBuiltinNode build(List<LamaNode> args) {
        if (!args.isEmpty()) {
            throw LamaException.create("Read does not accept arguments", args.getFirst());
        }
        return ReadBuiltinNodeGen.create();
    }
}
