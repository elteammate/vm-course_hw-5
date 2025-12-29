package space.elteammate.lama.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.builtin.ReadBuiltinNode;
import space.elteammate.lama.nodes.builtin.WriteBuiltinNode;
import space.elteammate.lama.nodes.cflow.DoWhileNode;
import space.elteammate.lama.nodes.cflow.ForLoopNode;
import space.elteammate.lama.nodes.cflow.IfThenElseNode;
import space.elteammate.lama.nodes.cflow.WhileDoNode;
import space.elteammate.lama.nodes.expr.SeqNode;
import space.elteammate.lama.nodes.literal.NoopNode;
import space.elteammate.lama.nodes.literal.NumNode;
import space.elteammate.lama.nodes.scope.*;
import space.elteammate.lama.types.ModuleObject;

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

        private record LookupGlobal(int slot) implements LookupResult {
        }

        private record LookupLocal(int slot) implements LookupResult {
        }

        private sealed interface ScopeItem {
        }

        private record ScopeBuiltin(Function<List<LamaNode>, LamaNode> builder) implements ScopeItem {
        }

        private record ScopeFrameSlot(int slot) implements ScopeItem {
        }

        private record ScopeGlobal(int slot) implements ScopeItem {
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
        int numGlobals;

        Telescope() {
            scopes = new ArrayList<>(8);
            scopes.add(new Scope(null));
            numGlobals = 0;
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
            if (scopes.size() <= 1) {
                throw new IllegalStateException("Can't add variables to first scope");
            } else if (scopes.size() == 2) {
                int slot = numGlobals;
                numGlobals++;
                scope.items.put(name, new ScopeGlobal(slot));
                return new LookupGlobal(slot);
            } else {
                int slot = scope.frame.addSlot(FrameSlotKind.Long, name, null);
                scope.items.put(name, new ScopeFrameSlot(slot));
                return new LookupLocal(slot);
            }
        }

        LookupResult lookup(String name) {
            for (int i = scopes.size() - 1; i >= 0; i--) {
                Scope scope = scopes.get(i);
                if (scope.items.containsKey(name)) {
                    ScopeItem item = scope.items.get(name);

                    int fi = i;
                    return switch (item) {
                        case ScopeBuiltin(var builder) -> new LookupBuiltin(builder);
                        case ScopeFrameSlot(var idx) when fi == 1 -> new LookupGlobal(idx);
                        case ScopeFrameSlot(var idx) -> new LookupLocal(idx);
                        case ScopeGlobal(var idx) -> new LookupGlobal(idx);
                        case null -> throw new IllegalStateException("Unknown scope item");
                    };
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
    public LamaNode visitScopedExpr(LamaParser.ScopedExprContext ctx) {
        if (ctx.definitions().isEmpty()) {
            return visit(ctx.expr());
        }

        telescope.pushScope();
        Supplier<LamaNode> init = processDefinitions(ctx.definitions());
        LamaNode expr = SeqNode.create(init.get(), visit(ctx.expr()));
        telescope.popScope();
        return parser.withSource(expr, ctx);
    }

    @Override
    public LamaNode visitParenthesized(LamaParser.ParenthesizedContext ctx) {
        return parser.withSource(visit(ctx.scoped()), ctx);

    }

    public ModuleObject processModule(LamaParser.ModuleContext ctx) {
        telescope.pushFrame();

        Supplier<LamaNode> init = processDefinitions(ctx.definitions());

        LamaNode expr = SeqNode.create(init.get(), visit(ctx.expr()));

        FrameDescriptor frame = telescope.popFrame();
        ModuleNode node = parser.withSource(
                new ModuleNode(telescope.numGlobals, expr),
                ctx
        );

        return new ModuleObject(node, frame);
    }

    @Override
    public LamaNode visitSimple(LamaParser.SimpleContext ctx) {
        return visit(ctx.simpleExpr());
    }

    @Override
    public LamaNode visitLookup(LamaParser.LookupContext ctx) {
        String name = ctx.IDENT().getText();
        Telescope.LookupResult lookup = telescope.lookup(name);

        return parser.withSource(generateLoad(lookup, name, ctx), ctx);
    }

    @Override
    public LamaNode visitAssignment(LamaParser.AssignmentContext ctx) {
        var lvalue = ctx.lvalue();
        var expr = visit(ctx.expr());
        if (lvalue instanceof LamaParser.LLookupContext varName) {
            Telescope.LookupResult lookup = telescope.lookup(varName.getText());
            return generateStore(lookup, lvalue.getText(), expr, ctx);
        } else {
            throw new ParsingException("Can't assign to that", source, ctx.start);
        }
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
            case Telescope.LookupLocal lookupLocal -> {
                throw new ParsingException("Can't call local variables yet", source, ctx.start);
            }
            case null ->
                throw new ParsingException("Name " + fnName + "is not defined", source, ctx.start);
        }
    }

    @Override
    public LamaNode visitSeq(LamaParser.SeqContext ctx) {
        LamaNode prev = visit(ctx.expr(0));
        LamaNode expr = visit(ctx.expr(1));
        return parser.withSource(SeqNode.create(prev, expr), ctx);
    }

    private LamaNode generateLoad(Telescope.LookupResult lookup, String name, ParserRuleContext ctx) {
        switch (lookup) {
            case Telescope.LookupBuiltin builtin ->
                    throw new ParsingException("Builtins can't be promoted to function objects", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> {
                return LoadGlobalNodeGen.create(slot);
            }
            case Telescope.LookupLocal(var slot) -> {
                return LoadLocalNodeGen.create(slot);
            }
            case null ->
                    throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        }
    }

    private LamaNode generateStore(Telescope.LookupResult lookup, String name, LamaNode value, ParserRuleContext ctx) {
        switch (lookup) {
            case Telescope.LookupBuiltin _ ->
                    throw new ParsingException("Builtins can't be written to", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> {
                return StoreGlobalNodeGen.create(value, slot);
            }
            case Telescope.LookupLocal(var slot) -> {
                return StoreLocalNodeGen.create(value, slot);
            }
            case null ->
                    throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        }
    }

    private Supplier<LamaNode> processDefinitions(List<LamaParser.DefinitionsContext> ctx) {
        Supplier<LamaNode> init = NoopNode::new;
        for (LamaParser.DefinitionsContext c : ctx) {
            Supplier<LamaNode> finalInit = init;
            init = () -> SeqNode.create(finalInit.get(), processDefinitions(c).get());
        }
        return init;
    }

    private Supplier<LamaNode> processDefinitions(LamaParser.DefinitionsContext ctx) {
        if (ctx instanceof LamaParser.VarDefinitionsContext c) {
            return processVarDefinitions(c);
        }
        throw new RuntimeException("Unknown context");
    }

    private Supplier<LamaNode> processVarDefinitions(LamaParser.VarDefinitionsContext ctx) {
        Supplier<LamaNode> init = NoopNode::new;
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            TerminalNode ident = ctx.IDENT(i);
            String name = ident.getText();
            var lookup = telescope.addVar(name);

            LamaParser.SimpleExprContext initializer = ctx.simpleExpr(i);
            if (initializer != null) {
                Supplier<LamaNode> finalInit = init;
                init = () -> SeqNode.create(finalInit.get(), parser.withSource(
                        generateStore(lookup, name, visit(initializer), ctx),
                        ctx
                ));
            }
        }
        return init;
    }

    @Override
    public LamaNode visitSkip(LamaParser.SkipContext ctx) {
        return parser.withSource(new NoopNode(), ctx);
    }

    @Override
    public LamaNode visitWhileDoLoop(LamaParser.WhileDoLoopContext ctx) {
        telescope.pushScope();
        Supplier<LamaNode> init = processDefinitions(ctx.definitions());
        LamaNode cond = SeqNode.create(init.get(), visit(ctx.cond));
        LamaNode body = visit(ctx.body);
        telescope.popScope();
        return parser.withSource(new WhileDoNode(cond, body), ctx);
    }

    @Override
    public LamaNode visitDoWhileLoop(LamaParser.DoWhileLoopContext ctx) {
        telescope.pushScope();
        Supplier<LamaNode> init = processDefinitions(ctx.definitions());
        LamaNode body = SeqNode.create(init.get(), visit(ctx.body));
        LamaNode cond = visit(ctx.cond);
        telescope.popScope();
        return parser.withSource(new DoWhileNode(body, cond), ctx);
    }

    @Override
    public LamaNode visitIfStmt(LamaParser.IfStmtContext ctx) {
        return visit(ctx.ifStmtMiddle());
    }

    @Override
    public LamaNode visitIfStmtMiddle(LamaParser.IfStmtMiddleContext ctx) {
        telescope.pushScope();
        Supplier<LamaNode> init = processDefinitions(ctx.definitions());
        LamaNode cond = SeqNode.create(init.get(), visit(ctx.cond));
        LamaNode then = visit(ctx.then);
        LamaNode else_ = visit(ctx.ifRest());
        telescope.popScope();
        return parser.withSource(new IfThenElseNode(cond, then, else_), ctx);
    }

    @Override
    public LamaNode visitIfRestEnd(LamaParser.IfRestEndContext ctx) {
        return parser.withSource(new NoopNode(), ctx);
    }

    @Override
    public LamaNode visitIfElseEnd(LamaParser.IfElseEndContext ctx) {
        return visit(ctx.else_);
    }

    @Override
    public LamaNode visitIfCont(LamaParser.IfContContext ctx) {
        return visit(ctx.ifStmtMiddle());
    }

    @Override
    public LamaNode visitForLoop(LamaParser.ForLoopContext ctx) {
        telescope.pushScope();
        Supplier<LamaNode> defInit = processDefinitions(ctx.definitions());
        LamaNode init = SeqNode.create(defInit.get(), visit(ctx.init));
        LamaNode cond = visit(ctx.cond);
        LamaNode step = visit(ctx.step);
        LamaNode body = visit(ctx.body);
        telescope.popScope();
        return parser.withSource(new ForLoopNode(init, cond, step, body), ctx);
    }
}
