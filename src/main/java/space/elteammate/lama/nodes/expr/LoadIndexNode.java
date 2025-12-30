package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;

@NodeChild("collection")
@NodeChild("index")
public abstract class LoadIndexNode extends LamaNode {
    @Child
    TruffleString.ReadByteNode readByteNode;

    LoadIndexNode() {
        super();
        readByteNode = TruffleString.ReadByteNode.create();
    }

    @Specialization
    protected long loadFromString(MutableTruffleString collection, long index) {
        if (index < 0 || index > collection.byteLength(TruffleString.Encoding.BYTES)) {
            throw LamaException.create("String index out of bounds", this);
        }
        return readByteNode.execute(collection, (int)index, TruffleString.Encoding.BYTES);
    }

    @Fallback
    public static Object typeError(Object collection, Object index, @Bind Node node) {
        throw LamaException.create("Cannot index", node);
    }
}
