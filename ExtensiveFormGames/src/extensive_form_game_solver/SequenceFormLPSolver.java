package extensive_form_game_solver;

import extensive_form_game.Game;
import extensive_form_game.Game.Action;
import extensive_form_game.Game.Node;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gurobi.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;


public class SequenceFormLPSolver extends PrimalSequenceForm {
    Game game;

    GRBVar[] dualVars; // indexed as [informationSetId]. Note that we expect information sets to be 1-indexed, but the code corrects for when this is not the case

    TIntList[] sequenceFormDualMatrix; // indexed as [dual sequence id][information set]
    TIntDoubleMap[] dualPayoffMatrix; // indexed as [dual sequence][primal sequence]

    int numDualSequences;
    int numDualInformationSets;

    String[] dualSequenceNames;

    TIntObjectMap<GRBConstr> dualConstraints; // indexed as [sequenceId]

    public SequenceFormLPSolver(Game game, int playerToSolveFor) {
        this(game, playerToSolveFor, 1e-6);
    }

    public SequenceFormLPSolver(Game game, int playerToSolveFor, double tol) {
        super(game, playerToSolveFor, tol);
        this.game = game;
        try {
            env = new GRBEnv("mip.log");
            model = new GRBModel(env);
        } catch (GRBException e) {
            System.out.println("Error SequenceFormLPSolver(): Gurobi setup failed");
        }

        this.playerToSolveFor = playerToSolveFor;
        this.playerNotToSolveFor = (playerToSolveFor % 2) + 1;


        initializeDualDataStructures();
        //modelStrategyVars = new ArrayList<GRBVar>();
        //dualVars = new ArrayList<GRBVar>();
        //strategyVarsByRealGameSequences = new ArrayList<GRBVar>();

        try {
            setUpDual(tol);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes the arrays and other data structure objects that we use.
     */
    @SuppressWarnings("unchecked")
    private void initializeDualDataStructures() {
        int numInformationSets = 0;
        if (playerToSolveFor == 1) {
            numInformationSets = game.getNumInformationSetsPlayer1();
        } else {
            numInformationSets = game.getNumInformationSetsPlayer2();
        }
        numDualSequences = playerNotToSolveFor == 1 ? game.getNumSequencesP1() : game.getNumSequencesP2();
        sequenceFormDualMatrix = new TIntList[numDualSequences];
        for (int i = 0; i < numDualSequences; i++) {
            sequenceFormDualMatrix[i] =  new TIntArrayList();
        }
        numDualInformationSets = playerNotToSolveFor == 1 ? game.getNumInformationSetsPlayer1() : game.getNumInformationSetsPlayer2();
        dualSequenceNames = new String[numDualSequences];
        dualPayoffMatrix = new TIntDoubleHashMap[numDualSequences];
        for (int i = 0; i < numDualSequences; i++) {
            dualPayoffMatrix[i] = new TIntDoubleHashMap();
        }

        // ensure that we have a large enough array for both the case where information sets start at 1 and 0
        dualConstraints = new TIntObjectHashMap<GRBConstr>();
    }



    /**
     * Builds the LP model based on the game instance.
     * @throws GRBException
     */
    private void setUpDual(double tol) throws GRBException {
//        setCplexParameters(tol);
        dualSequenceNames[0] = "root";
        CreateDualVariablesAndConstraints();
        SetObjective();
    }

    private void CreateDualVariablesAndConstraints() throws GRBException {
        int numVars = 0;
        if (playerToSolveFor == 1) {
            numVars = game.getNumInformationSetsPlayer2() + 1;
        } else {
            numVars = game.getNumInformationSetsPlayer1() + 1;
        }
        String[] names = new String[numVars];
        for (int i = 0; i < numVars; i++) names[i] = "Y" + i;
        double[] lb = new double[numVars];
        char[] types = new char[numVars];
        Arrays.fill(lb, -Double.MAX_VALUE);
        Arrays.fill(types, GRB.CONTINUOUS);
        this.dualVars = model.addVars(lb, null, null, types, names);
        model.update();


        InitializeDualSequenceMatrix();
        InitializeDualPayoffMatrix();
        for (int sequenceId = 0; sequenceId < numDualSequences; sequenceId++) {
            CreateDualConstraintForSequence(sequenceId);
        }
    }

    private void InitializeDualSequenceMatrix() throws GRBException {
        sequenceFormDualMatrix[0].add(0);
        InitializeDualSequenceMatrixRecursive(game.getRoot(), new TIntHashSet(), 0);
    }

    private void InitializeDualSequenceMatrixRecursive(int currentNodeId, TIntSet visited, int parentSequenceId) throws GRBException {
        Node node = this.game.getNodeById(currentNodeId);
        if (node.isLeaf()) return;

        if (playerNotToSolveFor == node.getPlayer() && !visited.contains(node.getInformationSet())) {
            visited.add(node.getInformationSet());
            int informationSetMatrixId = node.getInformationSet() + (1-game.getSmallestInformationSetId(playerNotToSolveFor)); // map information set ID to 1 indexing. Assumes that information sets are named by consecutive integers
            sequenceFormDualMatrix[parentSequenceId].add(informationSetMatrixId);
            for (Action action : node.getActions()) {
                int newSequenceId = getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
                sequenceFormDualMatrix[newSequenceId].add(informationSetMatrixId);
                InitializeDualSequenceMatrixRecursive(action.getChildId(), visited, newSequenceId);
            }
        } else {
            for (Action action : node.getActions()) {
                int newSequenceId = playerNotToSolveFor == node.getPlayer()? getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName()) : parentSequenceId;
                InitializeDualSequenceMatrixRecursive(action.getChildId(), visited, newSequenceId);
            }
        }

    }

    private void InitializeDualPayoffMatrix() {
        InitializeDualPayoffMatrixRecursive(game.getRoot(), 0, 0, 1);     // Start with the root sequences
    }

    private void InitializeDualPayoffMatrixRecursive(int currentNodeId, int primalSequence, int dualSequence, double natureProbability) {
        Node node = this.game.getNodeById(currentNodeId);

        if (node.isLeaf()) {
            int valueMultiplier = playerToSolveFor == 1? -1 : 1;
            double leafValue = valueMultiplier * natureProbability * node.getValue();
            if (dualPayoffMatrix[dualSequence].containsKey(primalSequence)) {
                dualPayoffMatrix[dualSequence].put(primalSequence, leafValue + dualPayoffMatrix[dualSequence].get(primalSequence));
            } else {
                dualPayoffMatrix[dualSequence].put(primalSequence, leafValue);
            }
        } else {
            for (Action action : node.getActions()) {
                int newPrimalSequence = node.getPlayer() == playerToSolveFor? getSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName()) : primalSequence;
                int newDualSequence = node.getPlayer() == playerNotToSolveFor? getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName()) : dualSequence;
                double newNatureProbability = node.getPlayer() == 0? natureProbability * action.getProbability() : natureProbability;
                InitializeDualPayoffMatrixRecursive(action.getChildId(), newPrimalSequence, newDualSequence, newNatureProbability);
            }
        }
    }

    private void CreateDualConstraintForSequence(int sequenceId) throws GRBException{
        GRBLinExpr lhs = new GRBLinExpr();
        for (int i = 0; i < sequenceFormDualMatrix[sequenceId].size(); i++) {
            int informationSetId = sequenceFormDualMatrix[sequenceId].get(i);// + (1-game.getSmallestInformationSetId(playerNotToSolveFor)); // map information set ID to 1 indexing. Assumes that information sets are named by consecutive integers
            int valueMultiplier = i == 0? 1 : -1;
            lhs.addTerm(valueMultiplier, dualVars[informationSetId]);
        }

        TIntDoubleIterator it = dualPayoffMatrix[sequenceId].iterator();
        for ( int i = dualPayoffMatrix[sequenceId].size(); i-- > 0; ) {
            it.advance();
            lhs.addTerm(-it.value(), strategyVarsBySequenceId[it.key()]);
        }


        dualConstraints.put(sequenceId, model.addConstr(lhs, GRB.GREATER_EQUAL, 0, "Dual"+sequenceId));
    }


    private void SetObjective() throws GRBException {
        GRBLinExpr obj = new GRBLinExpr();
        obj.addTerm(1.0, dualVars[0]);
        model.setObjective(obj, GRB.MINIMIZE);
    }

    public GRBVar[] getDualVars() {
        return dualVars;
    }

    public TIntList[] getSequenceFormDualMatrix() {
        return sequenceFormDualMatrix;
    }

    public TIntDoubleMap[] getDualPayoffMatrix() {
        return dualPayoffMatrix;
    }

    public int getNumDualSequences() {
        return numDualSequences;
    }

    public TIntObjectMap<GRBConstr> getDualConstraints() {
        return dualConstraints;
    }
}
