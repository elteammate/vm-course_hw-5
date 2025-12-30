package space.elteammate.lama.types;

import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.MutableTruffleString;
import com.oracle.truffle.api.strings.TruffleString;

@TypeSystem({
        long.class,
        Sexp.class,
        LamaList.class,
        MutableTruffleString.class,
        Object[].class,
})
public abstract class LamaTypes {
}
