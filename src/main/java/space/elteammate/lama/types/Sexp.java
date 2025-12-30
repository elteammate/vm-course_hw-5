package space.elteammate.lama.types;

import com.oracle.truffle.api.strings.TruffleString;

public record Sexp(TruffleString tag, Object[] items) {
}
