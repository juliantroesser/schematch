package de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.similarity.string.Cosine;
import de.uni_marburg.schematch.similarity.string.JaroWinkler;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import de.uni_marburg.schematch.similarity.string.LongestCommonSubsequence;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.*;
import java.util.stream.Collectors;

public class SimilarityFlooding extends Matcher {

    final static Logger log = (Logger) LogManager.getLogger(SimilarityFlooding.class);

    @Getter
    @Setter
    private String propagationCoefficientPolicy;
    @Getter
    @Setter
    private String fixpointFormula;
    @Getter
    @Setter
    private String useDependencyInformation;

    //TODO: Aktuell funktioniert es nur für Schemata mit genau einer Tabelle -> Alex fragen wie genau umsetzen
    //TODO: Threshold Select u.w. implementieren (wo?)
    //TODO: Alex fragen warum Metadata = null wenn in Ordner enthalten
    //TODO: Alex fragen wie andere Metriken einführbar

    @Override
    public float[][] match(MatchTask matchTask, MatchingStep matchStep) {

        PropagationCoefficientPolicy policy;
        FixpointFormula formula;

        policy = switch (propagationCoefficientPolicy) {
            case "INVERSE_AVERAGE" -> PropagationCoefficientPolicy.INVERSE_AVERAGE;
            case "INVERSE_PRODUCT" -> PropagationCoefficientPolicy.INVERSE_PRODUCT;
            case "CONSTANT_ONE" -> PropagationCoefficientPolicy.CONSTANT_ONE;
            default -> throw new RuntimeException("No such propagation coefficient policy: " + propagationCoefficientPolicy);
        };

        formula = switch (fixpointFormula) {
            case "BASIC" -> FixpointFormula.BASIC;
            case "FORMULA_A" -> FixpointFormula.FORMULA_A;
            case "FORMULA_B" -> FixpointFormula.FORMULA_B;
            case "FORMULA_C" -> FixpointFormula.FORMULA_C;
            default -> throw new RuntimeException("No such fixpoint formula: " + fixpointFormula);
        };

        Database sourceDb = matchTask.getScenario().getSourceDatabase();
        Database targetDb = matchTask.getScenario().getTargetDatabase();

        //Convert Database/Schemata into Graphs
        Graph<Node, LabelEdge> sourceGraph = transformIntoGraphRepresentation(sourceDb);
        Graph<Node, LabelEdge> targetGraph = transformIntoGraphRepresentation(targetDb);

        //Combine both Graphs into a connectivity-graph
        Graph<NodePair, LabelEdge> connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);
        //Transform the connectivity-graph into the propagation-graph on which the algorithm executes
        Graph<NodePair, CoefficientEdge> propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, policy);

        //Calculate the initial mapping (similarity) values
        Map<NodePair, Double> initialMapping = calculateInitialMapping(propagationGraph);

        //Run the similarity-flooding algorithm
        Map<NodePair, Double> floodingResults = similarityFlooding(propagationGraph, initialMapping, formula);

        //Only keep Mappings with elements from the original schemata
        //Map<NodePair, Double> filteredMapping = removeHelperNodePairs(floodingResults);

        Map<NodePair, Double> floodingResultsWithAppliedTypingConstraints = applyTypingConstraint(floodingResults); //Verschlechtert das Ergebnis leicht?

        //return convertSimilarityMapToMatrix(floodingResultsWithAppliedTypingConstraints, matchTask);
        return convertSimilarityMapToMatrix(floodingResults, matchTask);
    }

    private Graph<Node, LabelEdge> transformIntoGraphRepresentation(Database db) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        Node databaseNode = new Node("DATABASE", NodeType.DATABASE, null, true, false);
        Node tableNode = new Node("TABLE", NodeType.TABLE, null, true, false);
        Node columnNode = new Node("COLUMN", NodeType.COLUMN, null, true, false);
        Node columnTypeNode = new Node("COLUMN_TYPE", NodeType.COLUMN_TYPE, null, true, false);
        Node constraintNode = new Node("CONSTRAINT", NodeType.CONSTRAINT, null, true, false);

        graphRepresentation.addVertex(databaseNode);
        graphRepresentation.addVertex(tableNode);
        graphRepresentation.addVertex(columnNode);
        graphRepresentation.addVertex(columnTypeNode);
        graphRepresentation.addVertex(constraintNode);

        int tid = 1;
        int cid = 1;
        int ctid = 1;
        int cnstid = 1;

        Node currentDatabaseNode = new Node("S", NodeType.DATABASE, null,  true, true);
        graphRepresentation.addVertex(currentDatabaseNode);

        Node databaseName = new Node(db.getName(), NodeType.DATABASE, null, false, false);
        graphRepresentation.addVertex(databaseName);

        graphRepresentation.addEdge(currentDatabaseNode, databaseNode, new LabelEdge("TYPE"));
        graphRepresentation.addEdge(currentDatabaseNode, databaseName, new LabelEdge("NAME"));

        for(Table table : db.getTables()) {

            Node currentTableNode = new Node("T" + tid++, NodeType.TABLE, null, true, true);
            graphRepresentation.addVertex(currentTableNode);

            Node tableName = new Node(table.getName(), NodeType.TABLE, null, false, false);
            graphRepresentation.addVertex(tableName);

            graphRepresentation.addEdge(currentDatabaseNode, currentTableNode, new LabelEdge("TABLE"));
            graphRepresentation.addEdge(currentTableNode, tableNode, new LabelEdge("TYPE"));
            graphRepresentation.addEdge(currentTableNode, tableName, new LabelEdge("NAME"));

            for(Column column : table.getColumns()) {

                Node currentColumnNode = new Node("C" + cid++, NodeType.COLUMN, null, true, true);
                graphRepresentation.addVertex(currentColumnNode);

                Node columnName = new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, false);
                graphRepresentation.addVertex(columnName);

                graphRepresentation.addEdge(currentTableNode, currentColumnNode, new LabelEdge("COLUMN"));
                graphRepresentation.addEdge(currentColumnNode, columnNode, new LabelEdge("TYPE"));
                graphRepresentation.addEdge(currentColumnNode, columnName, new LabelEdge("NAME"));

                //Mit ColumnTypeNode verbinden
                Node columnDataType = new Node(column.getDatatype().toString(), NodeType.COLUMN_TYPE, column.getDatatype(), true, false);
                boolean dataTypeNodeExistsInGraph = graphRepresentation.containsVertex(columnDataType);

                if(dataTypeNodeExistsInGraph) { //Dann Kante zu

                    Set<LabelEdge> edgesOfIdToColumnType = graphRepresentation.incomingEdgesOf(columnDataType);
                    LabelEdge edgeOfIdToColumnType = edgesOfIdToColumnType.stream().findFirst().orElseThrow(() -> new NoSuchElementException("No such data Type edge present in the graph"));
                    Node columnTypeIdentifier = graphRepresentation.getEdgeSource(edgeOfIdToColumnType);

                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("SQL_TYPE"));


                } else { //Neuen Knoten anlegen

                    Node columnTypeIdentifier = new Node("CT" + ctid++, NodeType.COLUMN_TYPE, column.getDatatype(), true, true);

                    graphRepresentation.addVertex(columnDataType);
                    graphRepresentation.addVertex(columnTypeIdentifier);
                    graphRepresentation.addEdge(columnTypeIdentifier, columnDataType, new LabelEdge("NAME"));
                    graphRepresentation.addEdge(columnTypeIdentifier, columnTypeNode, new LabelEdge("TYPE"));
                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("SQL_TYPE"));
                }
                //TODO: Add Constraints

//                if(useDependencyInformation.equals("true")) {
//                    boolean isUniqueColumn = isUniqueColumn(column, db);
//                    boolean isFunctionalDependency = isFunctionalDependency(column, db);
//                }
//                //Get Constraints for Column

            }
        }

        return graphRepresentation;
    }

    private Graph<NodePair, LabelEdge> createConnectivityGraph(Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) {

        System.out.println("Graph1 has: #" + graph1.vertexSet().size() + " Nodes");
        System.out.println("Graph2 has: #" + graph2.vertexSet().size() + " Nodes");

        Graph<NodePair, LabelEdge> connectivityGraph = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        for (LabelEdge label1 : graph1.edgeSet()) {
            for (LabelEdge label2 : graph2.edgeSet()) {
                if (label1 != label2 && label1.equals(label2)) {

                    Node sourceVertex1 = graph1.getEdgeSource(label1);
                    Node targetVertex1 = graph1.getEdgeTarget(label1);

                    Node sourceVertex2 = graph2.getEdgeSource(label2);
                    Node targetVertex2 = graph2.getEdgeTarget(label2);

                    NodePair connectedSourceNode = new NodePair(sourceVertex1, sourceVertex2);
                    NodePair connectedTargetNode = new NodePair(targetVertex1, targetVertex2);

                    connectivityGraph.addVertex(connectedSourceNode);
                    connectivityGraph.addVertex(connectedTargetNode);
                    connectivityGraph.addEdge(connectedSourceNode, connectedTargetNode, new LabelEdge(label1.getValue()));
                }
            }
        }

        return connectivityGraph;
    }

    private Graph<NodePair, CoefficientEdge> inducePropagationGraph(Graph<NodePair, LabelEdge> connectivityGraph, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2, PropagationCoefficientPolicy policy) {

        Graph<NodePair, CoefficientEdge> propagationGraph = new DefaultDirectedWeightedGraph<>(CoefficientEdge.class);

        //Prüfen zu welchen Vorwärtskanten eine Rückwärtskante hinzugefügt werden muss
        Set<LabelEdge> reverseEdges = new HashSet<>();

        for (LabelEdge label : connectivityGraph.edgeSet()) {
            NodePair sourceNodePair = connectivityGraph.getEdgeSource(label);
            NodePair targetNodePair = connectivityGraph.getEdgeTarget(label);

            if (!connectivityGraph.containsEdge(targetNodePair, sourceNodePair)) {
                reverseEdges.add(label);
            }
        }

        //Rückwärtskanten hinzufügen
        for (LabelEdge label : reverseEdges) {
            NodePair sourceNodePair = connectivityGraph.getEdgeSource(label);
            NodePair targetNodePair = connectivityGraph.getEdgeTarget(label);

            //Kante umgekehrt einfügen
            connectivityGraph.addEdge(targetNodePair, sourceNodePair, new LabelEdge(label.toString()));
        }


        //Würde auch direkt über Graph1 und Graph2 gehen, anstatt Umweg über connectivityGraph, aber damit mehr Flexibilität aktuellen Weg beibehalten
        for (LabelEdge label : connectivityGraph.edgeSet()) {

            NodePair nodePairSource = connectivityGraph.getEdgeSource(label);
            Node node1 = nodePairSource.getFirstNode();
            Node node2 = nodePairSource.getSecondNode();

            double propagationCoefficient;

            try {
                propagationCoefficient = policy.evaluate(label, node1, node2, graph1, graph2);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            NodePair nodePairTarget = connectivityGraph.getEdgeTarget(label);

            //Kante zum Propagation Graph hinzufügen
            propagationGraph.addVertex(nodePairSource);
            propagationGraph.addVertex(nodePairTarget);
            propagationGraph.addEdge(nodePairSource, nodePairTarget, new CoefficientEdge(propagationCoefficient));
        }

        return propagationGraph;
    }

    private Map<NodePair, Double> calculateInitialMapping(Graph<NodePair, CoefficientEdge> propagationGraph) {

        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {

            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();

            Levenshtein l = new Levenshtein();

            //TODO: Custom Startwerte verbessern

            if (node1.getNodeType().equals(node2.getNodeType())) { // score: 0.8080795
                if (node1.getNodeType().equals(NodeType.DATABASE)) { //Falls beide Schema, dann auf Namensgleichheit achten, aktuell allerdings jeweils nur genau ein Schema Node pro Graph, muss also zusammenpassen
                    initialMapping.put(mappingPair, 1.0);

                } else if (node1.getNodeType().equals(NodeType.TABLE)) { //Falls beide Tables sind: Namen der Tables vergleichen
                    double similarity = l.compare(node1.getValue(), node2.getValue());
                    initialMapping.put(mappingPair, similarity);

                } else if (node1.getNodeType().equals(NodeType.COLUMN)) { //Falls beide Columns sind: Namen der Columns vergleichen
                    double similarity = l.compare(node1.getValue(), node2.getValue());
                    initialMapping.put(mappingPair, similarity);

                } else if (node1.getNodeType().equals(NodeType.COLUMN_TYPE) && node1.getDatatype() != null && node2.getDatatype() != null) {
                    if (node1.getDatatype().equals(node2.getDatatype())) {
                        initialMapping.put(mappingPair, 1.0);
                    } else {
                        initialMapping.put(mappingPair, 0.1);
                    }
                } else if (node1.getNodeType().equals(NodeType.CONSTRAINT)) {
                    if (node1.getValue().equals("PRIMARY KEY") && node2.getValue().equals("PRIMARY KEY")) {
                        initialMapping.put(mappingPair, 1.0);
                    } else if ((node1.getValue().equals("PRIMARY KEY") && node2.getValue().equals("UNIQUE")) || (node1.getValue().equals("UNIQUE") && node2.getValue().equals("PRIMARY KEY"))) {
                        initialMapping.put(mappingPair, 0.75);
                    }
                } else {
                    initialMapping.put(mappingPair, 0.0);
                }
            } else {
                initialMapping.put(mappingPair, 0.0);
            }
        }

//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.69934183
//
//            Node node1 = mappingPair.getFirstNode();
//            Node node2 = mappingPair.getSecondNode();
//
//            if(node1.isIDNode() || node2.isIDNode()) { //Mapping with artifical Nodes
//                initialMapping.put(mappingPair, 0.0); //Bei 0 lassen beste Ergebnisse //Bei negativen Werten keine Konvergenz //>0 monoton fallender Score
//
//            } else { //Ansonsten String Similarity bestimmen
//
//                //Beeinflusst Similarity Wert die Anzahl der Iterationen?
//
//                Levenshtein l = new Levenshtein();
//                double similarity = l.compare(node1.getValue(), node2.getValue());
//                initialMapping.put(mappingPair, similarity);

//                Cosine cosine = new Cosine(); //score: 0.6698534
//                double similarity = cosine.compare(node1.getValue(), node2.getValue());
//                initialMapping.put(mappingPair, similarity);

//                JaroWinkler jaroWinkler = new JaroWinkler(); //score: 0.5128041
//                double similarity = jaroWinkler.compare(node1.getValue(), node2.getValue());
//                initialMapping.put(mappingPair, similarity);

//                LongestCommonSubsequence lcs = new LongestCommonSubsequence(); //score: 0.65468425
//                double similarity = lcs.compare(node1.getValue(), node2.getValue());
//                initialMapping.put(mappingPair, similarity);
//            }
//        }

//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.1686265
//            initialMapping.put(mappingPair, 1.0);
//        }

//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.17336717
//            initialMapping.put(mappingPair, 0.25);
//        }

//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.18792313
//            initialMapping.put(mappingPair, 0.25);
//        }

//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.21153724
//            initialMapping.put(mappingPair, 0.1);
//        }
//
//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.19543971
//            initialMapping.put(mappingPair, 0.05);
//        }
//
//        for (NodePair mappingPair : propagationGraph.vertexSet()) { //Score: 0.19543971
//            initialMapping.put(mappingPair, 0.01);
//        }

        return initialMapping;
    }

    private Map<NodePair, Double> similarityFlooding(Graph<NodePair, CoefficientEdge> propagationGraph, Map<NodePair, Double> initialMapping, FixpointFormula formula) {

        System.out.println("Propagation Graph: #Arcs: " + propagationGraph.edgeSet().size());
        System.out.println("Propagation Graph: #Node Pairs: " + propagationGraph.vertexSet().size());

        Map<NodePair, Double> sigma_0 = new HashMap<>(initialMapping);
        Map<NodePair, Double> sigma_i = new HashMap<>(initialMapping);
        Map<NodePair, Double> sigma_i_plus_1 = new HashMap<>();

        double EPSILON = 0.05;
        int MAX_ITERATIONS = 100000;

        boolean convergence = false;
        int iterationCount = 0;

        while (!convergence && iterationCount < MAX_ITERATIONS) {

            double maxValueCurrentIteration = -1;

            for (NodePair node : sigma_0.keySet()) {

                //Alle Nachbarn bekommen (=Startpunkte eingehender Kanten)
                Set<CoefficientEdge> incomingEdges = propagationGraph.incomingEdgesOf(node);

                Set<NodePair> neighborNodes = incomingEdges.stream()
                        .map(propagationGraph::getEdgeSource)
                        .collect(Collectors.toSet());

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
            sigma_i_plus_1.clear();
            iterationCount++;
        }

        System.out.println("Iterations: " + iterationCount);

        //return filterMapping(sigma_i);
        return sigma_i;
    }

    private boolean hasConverged(Map<NodePair, Double> sigma_i, Map<NodePair, Double> sigma_i_plus_1, double epsilon) {

        double residualSum = 0;

        for (NodePair node : sigma_i.keySet()) {

            double value_i = sigma_i.get(node);
            double value_i_plus_1 = sigma_i_plus_1.get(node);

            residualSum = residualSum + Math.abs(value_i - value_i_plus_1);
        }

        return residualSum < epsilon;
    }

    private float[][] convertSimilarityMapToMatrix(Map<NodePair, Double> mapping, MatchTask matchTask) {

        List<Column> sourceColumns = matchTask.getScenario().getSourceDatabase().getTables().stream().findFirst().get().getColumns();
        List<Column> targetColumns = matchTask.getScenario().getTargetDatabase().getTables().stream().findFirst().get().getColumns();

        //float[][] simMatrix = new float[sourceColumns.size()][targetColumns.size()];
        float[][] simMatrix = matchTask.getEmptySimMatrix();

        for(int i = 0; i < sourceColumns.size(); i++) {

            String sourceLabel = sourceColumns.get(i).getLabel();
            Node sourceNode = new Node(sourceLabel, NodeType.COLUMN, null, false, false);

            for(int j = 0; j < targetColumns.size(); j++) {

                String targetLabel = targetColumns.get(j).getLabel();
                Node targetNode = new Node(targetLabel, NodeType.COLUMN, null, false, false);

                float similarity = mapping.getOrDefault(new NodePair(sourceNode, targetNode), 0.0).floatValue();
                simMatrix[i][j] = similarity;
            }
        }

        return simMatrix;
    }

    public Map<NodePair, Double> applyTypingConstraint(Map<NodePair, Double> mapping) {

        Map<NodePair, Double> filteredMapping = new HashMap<>();

        for(Map.Entry<NodePair, Double> entry : mapping.entrySet()) {

            Node firstNode = entry.getKey().getFirstNode();
            Node secondNode = entry.getKey().getSecondNode();

            if(!firstNode.isHelperNode() && !secondNode.isHelperNode() && firstNode.getNodeType().equals(secondNode.getNodeType())) { //If both Nodes represent the same Type like Schema, Table, Column, etc. then the mapping is kept

                if(firstNode.getNodeType().equals(NodeType.COLUMN) && firstNode.getDatatype() != null && secondNode.getDatatype() != null) { //If both Nodes represent a Column then
                    if(firstNode.getDatatype().equals(secondNode.getDatatype())) { //both should have the same Datatype, otherwise the mapping is discarded
                        filteredMapping.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    filteredMapping.put(entry.getKey(), entry.getValue()); //The rest of the Mappings between Tables, Schemas, etc.
                }
            }
        }

        return filteredMapping;
    }

//    private boolean isUniqueColumn(Column column, Database db) {
//
//        //Alle UniqueColumnCombinations der Grösse 1 in denen die aktuelle Spalte (column) enthalten ist
//        return !db.getMetadata().getUniqueColumnCombinations(column, 1).isEmpty();
//    }
//
//    private boolean isFunctionalDependency(Column column, Database db) {
//
//        return !db.getMetadata().getFunctionalDependencies(column, 1).isEmpty();
//    }

//    private boolean isColumn(Column column, Database db) {
//
//        db.getMetadata().getFunctionalDependencies()
//
//    }

}
