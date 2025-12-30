package space.elteammate.lama.nodes.cflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import space.elteammate.lama.nodes.LamaNode;

public class WhileDoNode extends LamaNode {
    @Child
    private LamaNode condition;

    @Child
    private LamaNode body;

    private final ConditionProfile profile;

    public WhileDoNode(LamaNode condition, LamaNode body) {
        this.condition = condition;
        this.body = body;
        profile = ConditionProfile.create();
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        while (!profile.profile(condition.execute(virtualFrame).equals(0L))) {
            body.execute(virtualFrame);
        }
        return 0L;
    }
}
