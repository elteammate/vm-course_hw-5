package space.elteammate.lama.parser;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.Token;

public class ParsingException extends RuntimeException {
    public ParsingException(String message, Source source, Token token) {
        super(message);
    }
}
