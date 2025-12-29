package space.elteammate.lama;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStreams;
import space.elteammate.lama.nodes.LamaRootNode;
import space.elteammate.lama.parser.LamaNodeParser;
import space.elteammate.lama.parser.ParsingException;
import space.elteammate.lama.types.ModuleObject;

import java.io.IOException;

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
        return new LamaContext(env);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        try {
            Source source = request.getSource();
            LamaNodeParser parser = new LamaNodeParser(source);
            ModuleObject module = parser.parse(CharStreams.fromReader(source.getReader()));
            LamaRootNode root = new LamaRootNode(this, module.moduleNode(), module.entrypointFrameDescriptor());
            return root.getCallTarget();
        } catch (ParsingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void finalizeContext(LamaContext context) {
    }
}
