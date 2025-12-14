package space.elteammate.lama.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import space.elteammate.lama.LamaLanguage;
import space.elteammate.lama.types.LamaTypes;
import space.elteammate.lama.types.LamaTypesGen;

@TypeSystemReference(LamaTypes.class)
@NodeInfo(language = LamaLanguage.ID)
public abstract class LamaNode extends Node {
    public abstract Object execute(VirtualFrame virtualFrame);

    public long executeNum(VirtualFrame virtualFrame)
            throws UnexpectedResultException {
        return LamaTypesGen.expectLong(this.execute(virtualFrame));
    }
}
