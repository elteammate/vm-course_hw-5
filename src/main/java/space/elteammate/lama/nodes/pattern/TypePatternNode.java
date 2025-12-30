package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.types.FunctionObject;
import space.elteammate.lama.types.Sexp;

public class TypePatternNode {
    public static class Box extends BasePatternNode {
        @Override
        public boolean match(VirtualFrame frame, Object scrutinee) {
            return !(scrutinee instanceof Long);
        }
    }

    public static class Val extends BasePatternNode {
        @Override
        public boolean match(VirtualFrame frame, Object scrutinee) {
            return scrutinee instanceof Long;
        }
    }

    public static class Str extends BasePatternNode {
        @Override
        public boolean match(VirtualFrame frame, Object scrutinee) {
            return scrutinee instanceof MutableTruffleString;
        }
    }

    public static class Array extends BasePatternNode {
        @Override
        public boolean match(VirtualFrame frame, Object scrutinee) {
            return scrutinee instanceof Object[];
        }
    }

    public static class Sexp extends BasePatternNode {
        @Override
        public boolean match(VirtualFrame frame, Object scrutinee) {
            return scrutinee instanceof space.elteammate.lama.types.Sexp[];
        }
    }

    public static class Fun extends BasePatternNode {
        @Override
        public boolean match(VirtualFrame frame, Object scrutinee) {
            return scrutinee instanceof FunctionObject;
        }
    }
}
