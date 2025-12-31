package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;

@NodeChild("expr")
@NodeField(name = "slot", type = int.class)
public abstract class StoreArg extends LamaNode {
    protected abstract int getSlot();

    @Specialization
    public Object doStore(
            VirtualFrame frame,
            Object value
    ) {
        frame.getArguments()[getSlot()] = value;
        return value;
    }
}
