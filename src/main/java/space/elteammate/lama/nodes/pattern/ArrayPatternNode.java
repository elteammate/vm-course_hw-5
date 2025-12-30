package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;

public final class ArrayPatternNode extends BasePatternNode {
    @Children
    private BasePatternNode[] items;

    private final ConditionProfile typeProfile;
    private final ConditionProfile shapeProfile;
    private final ConditionProfile valuesProfile;

    public ArrayPatternNode(BasePatternNode[] items) {
        this.items = items;
        typeProfile = ConditionProfile.create();
        shapeProfile = ConditionProfile.create();
        valuesProfile = ConditionProfile.create();
    }

    @Override
    @ExplodeLoop
    public boolean match(VirtualFrame frame, Object scrutinee) {
        if (!typeProfile.profile(scrutinee instanceof Object[])) {
            return false;
        }

        assert scrutinee instanceof Object[];
        Object[] array = (Object[]) scrutinee;
        if (!shapeProfile.profile(array.length == items.length)) {
            return false;
        }

        for (int i = 0; i < items.length; i++) {
            if (!valuesProfile.profile(items[i].match(frame, array[i]))) {
                return false;
            }
        }

        return true;
    }
}
