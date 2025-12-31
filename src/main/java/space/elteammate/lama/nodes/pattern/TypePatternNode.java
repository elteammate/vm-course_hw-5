package space.elteammate.lama.nodes.pattern;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.MutableTruffleString;
import space.elteammate.lama.types.ClosureObject;

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
            return scrutinee instanceof ClosureObject;
        }
    }
}
