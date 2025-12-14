package space.elteammate.lama.parser;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.*;
import space.elteammate.lama.nodes.expr.AddNodeGen;
import space.elteammate.lama.nodes.expr.MulNodeGen;
import space.elteammate.lama.nodes.expr.SubNodeGen;
import space.elteammate.lama.nodes.literal.NumNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class LamaNodeParser {
    private final Map<String, Infix> infixOperators = new HashMap<>();

    private Source source;

    public LamaNodeParser(Source source) {
        this.source = source;
        infixOperators.put("+", new Infix("+", 10, 11, AddNodeGen::create));
        infixOperators.put("-", new Infix("-", 10, 11, SubNodeGen::create));
        infixOperators.put("*", new Infix("*", 20, 21, MulNodeGen::create));
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public LamaNode parse(CharStream program) {
        var lexer = new LamaLexer(program);
        var parser = new LamaParser(new CommonTokenStream(lexer));
        var visitor = new LamaNodeVisitor(this, source);
        return visitor.visit(parser.module());
    }

    private record Infix(
            String literal,
            int lbp,
            int rbp,
            BiFunction<LamaNode, LamaNode, LamaNode> ctor
    ) {
    }

    public static <T extends LamaNode> T withSource(T node, ParserRuleContext ctx, Source source) {
        Token start = ctx.getStart();
        Token end = ctx.getStop();
        node.setSourceSection(source.createSection(start.getStartIndex(), end.getStopIndex() - start.getStartIndex() + 1));
        return node;
    }

    private static <T extends LamaNode> T withSource(T node, TerminalNode terminal, Source source) {
        Token token = terminal.getSymbol();
        node.setSourceSection(source.createSection(token.getStartIndex(), token.getStopIndex() - token.getStartIndex() + 1));
        return node;
    }

    private class PrattParser {
        private final List<LamaNode> exprs;
        private final List<String> operators;
        private final List<TerminalNode> opNodes;
        private int nextExpr;
        private int nextOp;

        PrattParser(List<LamaNode> exprs, List<String> operators, List<TerminalNode> opNodes) {
            this.exprs = exprs;
            this.operators = operators;
            this.opNodes = opNodes;
            this.nextExpr = 0;
            this.nextOp = 0;
        }

        LamaNode parseExpr(int minBp) {
            LamaNode lhs = this.exprs.get(this.nextExpr++);

            while (this.nextOp < this.operators.size()) {
                var opLiteral = this.operators.get(this.nextOp);
                var op = infixOperators.get(opLiteral);

                if (op == null) {
                    var opNode = this.opNodes.get(this.nextOp);
                    var tempNode = withSource(new NumNode(0), opNode, source);
                    throw LamaException.create("operator not found", tempNode);
                }

                if (op.lbp < minBp) {
                    break;
                }

                this.nextOp++;

                var rhs = this.parseExpr(op.rbp);
                var binaryNode = op.ctor.apply(lhs, rhs);
                var start = lhs.getSourceSection().getCharIndex();
                var length = rhs.getSourceSection().getCharEndIndex() - start;
                binaryNode.setSourceSection(source.createSection(start, length));
                lhs = binaryNode;
            }

            return lhs;
        }
    }


    public LamaNode prattParse(List<LamaNode> exprs, List<String> operators, List<TerminalNode> opNodes) {
        PrattParser parser = new PrattParser(exprs, operators, opNodes);
        return parser.parseExpr(0);
    }

}
