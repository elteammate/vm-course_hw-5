package space.elteammate.lama.nodes.cflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import space.elteammate.lama.nodes.LamaNode;

public class DoWhileNode extends LamaNode {
    @Child
    private LamaNode body;

    @Child
    private LamaNode condition;

    private final ConditionProfile profile;

    public DoWhileNode(LamaNode body, LamaNode condition) {
        this.body = body;
        this.condition = condition;
        profile = ConditionProfile.create();
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        do {
            body.execute(virtualFrame);
        } while (!profile.profile(condition.execute(virtualFrame).equals(0L)));
        return 0L;
    }
}
