package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.Datatype;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.hasConverged;
import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.populateSimMatrix;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SimilarityFlooding extends Matcher {

    private static final Logger log = LogManager.getLogger(SimilarityFlooding.class);

    private String propCoeffPolicy;
    private String fixpoint;
    private String UCCQuick;
    private String UCCComplete;
    private String lengthScoreWeight;
    private String valueScoreWeight;
    private String postionScoreWeight;
    private String nameSuffixScoreWeight;
    private String uccFilterThreshold;
    private String labelScoreWeight;
    private String selectThresholdWeight;

    @Override
    public float[][] match(MatchTask matchTask, MatchingStep matchStep) {

        PropagationCoefficientPolicy policy;
        FixpointFormula formula;

        policy = switch (propCoeffPolicy) {
            case "INV_AVG" -> PropagationCoefficientPolicy.INVERSE_AVERAGE;
            case "INV_PROD" -> PropagationCoefficientPolicy.INVERSE_PRODUCT;
            case "CONSTANT_ONE" -> PropagationCoefficientPolicy.CONSTANT_ONE;
            default -> throw new RuntimeException("No such propagation coefficient policy: " + propCoeffPolicy);
        };

        formula = switch (fixpoint) {
            case "BASIC" -> FixpointFormula.BASIC;
            case "A" -> FixpointFormula.FORMULA_A;
            case "B" -> FixpointFormula.FORMULA_B;
            case "C" -> FixpointFormula.FORMULA_C;
            case "BASIC_Lambda" -> FixpointFormula.BASIC_Lambda;
            case "A_Lambda" -> FixpointFormula.FORMULA_A_Lambda;
            case "B_Lambda" -> FixpointFormula.FORMULA_B_Lambda;
            case "C_Lambda" -> FixpointFormula.FORMULA_C_Lambda;
            default -> throw new RuntimeException("No such fixpoint formula: " + fixpoint);
        };

        boolean fdv1 = false;
        boolean fdv2 = false;
        boolean uccv1 = Boolean.parseBoolean(UCCQuick);
        boolean uccv2 = Boolean.parseBoolean(UCCComplete);
        boolean indv1 = false;
        boolean indv2 = false;

        float[][] simMatrix = matchTask.getEmptySimMatrix();

        Database sourceDb = matchTask.getScenario().getSourceDatabase();
        Database targetDb = matchTask.getScenario().getTargetDatabase();

        Graph<Node, LabelEdge> sourceGraph = transformIntoGraphRepresentationSchema(sourceDb, fdv1, fdv2, uccv1, uccv2, indv1, indv2);
        Graph<Node, LabelEdge> targetGraph = transformIntoGraphRepresentationSchema(targetDb, fdv1, fdv2, uccv1, uccv2, indv1, indv2);

        //Combine both Graphs into a connectivity-graph
        Graph<NodePair, LabelEdge> connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);

        //Transform the connectivity-graph into the propagation-graph on which the algorithm executes
        Graph<NodePair, CoefficientEdge> propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, policy);

        //Calculate the initial mapping (similarity) values
        Map<NodePair, Double> initialMapping = calculateInitialMapping(propagationGraph);

        //Run the similarity-flooding algorithm
        Map<NodePair, Double> floodingResults = similarityFlooding(propagationGraph, initialMapping, formula);

        //Apply constraints/filters to the result
        Map<NodePair, Double> filteredFloodingResults = filterMapping(floodingResults);

        for (Table sourceTable : matchTask.getScenario().getSourceDatabase().getTables()) {
            for (Table targetTable : matchTask.getScenario().getTargetDatabase().getTables()) {
                populateSimMatrix(simMatrix, filteredFloodingResults, sourceTable, targetTable);
            }
        }

        return simMatrix;
    }

    public Graph<Node, LabelEdge> transformIntoGraphRepresentationSchema(Database db, boolean fdv1, boolean fdv2, boolean uccv1, boolean uccv2, boolean indv1, boolean indv2) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        Node schemaNode = new Node("Schema", NodeType.DATABASE, null, false, null, null, null);
        Node tableNode = new Node("Table", NodeType.TABLE, null, false, null, null, null);
        Node columnNode = new Node("Column", NodeType.COLUMN, null, false, null, null, null);
        Node columnTypeNode = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null, null);

        graphRepresentation.addVertex(schemaNode);
        graphRepresentation.addVertex(tableNode);
        graphRepresentation.addVertex(columnNode);
        graphRepresentation.addVertex(columnTypeNode);

        int uniqueID = 1;

        Node databaseName = new Node(db.getName(), NodeType.DATABASE, null, false, null, null, null);
        graphRepresentation.addVertex(databaseName);

        Node currentDatabaseNode = new Node("NodeID" + uniqueID++, NodeType.DATABASE, null, true, databaseName, null, null);
        graphRepresentation.addVertex(currentDatabaseNode);

        graphRepresentation.addEdge(currentDatabaseNode, schemaNode, new LabelEdge("type"));
        graphRepresentation.addEdge(currentDatabaseNode, databaseName, new LabelEdge("name"));

        for (Table table : db.getTables()) {

            Node tableName = new Node(table.getName(), NodeType.TABLE, null, false, null, null, null);
            graphRepresentation.addVertex(tableName);

            Node currentTableNode = new Node("NodeID" + uniqueID++, NodeType.TABLE, null, true, tableName, null, null);
            graphRepresentation.addVertex(currentTableNode);

            graphRepresentation.addEdge(currentDatabaseNode, currentTableNode, new LabelEdge("table"));
            graphRepresentation.addEdge(currentTableNode, tableNode, new LabelEdge("type"));
            graphRepresentation.addEdge(currentTableNode, tableName, new LabelEdge("name"));

            for (Column column : table.getColumns()) {

                Node columnName = new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, null, table, column);
                graphRepresentation.addVertex(columnName);

                Node currentColumnNode = new Node("NodeID" + uniqueID++, NodeType.COLUMN, column.getDatatype(), true, columnName, table, null);
                graphRepresentation.addVertex(currentColumnNode);

                graphRepresentation.addEdge(currentTableNode, currentColumnNode, new LabelEdge("column"));
                graphRepresentation.addEdge(currentColumnNode, columnNode, new LabelEdge("type"));
                graphRepresentation.addEdge(currentColumnNode, columnName, new LabelEdge("name"));

                Node columnDataType = new Node(column.getDatatype().toString(), NodeType.COLUMN_TYPE, column.getDatatype(), false, null, null, null);
                boolean dataTypeNodeExistsInGraph = graphRepresentation.containsVertex(columnDataType);

                if (dataTypeNodeExistsInGraph) { //Dann Kante zu

                    Set<LabelEdge> edgesOfIdToColumnType = graphRepresentation.incomingEdgesOf(columnDataType);
                    LabelEdge edgeOfIdToColumnType = edgesOfIdToColumnType.stream().findFirst().orElseThrow(() -> new NoSuchElementException("No such data Type edge present in the graph"));
                    Node columnTypeIdentifier = graphRepresentation.getEdgeSource(edgeOfIdToColumnType);
                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("datatype"));

                } else { //Neuen Knoten anlegen

                    Node columnTypeIdentifier = new Node("NodeID" + uniqueID++, NodeType.COLUMN_TYPE, column.getDatatype(), true, columnDataType, null, null);

                    graphRepresentation.addVertex(columnDataType);
                    graphRepresentation.addVertex(columnTypeIdentifier);
                    graphRepresentation.addEdge(columnTypeIdentifier, columnDataType, new LabelEdge("name"));
                    graphRepresentation.addEdge(columnTypeIdentifier, columnTypeNode, new LabelEdge("type"));
                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("datatype"));
                }
            }
        }

        //Extending the schema-graph with dependency information:

        //Add Constraint Node only when introducing additional Nodes for dependency

        Node constraintNode = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null, null);
        if (fdv2 || uccv2 || indv2) {
            graphRepresentation.addVertex(constraintNode);
        }

        if (fdv1) { //New Edges for fdv1

            Collection<FunctionalDependency> functionalDependencies;

            functionalDependencies = db.getMetadata().getMeaningfulFunctionalDependencies();

            for (FunctionalDependency functionalDependency : filterFunctionalDependencies(functionalDependencies)) {

                List<Node> determinantIdNodes = new ArrayList<>();

                for (Column determinant : functionalDependency.getDeterminant()) {
                    LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(determinant.getLabel(), NodeType.COLUMN, determinant.getDatatype(), false, null, determinant.getTable(), null)).stream().findFirst().get();
                    Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);
                    determinantIdNodes.add(determinantIDNode);
                }

                Column dependant = functionalDependency.getDependant();

                LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable(), null)).stream().findFirst().get();
                Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                for (Node determinantIDNode : determinantIdNodes) {
                    graphRepresentation.addEdge(determinantIDNode, dependantIDNode, new LabelEdge("determines"));
                }
            }
        }

        if (fdv2) { //New vertices and edges for fdv2

            Collection<FunctionalDependency> functionalDependencies;

            functionalDependencies = db.getMetadata().getMeaningfulFunctionalDependencies();

            int fdID = 1;

            for (FunctionalDependency functionalDependency : filterFunctionalDependencies(functionalDependencies)) {

                Node fdNode = new Node("FD" + fdID++, NodeType.CONSTRAINT, null, true, null, null, null);
                graphRepresentation.addVertex(fdNode);
                graphRepresentation.addEdge(fdNode, constraintNode, new LabelEdge("type"));

                List<Node> determinantIdNodes = new ArrayList<>();

                for (Column determinant : functionalDependency.getDeterminant()) {
                    LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(determinant.getLabel(), NodeType.COLUMN, determinant.getDatatype(), false, null, determinant.getTable(), null)).stream().findFirst().get();
                    Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);
                    determinantIdNodes.add(determinantIDNode);
                }

                Column dependant = functionalDependency.getDependant();

                LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable(), null)).stream().findFirst().get();
                Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                for (Node determinantIDNode : determinantIdNodes) {
                    graphRepresentation.addEdge(fdNode, determinantIDNode, new LabelEdge("determinant"));
                }

                graphRepresentation.addEdge(fdNode, dependantIDNode, new LabelEdge("dependant"));
            }
        }

        if (uccv1) { //new vertices and edges for uccv1

            List<UniqueColumnCombination> uniqueColumnCombinations = db.getMetadata().getUccs().stream().toList();

            for (UniqueColumnCombination ucc : filterUniqueColumnCombinations(uniqueColumnCombinations)) {

                if (!ucc.getColumnCombination().isEmpty()) {
                    int uccSize = ucc.getColumnCombination().size();
                    Node uccSizeNode = new Node("UCC#" + uccSize, NodeType.CONSTRAINT, null, false, null, null, null);

                    if (!graphRepresentation.containsVertex(uccSizeNode)) {
                        graphRepresentation.addVertex(uccSizeNode);
                    }

                    List<Node> nodesPartOfUcc = new ArrayList<>();

                    for (Column nodePartOfUcc : ucc.getColumnCombination()) {
                        LabelEdge edgeFromIDtoUccNode = graphRepresentation.incomingEdgesOf(new Node(nodePartOfUcc.getLabel(), NodeType.COLUMN, nodePartOfUcc.getDatatype(), false, null, nodePartOfUcc.getTable(), null)).stream().findFirst().get();
                        Node uccIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoUccNode);
                        nodesPartOfUcc.add(uccIDNode);
                    }

                    for (Node nodePartOfUcc : nodesPartOfUcc) {
                        graphRepresentation.addEdge(nodePartOfUcc, uccSizeNode, new LabelEdge("ucc"));
                    }
                }
            }
        }

        if (uccv2) { //new vertices and edges for uccv2

            List<UniqueColumnCombination> uniqueColumnCombinations = db.getMetadata().getUccs().stream().toList();
            int uccID = 1;

            for (UniqueColumnCombination ucc : filterUniqueColumnCombinations(uniqueColumnCombinations)) {

                Node uccNode = new Node("UCC" + uccID++, NodeType.CONSTRAINT, null, true, null, null, null);
                graphRepresentation.addVertex(uccNode);
                graphRepresentation.addEdge(uccNode, constraintNode, new LabelEdge("type"));

                int uccSize = ucc.getColumnCombination().size();
                Node uccSizeNode = new Node("UCC#" + uccSize, NodeType.CONSTRAINT, null, false, null, null, null);

                if (!graphRepresentation.containsVertex(uccSizeNode)) {
                    graphRepresentation.addVertex(uccSizeNode);
                }

                graphRepresentation.addEdge(uccNode, uccSizeNode, new LabelEdge("size"));

                List<Node> nodesPartOfUcc = new ArrayList<>();

                for (Column nodePartOfUcc : ucc.getColumnCombination()) {
                    LabelEdge edgeFromIDtoUccNode = graphRepresentation.incomingEdgesOf(new Node(nodePartOfUcc.getLabel(), NodeType.COLUMN, nodePartOfUcc.getDatatype(), false, null, nodePartOfUcc.getTable(), null)).stream().findFirst().get();
                    Node uccIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoUccNode);
                    nodesPartOfUcc.add(uccIDNode);
                }

                for (Node nodePartOfUcc : nodesPartOfUcc) {
                    graphRepresentation.addEdge(nodePartOfUcc, uccNode, new LabelEdge("ucc"));
                }
            }
        }

        if (indv1) { //new edges for indv1

            List<InclusionDependency> inclusionDependencies = db.getMetadata().getInds().stream().toList();

            for (InclusionDependency inclusionDependency : filterInclusionDependencies(inclusionDependencies)) {

                List<Node> dependantIdNodes = new ArrayList<>();
                List<Node> referencedIdNodes = new ArrayList<>();

                for (Column dependant : inclusionDependency.getDependant()) {
                    LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable(), null)).stream().findFirst().get();
                    Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);
                    dependantIdNodes.add(dependantIDNode);
                }

                for (Column referenced : inclusionDependency.getReferenced()) {
                    LabelEdge edgeFromIDtoReferenced = graphRepresentation.incomingEdgesOf(new Node(referenced.getLabel(), NodeType.COLUMN, referenced.getDatatype(), false, null, referenced.getTable(), null)).stream().findFirst().get();
                    Node referencedIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoReferenced);
                    referencedIdNodes.add(referencedIDNode);
                }

                for (Node referencedIDNode : referencedIdNodes) {
                    for (Node dependantIDNode : dependantIdNodes) {
                        graphRepresentation.addEdge(referencedIDNode, dependantIDNode, new LabelEdge("contains"));
                    }
                }
            }
        }

        if (indv2) { //new vertices and edges for indv2

            List<InclusionDependency> inclusionDependencies = db.getMetadata().getInds().stream().toList();
            int indID = 1;

            for (InclusionDependency inclusionDependency : filterInclusionDependencies(inclusionDependencies)) {

                List<Node> dependantIdNodes = new ArrayList<>();
                List<Node> referencedIdNodes = new ArrayList<>();

                for (Column dependant : inclusionDependency.getDependant()) {
                    LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable(), null)).stream().findFirst().get();
                    Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);
                    dependantIdNodes.add(dependantIDNode);
                }

                for (Column referenced : inclusionDependency.getReferenced()) {
                    LabelEdge edgeFromIDtoReferenced = graphRepresentation.incomingEdgesOf(new Node(referenced.getLabel(), NodeType.COLUMN, referenced.getDatatype(), false, null, referenced.getTable(), null)).stream().findFirst().get();
                    Node referencedIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoReferenced);
                    referencedIdNodes.add(referencedIDNode);
                }

                Node indNode = new Node("IND" + indID++, NodeType.CONSTRAINT, null, true, null, null, null);
                graphRepresentation.addVertex(indNode);
                graphRepresentation.addEdge(indNode, constraintNode, new LabelEdge("type"));

                for (Node referencedIDNode : referencedIdNodes) {
                    graphRepresentation.addEdge(indNode, referencedIDNode, new LabelEdge("referenced"));
                }

                for (Node dependantIDNode : dependantIdNodes) {
                    graphRepresentation.addEdge(indNode, dependantIDNode, new LabelEdge("dependant"));
                }
            }
        }

        return graphRepresentation;
    }

    public Graph<NodePair, LabelEdge> createConnectivityGraph(Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) {

        Graph<NodePair, LabelEdge> connectivityGraph = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        //Für jede Kante aus Graph1
        for (LabelEdge label1 : graph1.edgeSet()) {
            //Für jede Kante aus Graph2
            for (LabelEdge label2 : graph2.edgeSet()) {
                //Falls die Labels beider Kanten gleich sind
                if (label1 != label2 && label1.equals(label2)) {

                    Node sourceVertex1 = graph1.getEdgeSource(label1);
                    Node targetVertex1 = graph1.getEdgeTarget(label1);

                    Node sourceVertex2 = graph2.getEdgeSource(label2);
                    Node targetVertex2 = graph2.getEdgeTarget(label2);

                    //Knotenpaar1 erstellen mit NodePair(startKnotenKante1, startKnotenKante2)
                    NodePair connectedSourceNode = new NodePair(sourceVertex1, sourceVertex2);
                    //Knotenpaar2 erstellen mit NodePair(endKnotenKante1, endKnotenKante2)
                    NodePair connectedTargetNode = new NodePair(targetVertex1, targetVertex2);

                    //Beide Knoten zu Graph hinzufügen
                    connectivityGraph.addVertex(connectedSourceNode);
                    connectivityGraph.addVertex(connectedTargetNode);

                    //Kante von Knotenpaar1 zu Knotenpaar2 mit Label der beiden Kanten
                    connectivityGraph.addEdge(connectedSourceNode, connectedTargetNode, new LabelEdge(label1.getLabel()));
                }
            }
        }

        return connectivityGraph;
    }

    public Graph<NodePair, CoefficientEdge> inducePropagationGraph(Graph<NodePair, LabelEdge> connectivityGraph, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2, PropagationCoefficientPolicy policy) {

        Graph<NodePair, CoefficientEdge> propagationGraph = new DefaultDirectedWeightedGraph<>(CoefficientEdge.class);

        for (NodePair nodePair : connectivityGraph.vertexSet()) {
            propagationGraph.addVertex(nodePair);
        }

        for (NodePair nodePair : connectivityGraph.vertexSet()) {

            Node nodeGraph1 = nodePair.getFirstNode();
            Node nodeGraph2 = nodePair.getSecondNode();

            List<Map<String, Double>> propagationCoefficients = new ArrayList<>();

            try {
                propagationCoefficients = policy.evaluate(nodeGraph1, nodeGraph2, graph1, graph2);
            } catch (Exception e) {
                System.out.println("Not a policy");
            }

            Map<String, Double> countInLabelsTotal = propagationCoefficients.get(0);
            Map<String, Double> countOutLabelsTotal = propagationCoefficients.get(1);

            for (LabelEdge edge : connectivityGraph.incomingEdgesOf(nodePair)) {
                NodePair sourceNodePair = connectivityGraph.getEdgeSource(edge);
                NodePair targetNodePair = connectivityGraph.getEdgeTarget(edge);
                propagationGraph.addEdge(targetNodePair, sourceNodePair, new CoefficientEdge(countInLabelsTotal.get(edge.getLabel())));
            }

            for (LabelEdge edge : connectivityGraph.outgoingEdgesOf(nodePair)) {
                NodePair sourceNodePair = connectivityGraph.getEdgeSource(edge);
                NodePair targetNodePair = connectivityGraph.getEdgeTarget(edge);
                propagationGraph.addEdge(sourceNodePair, targetNodePair, new CoefficientEdge(countOutLabelsTotal.get(edge.getLabel())));
            }
        }

        return propagationGraph;
    }

    public Map<NodePair, Double> calculateInitialMapping(Graph<NodePair, CoefficientEdge> propagationGraph) {

//        double initial_fd_sim = Double.parseDouble(FDSim);
//        double initial_ucc_sim = Double.parseDouble(UCCSim);
//        double initial_ind_sim = Double.parseDouble(INDSim);

        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {
            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();
            Levenshtein l = new Levenshtein();
            double similarity;

            if (node1.getValue().startsWith("UCC#") && node2.getValue().startsWith("UCC#")) {
                String node1value = node1.getValue();
                String node2value = node2.getValue();
                int size1 = Integer.parseInt(node1value.substring(node1value.indexOf("#") + 1));
                int size2 = Integer.parseInt(node2value.substring(node2value.indexOf("#") + 1));

                similarity = 1.0 / (1.0 + Math.abs(size1 - size2));
            } else if (node1.getValue().startsWith("FD") && node2.getValue().startsWith("FD")) {
//                similarity = initial_fd_sim;
                similarity = 0.0;
            } else if (node1.getValue().startsWith("UCC") && node2.getValue().startsWith("UCC")) {
//                similarity = initial_ucc_sim;
                similarity = 0.0;
            } else if (node1.getValue().startsWith("IND") && node2.getValue().startsWith("IND")) {
//                similarity = initial_ind_sim;
                similarity = 0.0;
            } else if (node1.isIDNode() || node2.isIDNode()) {
                similarity = 0.0;
            } else {

                Column column1 = node1.getRepresentedColumn();
                Column column2 = node2.getRepresentedColumn();
                double labelSimilarity = l.compare(node1.getValue(), node2.getValue());

                if (column1 == null || column2 == null) {
                    similarity = labelSimilarity;

                } else {
                    double labelWeight = Double.parseDouble(this.getLabelScoreWeight());

                    double valueSimilarity = getValueSimilarityBetweenColumns(column1, column2);
                    similarity = labelWeight * labelSimilarity + (1 - labelWeight) * valueSimilarity;
                }
            }

            initialMapping.put(mappingPair, similarity);

        }
        return initialMapping;
    }

    public Map<NodePair, Double> similarityFlooding(Graph<NodePair, CoefficientEdge> propagationGraph, Map<NodePair, Double> initialMapping, FixpointFormula formula) {

        double EPSILON = 0.0001;
        int MAX_ITERATIONS = 200;
        boolean convergence = false;
        int iterationCount = 0;

        Map<NodePair, Double> sigma_0 = new HashMap<>(initialMapping);
        Map<NodePair, Double> sigma_i_plus_1 = new HashMap<>();
        Map<NodePair, Double> sigma_i = new HashMap<>(sigma_0);

        while (!convergence && iterationCount <= MAX_ITERATIONS) {

            double maxValueCurrentIteration = Double.MIN_VALUE;

            for (NodePair node : sigma_0.keySet()) {

                //Alle Nachbarn bekommen
                Set<CoefficientEdge> incomingEdges = propagationGraph.incomingEdgesOf(node); //Only neighbors from incoming edges
//                Set<NodePair> neighborNodes = incomingEdges.stream().map(propagationGraph::getEdgeSource).collect(Collectors.toSet());
                Set<NodePair> neighborNodes = new HashSet<>();
                for (CoefficientEdge edge : incomingEdges) {
                    NodePair source = propagationGraph.getEdgeSource(edge);
                    neighborNodes.add(source);
                }

                //Neuen Wert für Node auf Basis der Nachbarn berechnen
                double newValue;
                try {
                    newValue = formula.evaluate(node, neighborNodes, sigma_0, sigma_i, propagationGraph);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }

                //MaxWert der aktuellen Iteration speichern, damit später normalisieren möglich
                if (newValue > maxValueCurrentIteration) {
                    maxValueCurrentIteration = newValue;
                }

                sigma_i_plus_1.put(node, newValue);
            }

            //Normalisieren für die aktuelle Iteration
            for (NodePair node : sigma_0.keySet()) {
                sigma_i_plus_1.put(node, (sigma_i_plus_1.get(node) / maxValueCurrentIteration));
            }

            //Prüfen ob Residuum(sigma_i, sigma_i+1) konvergiert
            convergence = hasConverged(sigma_i, sigma_i_plus_1, EPSILON);
            sigma_i.putAll(sigma_i_plus_1);
            iterationCount++;
        }

        log.info("Ran {} Iterations for {} Nodes", iterationCount - 1, initialMapping.size());
        return sigma_i_plus_1;
    }

    //Only keep matching between elements of same kind, put simValue of IDNodes with their nameValues
    public Map<NodePair, Double> filterMapping(Map<NodePair, Double> mapping) {

        Map<NodePair, Double> filteredMapping = new HashMap<>();

        for (Map.Entry<NodePair, Double> entry : mapping.entrySet()) {

            Node node1 = entry.getKey().getFirstNode();
            Node node2 = entry.getKey().getSecondNode();
            Double simValue = entry.getValue();

            if (node1.isIDNode() && node2.isIDNode()) {

                NodePair pair = new NodePair(node1.getNameNode(), node2.getNameNode());

                //Only keep matches between Columns
                if (node1.getNodeType().equals(node2.getNodeType())) {
                    if (node1.getNodeType().equals(NodeType.COLUMN)) {
                        filteredMapping.put(pair, simValue);
                    }
                }
            }
        }
        return filteredMapping;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(getClass().getSimpleName());
        result.append("(");

        Field[] fields = getClass().getDeclaredFields();

        for (int i = 1; i < fields.length; i++) {
            try {
                fields[i].setAccessible(true);
                result.append(fields[i].getName()).append("=").append(fields[i].get(this)).append(" &  ");
                fields[i].setAccessible(false);
            } catch (IllegalAccessException ignored) {
            } // Cannot happen, we have set the field to be accessible
        }
        String res = result.toString();
        if (getClass().getDeclaredFields().length > 0) {
            res = res.substring(0, res.length() - 4);
        }
        return res + ")";
    }

    private Collection<FunctionalDependency> filterFunctionalDependencies(Collection<FunctionalDependency> functionalDependencies) {
        return functionalDependencies;
    }

    private Collection<UniqueColumnCombination> filterUniqueColumnCombinations(Collection<UniqueColumnCombination> uniqueColumnCombinations) {

        Collection<UniqueColumnCombination> filteredUCCs = new HashSet<>();

        double lengthScoreWeight = Double.parseDouble(this.lengthScoreWeight);
        double valueScoreWeight = Double.parseDouble(this.valueScoreWeight);
        double postionScoreWeight = Double.parseDouble(this.postionScoreWeight);
        double nameSuffixScoreWeight = Double.parseDouble(this.nameSuffixScoreWeight);
        double threshold = Double.parseDouble(this.uccFilterThreshold);

        for (UniqueColumnCombination ucc : uniqueColumnCombinations) {

            if (!ucc.getColumnCombination().isEmpty() && ucc.getColumnCombination().size() <= 3) { //(Primary) Keys very rarely consist of more than 3 columns
                double score = ucc.getPrimaryKeyScore(lengthScoreWeight, valueScoreWeight, postionScoreWeight, nameSuffixScoreWeight);

                if (score >= threshold) {
                    filteredUCCs.add(ucc);
                }
            }
        }

        System.out.println("Reduced AUCCs: " + uniqueColumnCombinations.size() + " -> " + filteredUCCs.size());

        return filteredUCCs;
    }

    private Collection<InclusionDependency> filterInclusionDependencies(Collection<InclusionDependency> inclusionDependencies) {
        return inclusionDependencies;
    }

    private Map<String, Double> getValueProbabilities(Column column) {
        Map<String, Integer> countMap = new HashMap<>();
        int total = 0;

        for (String value : column.getValues()) {
            if (!value.equalsIgnoreCase("null")) { //Ignore null values
                countMap.put(value, countMap.getOrDefault(value, 0) + 1);
                total++;
            }
        }

        Map<String, Double> probabilityMap = new HashMap<>();

        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            probabilityMap.put(entry.getKey(), entry.getValue() / (double) total);
        }

        return probabilityMap;
    }

    private double getDistanceBetweenProbabilityMaps(Map<String, Double> map1, Map<String, Double> map2) {

        double distance = 0.0;

        Set<String> possibleValues = new HashSet<>(map1.keySet());
        possibleValues.addAll(map2.keySet());

        for (String value : possibleValues) {
            Double probability1 = map1.getOrDefault(value, 0.0);
            Double probability2 = map2.getOrDefault(value, 0.0);

            distance += Math.pow(probability1 - probability2, 2);
        }
        return distance;
    }

}
