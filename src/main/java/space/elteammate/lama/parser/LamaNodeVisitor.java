package space.elteammate.lama.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.builtin.ReadBuiltinNode;
import space.elteammate.lama.nodes.builtin.WriteBuiltinNode;
import space.elteammate.lama.nodes.expr.SeqNode;
import space.elteammate.lama.nodes.expr.SeqNodeGen;
import space.elteammate.lama.nodes.literal.NumNode;
import space.elteammate.lama.nodes.scope.LoadGlobal;
import space.elteammate.lama.nodes.scope.ModuleNode;
import space.elteammate.lama.nodes.scope.StoreGlobalNodeGen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LamaNodeVisitor extends LamaBaseVisitor<LamaNode> {
    LamaNodeParser parser;
    private final Source source;
    private final Telescope telescope;

    private static final class Telescope {
        private sealed interface LookupResult {
        }

        private record LookupBuiltin(Function<List<LamaNode>, LamaNode> builder) implements LookupResult {
        }

        private record LookupGlobal(int idx) implements LookupResult {
        }

        private sealed interface ScopeItem {
        }

        private record ScopeBuiltin(Function<List<LamaNode>, LamaNode> builder) implements ScopeItem {
        }

        private record ScopeFrameSlot(int idx) implements ScopeItem {
        }

        private record Scope(
            HashMap<String, ScopeItem> items,
            FrameDescriptor.Builder frame
        ) {
            Scope(FrameDescriptor.Builder frame) {
                this(new HashMap<>(), frame);
            }
        }

        List<Scope> scopes;

        Telescope() {
            scopes = new ArrayList<>(8);
            scopes.add(new Scope(null));
        }

        FrameDescriptor.Builder topFrame() {
            return scopes.getLast().frame;
        }

        void pushScope() {
            scopes.add(new Scope(topFrame()));
        }

        void popScope() {
            scopes.removeLast();
        }

        void pushFrame() {
            scopes.add(new Scope(FrameDescriptor.newBuilder()));
        }

        FrameDescriptor popFrame() {
            return scopes.removeLast().frame.build();
        }

        void addBuiltin(String name, Function<List<LamaNode>, LamaNode> builder) {
            scopes.getFirst().items.put(name, new ScopeBuiltin(builder));
        }

        LookupResult addVar(String name) {
            Scope scope = scopes.getLast();
            int slot = scope.frame.addSlot(FrameSlotKind.Object, name, null);
            scope.items.put(name, new ScopeFrameSlot(slot));
            if (scopes.size() <= 1) {
                throw new IllegalStateException("Can't add variables to first scope");
            } else if (scopes.size() == 2) {
                return new LookupGlobal(slot);
            }
            throw new RuntimeException("TODO");
        }

        LookupResult lookup(String name) {
            for (int i = scopes.size() - 1; i >= 0; i--) {
                Scope scope = scopes.get(i);
                if (scope.items.containsKey(name)) {
                    ScopeItem item = scope.items.get(name);

                    if (i == 0 && item instanceof ScopeBuiltin(var builder)) {
                        return new LookupBuiltin(builder);
                    } else if (i == 1 && item instanceof ScopeFrameSlot(var idx)) {
                        return new LookupGlobal(idx);
                    } else {
                        throw new IllegalStateException("Unknown scope item");
                    }
                }
            }
            return null;
        }
    }

    LamaNodeVisitor(LamaNodeParser parser, Source source) {
        this.parser = parser;
        this.source = source;
        this.telescope = new Telescope();

        telescope.addBuiltin("read", ReadBuiltinNode::build);
        telescope.addBuiltin("write", WriteBuiltinNode::build);
    }

    @Override
    public LamaNode visitNumber(LamaParser.NumberContext ctx) {
        return parser.withSource(new NumNode(Long.parseLong(ctx.NUM().getText())), ctx);
    }

    @Override
    public LamaNode visitRawExpr(LamaParser.RawExprContext ctx) {
        if (ctx.OP().isEmpty()) {
            return visit(ctx.primary(0));
        }
        List<LamaNode> exprs = ctx.primary().stream().map(this::visit).toList();
        List<String> operators = ctx.OP().stream().map(ParseTree::getText).toList();
        return parser.parseExpr(exprs, operators, ctx.OP());
    }

    @Override
    public LamaNode visitParenthesized(LamaParser.ParenthesizedContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public LamaNode visitModule(LamaParser.ModuleContext ctx) {
        telescope.pushFrame();

        List<Supplier<LamaNode>> inits = processDefinitions(ctx.definitions());

        List<LamaNode> initExprs = new ArrayList<>();
        for (Supplier<LamaNode> init : inits) {
            initExprs.add(init.get());
        }

        LamaNode expr = SeqNode.fold(initExprs, visit(ctx.expr()));

        FrameDescriptor frame = telescope.popFrame();
        return parser.withSource(
                new ModuleNode(frame.getNumberOfSlots(), expr),
                ctx
        );
    }

    @Override
    public LamaNode visitLookup(LamaParser.LookupContext ctx) {
        String name = ctx.IDENT().getText();
        Telescope.LookupResult lookup = telescope.lookup(name);

        return parser.withSource(generateLoad(lookup, name, ctx), ctx);
    }

    @Override
    public LamaNode visitDirectCall(LamaParser.DirectCallContext ctx) {
        String fnName = ctx.IDENT().getText();
        List<LamaNode> args = ctx.args.stream().map(this::visit).toList();

        Telescope.LookupResult lookup = telescope.lookup(fnName);

        switch (lookup) {
            case Telescope.LookupBuiltin lookupBuiltin -> {
                return parser.withSource(
                        lookupBuiltin.builder().apply(args),
                        ctx
                );
            }
            case Telescope.LookupGlobal lookupGlobal -> {
                throw new ParsingException("Can't call global variables yet", source, ctx.start);
            }
            case null ->
                throw new ParsingException("Name " + fnName + "is not defined", source, ctx.start);
        }
    }

    @Override
    public LamaNode visitSeq(LamaParser.SeqContext ctx) {
        LamaNode prev = visit(ctx.expr(0));
        LamaNode expr = visit(ctx.expr(1));
        return parser.withSource(SeqNodeGen.create(prev, expr), ctx);
    }

    private LamaNode generateLoad(Telescope.LookupResult lookup, String name, LamaParser.LookupContext ctx) {
        switch (lookup) {
            case Telescope.LookupBuiltin builtin ->
                    throw new ParsingException("Builtins can't be promoted to function objects", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> {
                return new LoadGlobal(slot);
            }
            case null ->
                    throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        }
    }

    private LamaNode generateStore(Telescope.LookupResult lookup, String name, LamaNode value, LamaParser.VarDefinitionsContext ctx) {
        switch (lookup) {
            case Telescope.LookupBuiltin _ ->
                    throw new ParsingException("Builtins can't be written to", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> {
                return StoreGlobalNodeGen.create(value, slot);
            }
            case null ->
                    throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        }
    }

    private List<Supplier<LamaNode>> processDefinitions(List<LamaParser.DefinitionsContext> ctx) {
        List<Supplier<LamaNode>> inits = new ArrayList<>();
        for (LamaParser.DefinitionsContext c : ctx) {
            inits.addAll(processDefinitions(c));
        }
        return inits;
    }

    private List<Supplier<LamaNode>> processDefinitions(LamaParser.DefinitionsContext ctx) {
        switch (ctx) {
            case LamaParser.VarDefinitionsContext c -> {
                return processVarDefinitions(c);
            }
            default ->
                throw new RuntimeException("Unknown context");
        }
    }

    private List<Supplier<LamaNode>> processVarDefinitions(LamaParser.VarDefinitionsContext ctx) {
        List<Supplier<LamaNode>> inits = new ArrayList<>();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            TerminalNode ident = ctx.IDENT(i);
            String name = ident.getText();
            var lookup = telescope.addVar(name);

            LamaParser.ExprContext initializer = ctx.expr(i);
            if (initializer != null) {
                inits.add(() -> parser.withSource(
                        generateStore(lookup, name, visit(initializer), ctx),
                        ctx
                ));
            }
        }
        return inits;
    }
}
