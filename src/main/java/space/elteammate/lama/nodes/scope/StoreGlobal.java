package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.nodes.LamaNode;

@NodeChild("expr")
@NodeField(name = "slot", type = int.class)
public abstract class StoreGlobal extends LamaNode {
    protected abstract int getSlot();

    @Specialization
    public Object doStore(
            Object value,
            @Bind LamaContext ctx
    ) {
        ctx.setGlobal(getSlot(), value);
        return value;
    }
}
