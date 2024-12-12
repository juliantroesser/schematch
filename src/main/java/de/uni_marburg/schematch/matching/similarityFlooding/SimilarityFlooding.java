package de.uni_marburg.schematch.matching.similarityFlooding;

import de.uni_marburg.schematch.data.Column;
import de.uni_marburg.schematch.data.Database;
import de.uni_marburg.schematch.data.Table;
import de.uni_marburg.schematch.data.metadata.dependency.FunctionalDependency;
import de.uni_marburg.schematch.data.metadata.dependency.InclusionDependency;
import de.uni_marburg.schematch.data.metadata.dependency.UniqueColumnCombination;
import de.uni_marburg.schematch.matching.Matcher;
import de.uni_marburg.schematch.matchtask.MatchTask;
import de.uni_marburg.schematch.matchtask.matchstep.MatchingStep;
import de.uni_marburg.schematch.matchtask.tablepair.TablePair;
import de.uni_marburg.schematch.similarity.string.Levenshtein;
import lombok.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import static de.uni_marburg.schematch.matching.similarityFlooding.SimilarityFloodingUtils.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class SimilarityFlooding extends Matcher {

    private static final Logger log = LogManager.getLogger(SimilarityFlooding.class);

    private String useWholeSchema;
    private String propagationCoefficientPolicy;
    private String fixpointFormula;
    private String epsilon;
    private String maxIterations;
    private String FDV1;
    private String FDV2;
    private String UCCV1;
    private String UCCV2;
    private String INDV1;
    private String INDV2;

    @Override
    public float[][] match(MatchTask matchTask, MatchingStep matchStep) {

        PropagationCoefficientPolicy policy;
        FixpointFormula formula;

        policy = switch (propagationCoefficientPolicy) {
            case "INVERSE_AVERAGE" -> PropagationCoefficientPolicy.INVERSE_AVERAGE;
            case "INVERSE_PRODUCT" -> PropagationCoefficientPolicy.INVERSE_PRODUCT;
            case "CONSTANT_ONE" -> PropagationCoefficientPolicy.CONSTANT_ONE;
            default ->
                    throw new RuntimeException("No such propagation coefficient policy: " + propagationCoefficientPolicy);
        };

        formula = switch (fixpointFormula) {
            case "BASIC" -> FixpointFormula.BASIC;
            case "FORMULA_A" -> FixpointFormula.FORMULA_A;
            case "FORMULA_B" -> FixpointFormula.FORMULA_B;
            case "FORMULA_C" -> FixpointFormula.FORMULA_C;
            default -> throw new RuntimeException("No such fixpoint formula: " + fixpointFormula);
        };


        boolean wholeSchema = Boolean.parseBoolean(useWholeSchema);
        boolean fdv1 = Boolean.parseBoolean(FDV1);
        boolean fdv2 = Boolean.parseBoolean(FDV2);
        boolean uccv1 = Boolean.parseBoolean(UCCV1);
        boolean uccv2 = Boolean.parseBoolean(UCCV2);
        boolean indv1 = Boolean.parseBoolean(INDV1);
        boolean indv2 = Boolean.parseBoolean(INDV2);

        float[][] simMatrix = matchTask.getEmptySimMatrix();

        Database sourceDb = matchTask.getScenario().getSourceDatabase();
        Database targetDb = matchTask.getScenario().getTargetDatabase();

        Graph<Node, LabelEdge> sourceGraph;
        Graph<Node, LabelEdge> targetGraph;
        Graph<NodePair, LabelEdge> connectivityGraph;
        Graph<NodePair, CoefficientEdge> propagationGraph;
        Map<NodePair, Double> initialMapping;
        Map<NodePair, Double> floodingResults;
        Map<NodePair, Double> filteredFloodingResults;

        if (!wholeSchema) {

            for (TablePair tablePair : matchTask.getTablePairs()) {

                Table sourceTable = tablePair.getSourceTable();
                Table targetTable = tablePair.getTargetTable();

                sourceGraph = transformIntoGraphRepresentationTable(sourceDb, sourceTable, fdv1, fdv2, uccv1, uccv2, indv1, indv2);
                targetGraph = transformIntoGraphRepresentationTable(targetDb, targetTable, fdv1, fdv2, uccv1, uccv2, indv1, indv2);
                connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);
                propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, policy);
                initialMapping = calculateInitialMapping(propagationGraph);
                floodingResults = similarityFlooding(propagationGraph, initialMapping, formula);
                filteredFloodingResults = filterMapping(floodingResults);

                populateSimMatrix(simMatrix, filteredFloodingResults, sourceTable, targetTable);
            }

        } else {

            sourceGraph = transformIntoGraphRepresentationSchema(sourceDb, fdv1, fdv2, uccv1, uccv2, indv1, indv2);
            targetGraph = transformIntoGraphRepresentationSchema(targetDb, fdv1, fdv2, uccv1, uccv2, indv1, indv2);

            //Combine both Graphs into a connectivity-graph
            connectivityGraph = createConnectivityGraph(sourceGraph, targetGraph);

            //Transform the connectivity-graph into the propagation-graph on which the algorithm executes
            propagationGraph = inducePropagationGraph(connectivityGraph, sourceGraph, targetGraph, policy);

            //Calculate the initial mapping (similarity) values
            initialMapping = calculateInitialMapping(propagationGraph);

            //Run the similarity-flooding algorithm
            floodingResults = similarityFlooding(propagationGraph, initialMapping, formula);

            //Apply constraints/filters to the result
            filteredFloodingResults = filterMapping(floodingResults);

            for (Table sourceTable : matchTask.getScenario().getSourceDatabase().getTables()) {
                for (Table targetTable : matchTask.getScenario().getTargetDatabase().getTables()) {
                    populateSimMatrix(simMatrix, filteredFloodingResults, sourceTable, targetTable);
                }
            }

            //return convertSimilarityMapToMatrix(filteredFloodingResults, matchTask);
        }
        return simMatrix;
    }

    public Graph<Node, LabelEdge> transformIntoGraphRepresentationSchema(Database db, boolean fdv1, boolean fdv2, boolean uccv1, boolean uccv2, boolean indv1, boolean indv2) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        Node schemaNode = new Node("Schema", NodeType.DATABASE, null, false, null, null);
        Node tableNode = new Node("Table", NodeType.TABLE, null, false, null, null);
        Node columnNode = new Node("Column", NodeType.COLUMN, null, false, null, null);
        Node columnTypeNode = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null);

        graphRepresentation.addVertex(schemaNode);
        graphRepresentation.addVertex(tableNode);
        graphRepresentation.addVertex(columnNode);
        graphRepresentation.addVertex(columnTypeNode);

        int uniqueID = 1;

        Node databaseName = new Node(db.getName(), NodeType.DATABASE, null, false, null, null);
        graphRepresentation.addVertex(databaseName);

        Node currentDatabaseNode = new Node("NodeID" + uniqueID++, NodeType.DATABASE, null, true, databaseName, null);
        graphRepresentation.addVertex(currentDatabaseNode);

        graphRepresentation.addEdge(currentDatabaseNode, schemaNode, new LabelEdge("type"));
        graphRepresentation.addEdge(currentDatabaseNode, databaseName, new LabelEdge("name"));

        for (Table table : db.getTables()) {

            Node tableName = new Node(table.getName(), NodeType.TABLE, null, false, null, null);
            graphRepresentation.addVertex(tableName);

            Node currentTableNode = new Node("NodeID" + uniqueID++, NodeType.TABLE, null, true, tableName, null);
            graphRepresentation.addVertex(currentTableNode);

            graphRepresentation.addEdge(currentDatabaseNode, currentTableNode, new LabelEdge("table"));
            graphRepresentation.addEdge(currentTableNode, tableNode, new LabelEdge("type"));
            graphRepresentation.addEdge(currentTableNode, tableName, new LabelEdge("name"));

            for (Column column : table.getColumns()) {

                Node columnName = new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, null, table);
                graphRepresentation.addVertex(columnName);

                Node currentColumnNode = new Node("NodeID" + uniqueID++, NodeType.COLUMN, column.getDatatype(), true, columnName, table);
                graphRepresentation.addVertex(currentColumnNode);

                graphRepresentation.addEdge(currentTableNode, currentColumnNode, new LabelEdge("column"));
                graphRepresentation.addEdge(currentColumnNode, columnNode, new LabelEdge("type"));
                graphRepresentation.addEdge(currentColumnNode, columnName, new LabelEdge("name"));

                Node columnDataType = new Node(column.getDatatype().toString(), NodeType.COLUMN_TYPE, column.getDatatype(), false, null, null);
                boolean dataTypeNodeExistsInGraph = graphRepresentation.containsVertex(columnDataType);

                if (dataTypeNodeExistsInGraph) { //Dann Kante zu

                    Set<LabelEdge> edgesOfIdToColumnType = graphRepresentation.incomingEdgesOf(columnDataType);
                    LabelEdge edgeOfIdToColumnType = edgesOfIdToColumnType.stream().findFirst().orElseThrow(() -> new NoSuchElementException("No such data Type edge present in the graph"));
                    Node columnTypeIdentifier = graphRepresentation.getEdgeSource(edgeOfIdToColumnType);
                    graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("datatype"));

                } else { //Neuen Knoten anlegen

                    Node columnTypeIdentifier = new Node("NodeID" + uniqueID++, NodeType.COLUMN_TYPE, column.getDatatype(), true, columnDataType, null);

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

        Node constraintNode = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null);
        if (fdv2 || uccv2 || indv2) {
            graphRepresentation.addVertex(constraintNode);
        }

        if (fdv1) { //New Edges for fdv1

            List<FunctionalDependency> functionalDependencies = db.getMetadata().getFds().stream().toList();

            for (FunctionalDependency functionalDependency : functionalDependencies) {

                List<Node> determinantIdNodes = new ArrayList<>();

                for (Column determinant : functionalDependency.getDeterminant()) {
                    LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(determinant.getLabel(), NodeType.COLUMN, determinant.getDatatype(), false, null, determinant.getTable())).stream().findFirst().get();
                    Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);
                    determinantIdNodes.add(determinantIDNode);
                }

                Column dependant = functionalDependency.getDependant();

                LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                for (Node determinantIDNode : determinantIdNodes) {
                    graphRepresentation.addEdge(determinantIDNode, dependantIDNode, new LabelEdge("determines"));
                }
            }
        }

        if (fdv2) { //New vertices and edges for fdv2

            List<FunctionalDependency> functionalDependencies = db.getMetadata().getFds().stream().toList();
            int fdID = 1;

            for (FunctionalDependency functionalDependency : functionalDependencies) {

                Node fdNode = new Node("FD" + fdID++, NodeType.CONSTRAINT, null, false, null, null);
                graphRepresentation.addVertex(fdNode);
                graphRepresentation.addEdge(fdNode, constraintNode, new LabelEdge("type"));

                List<Node> determinantIdNodes = new ArrayList<>();

                for (Column determinant : functionalDependency.getDeterminant()) {
                    LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(determinant.getLabel(), NodeType.COLUMN, determinant.getDatatype(), false, null, determinant.getTable())).stream().findFirst().get();
                    Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);
                    determinantIdNodes.add(determinantIDNode);
                }

                Column dependant = functionalDependency.getDependant();

                LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                for (Node determinantIDNode : determinantIdNodes) {
                    graphRepresentation.addEdge(fdNode, determinantIDNode, new LabelEdge("determinant"));
                }

                graphRepresentation.addEdge(fdNode, dependantIDNode, new LabelEdge("dependant"));
            }
        }

        if (uccv1) { //new vertices and edges for uccv1

            List<UniqueColumnCombination> uniqueColumnCombinations = db.getMetadata().getUccs().stream().toList();

            for (UniqueColumnCombination ucc : uniqueColumnCombinations) {

                int uccSize = ucc.getColumnCombination().size();
                Node uccSizeNode = new Node("UCC#" + uccSize, NodeType.CONSTRAINT, null, false, null, null);

                if (!graphRepresentation.containsVertex(uccSizeNode)) {
                    graphRepresentation.addVertex(uccSizeNode);
                }

                List<Node> nodesPartOfUcc = new ArrayList<>();

                for (Column nodePartOfUcc : ucc.getColumnCombination()) {
                    LabelEdge edgeFromIDtoUccNode = graphRepresentation.incomingEdgesOf(new Node(nodePartOfUcc.getLabel(), NodeType.COLUMN, nodePartOfUcc.getDatatype(), false, null, nodePartOfUcc.getTable())).stream().findFirst().get();
                    Node uccIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoUccNode);
                    nodesPartOfUcc.add(uccIDNode);
                }

                for (Node nodePartOfUcc : nodesPartOfUcc) {
                    graphRepresentation.addEdge(nodePartOfUcc, uccSizeNode, new LabelEdge("ucc"));
                }
            }
        }

        if (uccv2) { //new vertices and edges for uccv2

            List<UniqueColumnCombination> uniqueColumnCombinations = db.getMetadata().getUccs().stream().toList();
            int uccID = 1;

            for (UniqueColumnCombination ucc : uniqueColumnCombinations) {

                Node uccNode = new Node("UCC" + uccID++, NodeType.CONSTRAINT, null, false, null, null);
                graphRepresentation.addVertex(uccNode);
                graphRepresentation.addEdge(uccNode, constraintNode, new LabelEdge("type"));

                int uccSize = ucc.getColumnCombination().size();
                Node uccSizeNode = new Node("UCC#" + uccSize, NodeType.CONSTRAINT, null, false, null, null);

                if (!graphRepresentation.containsVertex(uccSizeNode)) {
                    graphRepresentation.addVertex(uccSizeNode);
                }

                graphRepresentation.addEdge(uccNode, uccSizeNode, new LabelEdge("size"));

                List<Node> nodesPartOfUcc = new ArrayList<>();

                for (Column nodePartOfUcc : ucc.getColumnCombination()) {
                    LabelEdge edgeFromIDtoUccNode = graphRepresentation.incomingEdgesOf(new Node(nodePartOfUcc.getLabel(), NodeType.COLUMN, nodePartOfUcc.getDatatype(), false, null, nodePartOfUcc.getTable())).stream().findFirst().get();
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

            for (InclusionDependency inclusionDependency : inclusionDependencies) {

                List<Node> dependantIdNodes = new ArrayList<>();
                List<Node> referencedIdNodes = new ArrayList<>();

                for (Column dependant : inclusionDependency.getDependant()) {
                    LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                    Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);
                    dependantIdNodes.add(dependantIDNode);
                }

                for (Column referenced : inclusionDependency.getReferenced()) {
                    LabelEdge edgeFromIDtoReferenced = graphRepresentation.incomingEdgesOf(new Node(referenced.getLabel(), NodeType.COLUMN, referenced.getDatatype(), false, null, referenced.getTable())).stream().findFirst().get();
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

            for (InclusionDependency inclusionDependency : inclusionDependencies) {

                List<Node> dependantIdNodes = new ArrayList<>();
                List<Node> referencedIdNodes = new ArrayList<>();

                for (Column dependant : inclusionDependency.getDependant()) {
                    LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                    Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);
                    dependantIdNodes.add(dependantIDNode);
                }

                for (Column referenced : inclusionDependency.getReferenced()) {
                    LabelEdge edgeFromIDtoReferenced = graphRepresentation.incomingEdgesOf(new Node(referenced.getLabel(), NodeType.COLUMN, referenced.getDatatype(), false, null, referenced.getTable())).stream().findFirst().get();
                    Node referencedIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoReferenced);
                    referencedIdNodes.add(referencedIDNode);
                }

                Node indNode = new Node("IND" + indID++, NodeType.CONSTRAINT, null, false, null, null);
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

    public Graph<Node, LabelEdge> transformIntoGraphRepresentationTable(Database db, Table table, boolean fdv1, boolean fdv2, boolean uccv1, boolean uccv2, boolean indv1, boolean indv2) {

        Graph<Node, LabelEdge> graphRepresentation = new DefaultDirectedWeightedGraph<>(LabelEdge.class);

        Node tableNode = new Node("Table", NodeType.TABLE, null, false, null, null);
        Node columnNode = new Node("Column", NodeType.COLUMN, null, false, null, null);
        Node columnTypeNode = new Node("ColumnType", NodeType.COLUMN_TYPE, null, false, null, null);

        graphRepresentation.addVertex(tableNode);
        graphRepresentation.addVertex(columnNode);
        graphRepresentation.addVertex(columnTypeNode);

        int uniqueID = 1;

        Node tableName = new Node(table.getName(), NodeType.TABLE, null, false, null, null);
        graphRepresentation.addVertex(tableName);

        Node currentTableNode = new Node("NodeID" + uniqueID++, NodeType.TABLE, null, true, tableName, null);
        graphRepresentation.addVertex(currentTableNode);

        graphRepresentation.addEdge(currentTableNode, tableNode, new LabelEdge("type"));
        graphRepresentation.addEdge(currentTableNode, tableName, new LabelEdge("name"));

        for (Column column : table.getColumns()) {

            Node columnName = new Node(column.getLabel(), NodeType.COLUMN, column.getDatatype(), false, null, table);
            graphRepresentation.addVertex(columnName);

            Node currentColumnNode = new Node("NodeID" + uniqueID++, NodeType.COLUMN, column.getDatatype(), true, columnName, table);
            graphRepresentation.addVertex(currentColumnNode);

            graphRepresentation.addEdge(currentTableNode, currentColumnNode, new LabelEdge("column"));
            graphRepresentation.addEdge(currentColumnNode, columnNode, new LabelEdge("type"));
            graphRepresentation.addEdge(currentColumnNode, columnName, new LabelEdge("name"));

            Node columnDataType = new Node(column.getDatatype().toString(), NodeType.COLUMN_TYPE, column.getDatatype(), false, null, null);
            boolean dataTypeNodeExistsInGraph = graphRepresentation.containsVertex(columnDataType);

            if (dataTypeNodeExistsInGraph) { //Dann Kante zu

                Set<LabelEdge> edgesOfIdToColumnType = graphRepresentation.incomingEdgesOf(columnDataType);
                LabelEdge edgeOfIdToColumnType = edgesOfIdToColumnType.stream().findFirst().orElseThrow(() -> new NoSuchElementException("No such data Type edge present in the graph"));
                Node columnTypeIdentifier = graphRepresentation.getEdgeSource(edgeOfIdToColumnType);
                graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("datatype"));

            } else { //Neuen Knoten anlegen

                Node columnTypeIdentifier = new Node("NodeID" + uniqueID++, NodeType.COLUMN_TYPE, column.getDatatype(), true, columnDataType, null);

                graphRepresentation.addVertex(columnDataType);
                graphRepresentation.addVertex(columnTypeIdentifier);
                graphRepresentation.addEdge(columnTypeIdentifier, columnDataType, new LabelEdge("name"));
                graphRepresentation.addEdge(columnTypeIdentifier, columnTypeNode, new LabelEdge("type"));
                graphRepresentation.addEdge(currentColumnNode, columnTypeIdentifier, new LabelEdge("datatype"));
            }
        }

        Node constraintNode = new Node("Constraint", NodeType.CONSTRAINT, null, false, null, null);
        if (fdv2 || uccv2 || indv2) {
            graphRepresentation.addVertex(constraintNode);
        }

        if (fdv1) { //New Edges for fdv1

            Collection<FunctionalDependency> fdsOfTable = getAllFDsOfTable(db, table);
            for (FunctionalDependency functionalDependency : fdsOfTable) {

                List<Node> determinantIdNodes = new ArrayList<>();

                for (Column determinant : functionalDependency.getDeterminant()) {
                    LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(determinant.getLabel(), NodeType.COLUMN, determinant.getDatatype(), false, null, determinant.getTable())).stream().findFirst().get();
                    Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);
                    determinantIdNodes.add(determinantIDNode);
                }

                Column dependant = functionalDependency.getDependant();

                LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                for (Node determinantIDNode : determinantIdNodes) {
                    graphRepresentation.addEdge(determinantIDNode, dependantIDNode, new LabelEdge("determines"));
                }
            }
        }

        if (fdv2) { //New vertices and edges for fdv2

            Collection<FunctionalDependency> fdsOfTable = getAllFDsOfTable(db, table);
            int fdID = 1;

            for (FunctionalDependency functionalDependency : fdsOfTable) {

                Node fdNode = new Node("FD" + fdID++, NodeType.CONSTRAINT, null, false, null, null);
                graphRepresentation.addVertex(fdNode);
                graphRepresentation.addEdge(fdNode, constraintNode, new LabelEdge("type"));

                List<Node> determinantIdNodes = new ArrayList<>();

                for (Column determinant : functionalDependency.getDeterminant()) {
                    LabelEdge edgeFromIDtoDeterminant = graphRepresentation.incomingEdgesOf(new Node(determinant.getLabel(), NodeType.COLUMN, determinant.getDatatype(), false, null, determinant.getTable())).stream().findFirst().get();
                    Node determinantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDeterminant);
                    determinantIdNodes.add(determinantIDNode);
                }

                Column dependant = functionalDependency.getDependant();

                LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);

                for (Node determinantIDNode : determinantIdNodes) { //Changed order
                    graphRepresentation.addEdge(fdNode, determinantIDNode, new LabelEdge("determinant"));
                }

                graphRepresentation.addEdge(fdNode, dependantIDNode, new LabelEdge("dependant"));
            }
        }

        if (uccv1) { //new vertices and edges for uccv1

            Collection<UniqueColumnCombination> UCCsOfTable = getAllUCCsOfTable(db, table);

            for (UniqueColumnCombination ucc : UCCsOfTable) {

                int uccSize = ucc.getColumnCombination().size();
                Node uccSizeNode = new Node("UCC#" + uccSize, NodeType.CONSTRAINT, null, false, null, null);

                if (!graphRepresentation.containsVertex(uccSizeNode)) {
                    graphRepresentation.addVertex(uccSizeNode);
                }

                List<Node> nodesPartOfUcc = new ArrayList<>();

                for (Column nodePartOfUcc : ucc.getColumnCombination()) {
                    LabelEdge edgeFromIDtoUccNode = graphRepresentation.incomingEdgesOf(new Node(nodePartOfUcc.getLabel(), NodeType.COLUMN, nodePartOfUcc.getDatatype(), false, null, nodePartOfUcc.getTable())).stream().findFirst().get();
                    Node uccIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoUccNode);
                    nodesPartOfUcc.add(uccIDNode);
                }

                for (Node nodePartOfUcc : nodesPartOfUcc) {
                    graphRepresentation.addEdge(nodePartOfUcc, uccSizeNode, new LabelEdge("ucc"));
                }
            }
        }

        if (uccv2) { //new vertices and edges for uccv2

            Collection<UniqueColumnCombination> UCCsOfTable = getAllUCCsOfTable(db, table);
            int uccID = 1;

            for (UniqueColumnCombination ucc : UCCsOfTable) {

                Node uccNode = new Node("UCC" + uccID++, NodeType.CONSTRAINT, null, false, null, null);
                graphRepresentation.addVertex(uccNode);
                graphRepresentation.addEdge(uccNode, constraintNode, new LabelEdge("type"));

                int uccSize = ucc.getColumnCombination().size();
                Node uccSizeNode = new Node("UCC#" + uccSize, NodeType.CONSTRAINT, null, false, null, null);

                if (!graphRepresentation.containsVertex(uccSizeNode)) {
                    graphRepresentation.addVertex(uccSizeNode);
                }

                graphRepresentation.addEdge(uccNode, uccSizeNode, new LabelEdge("size"));

                List<Node> nodesPartOfUcc = new ArrayList<>();

                for (Column nodePartOfUcc : ucc.getColumnCombination()) {
                    LabelEdge edgeFromIDtoUccNode = graphRepresentation.incomingEdgesOf(new Node(nodePartOfUcc.getLabel(), NodeType.COLUMN, nodePartOfUcc.getDatatype(), false, null, nodePartOfUcc.getTable())).stream().findFirst().get();
                    Node uccIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoUccNode);
                    nodesPartOfUcc.add(uccIDNode);
                }

                for (Node nodePartOfUcc : nodesPartOfUcc) {
                    graphRepresentation.addEdge(nodePartOfUcc, uccNode, new LabelEdge("ucc"));
                }
            }
        }

        if (indv1) { //new edges for indv1

            Collection<InclusionDependency> INDsOfTable = getAllINDsOfTable(db, table);

            for (InclusionDependency inclusionDependency : INDsOfTable) {

                List<Node> dependantIdNodes = new ArrayList<>();
                List<Node> referencedIdNodes = new ArrayList<>();

                for (Column dependant : inclusionDependency.getDependant()) {
                    LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                    Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);
                    dependantIdNodes.add(dependantIDNode);
                }

                for (Column referenced : inclusionDependency.getReferenced()) {
                    LabelEdge edgeFromIDtoReferenced = graphRepresentation.incomingEdgesOf(new Node(referenced.getLabel(), NodeType.COLUMN, referenced.getDatatype(), false, null, referenced.getTable())).stream().findFirst().get();
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

            Collection<InclusionDependency> INDsOfTable = getAllINDsOfTable(db, table);
            int indID = 1;

            for (InclusionDependency inclusionDependency : INDsOfTable) {

                List<Node> dependantIdNodes = new ArrayList<>();
                List<Node> referencedIdNodes = new ArrayList<>();

                for (Column dependant : inclusionDependency.getDependant()) {
                    LabelEdge edgeFromIDtoDependant = graphRepresentation.incomingEdgesOf(new Node(dependant.getLabel(), NodeType.COLUMN, dependant.getDatatype(), false, null, dependant.getTable())).stream().findFirst().get();
                    Node dependantIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoDependant);
                    dependantIdNodes.add(dependantIDNode);
                }

                for (Column referenced : inclusionDependency.getReferenced()) {
                    LabelEdge edgeFromIDtoReferenced = graphRepresentation.incomingEdgesOf(new Node(referenced.getLabel(), NodeType.COLUMN, referenced.getDatatype(), false, null, referenced.getTable())).stream().findFirst().get();
                    Node referencedIDNode = graphRepresentation.getEdgeSource(edgeFromIDtoReferenced);
                    referencedIdNodes.add(referencedIDNode);
                }

                Node indNode = new Node("IND" + indID++, NodeType.CONSTRAINT, null, false, null, null);
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
                    connectivityGraph.addEdge(connectedSourceNode, connectedTargetNode, new LabelEdge(label1.getLabel()));
                }
            }
        }

//        System.out.println("Vertices");
//        for(NodePair node : connectivityGraph.vertexSet()) {
//            System.out.println(node);
//        }
//
//        System.out.println("Edges");
//        for(LabelEdge edge : connectivityGraph.edgeSet()) {
//            System.out.println("(" + connectivityGraph.getEdgeSource(edge) + ", " + edge.getLabel() + ", " + connectivityGraph.getEdgeTarget(edge) + ")");
//        }

//        System.out.println("PCG has: " + connectivityGraph.vertexSet().size() + " vertices");
//        System.out.println("PCG has: " + connectivityGraph.edgeSet().size() + " edges");

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


//        System.out.println("Edges:" );
//        for(CoefficientEdge edge : propagationGraph.edgeSet()) {
//            System.out.println("(" + propagationGraph.getEdgeSource(edge) + ", " + edge.getCoefficient() + ", " + propagationGraph.getEdgeTarget(edge) + ")");
//        }

        return propagationGraph;
    }

    public Map<NodePair, Double> calculateInitialMapping(Graph<NodePair, CoefficientEdge> propagationGraph) {

        //TODO: Initial NodePair Similarity as Parameter.

        Map<NodePair, Double> initialMapping = new HashMap<>();

        for (NodePair mappingPair : propagationGraph.vertexSet()) {

            Node node1 = mappingPair.getFirstNode();
            Node node2 = mappingPair.getSecondNode();
            Levenshtein l = new Levenshtein();
            double similarity;

//            if (node1.isIDNode() || node2.isIDNode()) { //Mapping with artifical Nodes
//                similarity = 0.0; //Bei 0 lassen beste Ergebnisse //Bei negativen Werten keine Konvergenz //>0 monoton fallender Score
//            } else if (node1.getNodeType().equals(NodeType.COLUMN) && node2.getNodeType().equals(NodeType.COLUMN)) {
//                if (node1.getValue().equals("Column") && node2.getValue().equals("Column")) {
//                    similarity = 1.0;
//                } else {
//                    similarity = l.compare(node1.getValue(), node2.getValue());
//                }
//            } else if (node1.getNodeType().equals(NodeType.COLUMN_TYPE) && node2.getNodeType().equals(NodeType.COLUMN_TYPE)) {
//                if (node1.getValue().equals("ColumnType") && node2.getValue().equals("ColumnType")) {
//                    similarity = 1.0;
//                } else {
//                    similarity = getDatatypeSimilarity(node1.getDatatype(), node2.getDatatype());
//                }
//            } else if (node1.getNodeType().equals(NodeType.TABLE) && node2.getNodeType().equals(NodeType.TABLE)) {
//                if (node1.getValue().equals("Table") && node2.getValue().equals("Table")) {
//                    similarity = 1.0;
//                } else {
//                    similarity = l.compare(node1.getValue(), node2.getValue());
//                }
//            } else if (node1.getNodeType().equals(NodeType.DATABASE) && node2.getNodeType().equals(NodeType.DATABASE)) {
//                if (node1.getValue().equals("Database") && node2.getValue().equals("Database")) {
//                    similarity = 1.0;
//                } else {
//                    similarity = l.compare(node1.getValue(), node2.getValue()); //TODO: Wenn nur source und target als Name, was tun?
//                }
//            } else if (node1.getNodeType().equals(NodeType.CONSTRAINT) && node2.getNodeType().equals(NodeType.CONSTRAINT)) {
//                if (node1.getValue().startsWith("FD") && node2.getValue().startsWith("FD")) { //TODO: Beachten wenn Column mit FD beginnt.
//                    similarity = 0.0; //TODO: 0 oder 1 besser?
//                } else if (node1.getValue().startsWith("UCC#") && node2.getValue().startsWith("UCC#")) {
//                    int uccSize1 = Integer.parseInt(node1.getValue().replaceAll("\\D", ""));
//                    int uccSize2 = Integer.parseInt(node2.getValue().replaceAll("\\D", ""));
//                    similarity = 1.0 / (1.0 + Math.abs(uccSize1 - uccSize2));
//                } else if (node1.getValue().startsWith("UCC") && node2.getValue().startsWith("UCC")) {
//                    similarity = 1.0; //TODO: 0 oder 1 besser?
//                } else if (node1.getValue().startsWith("IND") && node2.getValue().startsWith("IND")) {
//                    similarity = 1.0; //TODO: 0 oder 1 besser?
//                } else {
//                    similarity = -1.0;
//                }
//            } else {
//                similarity = 0.0;
//            }

            if (node1.isIDNode() || node2.isIDNode()) { //Mapping with artifical Nodes
                similarity = 0.0; //Bei 0 lassen beste Ergebnisse //Bei negativen Werten keine Konvergenz //>0 monoton fallender Score
//            } else if (node1.getNodeType().equals(NodeType.CONSTRAINT) && node2.getNodeType().equals(NodeType.CONSTRAINT)) { //Mapping
//                if (node1.getValue().startsWith("FD") && node2.getValue().startsWith("FD")) { //TODO: Beachten wenn Column mit FD beginnt.
//                    similarity = 0.0; //TODO: 0 oder 1 besser?
//                } else if (node1.getValue().startsWith("UCC#") && node2.getValue().startsWith("UCC#")) {
//                    int uccSize1 = Integer.parseInt(node1.getValue().replaceAll("\\D", ""));
//                    int uccSize2 = Integer.parseInt(node2.getValue().replaceAll("\\D", ""));
//                    similarity = 1.0 / (1.0 + Math.abs(uccSize1 - uccSize2));
//                } else if (node1.getValue().startsWith("UCC") && node2.getValue().startsWith("UCC")) {
//                    similarity = 1.0; //TODO: 0 oder 1 besser?
//                } else if (node1.getValue().startsWith("IND") && node2.getValue().startsWith("IND")) {
//                    similarity = 1.0; //TODO: 0 oder 1 besser?
//                } else {
//                    similarity = -1.0;
//                }
            } else {
                similarity = l.compare(node1.getValue(), node2.getValue());
            }

            initialMapping.put(mappingPair, similarity);
        }

//        Map<NodePair, Double> sortedMap = initialMapping.entrySet()
//                .stream()
//                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Compare in descending order
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        Map.Entry::getValue,
//                        (e1, e2) -> e1, // In case of duplicate keys (not likely here), keep the first
//                        LinkedHashMap::new // Maintain order of insertion
//                ));

        // Print the sorted entries
//        sortedMap.forEach((key, value) ->
//                System.out.println(key + " -> " + value)
//        );

        return initialMapping;
    }

    public Map<NodePair, Double> similarityFlooding(Graph<NodePair, CoefficientEdge> propagationGraph, Map<NodePair, Double> initialMapping, FixpointFormula formula) {

        double EPSILON = Double.parseDouble(epsilon);
        int MAX_ITERATIONS = Integer.parseInt(maxIterations);
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
                Set<NodePair> neighborNodes = incomingEdges.stream().map(propagationGraph::getEdgeSource).collect(Collectors.toSet());

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

    private Collection<FunctionalDependency> getAllFDsOfTable(Database db, Table table) {

        Set<FunctionalDependency> FDs = new HashSet<>();

        //getFunctionalDependencies gets all dependencies where the column is either right or left hand side

        for(Column column : table.getColumns()) {
            Collection<FunctionalDependency> FDsOfColumn = db.getMetadata().getFunctionalDependencies(column);
            if(FDsOfColumn != null) {
                FDs.addAll(FDsOfColumn);
            }
        }

        return FDs;
    }

    private Collection<UniqueColumnCombination> getAllUCCsOfTable(Database db, Table table) {

        Set<UniqueColumnCombination> UCCs = new HashSet<>();

        for(Column column : table.getColumns()) {
            Collection<UniqueColumnCombination> UCCsOfColumn = db.getMetadata().getUniqueColumnCombinations(column);
            if(UCCsOfColumn != null) {
                UCCs.addAll(UCCsOfColumn);
            }
        }

        return UCCs;
    }

    private Collection<InclusionDependency> getAllINDsOfTable(Database db, Table table) {

        Set<InclusionDependency> INDs = new HashSet<>();

        for(Column column : table.getColumns()) {
            Collection<InclusionDependency> INDsOfColumn = db.getMetadata().getInclusionDependencies(column);
            if(INDsOfColumn != null) {
                INDs.addAll(INDsOfColumn);
            }
        }
        //TODO: Check all INDs in same table
        return INDs;
    }
}
