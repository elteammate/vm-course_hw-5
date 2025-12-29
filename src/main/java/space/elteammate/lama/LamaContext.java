package space.elteammate.lama;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.nodes.Node;
import space.elteammate.lama.types.FunctionObject;

import java.io.*;

@Bind.DefaultExpression("get($node)")
public class LamaContext {
    private final BufferedReader input;
    private final BufferedWriter output;
    @CompilerDirectives.CompilationFinal
    private Object[] globals;
    private FunctionObject[] functionObjects;
    private static final TruffleLanguage.ContextReference<LamaContext> REFERENCE =
            TruffleLanguage.ContextReference.create(LamaLanguage.class);

    public record Descriptor(
           int numGlobals,
           FunctionObject[] functionObjects
    ) {
    }

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

    public void initialize(Descriptor descriptor) {
        globals = new Object[descriptor.numGlobals];
        for (int i = 0; i < descriptor.numGlobals; ++i) {
            globals[i] = 0L;
        }

        functionObjects = descriptor.functionObjects();
    }

    public Object getGlobal(int slot) {
        return globals[slot];
    }

    public void setGlobal(int slot, Object value) {
        globals[slot]  = value;
    }

    public FunctionObject getFunction(int fnSlot) {
        return functionObjects[fnSlot];
    }
}
