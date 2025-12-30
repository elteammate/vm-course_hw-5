package space.elteammate.lama.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import space.elteammate.lama.types.LamaTypes;
import space.elteammate.lama.types.LamaTypesGen;

@TypeSystemReference(LamaTypes.class)
public abstract class LamaNode extends LamaBaseNode {
    public abstract Object execute(VirtualFrame virtualFrame);

    public long executeNum(VirtualFrame virtualFrame)
            throws UnexpectedResultException {
        return LamaTypesGen.expectLong(this.execute(virtualFrame));
    }
}
