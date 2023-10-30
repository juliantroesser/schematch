package de.uni_marburg.schematch.matching.similarity.label;

import de.uni_marburg.schematch.similarity.string.JaroWinkler;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JaroWinklerMatcher extends LabelSimilarityMatcher {
    public JaroWinklerMatcher() {
        super(new JaroWinkler());
    }
}
