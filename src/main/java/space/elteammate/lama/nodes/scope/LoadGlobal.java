package space.elteammate.lama.nodes.scope;


import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.nodes.LamaNode;

public final class LoadGlobal extends LamaNode {
    int slot;

    public LoadGlobal(int slot) {
        this.slot = slot;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        return LamaContext.get(this).getGlobal(slot);
    }
}
