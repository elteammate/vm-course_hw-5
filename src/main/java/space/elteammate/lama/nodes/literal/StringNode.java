package space.elteammate.lama.nodes.literal;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;
import space.elteammate.lama.nodes.LamaNode;

public class StringNode extends LamaNode {
    private final byte[] value;

    @Child
    private MutableTruffleString.FromByteArrayNode conversion;

    public StringNode(byte[] value) {
        this.value = value;
        conversion = MutableTruffleString.FromByteArrayNode.create();
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return conversion.execute(
                value,
                0,
                value.length,
                TruffleString.Encoding.BYTES,
                true
        );
    }
}
