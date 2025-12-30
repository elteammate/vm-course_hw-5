package space.elteammate.lama.nodes;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.source.SourceSection;
import space.elteammate.lama.LamaLanguage;

@NodeInfo(language = LamaLanguage.ID)
public abstract class LamaBaseNode extends Node {
    private SourceSection sourceSection;

    @Override
    public SourceSection getSourceSection() {
        return this.sourceSection;
    }

    public void setSourceSection(SourceSection sourceSection) {
        this.sourceSection = sourceSection;
    }
}
