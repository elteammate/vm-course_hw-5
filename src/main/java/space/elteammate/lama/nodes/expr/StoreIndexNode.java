package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.types.Sexp;

@NodeChild("collection")
@NodeChild("index")
@NodeChild("value")
public abstract class StoreIndexNode extends LamaNode {
    @Child
    MutableTruffleString.WriteByteNode writeByteNode;

    StoreIndexNode() {
        super();
        writeByteNode = MutableTruffleString.WriteByteNode.create();
    }

    @Specialization
    protected long storeToString(MutableTruffleString collection, long index, long value) {
        if (index < 0 || index > collection.byteLength(TruffleString.Encoding.BYTES)) {
            throw LamaException.create("String index out of bounds", this);
        }
        writeByteNode.execute(collection, (int) index, (byte) value, TruffleString.Encoding.BYTES);
        return value;
    }

    @Specialization
    protected Object storeToArray(Object[] collection, long index, Object value) {
        if (index < 0 || index > collection.length) {
            throw LamaException.create("Array index out of bounds", this);
        }
        collection[(int) index] = value;
        return value;
    }

    @Specialization
    protected Object storeToSexp(Sexp collection, long index, Object value) {
        if (index < 0 || index > collection.items().length) {
            throw LamaException.create("Array index out of bounds", this);
        }
        collection.items()[(int) index] = value;
        return value;
    }

    @Fallback
    public static Object typeError(Object collection, Object index, Object value, @Bind Node node) {
        throw LamaException.create("Cannot index", node);
    }
}
