package space.elteammate.lama.nodes.cflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import space.elteammate.lama.nodes.LamaNode;

public class ForLoopNode extends LamaNode {
    @Child
    private LamaNode init;

    @Child
    private LamaNode condition;

    @Child
    private LamaNode step;

    @Child
    private LamaNode body;

    private final ConditionProfile profile;

    public ForLoopNode(LamaNode init, LamaNode condition, LamaNode step, LamaNode body) {
        this.init = init;
        this.condition = condition;
        this.step = step;
        this.body = body;
        profile = ConditionProfile.create();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        for (
                init.execute(frame);
                profile.profile(!profile.profile(condition.execute(frame).equals(0L)));
                step.execute(frame)
        ) {
            body.execute(frame);
        }
        return 0L;
    }
}
