package space.elteammate.lama.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.AddNodeGen;
import space.elteammate.lama.nodes.NumNode;

public class LamaNodeParser {
    public static LamaNode parse(CharStream program) {
        var lexer = new LamaLexer(program);
        var parser = new LamaParser(new CommonTokenStream(lexer));
        var visitor = new LamaVisitor();
        return visitor.visit(parser.module());
    }

    private static class LamaVisitor extends LamaBaseVisitor<LamaNode> {
        @Override
        public LamaNode visitNumber(LamaParser.NumberContext ctx) {
            return new NumNode(Long.parseLong(ctx.NUM().getText()));
        }

        @Override
        public LamaNode visitAdd(LamaParser.AddContext ctx) {
            var lhs = visit(ctx.expr());
            var rhs = new NumNode(Long.parseLong(ctx.NUM().getText()));
            return AddNodeGen.create(lhs, rhs);
        }

        @Override
        public LamaNode visitModule(LamaParser.ModuleContext ctx) {
            return visit(ctx.expr());
        }
    }
}
