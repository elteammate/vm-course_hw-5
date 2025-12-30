package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import space.elteammate.lama.nodes.LamaBaseNode;

public abstract class BasePatternNode extends LamaBaseNode {
    public abstract boolean match(VirtualFrame frame, Object scrutinee);
}
