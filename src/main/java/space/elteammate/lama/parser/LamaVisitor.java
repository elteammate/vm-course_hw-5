package space.elteammate.lama.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.tree.ParseTree;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.literal.NumNode;
import space.elteammate.lama.nodes.scope.ModuleNode;

import java.util.List;

final class LamaNodeVisitor extends LamaBaseVisitor<LamaNode> {
    LamaNodeParser parser;
    private final Source source;

    private static sealed interface LookupResult {
    }

    private static final class LookupBuiltin implements LookupResult {

    }

    LamaNodeVisitor(LamaNodeParser parser, Source source) {
        this.parser = parser;
        this.source = source;
    }

    @Override
    public LamaNode visitNumber(LamaParser.NumberContext ctx) {
        return LamaNodeParser.withSource(new NumNode(Long.parseLong(ctx.NUM().getText())), ctx, this.source);
    }

    @Override
    public LamaNode visitRawExpr(LamaParser.RawExprContext ctx) {
        if (ctx.OP().isEmpty()) {
            return visit(ctx.primary(0));
        }
        List<LamaNode> exprs = ctx.primary().stream().map(this::visit).toList();
        List<String> operators = ctx.OP().stream().map(ParseTree::getText).toList();
        return this.parser.prattParse(exprs, operators, ctx.OP());
    }

    @Override
    public LamaNode visitParenthesized(LamaParser.ParenthesizedContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public LamaNode visitModule(LamaParser.ModuleContext ctx) {
        LamaNode expr = visit(ctx.expr());
        return new ModuleNode(FrameDescriptor.newBuilder().build(), expr);
    }

    @Override
    public LamaNode visitDirectCall(LamaParser.DirectCallContext ctx) {
        String fnName = ctx.IDENT().getText();
        List<LamaNode> args = ctx.expr().stream().map(this::visit).toList();
        return new DirectCallNode(fnName, args);
    }
}
