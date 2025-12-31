package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ClosureObject;

@NodeChild("expr")
@NodeField(name = "slot", type = int.class)
public abstract class StoreCapture extends LamaNode {
    protected abstract int getSlot();

    @Specialization
    public Object doStore(
            VirtualFrame frame,
            Object value
    ) {
        Object[] args = frame.getArguments();
        ClosureObject closure = (ClosureObject) args[args.length - 1];
        closure.captures()[getSlot()] = value;
        return value;
    }
}
