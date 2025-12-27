package space.elteammate.lama;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node;

import java.io.*;

@Bind.DefaultExpression("get($node)")
public class LamaContext {
    private final BufferedReader input;
    private final BufferedWriter output;
    @CompilerDirectives.CompilationFinal
    private Object[] globals;
    private static final TruffleLanguage.ContextReference<LamaContext> REFERENCE =
            TruffleLanguage.ContextReference.create(LamaLanguage.class);

    public LamaContext(TruffleLanguage.Env env) {
        this.input = new BufferedReader(new InputStreamReader(env.in()));
        this.output = new BufferedWriter(new OutputStreamWriter(env.out()));
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

    public void initializeGlobals(int numGlobals) {
        globals = new Object[numGlobals];
        for (int i = 0; i < numGlobals; ++i) {
            globals[i] = 0L;
        }
    }

    public Object getGlobal(int slot) {
        return globals[slot];
    }

    public void setGlobal(int slot, Object value) {
        globals[slot]  = value;
    }
}
