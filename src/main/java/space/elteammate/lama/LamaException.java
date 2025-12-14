package space.elteammate.lama;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

public class LamaException extends AbstractTruffleException {
    LamaException(String message, Node location) {
        super(message, location);
    }

    @CompilerDirectives.TruffleBoundary
    public static AbstractTruffleException create(String message, Node location) {
        return new LamaException(message, location);
    }

    @CompilerDirectives.TruffleBoundary
    public static AbstractTruffleException typeError(Node location, String operationName, Object... values) {
        StringBuilder result = new StringBuilder();
        result.append("Type error");

        AbstractTruffleException ex = LamaException.create("", location);
        if (location != null) {
            SourceSection ss = ex.getEncapsulatingSourceSection();
            if (ss != null && ss.isAvailable()) {
                result.append(" at ").append(ss.getSource().getName()).append(" line ").append(ss.getStartLine()).append(" col ").append(ss.getStartColumn());
            }
        }

        result.append(": operation");
        if (location != null) {
            result.append(" \"").append(operationName).append("\"");
        }

        result.append(" not defined for");

        String sep = " ";
        for (Object value : values) {
            result.append(sep);
            sep = ", ";
            result.append(value);
        }
        return LamaException.create(result.toString(), location);
    }
}
