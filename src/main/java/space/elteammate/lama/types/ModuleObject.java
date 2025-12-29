package space.elteammate.lama.types;

import com.oracle.truffle.api.frame.FrameDescriptor;
import space.elteammate.lama.nodes.scope.ModuleNode;

public record ModuleObject(
        ModuleNode moduleNode,
        FrameDescriptor entrypointFrameDescriptor
) {

}
