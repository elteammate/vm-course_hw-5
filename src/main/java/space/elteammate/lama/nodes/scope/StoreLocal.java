package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;

@NodeChild("expr")
@NodeField(name = "slot", type = int.class)
public abstract class StoreLocal extends LamaNode {
    protected abstract int getSlot();

    @Specialization(guards = {"frame.isLong(getSlot())"})
    public long doStoreLong(
            VirtualFrame frame,
            long value
    ) {
        frame.setLong(getSlot(), value);
        return value;
    }

    @Specialization(replaces = {"doStoreLong"})
    public Object doStore(
            VirtualFrame frame,
            Object value
    ) {
        frame.getFrameDescriptor().setSlotKind(getSlot(), FrameSlotKind.Object);
        frame.setObject(getSlot(), value);
        return value;
    }
}
