package space.elteammate.lama;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStreams;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.LamaRootNode;
import space.elteammate.lama.nodes.scope.ModuleNode;
import space.elteammate.lama.parser.LamaNodeParser;

import java.io.*;

public class TruffleLamaMain {
    static void main(String[] args) throws IOException {
        if (args.length == 0) {
            runFile(new InputStreamReader(System.in), "<stdin>");
        } else if (args.length == 1) {
            runFile(new FileReader(args[0]), args[0]);
        }
    }

    private static void runFile(Reader reader, String filename) throws IOException {
        LamaLanguage lang = new LamaLanguage();
        Source source = Source.newBuilder(LamaLanguage.ID, reader, filename).build();
        LamaNodeParser parser = new LamaNodeParser(source);
        ModuleNode node = (ModuleNode)parser.parse(CharStreams.fromReader(source.getReader()));
        LamaRootNode root = new LamaRootNode(lang, node);
        root.getCallTarget().call();
    }
}
