package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.types.Sexp;

public final class SexpPatternNode extends BasePatternNode {
    private final TruffleString tag;

    @Children
    private BasePatternNode[] items;

    private final ConditionProfile typeProfile;
    private final ConditionProfile shapeProfile;
    private final ConditionProfile valuesProfile;

    public SexpPatternNode(TruffleString tag, BasePatternNode[] items) {
        this.tag = tag;
        this.items = items;
        typeProfile = ConditionProfile.create();
        shapeProfile = ConditionProfile.create();
        valuesProfile = ConditionProfile.create();
    }

    @Override
    @ExplodeLoop
    public boolean match(VirtualFrame frame, Object scrutinee) {
        if (!typeProfile.profile(scrutinee instanceof Sexp)) {
            return false;
        }

        assert scrutinee instanceof Sexp;
        Sexp sexp = (Sexp) scrutinee;
        if (!shapeProfile.profile(sexp.tag().equals(tag) && sexp.items().length == items.length)) {
            return false;
        }

        for (int i = 0; i < items.length; i++) {
            if (!valuesProfile.profile(items[i].match(frame, sexp.items()[i]))) {
                return false;
            }
        }

        return true;
    }
}
