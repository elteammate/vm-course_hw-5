package space.elteammate.lama.types;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
public class ModuleResultObject implements TruffleObject {
    public static final ModuleResultObject INSTANCE = new ModuleResultObject();

    private ModuleResultObject() {
    }

    @ExportMessage
    boolean isNull() {
        return true;
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return this.toString();
    }

    @Override
    public String toString() {
        return "<module>";
    }
}
