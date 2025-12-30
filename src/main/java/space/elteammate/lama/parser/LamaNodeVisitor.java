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
import space.elteammate.lama.nodes.cflow.DoWhileNode;
import space.elteammate.lama.nodes.cflow.ForLoopNode;
import space.elteammate.lama.nodes.cflow.IfThenElseNode;
import space.elteammate.lama.nodes.cflow.WhileDoNode;
import space.elteammate.lama.nodes.expr.*;
import space.elteammate.lama.nodes.literal.*;
import space.elteammate.lama.nodes.pattern.*;
import space.elteammate.lama.nodes.scope.*;
import space.elteammate.lama.types.FunctionObject;
import space.elteammate.lama.types.ModuleObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LamaNodeVisitor extends LamaBaseVisitor<LamaNode> {
    private final LamaLanguage lang;
    private final LamaNodeParser parser;
    private final Source source;
    private final Telescope telescope;
    private final List<FunctionObject> functions;

    @Override
    public LamaNode visitErrorNode(ErrorNode node) {
        throw new ParsingException("Failed to parse", source, node.getSymbol());
    }

    private static final class Telescope {
        private sealed interface LookupResult {}

        private record LookupBuiltin(Function<List<LamaNode>, LamaNode> builder) implements LookupResult {}

        private record LookupGlobal(int slot) implements LookupResult {}

        private record LookupLocal(int slot) implements LookupResult {}

        private record LookupArg(int slot) implements LookupResult {}

        private record LookupFunction(int fnSlot) implements LookupResult {}

        private sealed interface ScopeItem {}

        private record ScopeBuiltin(Function<List<LamaNode>, LamaNode> builder) implements ScopeItem {}

        private record ScopeFrameSlot(int slot) implements ScopeItem {}

        private record ScopeArg(int slot) implements ScopeItem {}

        private record ScopeGlobal(int slot) implements ScopeItem {}

        private record ScopeFunction(int fnSlot) implements ScopeItem {}

        private record Scope(
                HashMap<String, ScopeItem> items,
                FrameDescriptor.Builder frame,
                AtomicInteger numParams
        ) {

            Scope(FrameDescriptor.Builder frame) {
                this(new HashMap<>(), frame, new AtomicInteger(0));
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

        void addFunction(String name, int fnSlot) {
            Scope scope = scopes.getLast();
            scope.items.put(name, new ScopeFunction(fnSlot));
        }

        void addParam(String name) {
            Scope scope = scopes.getLast();
            int slot = scope.numParams.getAndIncrement();
            scope.items.put(name, new ScopeArg(slot));
        }

        public int getNumParams() {
            return scopes.getLast().numParams.get();
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
                        case ScopeArg(var idx) -> new LookupArg(idx);
                        case ScopeFunction(var idx) -> new LookupFunction(idx);
                        case null -> throw new IllegalStateException("Unknown scope item");
                    };
                }
            }
            return null;
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
                new ModuleNode(new LamaContext.Descriptor(
                        telescope.numGlobals,
                        functions.toArray(new FunctionObject[0])
                ), expr),
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
        } else if (lvalue instanceof LamaParser.LIndexingContext lIndexing) {
            LamaNode collection = visit(lIndexing.collection);
            LamaNode index = visit(lIndexing.index);
            return StoreIndexNodeGen.create(collection, index, expr);
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
            case Telescope.LookupGlobal lookupGlobal -> {
                throw new ParsingException("Can't call global variables yet", source, ctx.start);
            }
            case Telescope.LookupLocal lookupLocal -> {
                throw new ParsingException("Can't call local variables yet", source, ctx.start);
            }
            case Telescope.LookupArg lookupArg -> {
                throw new ParsingException("Can't call args variables yet", source, ctx.start);
            }
            case null -> throw new ParsingException("Name " + fnName + " is not defined", source, ctx.start);
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
            case Telescope.LookupFunction fn ->
                    throw new ParsingException("Functions can't be loaded yet", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> {
                return LoadGlobalNodeGen.create(slot);
            }
            case Telescope.LookupLocal(var slot) -> {
                return LoadLocalNodeGen.create(slot);
            }
            case Telescope.LookupArg(var slot) -> {
                return LoadArgNodeGen.create(slot);
            }
            case null -> throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        }
    }

    private LamaNode generateStore(Telescope.LookupResult lookup, String name, LamaNode value, ParserRuleContext ctx) {
        switch (lookup) {
            case Telescope.LookupBuiltin _ ->
                    throw new ParsingException("Builtins can't be written to", source, ctx.start);
            case Telescope.LookupFunction _ ->
                    throw new ParsingException("Functions can't be written to", source, ctx.start);
            case Telescope.LookupGlobal(var slot) -> {
                return StoreGlobalNodeGen.create(value, slot);
            }
            case Telescope.LookupLocal(var slot) -> {
                return StoreLocalNodeGen.create(value, slot);
            }
            case Telescope.LookupArg _ ->
                    throw new ParsingException("Args can't be written to", source, ctx.start);
            case null -> throw new ParsingException("Variable " + name + " is not defined", source, ctx.start);
        }
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

    public Supplier<LamaNode> processFunDefinition(LamaParser.FunDefinitionContext ctx) {
        String name = ctx.IDENT().getText();
        int fnSlot = addFunction(name, null);

        return () -> {
            telescope.pushFrame();
            Supplier<LamaNode> init = processFunParams(ctx.funParams());
            LamaNode body = visit(ctx.funBody().body);
            body = SeqNode.create(init.get(), body);
            int numParams = telescope.getNumParams();
            FrameDescriptor frame = telescope.popFrame();

            LamaRootNode rootNode = new LamaRootNode(lang, body, frame);
            FunctionObject fn = new FunctionObject(rootNode.getCallTarget(), numParams);
            functions.set(fnSlot, fn);

            return new NoopNode();
        };
    }

    public int addFunction(String name, FunctionObject fn) {
        int fnSlot = functions.size();
        functions.add(fn);
        telescope.addFunction(name, fnSlot);
        return fnSlot;
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
        telescope.pushScope();
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

    private CaseNode.Branch processBranch(LamaParser.CaseBranchContext ctx) {
        telescope.pushScope();
        CaseNode.Branch branch = new CaseNode.Branch(
                processPattern(ctx.pattern()),
                visit(ctx.scoped())
        );
        telescope.popScope();
        return branch;
    }

    private BasePatternNode processPattern(LamaParser.PatternContext ctx) {
        return switch (ctx) {
            case LamaParser.SexpPattContext c -> processSexpPattern(c);
            case LamaParser.BindingContext c -> processBindingPattern(c);
            case LamaParser.AsPattContext c -> processAsPattern(c);
            case LamaParser.WildcardContext c -> processWildcardPattern(c);
            case LamaParser.EmptyListPattContext c -> processEmptyListPattern(c);
            case LamaParser.ListPattContext c -> processListPattern(c);
            case LamaParser.ConsPattContext c -> processConsPattern(c);
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
        return parser.withSource(new ConstPatternNode((long)unescaped), ctx);
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
