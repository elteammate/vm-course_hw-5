package space.elteammate.lama.parser;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.Token;

public class ParsingException extends RuntimeException {
    private Source source;
    private Token token;

    public ParsingException(String message, Source source, Token token) {
        super(message);
        this.source = source;
        this.token = token;
    }

    public Source getSource() {
        return source;
    }

    public Token getToken() {
        return token;
    }
}
