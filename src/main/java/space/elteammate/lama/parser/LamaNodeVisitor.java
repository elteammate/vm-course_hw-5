package space.elteammate.lama.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.strings.TruffleString;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import space.elteammate.lama.LamaContext;
import space.elteammate.lama.LamaLanguage;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.builtin.LengthBuiltinNode;
import space.elteammate.lama.nodes.builtin.ReadBuiltinNode;
import space.elteammate.lama.nodes.builtin.StringBuiltinNode;
import space.elteammate.lama.nodes.builtin.WriteBuiltinNode;
import space.elteammate.lama.nodes.cflow.*;
import space.elteammate.lama.nodes.expr.*;
import space.elteammate.lama.nodes.literal.*;
import space.elteammate.lama.nodes.pattern.*;
import space.elteammate.lama.nodes.scope.*;
import space.elteammate.lama.types.LamaCallTarget;
import space.elteammate.lama.types.ModuleObject;
import space.elteammate.lama.util.CachedSupplier;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LamaNodeVisitor extends LamaBaseVisitor<LamaNode> {
    private final LamaLanguage lang;
    private final LamaNodeParser parser;
    private final Source source;
    private final Telescope telescope;
    private final List<LamaCallTarget> functions;

    @Override
    public LamaNode visitErrorNode(ErrorNode node) {
        throw new ParsingException("Failed to parse", source, node.getSymbol());
    }

    private static final class Telescope {
        private sealed interface LookupResult {}

        private sealed interface StaticLookupResult extends LookupResult {}

        private record LookupBuiltin(Function<List<LamaNode>, LamaNode> builder) implements StaticLookupResult {}

        private record LookupGlobal(int slot) implements StaticLookupResult {}

        private record LookupLocal(int slot) implements LookupResult {}

        private record LookupArg(int slot) implements LookupResult {}

        private record LookupCapture(int slot) implements LookupResult {}

        private record LookupFunction(int fnSlot) implements StaticLookupResult {}

        private record LookupSelf() implements LookupResult {}

        private record LookupLazyClosure(Supplier<LazyClosure> cl) implements LookupResult {}

        private record LazyClosure(
                LookupResult[] captures,
                int fnSlot
        ) {}

        private enum ScopeType {
            BUILTIN,
            GLOBAL,
            LOCAL,
            ARG,
            CAPTURES,
        }

        private record Scope(
                HashMap<String, LookupResult> items,
                ScopeType type
        ) {
            Scope(ScopeType type) {
                this(new HashMap<>(), type);
            }
        }

        private record Frame(
                AtomicInteger numParams,
                List<Scope> scopes,
                List<LookupResult> captures,
                // used if closure is optimized to function
                List<IdNode> selfUsages,
                List<IdNode> selfCalls,
                FrameDescriptor.Builder desc
        ) {
            Frame() {
                this(
                        new AtomicInteger(0),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        FrameDescriptor.newBuilder()
                );
            }

            LookupResult lookup(String name) {
                int i = scopes.size() - 1;
                for (; i >= 0; i--) {
                    Scope scope = scopes.get(i);
                    if (scope.items.containsKey(name)) {
                        return scope.items.get(name);
                    }
                }
                return null;
            }

            void pushScope(ScopeType type) {
                scopes.add(new Scope(type));
            }

            void popScope() {
                scopes.removeLast();
            }

            Frame withScope(ScopeType type) {
                pushScope(type);
                return this;
            }
        }

        int numGlobals;
        private final List<Frame> frames;
        private final Scope builtinScope;

        Telescope() {
            frames = new ArrayList<>();
            frames.add(new Frame()
                    .withScope(ScopeType.BUILTIN)
                    .withScope(ScopeType.GLOBAL)
            );
            numGlobals = 0;
            builtinScope = frames.getFirst().scopes.getFirst();
        }

        boolean isGlobal() {
            return frames.size() == 1;
        }

        Frame topFrame() {
            return frames.getLast();
        }

        int frameIndex() {
            return frames.size() - 1;
        }

        Scope topScope() {
            return topFrame().scopes.getLast();
        }

        void pushScope(ScopeType type) {
            topFrame().pushScope(type);
        }

        void popScope() {
            topFrame().popScope();
        }

        void pushFrame() {
            frames.add(new Frame()
                    .withScope(ScopeType.CAPTURES)
                    .withScope(ScopeType.ARG)
            );
        }

        Frame popFrame() {
            return frames.removeLast();
        }

        void addBuiltin(String name, Function<List<LamaNode>, LamaNode> builder) {
            builtinScope.items.put(name, new LookupBuiltin(builder));
        }

        LookupResult addVar(String name) {
            Scope scope = topScope();
            if (scope.type == ScopeType.GLOBAL) {
                int slot = numGlobals;
                numGlobals++;
                LookupGlobal lookup = new LookupGlobal(slot);
                scope.items.put(name, lookup);
                return lookup;
            } else if (scope.type == ScopeType.LOCAL) {
                int slot = topFrame().desc.addSlot(FrameSlotKind.Long, name, null);
                LookupLocal lookup = new LookupLocal(slot);
                scope.items.put(name, lookup);
                return lookup;
            } else {
                throw new IllegalStateException("Variables can only be added to global or local scope");
            }
        }

        void addFunction(String name, int fnSlot) {
            Scope scope = topScope();
            assert scope.type == ScopeType.GLOBAL;
            scope.items.put(name, new LookupFunction(fnSlot));
        }

        void addSelf(String name) {
            Scope scope = topFrame().scopes.getFirst();
            assert scope.type == ScopeType.CAPTURES;
            scope.items.put(name, new LookupSelf());
        }

        void addClosure(String name, Supplier<LazyClosure> cl) {
            Scope scope = topScope();
            assert scope.type == ScopeType.LOCAL || scope.type == ScopeType.GLOBAL;
            scope.items.put(name, new LookupLazyClosure(cl));
        }

        void addParam(String name) {
            Scope scope = topScope();
            assert scope.type == ScopeType.ARG;
            int slot = topFrame().numParams.getAndIncrement();
            scope.items.put(name, new LookupArg(slot));
        }

        LookupResult lookup(String name) {
            return lookup(name, frames.size() - 1);
        }

        LookupResult lookup(String name, int f) {
            Frame frame = frames.get(f);
            LookupResult lookup = frame.lookup(name);
            if (lookup != null) return lookup;
            if (f == 0) return null;
            lookup = lookup(name, f - 1);
            if (lookup == null) return null;
            if (lookup instanceof StaticLookupResult) return lookup;
            lookup = captureFromPrev(frame, lookup);
            frame.scopes.getFirst().items.put(name, lookup);
            return lookup;
        }

        LookupResult captureFromPrev(Frame inner, LookupResult c) {
            inner.captures.add(c);
            int slot = inner.captures.indexOf(c);
            if (slot == -1) {
                slot = inner.captures.size();
                inner.captures.add(c);
            }
            return new LookupCapture(slot);
        }
    }

    LamaNodeVisitor(LamaLanguage lang, LamaNodeParser parser, Source source) {
        this.lang = lang;
        this.parser = parser;
        this.source = source;
        this.telescope = new Telescope();
        this.functions = new ArrayList<>();

        telescope.addBuiltin("read", ReadBuiltinNode::build);
        telescope.addBuiltin("write", WriteBuiltinNode::build);
        telescope.addBuiltin("length", LengthBuiltinNode::build);
        telescope.addBuiltin("string", StringBuiltinNode::build);
    }

    private long parseNumber(Token t) {
        try {
            return Long.parseLong(t.getText());
        } catch (NumberFormatException e) {
            throw new ParsingException("Not a number", source, t);
        }
    }

    private char parseChar(Token t) {
        String text = t.getText();
        String content = text.substring(1, text.length() - 1);
        if (content.startsWith("\\")) {
            if (content.length() != 2) {
                throw new ParsingException("Invalid char escape sequence in literal " + text, source, t);
            }
            return switch (content.charAt(1)) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case '\'' -> '\'';
                case '\\' -> '\\';
                default ->
                        throw new ParsingException("Unsupported escape sequence in char literal: " + text, source, t);
            };
        } else if (content.equals("''")) {
            return '\'';
        } else {
            if (content.length() != 1) {
                throw new ParsingException("Char literal must have length 1 or be an escape sequence: " + text, source, t);
            }
            return content.charAt(0);
        }
    }

    private String parseString(Token t) {
        String rawString = t.getText();
        return rawString
                .substring(1, rawString.length() - 1)
                .replace("\"\"", "\"");
    }

    @Override
    public LamaNode visitNumber(LamaParser.NumberContext ctx) {
        long num = parseNumber(ctx.NUM().getSymbol());
        return parser.withSource(new NumNode(num), ctx);
    }

    @Override
    public LamaNode visitChar(LamaParser.CharContext ctx) {
        char c = parseChar(ctx.CHAR().getSymbol());
        return parser.withSource(new NumNode(c), ctx);
    }

    @Override
    public LamaNode visitString(LamaParser.StringContext ctx) {
        String unescaped = parseString(ctx.STRING().getSymbol());
        return parser.withSource(
                new StringNode(unescaped.getBytes()),
                ctx
        );
    }

    @Override
    public LamaNode visitArray(LamaParser.ArrayContext ctx) {
        LamaNode[] items = ctx.items.stream().map(this::visit).toArray(LamaNode[]::new);
        return parser.withSource(new ArrayNode(items), ctx);
    }

    @Override
    public LamaNode visitRawExpr(LamaParser.RawExprContext ctx) {
        if (ctx.op().isEmpty()) {
            return visit(ctx.primary(0));
        }
        List<LamaNode> exprs = ctx.primary().stream().map(this::visit).toList();
        List<String> operators = ctx.op().stream().map(ParseTree::getText).toList();
        return parser.parseExpr(exprs, operators, ctx.op());
    }

    @Override
    public LamaNode visitScopedExpr(LamaParser.ScopedExprContext ctx) {
        if (ctx.definitions().isEmpty()) {
            return visit(ctx.expr());
        }

        telescope.pushScope(Telescope.ScopeType.LOCAL);
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
        Supplier<LamaNode> init = processDefinitions(ctx.definitions());

        LamaNode expr = SeqNode.create(init.get(), visit(ctx.expr()));

        Telescope.Frame frame = telescope.topFrame();
        assert frame.captures.isEmpty();
        assert frame.numParams.get() == 0;
        ModuleNode node = new ModuleNode(new LamaContext.Descriptor(
                telescope.numGlobals,
                functions.toArray(new LamaCallTarget[0])
        ), expr);
        node = parser.withSource(node, ctx);

        return new ModuleObject(node, frame.desc.build());
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
        } else if (lvalue instanceof LamaParser.LIndexingContext lIndexing) {
            LamaNode collection = visit(lIndexing.collection);
            LamaNode index = visit(lIndexing.index);
            return parser.withSource(StoreIndexNodeGen.create(collection, index, expr), ctx);
        } else {
            throw new ParsingException("Can't assign to that", source, ctx.start);
        }
    }

    @Override
    public LamaNode visitDirectCall(LamaParser.DirectCallContext ctx) {
        String fnName = ctx.IDENT().getText();
        List<LamaNode> args = ctx.args.stream().map(this::visit).toList();

        return generateDirectCall(fnName, args, ctx);
    }

    @Override
    public LamaNode visitDotCall(LamaParser.DotCallContext ctx) {
        String fnName = ctx.IDENT().getText();
        List<LamaNode> args = Stream.concat(
                Stream.of(visit(ctx.recv)),
                ctx.args.stream().map(this::visit)
        ).toList();

        return generateDirectCall(fnName, args, ctx);
    }

    private LamaNode generateDirectCall(
            String fnName,
            List<LamaNode> args,
            ParserRuleContext ctx
    ) {
        Telescope.LookupResult lookup = telescope.lookup(fnName);

        switch (lookup) {
            case Telescope.LookupBuiltin lookupBuiltin -> {
                return parser.withSource(
                        lookupBuiltin.builder().apply(args),
                        ctx
                );
            }
            case Telescope.LookupFunction lookupFunction -> {
                int fnSlot = lookupFunction.fnSlot();
                return parser.withSource(
                        new DirectCallNode(
                                fnSlot,
                                functions.get(fnSlot),
                                args.toArray(new LamaNode[0])
                        ),
                        ctx
                );
            }
            case Telescope.LookupLazyClosure(var cl) when cl.get().captures.length == 0 -> {
                int fnSlot = cl.get().fnSlot();
                return parser.withSource(
                        new DirectCallNode(
                                fnSlot,
                                functions.get(fnSlot),
                                args.toArray(new LamaNode[0])
                        ),
                        ctx
                );
            }
            case Telescope.LookupSelf ignoredSelf -> {
                IdNode call = new IdNode(new ClosureCallNode(
                        LoadSelfNodeGen.create(),
                        args.toArray(new LamaNode[0])
                ));
                telescope.topFrame().selfCalls.add(call);
                return parser.withSource(call, ctx);
            }
            case null -> throw new ParsingException("Function " + fnName + " is not defined", source, ctx.start);
            default -> {
                return parser.withSource(new ClosureCallNode(
                        generateLoad(lookup, fnName, ctx),
                        args.toArray(new LamaNode[0])
                ), ctx);
            }
        }
    }

    @Override
    public LamaNode visitSeq(LamaParser.SeqContext ctx) {
        LamaNode prev = visit(ctx.expr(0));
        LamaNode expr = visit(ctx.expr(1));
        return parser.withSource(SeqNode.create(prev, expr), ctx);
    }

    private LamaNode generateLoad(Telescope.LookupResult lookup, String name, ParserRuleContext ctx) {
        LamaNode load = switch (lookup) {
            case Telescope.LookupBuiltin builtin ->
                    throw new ParsingException("Builtins can't be promoted to function objects", source, ctx.start);
            case Telescope.LookupFunction fn -> new ClosureNode(fn.fnSlot, functions.get(fn.fnSlot), new LamaNode[0]);
            case Telescope.LookupLazyClosure lazyCl -> {
                Telescope.LazyClosure cl = lazyCl.cl.get();
                yield new ClosureNode(
                    cl.fnSlot,
                    functions.get(cl.fnSlot),
                    Arrays.stream(cl.captures).map(capture ->
                            generateLoad(capture, null, ctx)
                    ).toArray(LamaNode[]::new)
                );
            }
            case Telescope.LookupGlobal(var slot) -> LoadGlobalNodeGen.create(slot);
            case Telescope.LookupLocal(var slot) -> LoadLocalNodeGen.create(slot);
            case Telescope.LookupArg(var slot) -> LoadArgNodeGen.create(slot);
            case Telescope.LookupCapture(var slot) -> LoadCaptureNodeGen.create(slot);
            case Telescope.LookupSelf() -> {
                IdNode node = new IdNode(LoadSelfNodeGen.create());
                telescope.topFrame().selfUsages.add(node);
                yield node;
            }
            case null -> throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        };
        return parser.withSource(load, ctx);
    }

    private LamaNode generateStore(Telescope.LookupResult lookup, String name, LamaNode value, ParserRuleContext ctx) {
        LamaNode store = switch (lookup) {
            case Telescope.LookupBuiltin _ ->
                    throw new ParsingException("Builtins can't be written to", source, ctx.start);
            case Telescope.LookupFunction _ ->
                    throw new ParsingException("Functions can't be written to", source, ctx.start);
            case Telescope.LookupLazyClosure _, Telescope.LookupSelf _ ->
                    throw new ParsingException("Closures can't be written to", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> StoreGlobalNodeGen.create(value, slot);
            case Telescope.LookupLocal(var slot) -> StoreLocalNodeGen.create(value, slot);
            case Telescope.LookupArg(var slot) -> StoreArgNodeGen.create(value, slot);
            case Telescope.LookupCapture(var slot) -> StoreCaptureNodeGen.create(value, slot);
            case null -> throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        };
        return parser.withSource(store, ctx);
    }

    private Supplier<LamaNode> processDefinitions(List<LamaParser.DefinitionsContext> ctx) {
        Supplier<LamaNode> init = NoopNode::new;
        for (LamaParser.DefinitionsContext c : ctx) {
            Supplier<LamaNode> finalInit = init;
            Supplier<LamaNode> nextInit = processDefinitions(c);
            init = () -> SeqNode.create(finalInit.get(), nextInit.get());
        }
        return init;
    }

    private Supplier<LamaNode> processDefinitions(LamaParser.DefinitionsContext ctx) {
        if (ctx instanceof LamaParser.VarDefinitionsContext c) {
            return processVarDefinitions(c);
        } else if (ctx instanceof LamaParser.FunDefinitionContext c) {
            return processFunDefinition(c);
        }
        throw new RuntimeException("Unknown context");
    }

    private Supplier<LamaNode> processVarDefinitions(LamaParser.VarDefinitionsContext ctx) {
        Supplier<LamaNode> init = NoopNode::new;
        for (LamaParser.VarDefContext c : ctx.varDef()) {
            Supplier<LamaNode> finalInit = init;
            Supplier<LamaNode> nextInit = processVarDef(c);
            init = () -> SeqNode.create(finalInit.get(), nextInit.get());
        }
        return init;
    }

    private Supplier<LamaNode> processVarDef(LamaParser.VarDefContext ctx) {
        TerminalNode ident = ctx.IDENT();
        String name = ident.getText();
        var lookup = telescope.addVar(name);

        LamaParser.SimpleExprContext initializer = ctx.simpleExpr();
        if (initializer != null) {
            return () -> parser.withSource(
                    generateStore(lookup, name, visit(initializer), ctx),
                    ctx
            );
        } else {
            return NoopNode::new;
        }
    }

    public Supplier<LamaNode> processFunDefinition(LamaParser.FunDefinitionContext ctx) {
        String name = ctx.IDENT().getText();
        int fnSlot = functions.size();
        functions.add(null);
        boolean isGlobal = telescope.isGlobal();
        if (isGlobal) {
            telescope.addFunction(name, fnSlot);
        }

        // flat set of all closure instantiations
        List<Integer> inProcessing = new ArrayList<>();

        // if true, this means that if we fail to convert closure to static function,
        // we cannot proceed.
        AtomicBoolean mustOptimize = new AtomicBoolean(false);

        Supplier<Telescope.LazyClosure> fnCtor = () -> {
            telescope.pushFrame();
            if (!isGlobal) {
                telescope.addSelf(name);
                if (inProcessing.contains(fnSlot) && inProcessing.size() > 1) {
                    mustOptimize.set(true);
                    return new Telescope.LazyClosure(
                            new Telescope.LookupResult[0],
                            fnSlot
                    );
                }
            }
            inProcessing.add(fnSlot);
            Supplier<LamaNode> init = processFunParams(ctx.funParams());
            LamaNode body = visit(ctx.funBody().body);
            body = SeqNode.create(init.get(), body);
            Telescope.Frame frame = telescope.popFrame();

            LamaRootNode rootNode = new LamaRootNode(lang, body, frame.desc.build());
            LamaCallTarget fn = new LamaCallTarget(frame.numParams.get(), rootNode.getCallTarget());
            functions.set(fnSlot, fn);

            boolean optimized = false;
            if (frame.captures.isEmpty()) {
                for (IdNode selfUsage : frame.selfUsages) {
                    selfUsage.node = new ClosureNode(fnSlot, fn, new LamaNode[0]);
                }
                for (IdNode selfCall : frame.selfCalls) {
                    ClosureCallNode call = (ClosureCallNode) selfCall.node;
                    selfCall.node = new DirectCallNode(fnSlot, fn, call.callArguments);
                }
                optimized = true;
            }

            if (mustOptimize.get() && !optimized) {
                throw new ParsingException(
                        "Instantiating this closure will result in mutually recursive binding",
                        source, ctx.start
                );
            }

            return new Telescope.LazyClosure(
                    frame.captures.toArray(Telescope.LookupResult[]::new),
                    fnSlot
            );
        };

        if (isGlobal) {
            return () -> {
                fnCtor.get();
                return new NoopNode();
            };
        } else {
            telescope.addClosure(name, new CachedSupplier<>(fnCtor));
            return NoopNode::new;
        }
    }

    Supplier<LamaNode> processFunParams(LamaParser.FunParamsContext ctx) {
        if (ctx instanceof LamaParser.EmptyParamsContext) {
            return NoopNode::new;
        } else if (ctx instanceof LamaParser.ParamsContext c) {
            Supplier<LamaNode> init = NoopNode::new;
            for (TerminalNode param : c.IDENT()) {
                telescope.addParam(param.getText());
            }
            return init;
        } else {
            throw new RuntimeException("Unsupported context");
        }
    }

    @Override
    public LamaNode visitSkip(LamaParser.SkipContext ctx) {
        return parser.withSource(new NoopNode(), ctx);
    }

    @Override
    public LamaNode visitWhileDoLoop(LamaParser.WhileDoLoopContext ctx) {
        telescope.pushScope(Telescope.ScopeType.LOCAL);
        Supplier<LamaNode> init = processDefinitions(ctx.definitions());
        LamaNode cond = SeqNode.create(init.get(), visit(ctx.cond));
        LamaNode body = visit(ctx.body);
        telescope.popScope();
        return parser.withSource(new WhileDoNode(cond, body), ctx);
    }

    @Override
    public LamaNode visitDoWhileLoop(LamaParser.DoWhileLoopContext ctx) {
        telescope.pushScope(Telescope.ScopeType.LOCAL);
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
        telescope.pushScope(Telescope.ScopeType.LOCAL);
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
        telescope.pushScope(Telescope.ScopeType.LOCAL);
        Supplier<LamaNode> defInit = processDefinitions(ctx.definitions());
        LamaNode init = SeqNode.create(defInit.get(), visit(ctx.init));
        LamaNode cond = visit(ctx.cond);
        LamaNode step = visit(ctx.step);
        LamaNode body = visit(ctx.body);
        telescope.popScope();
        return parser.withSource(new ForLoopNode(init, cond, step, body), ctx);
    }

    @Override
    public LamaNode visitIndexing(LamaParser.IndexingContext ctx) {
        LamaNode collection = visit(ctx.collection);
        LamaNode index = visit(ctx.index);
        return parser.withSource(LoadIndexNodeGen.create(collection, index), ctx);
    }

    @Override
    public LamaNode visitSexp(LamaParser.SexpContext ctx) {
        TruffleString tag = TruffleString.fromConstant(ctx.SIDENT().getText(), TruffleString.Encoding.BYTES);
        LamaNode[] items = ctx.items.stream().map(this::visit).toArray(LamaNode[]::new);
        return parser.withSource(new SexpNode(tag, items), ctx);
    }

    @Override
    public LamaNode visitCaseExpr(LamaParser.CaseExprContext ctx) {
        telescope.pushScope(Telescope.ScopeType.LOCAL);
        Supplier<LamaNode> defInit = processDefinitions(ctx.definitions());
        LamaNode scrutinee = SeqNode.create(defInit.get(), visit(ctx.scrutinee));
        List<CaseNode.Branch> branches = ctx.caseBranch().stream().map(this::processBranch).toList();
        telescope.popScope();
        return parser.withSource(new CaseNode(scrutinee, branches), ctx);
    }

    @Override
    public LamaNode visitEmptyList(LamaParser.EmptyListContext ctx) {
        return parser.withSource(new NumNode(0), ctx);
    }

    @Override
    public LamaNode visitListCtor(LamaParser.ListCtorContext ctx) {
        LamaNode result = new NumNode(0L);
        var items = ctx.items;
        for (int i = items.size() - 1; i >= 0; i--) {
            result = ConsNodeGen.create(visit(items.get(i)), result);
        }
        return parser.withSource(result, ctx);
    }

    @Override
    public LamaNode visitInlineFn(LamaParser.InlineFnContext ctx) {
        telescope.pushFrame();
        Supplier<LamaNode> init = processFunParams(ctx.funParams());
        LamaNode body = visit(ctx.funBody().body);
        body = SeqNode.create(init.get(), body);
        Telescope.Frame frame = telescope.popFrame();

        LamaRootNode rootNode = new LamaRootNode(lang, body, frame.desc.build());
        LamaCallTarget fn = new LamaCallTarget(frame.numParams.get(), rootNode.getCallTarget());
        int fnSlot = functions.size();
        functions.add(fn);

        LamaNode[] captures = frame.captures.stream()
                .map(cl -> generateLoad(cl, null, ctx))
                .toArray(LamaNode[]::new);

        return parser.withSource(new ClosureNode(fnSlot, fn, captures), ctx);
    }

    @Override
    public LamaNode visitIndirectCall(LamaParser.IndirectCallContext ctx) {
        return parser.withSource(new ClosureCallNode(
                visit(ctx.fn),
                ctx.args.stream().map(this::visit).toArray(LamaNode[]::new)
        ), ctx);
    }

    private CaseNode.Branch processBranch(LamaParser.CaseBranchContext ctx) {
        telescope.pushScope(Telescope.ScopeType.LOCAL);
        CaseNode.Branch branch = new CaseNode.Branch(
                processPattern(ctx.pattern()),
                visit(ctx.scoped())
        );
        telescope.popScope();
        return branch;
    }

    private BasePatternNode processPattern(LamaParser.PatternContext ctx) {
        return switch (ctx) {
            case LamaParser.ConsPattContext c -> processConsPattern(c);
            case LamaParser.SimplePattContext c -> processPattern(c.simplePattern());
            default -> throw new ParsingException("Pattern is not handled", source, ctx.getStart());
        };
    }

    private BasePatternNode processPattern(LamaParser.SimplePatternContext ctx) {
        return switch (ctx) {
            case LamaParser.SexpPattContext c -> processSexpPattern(c);
            case LamaParser.BindingContext c -> processBindingPattern(c);
            case LamaParser.AsPattContext c -> processAsPattern(c);
            case LamaParser.WildcardContext c -> processWildcardPattern(c);
            case LamaParser.EmptyListPattContext c -> processEmptyListPattern(c);
            case LamaParser.ListPattContext c -> processListPattern(c);
            case LamaParser.ArrayPattContext c -> processArrayPattern(c);
            case LamaParser.NumPattContext c -> processNumPattern(c);
            case LamaParser.StringPattContext c -> processStringPattern(c);
            case LamaParser.CharPattContext c -> processCharPattern(c);
            case LamaParser.TruePattContext c -> processTruePattern(c);
            case LamaParser.FalsePattContext c -> processFalsePattern(c);
            case LamaParser.TBoxPattContext c -> processTBoxPattern(c);
            case LamaParser.TValPattContext c -> processTValPattern(c);
            case LamaParser.TStrPattContext c -> processTStrPattern(c);
            case LamaParser.TArrayPattContext c -> processTArrayPattern(c);
            case LamaParser.TSexpPattContext c -> processTSexpPattern(c);
            case LamaParser.TFunPattContext c -> processTFunPattern(c);
            case LamaParser.ParenthesisedPatternContext c -> processPattern(c.pattern());
            default -> throw new ParsingException("Pattern is not handled", source, ctx.getStart());
        };
    }

    private WildcardPatternNode processWildcardPattern(LamaParser.WildcardContext ctx) {
        return parser.withSource(new WildcardPatternNode(), ctx);
    }

    private SexpPatternNode processSexpPattern(LamaParser.SexpPattContext ctx) {
        TruffleString tag = TruffleString.fromConstant(
                ctx.SIDENT().getText(),
                TruffleString.Encoding.BYTES
        );
        BasePatternNode[] items = ctx.pattern().stream()
                .map(this::processPattern)
                .toArray(BasePatternNode[]::new);
        return parser.withSource(new SexpPatternNode(tag, items), ctx);
    }

    private BindingPatternNode processBindingPattern(LamaParser.BindingContext ctx) {
        Telescope.LookupResult lookup = telescope.addVar(ctx.IDENT().getText());
        assert lookup instanceof Telescope.LookupLocal;
        int slot = ((Telescope.LookupLocal) lookup).slot;
        return parser.withSource(new BindingPatternNode(slot), ctx);
    }

    private AsPatternNode processAsPattern(LamaParser.AsPattContext ctx) {
        Telescope.LookupResult lookup = telescope.addVar(ctx.IDENT().getText());
        assert lookup instanceof Telescope.LookupLocal;
        int slot = ((Telescope.LookupLocal) lookup).slot;
        BasePatternNode patt = processPattern(ctx.pattern());
        return parser.withSource(new AsPatternNode(slot, patt), ctx);
    }

    private ConstPatternNode processEmptyListPattern(LamaParser.EmptyListPattContext ctx) {
        return parser.withSource(new ConstPatternNode(0L), ctx);
    }

    private ConsPatternNode processConsPattern(LamaParser.ConsPattContext ctx) {
        return parser.withSource(new ConsPatternNode(
                processPattern(ctx.head),
                processPattern(ctx.tail)
        ), ctx);
    }

    private BasePatternNode processListPattern(LamaParser.ListPattContext ctx) {
        BasePatternNode result = new ConstPatternNode(0L);
        var patterns = ctx.pattern();
        for (int i = patterns.size() - 1; i >= 0; i--) {
            result = new ConsPatternNode(processPattern(patterns.get(i)), result);
        }
        return parser.withSource(result, ctx);
    }

    private ArrayPatternNode processArrayPattern(LamaParser.ArrayPattContext ctx) {
        BasePatternNode[] items = ctx.pattern().stream()
                .map(this::processPattern)
                .toArray(BasePatternNode[]::new);
        return parser.withSource(new ArrayPatternNode(items), ctx);
    }

    private ConstPatternNode processNumPattern(LamaParser.NumPattContext ctx) {
        return parser.withSource(new ConstPatternNode(parseNumber(ctx.NUM().getSymbol())), ctx);
    }

    private StringPatternNode processStringPattern(LamaParser.StringPattContext ctx) {
        String unescaped = parseString(ctx.STRING().getSymbol());
        TruffleString pattern = TruffleString.fromConstant(unescaped, TruffleString.Encoding.BYTES);
        return parser.withSource(new StringPatternNode(pattern), ctx);
    }

    private ConstPatternNode processCharPattern(LamaParser.CharPattContext ctx) {
        char unescaped = parseChar(ctx.CHAR().getSymbol());
        return parser.withSource(new ConstPatternNode((long) unescaped), ctx);
    }

    private TruePatternNode processTruePattern(LamaParser.TruePattContext ctx) {
        return parser.withSource(new TruePatternNode(), ctx);
    }

    private ConstPatternNode processFalsePattern(LamaParser.FalsePattContext ctx) {
        return parser.withSource(new ConstPatternNode(0L), ctx);
    }

    private TypePatternNode.Box processTBoxPattern(LamaParser.TBoxPattContext ctx) {
        return parser.withSource(new TypePatternNode.Box(), ctx);
    }

    private TypePatternNode.Val processTValPattern(LamaParser.TValPattContext ctx) {
        return parser.withSource(new TypePatternNode.Val(), ctx);
    }

    private TypePatternNode.Str processTStrPattern(LamaParser.TStrPattContext ctx) {
        return parser.withSource(new TypePatternNode.Str(), ctx);
    }

    private TypePatternNode.Array processTArrayPattern(LamaParser.TArrayPattContext ctx) {
        return parser.withSource(new TypePatternNode.Array(), ctx);
    }

    private TypePatternNode.Sexp processTSexpPattern(LamaParser.TSexpPattContext ctx) {
        return parser.withSource(new TypePatternNode.Sexp(), ctx);
    }

    private TypePatternNode.Fun processTFunPattern(LamaParser.TFunPattContext ctx) {
        return parser.withSource(new TypePatternNode.Fun(), ctx);
    }
}
