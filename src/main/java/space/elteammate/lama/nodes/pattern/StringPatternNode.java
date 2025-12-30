package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;

public final class StringPatternNode extends BasePatternNode {
    private final TruffleString pattern;

    private final ConditionProfile typeProfile;

    public StringPatternNode(TruffleString pattern) {
        this.pattern = pattern;
        typeProfile = ConditionProfile.create();
    }

    @Override
    public boolean match(VirtualFrame frame, Object scrutinee) {
        if (!typeProfile.profile(scrutinee instanceof MutableTruffleString)) {
            return false;
        }

        assert scrutinee instanceof MutableTruffleString;
        MutableTruffleString s = (MutableTruffleString) scrutinee;

        return s.equalsUncached(pattern, TruffleString.Encoding.BYTES);
    }
}
