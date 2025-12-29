package space.elteammate.lama.nodes.scope;


import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.nodes.LamaNode;

@NodeField(name = "slot", type = int.class)
public abstract class LoadGlobal extends LamaNode {
    protected abstract int getSlot();

    @Specialization
    public Object fetchGlobal(
            @Bind LamaContext context
    ) {
        return context.getGlobal(getSlot());
    }
}
