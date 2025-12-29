package space.elteammate.lama.nodes.scope;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import space.elteammate.lama.LamaLanguage;
import space.elteammate.lama.nodes.LamaNode;

public class LamaRootNode extends RootNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child private LamaNode node;

    public LamaRootNode(LamaLanguage lang, LamaNode node, FrameDescriptor frameDescriptor) {
        super(lang, frameDescriptor);
        this.node = node;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return node.execute(frame);
    }
}
