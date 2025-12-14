package space.elteammate.lama;

import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(
        id = LamaLanguage.ID,
        name = LamaLanguage.NAME,
        defaultMimeType = LamaLanguage.MIME_TYPE,
        characterMimeTypes = LamaLanguage.MIME_TYPE
)
public class LamaLanguage extends TruffleLanguage<LamaContext> {
    public static final String NAME = "Lama";
    public static final String ID = "lama";
    public static final String MIME_TYPE = "application/x-lama";

    @Override
    protected LamaContext createContext(Env env) {
        return new LamaContext();
    }
}
