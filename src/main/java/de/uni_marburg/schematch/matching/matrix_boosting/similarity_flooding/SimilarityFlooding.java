package de.uni_marburg.schematch.matching.matrix_boosting.similarity_flooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.Datatype;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SimilarityFlooding extends Matcher {

    private String useValentineGraphRepresentation;
    private String propagationCoefficientPolicy;
    private String fixpointFormula;
    private String epsilon;
    private String maxIterations;
    private String useUCCInformation;
    private String useFDInformation;

    //TODO: Aktuell funktioniert es nur für Schemata mit genau einer Tabelle -> Alex fragen wie genau umsetzen

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
        Graph<Node, LabelEdge> sourceGraph;
        Graph<Node, LabelEdge> targetGraph;

        if (Boolean.parseBoolean(useValentineGraphRepresentation)) {
            sourceGraph = transformIntoValentineGraphRepresentation(sourceDb);
            targetGraph = transformIntoValentineGraphRepresentation(targetDb);
        } else {
            sourceGraph = transformIntoGraphRepresentation(sourceDb);
            targetGraph = transformIntoGraphRepresentation(targetDb);
        }

        //Combine both Graphs into a connectivity-graph
        Graph<NodePair, LabelEdge> connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);
        //Transform the connectivity-graph into the propagation-graph on which the algorithm executes
        Graph<NodePair, CoefficientEdge> propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, policy);

        //Calculate the initial mapping (similarity) values
        Map<NodePair, Double> initialMapping = calculateInitialMappingValentine(propagationGraph);

        //Run the similarity-flooding algorithm
        Map<NodePair, Double> floodingResults = similarityFlooding(propagationGraph, initialMapping, formula);

        Map<NodePair, Double> filteredFloodingResults = filterMapping(floodingResults); //Verschlechtert das Ergebnis leicht?

        return convertSimilarityMapToMatrix(filteredFloodingResults, matchTask);
    }

    public Graph<Node, LabelEdge> transformIntoGraphRepresentation(Database db) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        //Valentine:
        //table_node = Node(TABLE, schema.name)
        //column_node = Node(COLUMN, schema.name)
        //col_type_node = Node(COLUMN_TYPE, schema.name)

        //Valentine: Ab hier __init__ (Klasse Graph)

        Node databaseNode = new Node("Database", NodeType.DATABASE, null, false, null, null);
        Node tableNode = new Node("Table", NodeType.TABLE, null, false, null, null);
        Node columnNode = new Node("Column", NodeType.COLUMN, null, false, null, null);
        Node columnTypeNode = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null);
        Node constraintNode = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null);

        graphRepresentation.addVertex(databaseNode);
        graphRepresentation.addVertex(tableNode);
        graphRepresentation.addVertex(columnNode);
        graphRepresentation.addVertex(columnTypeNode);
        graphRepresentation.addVertex(constraintNode);

        //Valentine: Unique_id = 1 //Nur eine ID für alle Knoten benutzt

        int uniqueID = 1;

        //TODO: Valentine benutzt Keinen Extra Schema/Database Typ Knoten, sondern fängt direkt mit der Table als Root an

        Node databaseName = new Node(db.getName(), NodeType.DATABASE, null, false, null, null);
        graphRepresentation.addVertex(databaseName);

        Node currentDatabaseNode = new Node("NodeID" + uniqueID++, NodeType.DATABASE, null, true, databaseName, null);
        graphRepresentation.addVertex(currentDatabaseNode);

        graphRepresentation.addEdge(currentDatabaseNode, databaseNode, new LabelEdge("type"));
        graphRepresentation.addEdge(currentDatabaseNode, databaseName, new LabelEdge("name"));

        //Valentine: Ab hier create_graph()

        for (Table table : db.getTables()) {

            Node tableName = new Node(table.getName(), NodeType.TABLE, null, false, null, null);
            graphRepresentation.addVertex(tableName);

            Node currentTableNode = new Node("NodeID" + uniqueID++, NodeType.TABLE, null, true, tableName, null);
            graphRepresentation.addVertex(currentTableNode);

            graphRepresentation.addEdge(currentDatabaseNode, currentTableNode, new LabelEdge("table"));
            graphRepresentation.addEdge(currentTableNode, tableNode, new LabelEdge("type"));
            graphRepresentation.addEdge(currentTableNode, tableName, new LabelEdge("name"));

            //Valentine: Ab hier add_and_connect(column) 1. Teil
            for (Column column : table.getColumns()) {

                Node columnName = new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, null, table);
                graphRepresentation.addVertex(columnName);

                //Valentine: IDNode = Node(NodeID#, schema.name)
                Node currentColumnNode = new Node("NodeID" + uniqueID++, NodeType.COLUMN, column.getDatatype(), true, columnName, table);
                graphRepresentation.addVertex(currentColumnNode);

                graphRepresentation.addEdge(currentTableNode, currentColumnNode, new LabelEdge("column"));
                graphRepresentation.addEdge(currentColumnNode, columnNode, new LabelEdge("type"));
                graphRepresentation.addEdge(currentColumnNode, columnName, new LabelEdge("name"));

                //Mit ColumnTypeNode verbinden
                //Valentine: DataTypeNode: Node(column.DataType, schema.Name)
                Node columnDataType = new Node(column.getDatatype().toString(), NodeType.COLUMN_TYPE, column.getDatatype(), true, null, null);
                boolean dataTypeNodeExistsInGraph = graphRepresentation.containsVertex(columnDataType);

                if (dataTypeNodeExistsInGraph) { //Dann Kante zu

                    Set<LabelEdge> edgesOfIdToColumnType = graphRepresentation.incomingEdgesOf(columnDataType);
                    LabelEdge edgeOfIdToColumnType = edgesOfIdToColumnType.stream().findFirst().orElseThrow(() -> new NoSuchElementException("No such data Type edge present in the graph"));
                    Node columnTypeIdentifier = graphRepresentation.getEdgeSource(edgeOfIdToColumnType);
                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("dataType"));

                } else { //Neuen Knoten anlegen

                    Node columnTypeIdentifier = new Node("NodeID" + uniqueID++, NodeType.COLUMN_TYPE, column.getDatatype(), true, columnDataType, null);

                    graphRepresentation.addVertex(columnDataType);
                    graphRepresentation.addVertex(columnTypeIdentifier);
                    graphRepresentation.addEdge(columnTypeIdentifier, columnDataType, new LabelEdge("NAME"));
                    graphRepresentation.addEdge(columnTypeIdentifier, columnTypeNode, new LabelEdge("TYPE"));
                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("SQL_TYPE"));
                }
                //TODO: Add Constraints

                if (useUCCInformation.equals("true")) {

                    //ucc info
                    boolean isUniqueColumn = isUniqueColumn(column, db);

                    if (isUniqueColumn) {

                        Node uccNode = new Node("UniqueColumnCombination", NodeType.CONSTRAINT, null, false, null, null);
                        boolean uccNodeExistsInGraph = graphRepresentation.containsVertex(uccNode);

                        if (uccNodeExistsInGraph) { //Dann nur Kante zu dieser
                            graphRepresentation.addEdge(currentColumnNode, uccNode, new LabelEdge("ucc"));
                        } else {
                            graphRepresentation.addVertex(uccNode);
                            graphRepresentation.addEdge(currentColumnNode, uccNode, new LabelEdge("ucc"));
                        }
                    }
                }
            }

            //Hier anfangen
            if (useFDInformation.equals("true")) {
                for (Column column : table.getColumns()) {

                    //fd info
                    List<Column> dependants = getAllDependants(column, db);

                    if (!dependants.isEmpty()) {

                        LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, null, null)).stream().findFirst().get();
                        Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);

                        for (Column dependant : dependants) {

                            LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, null)).stream().findFirst().get();
                            Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                            graphRepresentation.addEdge(determinantIDNode, dependantIDNode, new LabelEdge("fd"));
                        }
                    }
                }
            }

        }

        //Erst Kanten zwischen Knoten hinzufügen wenn auch alle Columns im Graph sind

        return graphRepresentation;
    }

    public Graph<Node, LabelEdge> transformIntoValentineGraphRepresentation(Database db) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        //Valentine:
        //table_node = Node(TABLE, schema.name)
        //column_node = Node(COLUMN, schema.name)
        //col_type_node = Node(COLUMN_TYPE, schema.name)

        //Valentine: Ab hier __init__ (Klasse Graph)

        Node tableNode = new Node("Table", NodeType.TABLE, null, false, null, null);
        Node columnNode = new Node("Column", NodeType.COLUMN, null, false, null, null);
        Node columnTypeNode = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null);

        graphRepresentation.addVertex(tableNode);
        graphRepresentation.addVertex(columnNode);
        graphRepresentation.addVertex(columnTypeNode);

        //Valentine: Unique_id = 1 //Nur eine ID für alle Knoten benutzt

        int uniqueId = 1;

        //TODO: Valentine benutzt Keinen Extra Schema/Database Typ Knoten, sondern fängt direkt mit der Table als Root an

        //Valentine: Ab hier create_graph()

        if (db.getTables().size() > 1) {
            throw new RuntimeException("Valentine Graph Representation only supports datasets with one table");
        }

        Table table = db.getTables().get(0); //Valentine unterstützt nur das Matchen von einer Tabelle zu genau einer anderen Tabelle

        Node tableName = new Node(table.getName(), NodeType.TABLE, null, false, null, null);
        graphRepresentation.addVertex(tableName);

        Node currentTableNode = new Node("NodeID" + uniqueId++, NodeType.TABLE, null, true, tableName, null);
        graphRepresentation.addVertex(currentTableNode);

        graphRepresentation.addEdge(currentTableNode, tableNode, new LabelEdge("type"));
        graphRepresentation.addEdge(currentTableNode, tableName, new LabelEdge("name"));

        for (Column column : table.getColumns()) {

            Node columnName = new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, null, table);
            graphRepresentation.addVertex(columnName);

            //Valentine: IDNode = Node(NodeID#, schema.name)
            Node currentColumnNode = new Node("NodeID" + uniqueId++, NodeType.COLUMN, column.getDatatype(), true, columnName, table);
            graphRepresentation.addVertex(currentColumnNode);

            //Valentine: AttributeNode: Node(column.name, schema.name)

            graphRepresentation.addEdge(currentTableNode, currentColumnNode, new LabelEdge("column"));
            graphRepresentation.addEdge(currentColumnNode, columnNode, new LabelEdge("type"));
            graphRepresentation.addEdge(currentColumnNode, columnName, new LabelEdge("name"));

            //Mit ColumnTypeNode verbinden
            //Valentine: DataTypeNode: Node(column.DataType, schema.Name)

            String valentineDataType = translateIntoValentineDataType(column.getDatatype());

            Node columnDataType = new Node(valentineDataType, NodeType.COLUMN_TYPE, null, false, null, null);
            boolean dataTypeNodeExistsInGraph = graphRepresentation.containsVertex(columnDataType);

            if (dataTypeNodeExistsInGraph) { //Dann Kante zu

                Set<LabelEdge> edgesOfIdToColumnType = graphRepresentation.incomingEdgesOf(columnDataType);
                LabelEdge edgeOfIdToColumnType = edgesOfIdToColumnType.stream().findFirst().orElseThrow(() -> new NoSuchElementException("No such data Type edge present in the graph"));
                Node columnTypeIdentifier = graphRepresentation.getEdgeSource(edgeOfIdToColumnType);

                //TODO: SQL_Type -> DATA_TYPE ändern
                graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("SQLtype"));


            } else { //Neuen Knoten anlegen

                Node columnTypeIdentifier = new Node("NodeID" + uniqueId++, NodeType.COLUMN_TYPE, null, true, columnDataType, null);

                graphRepresentation.addVertex(columnDataType);
                graphRepresentation.addVertex(columnTypeIdentifier);
                graphRepresentation.addEdge(columnTypeIdentifier, columnDataType, new LabelEdge("name"));
                graphRepresentation.addEdge(columnTypeIdentifier, columnTypeNode, new LabelEdge("type"));
                graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("SQLtype"));
            }
        }

        return graphRepresentation;
    }

    public Graph<NodePair, LabelEdge> createConnectivityGraph(Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) {

        Graph<NodePair, LabelEdge> connectivityGraph = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        //Valentine:
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
                    connectivityGraph.addEdge(connectedSourceNode, connectedTargetNode, new LabelEdge(label1.getValue()));
                }
            }
        }

        return connectivityGraph;
    }

    public Graph<NodePair, CoefficientEdge> inducePropagationGraph(Graph<NodePair, LabelEdge> connectivityGraph, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2, PropagationCoefficientPolicy policy) {

        Graph<NodePair, LabelEdge> connectivityGraphCopy = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        //Graph kopieren (shallow)
        for (LabelEdge edge : connectivityGraph.edgeSet()) {

            NodePair source = connectivityGraph.getEdgeSource(edge);
            NodePair target = connectivityGraph.getEdgeTarget(edge);

            connectivityGraphCopy.addVertex(source);
            connectivityGraphCopy.addVertex(target);
            connectivityGraphCopy.addEdge(source, target, edge);
        }

        Graph<NodePair, CoefficientEdge> propagationGraph = new DefaultDirectedWeightedGraph<>(CoefficientEdge.class);

        //Prüfen zu welchen Vorwärtskanten eine Rückwärtskante hinzugefügt werden muss
        Set<LabelEdge> reverseEdges = new HashSet<>();

        for (LabelEdge label : connectivityGraphCopy.edgeSet()) {
            NodePair sourceNodePair = connectivityGraphCopy.getEdgeSource(label);
            NodePair targetNodePair = connectivityGraphCopy.getEdgeTarget(label);

            if (!connectivityGraphCopy.containsEdge(targetNodePair, sourceNodePair)) {
                reverseEdges.add(label);
            }
        }

        //Rückwärtskanten hinzufügen
        for (LabelEdge label : reverseEdges) {
            NodePair sourceNodePair = connectivityGraphCopy.getEdgeSource(label);
            NodePair targetNodePair = connectivityGraphCopy.getEdgeTarget(label);

            //Kante umgekehrt einfügen
            connectivityGraphCopy.addEdge(targetNodePair, sourceNodePair, new LabelEdge(label.toString()));
        }


        //Würde auch direkt über Graph1 und Graph2 gehen, anstatt Umweg über connectivityGraph, aber damit mehr Flexibilität aktuellen Weg beibehalten
        for (LabelEdge label : connectivityGraphCopy.edgeSet()) {

            NodePair nodePairSource = connectivityGraphCopy.getEdgeSource(label);
            Node node1 = nodePairSource.getFirstNode();
            Node node2 = nodePairSource.getSecondNode();

            double propagationCoefficient;

            try {
                propagationCoefficient = policy.evaluate(label, node1, node2, graph1, graph2);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }

            NodePair nodePairTarget = connectivityGraphCopy.getEdgeTarget(label);

            //Kante zum Propagation Graph hinzufügen
            propagationGraph.addVertex(nodePairSource);
            propagationGraph.addVertex(nodePairTarget);
            propagationGraph.addEdge(nodePairSource, nodePairTarget, new CoefficientEdge(propagationCoefficient));
        }

        return propagationGraph;
    }

    public Map<NodePair, Double> calculateInitialMapping(Graph<NodePair, CoefficientEdge> propagationGraph) {

        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {

            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();

            Levenshtein l = new Levenshtein();

            //TODO: Custom Startwerte verbessern

            if (node1.getNodeType().equals(NodeType.CONSTRAINT) || node2.getNodeType().equals(NodeType.CONSTRAINT)) {
                initialMapping.put(mappingPair, 1.0); //Da aktuell nur UCC Knoten für Constraint exisitiert und somit nur ein Mapping von UCC->UCC
            }

            if (node1.getNodeType().equals(node2.getNodeType())) { // score: 0.8080795
                if (node1.getNodeType().equals(NodeType.DATABASE)) { //Falls beide Schema, dann auf Namensgleichheit achten, aktuell allerdings jeweils nur genau ein Schema Node pro Graph, muss also zusammenpassen
                    initialMapping.put(mappingPair, 1.0);

                } else if (node1.getNodeType().equals(NodeType.TABLE)) { //Falls beide Tables sind: Namen der Tables vergleichen
                    double similarity = l.compare(node1.getValue(), node2.getValue());
                    initialMapping.put(mappingPair, similarity);

                } else if (node1.getNodeType().equals(NodeType.COLUMN)) { //Falls beide Columns sind: Namen der Columns vergleichen
                    double similarity = l.compare(node1.getValue(), node2.getValue());
                    initialMapping.put(mappingPair, similarity);

                } else if (node1.getNodeType().equals(NodeType.COLUMN_TYPE)) {
                    if (node1.getDatatype() != null && node2.getDatatype() != null && node1.getDatatype().equals(node2.getDatatype())) {
                        initialMapping.put(mappingPair, 1.0);
                    } else {
                        initialMapping.put(mappingPair, 0.1); //0.1 funktioniert am besten
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

        return initialMapping;
    }

    public Map<NodePair, Double> calculateInitialMappingValentine(Graph<NodePair, CoefficientEdge> propagationGraph) {

        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {

            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();

            if (node1.isIDNode() || node2.isIDNode()) { //Mapping with artifical Nodes
                initialMapping.put(mappingPair, 0.0); //Bei 0 lassen beste Ergebnisse //Bei negativen Werten keine Konvergenz //>0 monoton fallender Score

            } else { //Ansonsten String Similarity bestimmen

                //Beeinflusst Similarity Wert die Anzahl der Iterationen?

                Levenshtein l = new Levenshtein();
                double similarity = l.compare(node1.getValue(), node2.getValue());
                initialMapping.put(mappingPair, similarity);
            }
        }

        return initialMapping;
    }

    public Map<NodePair, Double> similarityFlooding(Graph<NodePair, CoefficientEdge> propagationGraph, Map<NodePair, Double> initialMapping, FixpointFormula formula) {

        Map<NodePair, Double> sigma_0 = new HashMap<>(initialMapping);
        Map<NodePair, Double> sigma_i = new HashMap<>(initialMapping);
        Map<NodePair, Double> sigma_i_plus_1 = new HashMap<>();

        double EPSILON = Double.parseDouble(epsilon);
        int MAX_ITERATIONS = Integer.parseInt(maxIterations);

        boolean convergence = false;
        int iterationCount = 0;

        while (!convergence && iterationCount < MAX_ITERATIONS) {

            double maxValueCurrentIteration = Double.MIN_VALUE;

            for (NodePair node : sigma_0.keySet()) {

                //Alle Nachbarn bekommen (Ausgehend und Eingehend)
                Set<CoefficientEdge> incomingEdges = propagationGraph.incomingEdgesOf(node);
                //Set<CoefficientEdge> outgoingEdges = propagationGraph.outgoingEdgesOf(node);

                Set<NodePair> neighborNodesIncomingEdges = incomingEdges.stream().map(propagationGraph::getEdgeSource).collect(Collectors.toSet());
                //Set<NodePair> neighborNodesOutgoingEdges = outgoingEdges.stream().map(propagationGraph::getEdgeTarget).collect(Collectors.toSet());

                Set<NodePair> neighborNodes = new HashSet<>();
                neighborNodes.addAll(neighborNodesIncomingEdges);
                //neighborNodes.addAll(neighborNodesOutgoingEdges);

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

        return sigma_i;
    }

    public boolean hasConverged(Map<NodePair, Double> sigma_i, Map<NodePair, Double> sigma_i_plus_1, double epsilon) {
        return calcResidualVector(sigma_i, sigma_i_plus_1) < epsilon;
    }

    public double calcResidualVector(Map<NodePair, Double> sigma_i, Map<NodePair, Double> sigma_i_plus_1) {

        double residualSum = 0;

        for (NodePair node : sigma_i.keySet()) {

            double value_i = sigma_i.get(node);
            double value_i_plus_1 = sigma_i_plus_1.get(node);

            residualSum = residualSum + Math.pow((value_i - value_i_plus_1), 2);
        }

        return Math.sqrt(residualSum);
    }

    public float[][] convertSimilarityMapToMatrix(Map<NodePair, Double> mapping, MatchTask matchTask) {

        float[][] simMatrix = matchTask.getEmptySimMatrix();

        for (Table sourceTable : matchTask.getScenario().getSourceDatabase().getTables()) {
            for (Table targetTable : matchTask.getScenario().getTargetDatabase().getTables()) {

                List<Column> sourceColumns = sourceTable.getColumns();
                List<Column> targetColumns = targetTable.getColumns();

                for (int i = 0; i < sourceColumns.size(); i++) {

                    String sourceLabel = sourceColumns.get(i).getLabel();
                    Node sourceNode = new Node(sourceLabel, NodeType.COLUMN, null, false, null, sourceTable);

                    for (int j = 0; j < targetColumns.size(); j++) {

                        String targetLabel = targetColumns.get(j).getLabel();
                        Node targetNode = new Node(targetLabel, NodeType.COLUMN, null, false, null, targetTable);

                        //TODO: Problem falls zwei verschiedene Tabellen beide Source sind und Attribut mit gleichem Namen haben -> Node langen Namen geben

                        float similarity = mapping.getOrDefault(new NodePair(sourceNode, targetNode), 0.0).floatValue();

                        simMatrix[sourceTable.getOffset() + i][targetTable.getOffset() + j] = similarity;
                    }
                }
            }
        }

        return simMatrix;
    }

    private boolean isUniqueColumn(Column column, Database db) {
        try {
            List<UniqueColumnCombination> uccsThatIncludeColumn = db.getMetadata().getUniqueColumnCombinations(column, 1).stream().toList();
            return !uccsThatIncludeColumn.isEmpty();

        } catch (Exception e) {
            return false;
        }
    }

    private List<Column> getAllDependants(Column column, Database db) {

        List<Column> dependantsOfColumn = new ArrayList<>();

        try {
            for (FunctionalDependency dependency : db.getMetadata().getFunctionalDependencies(column, 1)) {
                dependantsOfColumn.add(dependency.getDependant());
            }
        } catch (Exception e) {
            dependantsOfColumn = new ArrayList<>();
        }

        return dependantsOfColumn;
    }

    private String translateIntoValentineDataType(Datatype datatype) {

        return switch (datatype) {
            case STRING -> "varchar";
            case INTEGER -> "int";
            case BOOLEAN -> "boolean";
            case FLOAT -> "float";
            case DATE -> "date";
            case TEXT -> "varchar";// long string (e.g., comments or descriptions) not implemented yet
            case GEO_LOCATION -> "location";
        };

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

                if (node1.getNodeType().equals(node2.getNodeType())) {
                    if (node1.getNodeType().equals(NodeType.DATABASE) || node1.getNodeType().equals(NodeType.TABLE) || node1.getNodeType().equals(NodeType.COLUMN)) {
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
        for (Field field : getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                result.append(field.getName()).append("=").append(field.get(this)).append(" &  ");
                field.setAccessible(false);
            } catch (IllegalAccessException ignored) {
            } // Cannot happen, we have set the field to be accessible
        }
        String res = result.toString();
        if (getClass().getDeclaredFields().length > 0) {
            res = res.substring(0, res.length() - 4);
        }
        return res + ")";
    }
}
