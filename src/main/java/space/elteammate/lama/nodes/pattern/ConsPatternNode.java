package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.types.LamaList;
import space.elteammate.lama.types.Sexp;

public final class ConsPatternNode extends BasePatternNode {
    @Child
    private BasePatternNode head;

    @Child
    private BasePatternNode tail;

    private final ConditionProfile typeProfile;
    private final ConditionProfile headProfile;

    public ConsPatternNode(BasePatternNode head, BasePatternNode tail) {
        this.head = head;
        this.tail = tail;
        typeProfile = ConditionProfile.create();
        headProfile = ConditionProfile.create();
    }

    @Override
    @ExplodeLoop
    public boolean match(VirtualFrame frame, Object scrutinee) {
        if (!typeProfile.profile(scrutinee instanceof LamaList)) {
            return false;
        }

        assert scrutinee instanceof LamaList;
        LamaList list = (LamaList) scrutinee;
        if (!headProfile.profile(head.match(frame, list.head()))) {
            return false;
        }
        return tail.match(frame, list.tail() == null ? 0L : list.tail());
    }
}
