package space.elteammate.lama.parser;

import com.oracle.truffle.api.source.Source;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import space.elteammate.lama.LamaLanguage;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.expr.AddNodeGen;
import space.elteammate.lama.nodes.expr.AndNodeGen;
import space.elteammate.lama.nodes.expr.DivNodeGen;
import space.elteammate.lama.nodes.expr.EqualsNodeGen;
import space.elteammate.lama.nodes.expr.GreaterOrEqualNodeGen;
import space.elteammate.lama.nodes.expr.GreaterThanNodeGen;
import space.elteammate.lama.nodes.expr.LessOrEqualNodeGen;
import space.elteammate.lama.nodes.expr.LessThanNodeGen;
import space.elteammate.lama.nodes.expr.ModNodeGen;
import space.elteammate.lama.nodes.expr.MulNodeGen;
import space.elteammate.lama.nodes.expr.NotEqualsNodeGen;
import space.elteammate.lama.nodes.expr.OrNodeGen;
import space.elteammate.lama.nodes.expr.SubNodeGen;
import space.elteammate.lama.types.ModuleObject;

import java.util.*;
import java.util.function.BiFunction;

public class LamaNodeParser {
    private LamaLanguage lang;

    public enum Associativity {
        LEFT,
        RIGHT,
        NONE
    }

    private static final class Precedence {
        private int level;
        private final Associativity associativity;
        private final Map<String, BiFunction<LamaNode, LamaNode, LamaNode>> operators;

        private Precedence(
                int level,
                Associativity associativity
        ) {
            this.level = level;
            this.associativity = associativity;
            this.operators = new HashMap<>();
        }
    }

    private final List<Precedence> precedenceTable = new ArrayList<>();
    private final Map<String, Precedence> operatorToPrecedence = new HashMap<>();

    private Source source;

    public LamaNodeParser(LamaLanguage lang, Source source) {
        this.lang = lang;
        this.source = source;

        precedenceTable.add(new Precedence(0, Associativity.NONE));
        precedenceTable.add(new Precedence(1, Associativity.NONE));
        addOperatorAtLevel(0, Associativity.NONE, "lowest", null);
        addOperatorAtLevel(1, Associativity.NONE, "highest", null);

        addOperatorAbove("lowest", ":", Associativity.RIGHT, (a, b) -> { throw new RuntimeException("todo"); });

        addOperatorAbove(":", "!!", Associativity.LEFT, OrNodeGen::create);

        addOperatorAbove("!!", "&&", Associativity.LEFT, AndNodeGen::create);

        addOperatorAbove("&&", "==", Associativity.NONE, EqualsNodeGen::create);
        addOperatorAtSameLevel("==", "!=", NotEqualsNodeGen::create);
        addOperatorAtSameLevel("==", "<=", LessOrEqualNodeGen::create);
        addOperatorAtSameLevel("==", "<", LessThanNodeGen::create);
        addOperatorAtSameLevel("==", ">", GreaterThanNodeGen::create);
        addOperatorAtSameLevel("==", ">=", GreaterOrEqualNodeGen::create);

        addOperatorAbove("==", "+", Associativity.LEFT, AddNodeGen::create);
        addOperatorAtSameLevel("+", "-", SubNodeGen::create);

        addOperatorAbove("+", "*", Associativity.LEFT, MulNodeGen::create);
        addOperatorAtSameLevel("*", "/", DivNodeGen::create);
        addOperatorAtSameLevel("*", "%", ModNodeGen::create);

    }

    private void addOperatorAtLevel(int level, Associativity associativity, String op, BiFunction<LamaNode, LamaNode, LamaNode> ctor) {
        Precedence precedence = new Precedence(level, associativity);
        precedence.operators.put(op, ctor);
        precedenceTable.add(precedence);
        precedenceTable.sort(Comparator.comparingInt(a -> a.level));
        operatorToPrecedence.put(op, precedence);
    }

    public void addOperatorAtSameLevel(String existingOp, String newOp, BiFunction<LamaNode, LamaNode, LamaNode> ctor) {
        Precedence precedence = operatorToPrecedence.get(existingOp);
        if (precedence == null) {
            throw new IllegalArgumentException("Operator " + existingOp + " not found");
        }
        precedence.operators.put(newOp, ctor);
        operatorToPrecedence.put(newOp, precedence);
    }

    public void addOperatorAbove(String existingOp, String newOp, Associativity associativity, BiFunction<LamaNode, LamaNode, LamaNode> ctor) {
        Precedence existingPrecedence = operatorToPrecedence.get(existingOp);
        if (existingPrecedence == null) {
            throw new IllegalArgumentException("Operator " + existingOp + " not found");
        }
        int newLevel = existingPrecedence.level + 1;
        for (Precedence p : precedenceTable) {
            if (p.level >= newLevel) {
                p.level++;
            }
        }
        addOperatorAtLevel(newLevel, associativity, newOp, ctor);
    }

    public void addOperatorBelow(String existingOp, String newOp, Associativity associativity, BiFunction<LamaNode, LamaNode, LamaNode> ctor) {
        Precedence existingPrecedence = operatorToPrecedence.get(existingOp);
        if (existingPrecedence == null) {
            throw new IllegalArgumentException("Operator " + existingOp + " not found");
        }
        int newLevel = existingPrecedence.level - 1;
        for (Precedence p : precedenceTable) {
            if (p.level >= newLevel) {
                p.level++;
            }
        }
        addOperatorAtLevel(newLevel, associativity, newOp, ctor);
    }


    public void setSource(Source source) {
        this.source = source;
    }

    public ModuleObject parse(CharStream program) {
        var lexer = new LamaLexer(program);
        var parser = new LamaParser(new CommonTokenStream(lexer));
        var visitor = new LamaNodeVisitor(lang, this, source);
        return visitor.processModule(parser.module());
    }

    public <T extends LamaNode> T withSource(T node, ParserRuleContext ctx) {
        Token start = ctx.getStart();
        Token end = ctx.getStop();
        node.setSourceSection(source.createSection(start.getStartIndex(), end.getStopIndex() - start.getStartIndex() + 1));
        return node;
    }

    public <T extends LamaNode> T withSource(T node, TerminalNode terminal) {
        Token token = terminal.getSymbol();
        node.setSourceSection(source.createSection(token.getStartIndex(), token.getStopIndex() - token.getStartIndex() + 1));
        return node;
    }

    private static final class MutableInt {
        int value;
        MutableInt(int value) { this.value = value; }
    }

    public LamaNode parseExpr(List<LamaNode> exprs, List<String> operators, List<TerminalNode> opNodes) {
        if (exprs.isEmpty()) {
            throw new IllegalArgumentException("exprs list cannot be empty");
        }
        if (exprs.size() != operators.size() + 1) {
            throw new IllegalArgumentException("invalid exprs/operators list sizes");
        }
        return parseExpr(exprs, operators, opNodes, 0, new MutableInt(0));
    }

    private LamaNode parseExpr(
            List<LamaNode> exprs,
            List<String> operators,
            List<TerminalNode> opNodes,
            int minPrecedence,
            MutableInt opIndex
    ) {
        LamaNode lhs = exprs.get(opIndex.value);

        while (opIndex.value < operators.size()) {
            String opLiteral = operators.get(opIndex.value);
            Precedence precedence = operatorToPrecedence.get(opLiteral);

            if (precedence == null) {
                throw new ParsingException("operator not found", source, opNodes.get(opIndex.value).getSymbol());
            }

            if (precedence.level < minPrecedence) {
                break;
            }

            opIndex.value++;

            int nextMinPrecedence = (precedence.associativity == Associativity.RIGHT)
                    ? precedence.level
                    : precedence.level + 1;

            LamaNode rhs = parseExpr(exprs, operators, opNodes, nextMinPrecedence, opIndex);
            BiFunction<LamaNode, LamaNode, LamaNode> ctor = precedence.operators.get(opLiteral);
            if (ctor == null) {
                throw new ParsingException("operator `" + opLiteral + "` does not have a constructor", source, opNodes.get(opIndex.value - 1).getSymbol());
            }
            LamaNode binaryNode = ctor.apply(lhs, rhs);

            var start = lhs.getSourceSection().getCharIndex();
            var length = rhs.getSourceSection().getCharEndIndex() - start;
            binaryNode.setSourceSection(source.createSection(start, length));
            lhs = binaryNode;
        }

        return lhs;
    }
}
