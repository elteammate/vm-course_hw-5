package space.elteammate.lama;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStreams;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.parser.LamaNodeParser;

import java.io.*;

public class TruffleLamaMain {
    static void main(String[] args) throws IOException {
        assert args.length < 2 : "Mumbler only accepts 1 or 0 files";
        if (args.length == 0) {
            repl();
        } else {
            runFile(args[0]);
        }
    }

    private static void repl() throws IOException {
        Console console = System.console();
        while (true) {
            String data = console.readLine("> ");
            if (data == null) {
                break;
            }
            LamaContext context = new LamaContext();
            Source source = Source.newBuilder(LamaLanguage.ID, data, "<console>").build();

            LamaNode node = LamaNodeParser.parse(CharStreams.fromReader(source.getReader()));

            Object result = node.execute(context.getGlobalFrame());

            System.out.println(result);
        }
    }

    private static void runFile(String filename) throws IOException {
        Reader reader = new FileReader(filename);
        Source source = Source.newBuilder(LamaLanguage.ID, reader, filename).build();
        LamaContext context = new LamaContext();
        LamaNode node = LamaNodeParser.parse(CharStreams.fromReader(source.getReader()));
        Object result = node.execute(context.getGlobalFrame());
        System.out.println(result);
    }
}
