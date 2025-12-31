package space.elteammate.lama.nodes.scope;


import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ClosureObject;

@NodeField(name = "slot", type = int.class)
public abstract class LoadCapture extends LamaNode {
    protected abstract int getSlot();

    @Specialization
    protected Object readObject(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        ClosureObject closure = (ClosureObject) args[args.length - 1];
        return closure.captures()[getSlot()];
    }
}
