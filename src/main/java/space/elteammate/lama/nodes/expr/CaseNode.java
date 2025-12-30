package space.elteammate.lama.nodes.expr;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import space.elteammate.lama.LamaException;
import space.elteammate.lama.nodes.LamaNode;
import space.elteammate.lama.nodes.pattern.BasePatternNode;

import java.util.List;

public class CaseNode extends LamaNode {

    public record Branch(BasePatternNode pattern, LamaNode expr) {}

    @Node.Children
    private BasePatternNode[] patterns;

    @Node.Children
    private LamaNode[] exprs;

    @Child
    private LamaNode scrutinee;

    public CaseNode(LamaNode scrutinee, List<Branch> branches) {
        this.scrutinee = scrutinee;
        patterns = new BasePatternNode[branches.size()];
        exprs = new LamaNode[branches.size()];
        for (int i = 0; i < branches.size(); i++) {
            patterns[i] = branches.get(i).pattern;
            exprs[i] = branches.get(i).expr;
        }
    }

    @Override
    @ExplodeLoop
    public Object execute(VirtualFrame virtualFrame) {
        Object scrutinee = this.scrutinee.execute(virtualFrame);
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].match(virtualFrame, scrutinee)) {
                return exprs[i].execute(virtualFrame);
            }
        }
        throw LamaException.create("Failed to match", this);
    }
}
