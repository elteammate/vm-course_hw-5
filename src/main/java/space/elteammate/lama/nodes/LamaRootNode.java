package space.elteammate.lama.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import space.elteammate.lama.LamaLanguage;

public class LamaRootNode extends RootNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child private LamaNode module;

    public LamaRootNode(LamaLanguage lang, LamaNode module,
                        FrameDescriptor frameDescriptor) {
        super(lang, frameDescriptor);
        this.module = module;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return module.execute(frame);
    }
}
