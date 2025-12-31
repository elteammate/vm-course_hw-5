package space.elteammate.lama.nodes.scope;


import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ClosureObject;

public abstract class LoadSelf extends LamaNode {
    @Specialization
    protected ClosureObject readObject(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        return (ClosureObject) args[args.length - 1];
    }
}
