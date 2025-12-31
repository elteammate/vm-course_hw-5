package space.elteammate.lama;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import space.elteammate.lama.types.LamaCallTarget;

import java.io.*;

@Bind.DefaultExpression("get($node)")
public class LamaContext {
    private final BufferedReader input;
    private final BufferedWriter output;
    @CompilerDirectives.CompilationFinal
    private Object[] globals;
    private LamaCallTarget[] functions;
    private LamaLanguage lang;
    private static final TruffleLanguage.ContextReference<LamaContext> REFERENCE =
            TruffleLanguage.ContextReference.create(LamaLanguage.class);

    public LamaLanguage getLang() {
        return lang;
    }

    public record Descriptor(
           int numGlobals,
           LamaCallTarget[] functions
    ) {
    }

    public LamaContext(TruffleLanguage.Env env, LamaLanguage lang) {
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new BufferedWriter(new OutputStreamWriter(env.out()));
        this.lang = lang;
    }

    public static LamaContext get(Node node) {
        return REFERENCE.get(node);
    }

    public BufferedReader getInput() {
        return input;
    }

    public BufferedWriter getOutput() {
        return output;
    }

    public void initialize(Descriptor descriptor) {
        globals = new Object[descriptor.numGlobals];
        for (int i = 0; i < descriptor.numGlobals; ++i) {
            globals[i] = 0L;
        }

        functions = descriptor.functions();
    }

    public Object getGlobal(int slot) {
        return globals[slot];
    }

    public void setGlobal(int slot, Object value) {
        globals[slot]  = value;
    }

    public LamaCallTarget getFunction(int fnSlot) {
        return functions[fnSlot];
    }
}
