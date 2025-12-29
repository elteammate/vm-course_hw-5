package space.elteammate.lama.nodes.cflow;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import space.elteammate.lama.nodes.LamaNode;

public class IfThenElseNode extends LamaNode {
    @Node.Child
    private LamaNode condition;

    @Node.Child
    private LamaNode then;

    @Node.Child
    private LamaNode else_;

    private final ConditionProfile profile;

    public IfThenElseNode(LamaNode condition, LamaNode then, LamaNode else_) {
        this.condition = condition;
        this.then = then;
        this.else_ = else_;
        profile = ConditionProfile.create();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (!profile.profile(condition.execute(frame).equals(0L))) {
            return then.execute(frame);
        } else {
            return else_.execute(frame);
        }
    }
}
