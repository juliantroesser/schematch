package de.uni_marburg.schematch.matching.similarityFlooding;

import com.sun.codemodel.JForEach;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import lombok.Getter;
import lombok.Setter;
import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.Map;

import static de.uni_marburg.schematch.matching.similarityFlooding.SchemaGraphBuilder.*;
import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.*;

@Setter
@Getter
public class InitialMappingMatcher extends Matcher {

    private static final double LABEL_SCORE_WEIGHT = 0.5;

    public Map<NodePair, Double> map;

    private String uccFilterThreshold;
    private String indFilterThreshold;
    private String fdFilterThreshold;

    @Override
    public float[][] match(MatchTask matchTask, MatchingStep matchStep) {
        float[][] simMatrix = matchTask.getEmptySimMatrix();

        Database sourceDb = matchTask.getScenario().getSourceDatabase();
        Database targetDb = matchTask.getScenario().getTargetDatabase();

        SchemaGraphBuilder builder = new SchemaGraphBuilder(this.uccFilterThreshold, this.indFilterThreshold, this.fdFilterThreshold);
        Graph<Node, LabelEdge> sourceGraph = builder.transformIntoGraphRepresentationSchema(sourceDb);
        Graph<Node, LabelEdge> targetGraph = builder.transformIntoGraphRepresentationSchema(targetDb);

        Graph<NodePair, LabelEdge> connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);
        Graph<NodePair, CoefficientEdge> propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, PropagationCoefficientPolicy.INVERSE_AVERAGE);

        Map<NodePair, Double> initialMapping = calculateInitialMapping(propagationGraph);
//        map = initialMapping;
        for (Table sourceTable : sourceDb.getTables()) {
            for (Table targetTable : targetDb.getTables()) {
                populateSimMatrix(simMatrix, initialMapping, sourceTable, targetTable);
            }
        }

        return simMatrix;
    }

    private Map<NodePair, Double> calculateInitialMapping(Graph<NodePair, CoefficientEdge> propagationGraph) {
        Map<NodePair, Double> initialMapping = new HashMap<>();
        Levenshtein levenshtein = new Levenshtein();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {
            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();
            double similarity;

            if (node1.isIDNode() || node2.isIDNode()
                    || node1.getValue().startsWith("FD") && node2.getValue().startsWith("FD")
                    || node1.getValue().startsWith("UCC") && node2.getValue().startsWith("UCC")
                    || node1.getValue().startsWith("IND") && node2.getValue().startsWith("IND")) {
                similarity = 0.0;
            } else {
                Column col1 = node1.getRepresentedColumn();
                Column col2 = node2.getRepresentedColumn();
                double labelSim = levenshtein.compare(node1.getValue(), node2.getValue());

                if (col1 == null || col2 == null) {
                    similarity = labelSim;
                } else {
                    double valueSim = getValueSimilarityBetweenColumns(col1, col2);
                    similarity = LABEL_SCORE_WEIGHT * labelSim + (1 - LABEL_SCORE_WEIGHT) * valueSim;
                }
            }

            initialMapping.put(mappingPair, similarity);
        }

        return initialMapping;

    }

}
