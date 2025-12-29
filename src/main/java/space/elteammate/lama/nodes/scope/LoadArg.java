package space.elteammate.lama.nodes.scope;


import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaNode;

@NodeField(name = "slot", type = int.class)
public abstract class LoadArg extends LamaNode {
    protected abstract int getSlot();

    @Specialization
    protected Object readObject(VirtualFrame frame) {
        return frame.getArguments()[getSlot()];
    }
}
