package space.elteammate.lama;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

public class LamaContext {
    private final FrameDescriptor globalFrameDescriptor;
    private MaterializedFrame globalFrame = null;

    public LamaContext() {
        this.globalFrameDescriptor = new FrameDescriptor();
        VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(null,
                this.globalFrameDescriptor);
    }

    public FrameDescriptor getGlobalFrameDescriptor() {
        return globalFrameDescriptor;
    }

    public MaterializedFrame getGlobalFrame() {
        return globalFrame;
    }
}
