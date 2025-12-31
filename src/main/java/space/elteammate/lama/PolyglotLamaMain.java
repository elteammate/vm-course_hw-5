package space.elteammate.lama;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

public final class PolyglotLamaMain {

    private static final String LAMA = "lama";

    static void main(String[] args) throws IOException {
        Source source;
        String file = null;
        boolean launcherOutput = true;
        for (String arg : args) {
            if (file == null) {
                file = arg;
            }
        }

        if (file == null) {
            source = Source.newBuilder(LAMA, new InputStreamReader(System.in), "<stdin>").interactive(!launcherOutput).build();
        } else {
            source = Source.newBuilder(LAMA, new File(file)).interactive(!launcherOutput).build();
        }

        System.exit(executeSource(source, System.in, System.out, launcherOutput));
    }

    public static int executeSource(Source source, InputStream in, PrintStream out, boolean launcherOutput) {
        Context context;
        PrintStream err = System.err;
        try {
            context = Context
                    .newBuilder(LAMA)
                    .in(in)
                    .out(out)
                    .option("engine.Compilation", "true")
                    // .option("engine.TraceCompilationDetails", "true")
                    .option("engine.CompilationFailureAction", "Print")
                    .option("engine.CompileImmediately", "true")
                    .allowAllAccess(true)
                    .build();
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            return 1;
        }

        if (launcherOutput) {
            out.println("== running on " + context.getEngine());
        }

        try {
            Value result = context.eval(source);
            if (launcherOutput && !result.isNull()) {
                out.println(result.toString());
            }
            return 0;
        } catch (PolyglotException ex) {
            if (ex.isInternalError()) {
                ex.printStackTrace();
            } else {
                err.println(ex.getMessage());
            }
            return 1;
        } finally {
            context.close();
        }
    }
}
