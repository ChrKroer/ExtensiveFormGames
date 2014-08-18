package edu.cmu.cs.kroer.extensive_form_game.solver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.cs.kroer.extensive_form_game.Game;
import edu.cmu.cs.kroer.extensive_form_game.Game.Action;
import edu.cmu.cs.kroer.extensive_form_game.Game.Node;
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
import ilog.concert.*;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;



public class SequenceFormLPSolver extends ZeroSumGameSolver {
	int playerToSolveFor;
	int playerNotToSolveFor;
	
	IloCplex cplex;
	//IloNumVar[] modelStrategyVars;
	IloNumVar[] dualVars; // indexed as [informationSetId]. Note that we expect information sets to be 1-indexed, but the code corrects for when this is not the case
	HashMap<String, IloNumVar>[] strategyVarsByInformationSet; // indexed as [inforationSetId][action.name]

	TIntList[] sequenceFormDualMatrix; // indexed as [dual sequence id][information set]
	TIntDoubleMap[] dualPayoffMatrix; // indexed as [dual sequence][primal sequence]
	
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP1; // indexed as [informationSetId][action.name]
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP2; // indexed as [informationSetId][action.name]
	IloNumVar[] strategyVarsBySequenceId;
	int numSequencesP1;
	int numSequencesP2;
	int numPrimalSequences;
	int numDualSequences;
	int numPrimalInformationSets;
	int numDualInformationSets;
	
	TIntObjectMap<IloConstraint> primalConstraints; // indexed as [informationSetId], without correcting for 1-indexing
	TIntObjectMap<IloRange> dualConstraints; // indexed as [sequenceId]
	double[] nodeNatureProbabilities; // indexed as [nodeId]. Returns the probability of that node being reached when considering only nature nodes
	int[] sequenceIdForNodeP1; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 1 on the path to the node. 
	int[] sequenceIdForNodeP2; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 2 on the path to the node. 

	public SequenceFormLPSolver(Game game, int playerToSolveFor) {
		super(game);
		try {
			cplex = new IloCplex();
		} catch (IloException e) {
			System.out.println("Error SequenceFormLPSolver(): CPLEX setup failed");
		}
		
		this.playerToSolveFor = playerToSolveFor;
		this.playerNotToSolveFor = (playerToSolveFor % 2) + 1;
		
		
		initializeDataStructures();
		//modelStrategyVars = new ArrayList<IloNumVar>();
		//dualVars = new ArrayList<IloNumVar>();
		//strategyVarsByRealGameSequences = new ArrayList<IloNumVar>();
		
		try {
			setUpModel();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	/** 
	 * Initializes the arrays and other data structure objects that we use.
	 */
	@SuppressWarnings("unchecked")
	private void initializeDataStructures() {
		int numInformationSets = 0;
		//int numDualInformationSets = 0;
		
		
		if (playerToSolveFor == 1) {
			numInformationSets = game.getNumInformationSetsPlayer1();
			//numDualInformationSets = game.getNumInformationSetsPlayer2();
		} else {
			numInformationSets = game.getNumInformationSetsPlayer2();
			//numDualInformationSets = game.getNumInformationSetsPlayer1();
		}
		this.strategyVarsByInformationSet = (HashMap<String, IloNumVar>[]) new HashMap[numInformationSets+1];
		for (int i = 0; i <= numInformationSets; i++) {
			this.strategyVarsByInformationSet[i] = new HashMap<String, IloNumVar>();
		}
		
		
		numPrimalSequences = playerToSolveFor == 1 ? game.getNumSequencesP1() : game.getNumSequencesP2();
		numDualSequences = playerNotToSolveFor == 1 ? game.getNumSequencesP1() : game.getNumSequencesP2();
		sequenceFormDualMatrix = new TIntList[numDualSequences];
		for (int i = 0; i < numDualSequences; i++) {
			sequenceFormDualMatrix[i] =  new TIntArrayList();
		}
		numPrimalInformationSets = playerToSolveFor == 1 ? game.getNumInformationSetsPlayer1() : game.getNumInformationSetsPlayer2();
		numDualInformationSets = playerNotToSolveFor == 1 ? game.getNumInformationSetsPlayer1() : game.getNumInformationSetsPlayer2();
		
		dualPayoffMatrix = new TIntDoubleHashMap[numDualSequences];
		for (int i = 0; i < numDualSequences; i++) {
			dualPayoffMatrix[i] = new TIntDoubleHashMap();
		}
		
		// ensure that we have a large enough array for both the case where information sets start at 1 and 0
		sequenceIdByInformationSetAndActionP1 = new TObjectIntMap[game.getNumInformationSetsPlayer1()+1];
		sequenceIdByInformationSetAndActionP2 = new TObjectIntMap[game.getNumInformationSetsPlayer2()+1];
		for (int i = 0; i <= game.getNumInformationSetsPlayer1(); i++) {
			sequenceIdByInformationSetAndActionP1[i] = new TObjectIntHashMap<String>();
		}
		for (int i = 0; i <= game.getNumInformationSetsPlayer2(); i++) {
			sequenceIdByInformationSetAndActionP2[i] = new TObjectIntHashMap<String>();
		}

		if (playerToSolveFor == 1) {
			strategyVarsBySequenceId = new IloNumVar[game.getNumSequencesP1()];
		} else {
			strategyVarsBySequenceId = new IloNumVar[game.getNumSequencesP2()];
		}
		
		primalConstraints = new TIntObjectHashMap<IloConstraint>();
		dualConstraints = new TIntObjectHashMap<IloRange>();
		nodeNatureProbabilities = new double[game.getNumNodes()+1]; // Use +1 to be robust for non-zero indexed nodes
		sequenceIdForNodeP1 = new int[game.getNumNodes()+1];
		sequenceIdForNodeP2 = new int[game.getNumNodes()+1];
		computeAuxiliaryInformationForNodes();
	}
	
	/**
	 * Tries to solve the current model. Currently relies on CPLEX to throw an exception if no model has been built.
	 */
	@Override
	public void solveGame() {
		try {
			if (cplex.solve()) {
				/*for (int i = 0; i < strategyVarsBySequenceId.length; i++) {
					IloNumVar v = strategyVarsBySequenceId[i];
					cplex.getValue(v);
				}*/
				strategyVars = cplex.getValues(strategyVarsBySequenceId);
				valueOfGame = -cplex.getObjValue();
			}
		} catch (IloException e) {
			e.printStackTrace();
			System.out.println("Error SequenceFormLPSolver::solveGame: solve exception");
		}
	}

	/**
	 * Creates and returns a mapping from variable names to the values they take on in the solution computed by CPLEX.
	 */
	@Override
	public TObjectDoubleMap<String> getStrategyVarMap() {
		TObjectDoubleMap<String> map = new TObjectDoubleHashMap<String>();
		for (IloNumVar v : strategyVarsBySequenceId) {
			try {
				map.put(v.getName(), cplex.getValue(v));
			} catch (UnknownObjectException e) {
				e.printStackTrace();
			} catch (IloException e) {
				e.printStackTrace();
			}
		}
		
		return map;
	}


	/**
	 * Prints the value of the game along with the names and computed values for each variable.
	 */
	@Override
	public void printStrategyVarsAndGameValue() {
		printGameValue();
		for (IloNumVar v : strategyVarsBySequenceId) {
			try {
				System.out.println(v.getName() + ": \t" + cplex.getValue(v));
			} catch (UnknownObjectException e) {
				e.printStackTrace();
			} catch (IloException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Prints the value of the game, as computed by CPLEX. If solve() has not been called, an exception will be thrown.
	 */
	@Override
	public void printGameValue() {
		try {
			System.out.println("SequenceFormLPSovel status: " + cplex.getStatus());
			if	(cplex.getStatus() == IloCplex.Status.Optimal) {
				System.out.println("Objective value: " + this.valueOfGame);
			} 				
		} catch (IloException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Writes the computed strategy to a file. An exception is thrown if solve() has not been called. 
	 * @param filename the absolute path to the file being written to
	 */
	public void writeStrategyToFile(String filename) throws IloException{
		try {
			FileWriter fw = new FileWriter(filename);
			for (IloNumVar v : strategyVarsBySequenceId) {
				fw.write(v.getName() + ": \t" + cplex.getValue(v) + "\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Writes the current model to a file. CPLEX throws an exception if the model is faulty or the path does not exist.
	 * @param filename the absolute path to the file being written to
	 */
	public void writeModelToFile(String filename) {
		try {
			cplex.exportModel(filename);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the parameters of CPLEX such that minimal output is produced.
	 */
	private void setCplexParameters() {
		try {
			cplex.setParam(IloCplex.IntParam.SimDisplay, 0);
			cplex.setParam(IloCplex.IntParam.MIPDisplay, 0);
			cplex.setParam(IloCplex.IntParam.TuningDisplay, 0);
			cplex.setParam(IloCplex.IntParam.BarDisplay, 0);
			cplex.setParam(IloCplex.IntParam.SiftDisplay, 0);
			cplex.setParam(IloCplex.IntParam.ConflictDisplay, 0);
			cplex.setParam(IloCplex.IntParam.NetDisplay, 0);
			cplex.setParam(IloCplex.DoubleParam.TiLim, 1e+75);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Builds the LP model based on the game instance.
	 * @throws IloException
	 */
	private void setUpModel() throws IloException {
		setCplexParameters();

		// The empty sequence is the 0'th sequence for each player
		numSequencesP1 = numSequencesP2 = 1;
		CreateSequenceFormIds(game.getRoot(), new TIntHashSet(), new TIntHashSet());
		assert(numSequencesP1 == game.getNumSequencesP1()); // Ensure that our recursive function agrees with the game reader on how many sequences there are
		assert(numSequencesP2 == game.getNumSequencesP2());
		
		// create root sequence var
		IloNumVar rootSequence = cplex.numVar(1, 1, "Xroot");
		strategyVarsBySequenceId[0] = rootSequence;
		CreateSequenceFormVariablesAndConstraints(game.getRoot(), rootSequence, new TIntHashSet());

		CreateDualVariablesAndConstraints();
		
		
		SetObjective();
	}

	/**
	 * Recursive function that traverses the game tree, assigning Id values, starting at 1 due to the empty sequence, to sequences in pre-order. Sequence IDs are only assigned if an information set has not previously been visited
	 * @param currentNodeId id into the game.nodes array
	 * @param visitedP1 an integer set indicating which information sets have already been visited for Player 1
	 * @param visitedP2 an integer set indicating which information sets have already been visited for Player 2
	 */
	private void CreateSequenceFormIds(int currentNodeId, TIntSet visitedP1, TIntSet visitedP2) {
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) return;
		
		for (Action action : node.getActions()) {
			if (node.getPlayer() == 1 && !visitedP1.contains(node.getInformationSet())) {
				sequenceIdByInformationSetAndActionP1[node.getInformationSet()].put(action.getName(), numSequencesP1++);
			} else if (node.getPlayer() == 2 && !visitedP2.contains(node.getInformationSet())) {
				sequenceIdByInformationSetAndActionP2[node.getInformationSet()].put(action.getName(), numSequencesP2++);
			}
			CreateSequenceFormIds(action.getChildId(), visitedP1, visitedP2);
		}		
		if (node.getPlayer() == 1) {
			visitedP1.add(node.getInformationSet());
		} else if (node.getPlayer() == 2) {
			visitedP2.add(node.getInformationSet());
		}
	}

	/**
	 * Creates sequence form variables in pre-order traversal. A constraint is also added to ensure that the probability sum over the new sequences sum to the value of the last seen sequence on the path to this information set 
	 * @param currentNodeId
	 * @param parentSequence last seen sequence belonging to the primal player
	 * @param visited keeps track of which information sets have been visited
	 * @throws IloException
	 */
	private void CreateSequenceFormVariablesAndConstraints(int currentNodeId, IloNumVar parentSequence, TIntSet visited) throws IloException{
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) return;
		
		if (node.getPlayer() == playerToSolveFor && !visited.contains(node.getInformationSet())) {
			visited.add(node.getInformationSet());
			IloLinearNumExpr sum = cplex.linearNumExpr();
			//sum.addTerm(-1, parentSequence);
			for (Action action : node.getActions()) {
				// real-valued variable in (0,1)
				IloNumVar v = cplex.numVar(0, 1, "X" + node.getInformationSet() + action.getName());
				strategyVarsByInformationSet[node.getInformationSet()].put(action.getName(), v);
				int sequenceId = getSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName());
				strategyVarsBySequenceId[sequenceId] = v;
				// add 1*v to the sum over all the sequences at the information set
				sum.addTerm(1, v);
				CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited);
			}
			// sum_{sequences} = parent_sequence. cplex.addEq returns a reference to the range object describing the constraint. This is useful for dynamically modifying the model in derived classes.
			primalConstraints.put(node.getInformationSet(), cplex.addEq(sum, parentSequence,"Primal"+node.getInformationSet()));
		} else {
			for (Action action : node.getActions()) {
				if (node.getPlayer() == playerToSolveFor) {
					// update parentSequence to be the current sequence
					IloNumVar v = strategyVarsByInformationSet[node.getInformationSet()].get(action.getName());
					CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited);
				} else {
					CreateSequenceFormVariablesAndConstraints(action.getChildId(), parentSequence, visited);
				}
			}
		}
	}


	private void CreateDualVariablesAndConstraints() throws IloException {
		int numVars = 0;
		if (playerToSolveFor == 1) {
			numVars = game.getNumInformationSetsPlayer2() + 1;
		} else {
			numVars = game.getNumInformationSetsPlayer1() + 1;
		}
		String[] names = new String[numVars];
		for (int i = 0; i < numVars; i++) names[i] = "Y" + i;
		this.dualVars = cplex.numVarArray(numVars, -Double.MAX_VALUE, Double.MAX_VALUE, names);
		

		InitializeDualSequenceMatrix();
		InitializeDualPayoffMatrix();
		for (int sequenceId = 0; sequenceId < numDualSequences; sequenceId++) {
			CreateDualConstraintForSequence(sequenceId);
		}
	}

	private void InitializeDualSequenceMatrix() throws IloException {
		sequenceFormDualMatrix[0].add(0);
		InitializeDualSequenceMatrixRecursive(game.getRoot(), new TIntHashSet(), 0);
	}
	
	private void InitializeDualSequenceMatrixRecursive(int currentNodeId, TIntSet visited, int parentSequenceId) throws IloException {
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
		InitializeDualPayoffMatrixRecursive(game.getRoot(), 0, 0, 1); 		// Start with the root sequences
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
	
	private void CreateDualConstraintForSequence(int sequenceId) throws IloException{
		IloLinearNumExpr lhs = cplex.linearNumExpr();
		for (int i = 0; i < sequenceFormDualMatrix[sequenceId].size(); i++) {
			int informationSetId = sequenceFormDualMatrix[sequenceId].get(i);// + (1-game.getSmallestInformationSetId(playerNotToSolveFor)); // map information set ID to 1 indexing. Assumes that information sets are named by consecutive integers
			int valueMultiplier = i == 0? 1 : -1;
			lhs.addTerm(valueMultiplier, dualVars[informationSetId]);
		}
		
		//IloLinearNumExpr rhs = cplex.linearNumExpr();
		TIntDoubleIterator it = dualPayoffMatrix[sequenceId].iterator();
		for ( int i = dualPayoffMatrix[sequenceId].size(); i-- > 0; ) {
			it.advance();
			lhs.addTerm(-it.value(), strategyVarsBySequenceId[it.key()]);
		}
		
				
		dualConstraints.put(sequenceId, cplex.addGe(lhs, 0, "Dual"+sequenceId));
	}
	
	/**
	 * Fills in the convenience arrays nodeNatureProbabilities and sequenceIdForNodeP1/2
	 */
	void computeAuxiliaryInformationForNodes() {
		computeAuxiliaryInformationForNodesRecursive(game.getRoot(), 0, 0, 1);
	}
	private void computeAuxiliaryInformationForNodesRecursive(int currentNodeId, int sequenceIdP1, int sequenceIdP2, double natureProbability) {
		Node node = this.game.getNodeById(currentNodeId);
		
		nodeNatureProbabilities[node.getNodeId()] = natureProbability;
		sequenceIdForNodeP1[currentNodeId] = sequenceIdP1;
		sequenceIdForNodeP2[currentNodeId] = sequenceIdP2;
		if (node.isLeaf()) return;
		
		for (Action action : node.getActions()) {
			int newSequenceIdP1= node.getPlayer() == 1? sequenceIdByInformationSetAndActionP1[node.getInformationSet()].get(action.getName()) : sequenceIdP1;
			int newSequenceIdP2= node.getPlayer() == 2? sequenceIdByInformationSetAndActionP2[node.getInformationSet()].get(action.getName()) : sequenceIdP2;
			double newNatureProbability = node.getPlayer() == 0? natureProbability * action.getProbability() : natureProbability;
			computeAuxiliaryInformationForNodesRecursive(action.getChildId(), newSequenceIdP1, newSequenceIdP2, newNatureProbability);
		}
	}
	
	int getSequenceIdForPlayerToSolveFor(int informationSet, String actionName) {
		if (playerToSolveFor == 1) {
			return sequenceIdByInformationSetAndActionP1[informationSet].get(actionName);
		} else {
			return sequenceIdByInformationSetAndActionP2[informationSet].get(actionName);
		}
	}
	
	int getSequenceIdForPlayerNotToSolveFor(int informationSet, String actionName) {
		if (playerNotToSolveFor == 1) {
			return sequenceIdByInformationSetAndActionP1[informationSet].get(actionName);
		} else {
			return sequenceIdByInformationSetAndActionP2[informationSet].get(actionName);
		}
	}

	
	private void SetObjective() throws IloException {
		cplex.addMinimize(cplex.prod(1, dualVars[0]));
	}

	
	public int getPlayerToSolveFor() {
		return playerToSolveFor;
	}

	public int getPlayerNotToSolveFor() {
		return playerNotToSolveFor;
	}

	public IloCplex getCplex() {
		return cplex;
	}

	public IloNumVar[] getDualVars() {
		return dualVars;
	}

	public HashMap<String, IloNumVar>[] getStrategyVarsByInformationSet() {
		return strategyVarsByInformationSet;
	}

	public TIntList[] getSequenceFormDualMatrix() {
		return sequenceFormDualMatrix;
	}

	public TIntDoubleMap[] getDualPayoffMatrix() {
		return dualPayoffMatrix;
	}

	public TObjectIntMap<String>[] getSequenceIdByInformationSetAndActionP1() {
		return sequenceIdByInformationSetAndActionP1;
	}

	public TObjectIntMap<String>[] getSequenceIdByInformationSetAndActionP2() {
		return sequenceIdByInformationSetAndActionP2;
	}

	public IloNumVar[] getStrategyVarsBySequenceId() {
		return strategyVarsBySequenceId;
	}

	public int getNumSequencesP1() {
		return numSequencesP1;
	}

	public int getNumSequencesP2() {
		return numSequencesP2;
	}

	public int getNumPrimalSequences() {
		return numPrimalSequences;
	}

	public int getNumDualSequences() {
		return numDualSequences;
	}

	public TIntObjectMap<IloConstraint> getPrimalConstraints() {
		return primalConstraints;
	}

	public TIntObjectMap<IloRange> getDualConstraints() {
		return dualConstraints;
	}
}












