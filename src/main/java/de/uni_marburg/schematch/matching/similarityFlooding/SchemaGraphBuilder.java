package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;

import java.util.*;

class SchemaGraphBuilder {
    private static final Logger log = LogManager.getLogger(SchemaGraphBuilder.class);
    private final DependencyFilter dependencyFilter;

    SchemaGraphBuilder(String uccFilterThreshold, String indFilterThreshold, String fdFilterThreshold) {
        this.dependencyFilter = new DependencyFilter(uccFilterThreshold, indFilterThreshold, fdFilterThreshold);
    }

    static Graph<NodePair, LabelEdge> createConnectivityGraph(Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2) {

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

    static Graph<NodePair, CoefficientEdge> inducePropagationGraph
            (Graph<NodePair, LabelEdge> connectivityGraph, Graph<Node, LabelEdge> graph1, Graph<Node, LabelEdge> graph2, PropagationCoefficientPolicy
                    policy) {

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
                log.info("Not a policy");
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

    Graph<Node, LabelEdge> transformIntoGraphRepresentationSchema(Database db) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        buildGraphRepresentation(db, graphRepresentation);

        //Extending the schema-graph with dependency information:

        Node constraintNode = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null, null);
        graphRepresentation.addVertex(constraintNode);

        if(!this.dependencyFilter.getFdFilterThreshold().isEmpty()) {
            functionalDependencies(db, graphRepresentation, constraintNode);
        }

        if(!this.dependencyFilter.getUccFilterThreshold().isEmpty()) {
            uniqueColumnCombinations(db, graphRepresentation, constraintNode);
        }

        if(!this.dependencyFilter.getIndFilterThreshold().isEmpty()) {
            inclusionDependencies(db, graphRepresentation, constraintNode);
        }

        return graphRepresentation;
    }

    private void buildGraphRepresentation(Database db, Graph<Node, LabelEdge> graphRepresentation) {
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
    }

    private void inclusionDependencies(Database db, Graph<Node, LabelEdge> graphRepresentation, Node constraintNode) {
        List<InclusionDependency> inclusionDependencies = db.getMetadata().getInds().stream().toList();
        int indID = 1;

        for (InclusionDependency inclusionDependency : dependencyFilter.filterInclusionDependencies(inclusionDependencies)) {

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

    @Deprecated
    private void uniqueColumnCombinationsLegacy(Database db, Graph<Node, LabelEdge> graphRepresentation, Node constraintNode) {
        List<UniqueColumnCombination> uniqueColumnCombinations = db.getMetadata().getUccs().stream().toList();
        int uccID = 1;

        for (UniqueColumnCombination ucc : dependencyFilter.filterUniqueColumnCombinations(uniqueColumnCombinations)) {

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

    private void uniqueColumnCombinations(Database db, Graph<Node, LabelEdge> graphRepresentation, Node constraintNode) {
        Collection<UniqueColumnCombination> uniqueColumnCombinations;

        uniqueColumnCombinations = db.getMetadata().getUccs().stream().toList();

        int uccID = 1;

        for (UniqueColumnCombination uniqueColumnCombination : dependencyFilter.filterUniqueColumnCombinations(uniqueColumnCombinations)) {

            Node uccNode = new Node("UCC" + uccID++, NodeType.CONSTRAINT, null, true, null, null, null);
            graphRepresentation.addVertex(uccNode);
            graphRepresentation.addEdge(uccNode, constraintNode, new LabelEdge("type"));

            List<Node> nodesPartOfUcc = new ArrayList<>();

            for(Column columnPartOfUCC : uniqueColumnCombination.getColumnCombination()) {
                LabelEdge edgeFromIDtoColumn = graphRepresentation.incomingEdgesOf(new Node(columnPartOfUCC.getLabel(), NodeType.COLUMN, columnPartOfUCC.getDatatype(), false, null, columnPartOfUCC.getTable(), null)).stream().findFirst().get();
                Node uccColumnIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoColumn);
                nodesPartOfUcc.add(uccColumnIDNode);
            }

            Table tableOfUCC = nodesPartOfUcc.get(0).getNameNode().getRepresentedColumn().getTable();

            List<Node> nodesNotPartOfUCC = new ArrayList<>();

            for(Column columnInTableOfUCC : tableOfUCC.getColumns()) {
                LabelEdge edgeFromIDtoColumn = graphRepresentation.incomingEdgesOf(new Node(columnInTableOfUCC.getLabel(), NodeType.COLUMN, columnInTableOfUCC.getDatatype(), false, null, columnInTableOfUCC.getTable(), null)).stream().findFirst().get();
                Node columnIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoColumn);

                if(!nodesPartOfUcc.contains(columnIDNode)) {
                    nodesNotPartOfUCC.add(columnIDNode);
                }
            }

            //log.debug("TotalNodesPartOfTable: {}", tableOfUCC.getColumns().size());
            //log.debug("nodesPartOfUcc: {}, nodesNotPartOfUCC: {}", nodesPartOfUcc.size(), nodesNotPartOfUCC.size());

            for(Node IDNode : nodesPartOfUcc) {
                graphRepresentation.addEdge(uccNode, IDNode, new LabelEdge("unique")); //TODO: Eventuell besseren Namen ausdenken
            }

            for(Node IDNode : nodesNotPartOfUCC) {
                graphRepresentation.addEdge(uccNode, IDNode, new LabelEdge("notunique")); //TODO: Definitiv besseren Namen ausdenken
            }
        }
    }

    private void functionalDependencies(Database db, Graph<Node, LabelEdge> graphRepresentation, Node constraintNode) {
        Collection<FunctionalDependency> functionalDependencies;

        functionalDependencies = db.getMetadata().getMeaningfulFunctionalDependencies(); //We only use fds that are not a UCC themselves, as keys are trivial fds

        int fdID = 1;

        for (FunctionalDependency functionalDependency : dependencyFilter.filterFunctionalDependencies(functionalDependencies)) {

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

}
