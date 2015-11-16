package edu.cmu.cs.kroer.extensive_form_game.solver;

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
import ilog.concert.IloConstraint;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.cs.kroer.extensive_form_game.Game;
import edu.cmu.cs.kroer.extensive_form_game.Game.Action;
import edu.cmu.cs.kroer.extensive_form_game.Game.Node;

public class BestResponseLPSolver extends ZeroSumGameSolver {
	
	Game game;
	
	int playerToSolveFor;
	int playerNotToSolveFor;
	
	double[][] opponentStrategy;
	
	IloCplex cplex;
	//IloNumVar[] modelStrategyVars;
	HashMap<String, IloNumVar>[] strategyVarsByInformationSet; // indexed as [inforationSetId][action.name]
	IloLinearNumExpr objective;
	
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP1; // indexed as [informationSetId][action.name]
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP2; // indexed as [informationSetId][action.name]
	IloNumVar[] strategyVarsBySequenceId;
	int numSequencesP1;
	int numSequencesP2;
	int numPrimalSequences;
	int numPrimalInformationSets;
	
	String[] primalSequenceNames;
	
	TIntObjectMap<IloConstraint> primalConstraints; // indexed as [informationSetId], without correcting for 1-indexing

	double[] nodeNatureProbabilities; // indexed as [nodeId]. Returns the probability of that node being reached when considering only nature nodes
	int[] sequenceIdForNodeP1; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 1 on the path to the node. 
	int[] sequenceIdForNodeP2; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 2 on the path to the node. 

	public BestResponseLPSolver(Game game, int playerToSolveFor, double[][] opponentStrategy) {
		super(game);
		this.game = game;
		this.opponentStrategy = opponentStrategy;
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
		numPrimalInformationSets = playerToSolveFor == 1 ? game.getNumInformationSetsPlayer1() : game.getNumInformationSetsPlayer2();

		
		primalSequenceNames = new String[numPrimalSequences];
		
		
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
		nodeNatureProbabilities = new double[game.getNumNodes()+1]; // Use +1 to be robust for non-zero indexed nodes
		sequenceIdForNodeP1 = new int[game.getNumNodes()+1];
		sequenceIdForNodeP2 = new int[game.getNumNodes()+1];
		
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
				valueOfGame = playerToSolveFor == player1 ? cplex.getObjValue() : -cplex.getObjValue();
			}
		} catch (IloException e) {
			e.printStackTrace();
			System.out.println("Error SequenceFormLPSolver::solveGame: solve exception");
		}
	}

	/**
	 * Creates and returns a mapping from variable names to the values they take on in the solution computed by CPLEX.
	 */
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
	 * Creates and returns a mapping from information set id and action name pairs to the probability of taking that action in the computed solution
	 */
	@SuppressWarnings("unchecked")
	public TObjectDoubleMap<String>[] getInformationSetActionProbabilities() {
		TObjectDoubleMap<String>[] map = new TObjectDoubleHashMap[numPrimalInformationSets];
		for (int informationSetId = 0; informationSetId < numPrimalInformationSets; informationSetId++) {
			map[informationSetId] = new TObjectDoubleHashMap<>();
			double sum = 0;
			for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
				try {
					sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
			for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
				try {
					if (sum > 0) {
						map[informationSetId].put(actionName, cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum);
					} else {
						map[informationSetId].put(actionName, 0);
					}
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	/**
	 * Creates and returns a mapping from information set id and action name pairs to the probability of taking that action in the computed solution
	 */
	public TIntDoubleMap[] getInformationSetActionProbabilitiesByActionId() {
		TIntDoubleMap[] map = new TIntDoubleHashMap[numPrimalInformationSets];
		for (int informationSetId = 0; informationSetId < numPrimalInformationSets; informationSetId++) {
			map[informationSetId] = new TIntDoubleHashMap();
			double sum = 0;
			for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
				try {
					sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId); actionId++) {
				String actionName = game.getActionsAtInformationSet(playerToSolveFor, informationSetId)[actionId].getName();
				try {
					if (sum > 0) {
						map[informationSetId].put(actionId, cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum);
					} else {
						map[informationSetId].put(actionId, 0);
					}
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	/**
	 * Creates and returns a mapping from information set id and action name pairs to the probability of taking that action in the computed solution
	 */
	@Override
	public double[][][] getStrategyProfile() {
		double[][][] map = new double[3][][];
		
		map[playerToSolveFor] = new double[numPrimalInformationSets][];
		for (int informationSetId = 0; informationSetId < numPrimalInformationSets; informationSetId++) {
			map[playerToSolveFor][informationSetId] = new double[game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId)];
			double sum = 0;
			for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
				try {
					sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId); actionId++) {
				String actionName = game.getActionsAtInformationSet(playerToSolveFor, informationSetId)[actionId].getName();
				try {
					if (sum > 0) {
						map[playerToSolveFor][informationSetId][actionId] = cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum;
					} else {
						map[playerToSolveFor][informationSetId][actionId] = 0;
					}
				} catch (IloException e) {
					e.printStackTrace();
				}
			}
		}
		map[playerNotToSolveFor] = opponentStrategy;
		
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
			System.out.println("Solve status: " + cplex.getStatus());
			if	(cplex.getStatus() == IloCplex.Status.Optimal) {
				System.out.println("Objective value: " + this.getValueOfGame());
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
			cplex.setParam(IloCplex.IntParam.MIPInterval, -1);
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

		objective = cplex.linearNumExpr();
		
		// The empty sequence is the 0'th sequence for each player
		numSequencesP1 = numSequencesP2 = 1;
		primalSequenceNames[0] = "root";
		CreateSequenceFormIds(game.getRoot(), new TIntHashSet(), new TIntHashSet());
		assert(numSequencesP1 == game.getNumSequencesP1()); // Ensure that our recursive function agrees with the game reader on how many sequences there are
		assert(numSequencesP2 == game.getNumSequencesP2());
		
		// create root sequence var
		IloNumVar rootSequence = cplex.numVar(1, 1, "Xroot");
		strategyVarsBySequenceId[0] = rootSequence;
		CreateSequenceFormVariablesAndConstraints(game.getRoot(), rootSequence, new TIntHashSet(), 1);
		
		
		setObjective();
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
				if (playerToSolveFor ==1) {
					primalSequenceNames[numSequencesP1-1] = Integer.toString(node.getInformationSet()) + ";" + action.getName();
				}
			} else if (node.getPlayer() == 2 && !visitedP2.contains(node.getInformationSet())) {
				sequenceIdByInformationSetAndActionP2[node.getInformationSet()].put(action.getName(), numSequencesP2++);
				if (playerToSolveFor == 2) {
					primalSequenceNames[numSequencesP2-1] = Integer.toString(node.getInformationSet()) + ";" + action.getName();
				}
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
	private void CreateSequenceFormVariablesAndConstraints(int currentNodeId, IloNumVar parentSequence, TIntSet visited, double probability) throws IloException{
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) {
			double value = playerToSolveFor == player1 ? node.getValue() : -node.getValue();
			objective.addTerm(probability * value, parentSequence);
			return;
		}
		
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
				CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited, probability);
			}
			// sum_{sequences} = parent_sequence. cplex.addEq returns a reference to the range object describing the constraint. This is useful for dynamically modifying the model in derived classes.
			primalConstraints.put(node.getInformationSet(), cplex.addEq(sum, parentSequence,"Primal"+node.getInformationSet()));
		} else {
			for (int actionId = 0; actionId < node.getActions().length; actionId++) {
			Action action = node.getActions()[actionId];
				if (node.getPlayer() == playerToSolveFor) {
					// update parentSequence to be the current sequence
					IloNumVar v = strategyVarsByInformationSet[node.getInformationSet()].get(action.getName());
					CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited, probability);
				} else {
					double newProbability = getProbabilityOfAction(node, actionId) * probability;
					CreateSequenceFormVariablesAndConstraints(action.getChildId(), parentSequence, visited, newProbability);
				}
			}
		}
	}
	
	private double getProbabilityOfAction(Node node, int actionId) {
		if (node.getPlayer() == nature) {
			return node.getActions()[actionId].getProbability();
		} else if (node.getPlayer() == playerNotToSolveFor){
			return opponentStrategy[node.getInformationSet()][actionId];
		} else {
			System.out.println("BestResponseLPSolver::getProbabilityOfAction error: tried to get probability of playerToSolveFor action");
			return -1;
		}
	}

	private void setObjective() throws IloException {
		cplex.addMaximize(objective);
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
	
}
