package space.elteammate.lama.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import space.elteammate.lama.LamaLanguage;
import space.elteammate.lama.nodes.scope.ModuleNode;

public class LamaRootNode extends RootNode {
    @SuppressWarnings("FieldMayBeFinal")
    @Child private ModuleNode module;

    public LamaRootNode(LamaLanguage lang, ModuleNode module) {
        super(lang, module.getFrameDescriptor());
        this.module = module;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return module.execute(frame);
    }
}
