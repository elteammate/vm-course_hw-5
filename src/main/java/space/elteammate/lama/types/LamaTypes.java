package space.elteammate.lama.types;

import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;

@TypeSystem({long.class, TruffleString.class})
public abstract class LamaTypes {
}
