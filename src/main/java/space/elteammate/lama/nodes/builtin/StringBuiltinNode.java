package space.elteammate.lama.nodes.builtin;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString.Encoding;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.ClosureObject;
import space.elteammate.lama.types.LamaList;
import space.elteammate.lama.types.Sexp;

import java.util.List;

@NodeInfo(shortName = "string")
@NodeChild(value = "arg", type = LamaNode.class)
public abstract class StringBuiltinNode extends AbstractBuiltinNode {
    private static MutableTruffleString convert(String s) {
        byte[] bytes = s.getBytes();
        return MutableTruffleString.fromByteArrayUncached(
                bytes,
                0,
                bytes.length,
                Encoding.BYTES,
                false
        );
    }

    private static void appendString(StringBuilder sb, Object o) {
        if (o instanceof Long l) {
            sb.append(l);
        } else if (o instanceof MutableTruffleString s) {
            sb.append('"');
            sb.append(s);
            sb.append('"');
        } else if (o instanceof Object[] a) {
            sb.append('[');
            for (int i = 0; i < a.length; i++) {
                appendString(sb, a[i]);
                if (i < a.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append(']');
        } else if (o instanceof LamaList l) {
            sb.append('{');
            appendString(sb, l.head());
            l = l.tail();
            while (l != null) {
                sb.append(", ");
                appendString(sb, l.head());
                l = l.tail();
            }
            sb.append('}');
        } else if (o instanceof Sexp s) {
            sb.append(s.tag());
            if (s.items().length > 0) {
                sb.append(" (");
                for (int i = 0; i < s.items().length; i++) {
                    appendString(sb, s.items()[i]);
                    if (i < s.items().length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append(')');
            }
        } else if (o instanceof ClosureObject) {
            sb.append("<closure TODO>");
        } else {
            sb.append("*** invalid data ***");
        }
    }

    private MutableTruffleString stringify(Object arg) {
        StringBuilder sb = new StringBuilder();
        appendString(sb, arg);
        return convert(sb.toString());
    }

    @Specialization
    public MutableTruffleString stringNum(long arg) {
        return stringify(arg);
    }

    @Specialization
    public MutableTruffleString stringString(MutableTruffleString arg) {
        return stringify(arg);
    }

    @Specialization
    public MutableTruffleString stringArray(Object[] arg) {
        return stringify(arg);
    }

    @Specialization
    public MutableTruffleString stringSexp(Sexp arg) {
        return stringify(arg);
    }

    @Specialization
    public MutableTruffleString stringClosure(ClosureObject arg) {
        return stringify(arg);
    }

    @Fallback
    public static Object typeError(Object ignored, @Bind Node node) {
        throw LamaException.create("Cannot convert to string", node);
    }

    public static StringBuiltinNode build(List<LamaNode> args) {
        if (args.size() != 1) {
            throw LamaException.typeError(null, "string", args);
        } else {
            return StringBuiltinNodeGen.create(args.getFirst());
        }
    }
}
