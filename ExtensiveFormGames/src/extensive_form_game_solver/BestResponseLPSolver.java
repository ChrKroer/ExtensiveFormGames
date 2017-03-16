package extensive_form_game_solver;

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

import extensive_form_game.Game;
import extensive_form_game.Game.Action;
import extensive_form_game.Game.Node;

public class BestResponseLPSolver extends ZeroSumGameSolver {
	
	Game game;
	
	int playerToSolveFor;
	int playerNotToSolveFor;
	
	double[][] opponentStrategy;
	
	protected GRBEnv env;
	protected GRBModel model;
	//GRBVar[] modelStrategyVars;
	HashMap<String, GRBVar>[] strategyVarsByInformationSet; // indexed as [inforationSetId][action.name]
	GRBLinExpr objective;
	
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP1; // indexed as [informationSetId][action.name]
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP2; // indexed as [informationSetId][action.name]
	GRBVar[] strategyVarsBySequenceId;
	int numSequencesP1;
	int numSequencesP2;
	int numPrimalSequences;
	int numPrimalInformationSets;
	
	String[] primalSequenceNames;
	
	TIntObjectMap<GRBConstr> primalConstraints; // indexed as [informationSetId], without correcting for 1-indexing

	double[] nodeNatureProbabilities; // indexed as [nodeId]. Returns the probability of that node being reached when considering only nature nodes
	int[] sequenceIdForNodeP1; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 1 on the path to the node. 
	int[] sequenceIdForNodeP2; // indexed as [nodeId]. Returns the sequenceId of the last sequence belonging to Player 2 on the path to the node. 

	public BestResponseLPSolver(Game game, int playerToSolveFor, double[][] opponentStrategy) {
		super(game);
		this.game = game;
		this.opponentStrategy = opponentStrategy;
		try {
			env = new GRBEnv("mip.log");
			model = new GRBModel(env);
		} catch (GRBException e) {
			System.out.println("Error SequenceFormLPSolver(): CPLEX setup failed");
		}
		
		this.playerToSolveFor = playerToSolveFor;
		this.playerNotToSolveFor = (playerToSolveFor % 2) + 1;
		
		
		initializeDataStructures();
		//modelStrategyVars = new ArrayList<GRBVar>();
		//dualVars = new ArrayList<GRBVar>();
		//strategyVarsByRealGameSequences = new ArrayList<GRBVar>();
		
		try {
			setUpModel();
		} catch (GRBException e) {
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
		this.strategyVarsByInformationSet = (HashMap<String, GRBVar>[]) new HashMap[numInformationSets+1];
		for (int i = 0; i <= numInformationSets; i++) {
			this.strategyVarsByInformationSet[i] = new HashMap<String, GRBVar>();
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
			strategyVarsBySequenceId = new GRBVar[game.getNumSequencesP1()];
		} else {
			strategyVarsBySequenceId = new GRBVar[game.getNumSequencesP2()];
		}
		
		primalConstraints = new TIntObjectHashMap<GRBConstr>();
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
			model.optimize();
            strategyVars = new  double[strategyVarsBySequenceId.length];
            for (int i = 0; i < strategyVarsBySequenceId.length; i++) {
                GRBVar v = strategyVarsBySequenceId[i];
                strategyVars[i] = v.get(GRB.DoubleAttr.X);
            }
            double obj = model.get(GRB.DoubleAttr.ObjVal);
            valueOfGame = playerToSolveFor == player1 ? obj : -obj;
		} catch (GRBException e) {
			e.printStackTrace();
			System.out.println("Error SequenceFormLPSolver::solveGame: solve exception");
		}
	}

	/**
	 * Creates and returns a mapping from variable names to the values they take on in the solution computed by CPLEX.
	 */
	public TObjectDoubleMap<String> getStrategyVarMap() {
		TObjectDoubleMap<String> map = new TObjectDoubleHashMap<String>();
		for (GRBVar v : strategyVarsBySequenceId) {
			try {
				map.put(v.get(GRB.StringAttr.VarName), v.get(GRB.DoubleAttr.X));
			} catch (GRBException e) {
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
            for (GRBVar v : strategyVarsByInformationSet[informationSetId].values()) {
                try {
                    sum += v.get(GRB.DoubleAttr.X);
				} catch (GRBException e) {
					e.printStackTrace();
				}
			}
            for (GRBVar v : strategyVarsByInformationSet[informationSetId].values()) {
				try {
					if (sum > 0) {
                        map[informationSetId].put(v.get(GRB.StringAttr.VarName), v.get(GRB.DoubleAttr.X) / sum);
					} else {
                        map[informationSetId].put(v.get(GRB.StringAttr.VarName), 0);
					}
				} catch (GRBException e) {
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
            for (GRBVar v : strategyVarsByInformationSet[informationSetId].values()) {
                try {
                    sum += v.get(GRB.DoubleAttr.X);
				} catch (GRBException e) {
					e.printStackTrace();
				}
			}
            for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId); actionId++) {
                String actionName = game.getActionsAtInformationSet(playerToSolveFor, informationSetId)[actionId].getName();
                try {
                    if (sum > 0) {
                        map[informationSetId].put(actionId, strategyVarsByInformationSet[informationSetId].get(actionName).get(GRB.DoubleAttr.X) / sum);
                    } else {
                        map[informationSetId].put(actionId, 0);
                    }
                } catch (GRBException e) {
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
            for (GRBVar v : strategyVarsByInformationSet[informationSetId].values()) {
                try {
                    sum += v.get(GRB.DoubleAttr.X);
				} catch (GRBException e) {
					e.printStackTrace();
				}
			}
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolveFor, informationSetId); actionId++) {
				String actionName = game.getActionsAtInformationSet(playerToSolveFor, informationSetId)[actionId].getName();
				try {
					if (sum > 0) {
						map[playerToSolveFor][informationSetId][actionId] = strategyVarsByInformationSet[informationSetId].get(actionName).get(GRB.DoubleAttr.X) / sum;
					} else {
						map[playerToSolveFor][informationSetId][actionId] = 0;
					}
				} catch (GRBException e) {
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
        for (GRBVar v : strategyVarsBySequenceId) {
            try {
                System.out.println(v.get(GRB.StringAttr.VarName) + ": \t" + v.get(GRB.DoubleAttr.X));
            } catch (GRBException e) {
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
            int status = model.get(GRB.IntAttr.Status);
            System.out.println("Solve status: " + status);
            if	(status == GRB.Status.OPTIMAL) {
                System.out.println("Objective value: " + this.valueOfGame);
            }
        } catch (GRBException e) {
            e.printStackTrace();
        }
	}

    /**
     * Writes the computed strategy to a file. An exception is thrown if solve() has not been called.
     * @param filename the absolute path to the file being written to
     */
    public void writeStrategyToFile(String filename) throws GRBException{
        try {
            FileWriter fw = new FileWriter(filename);
            for (GRBVar v : strategyVarsBySequenceId) {
                fw.write(v.get(GRB.StringAttr.VarName) + ": \t" + v.get(GRB.DoubleAttr.X) + "\n");
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
			model.write(filename);
		} catch (GRBException e) {
			e.printStackTrace();
		}
	}
	
//	/**
//	 * Sets the parameters of CPLEX such that minimal output is produced.
//	 */
//	private void setCplexParameters() {
//		try {
//			cplex.setParam(IloCplex.IntParam.SimDisplay, 0);
//			cplex.setParam(IloCplex.IntParam.MIPDisplay, 0);
//			cplex.setParam(IloCplex.IntParam.MIPInterval, -1);
//			cplex.setParam(IloCplex.IntParam.TuningDisplay, 0);
//			cplex.setParam(IloCplex.IntParam.BarDisplay, 0);
//			cplex.setParam(IloCplex.IntParam.SiftDisplay, 0);
//			cplex.setParam(IloCplex.IntParam.ConflictDisplay, 0);
//			cplex.setParam(IloCplex.IntParam.NetDisplay, 0);
//			cplex.setParam(IloCplex.DoubleParam.TiLim, 1e+75);
//		} catch (GRBException e) {
//			e.printStackTrace();
//		}
//	}
	
	/**
	 * Builds the LP model based on the game instance.
	 * @throws GRBException
	 */
	private void setUpModel() throws GRBException {
//		setCplexParameters();

		objective = new GRBLinExpr();
		
		// The empty sequence is the 0'th sequence for each player
		numSequencesP1 = numSequencesP2 = 1;
		primalSequenceNames[0] = "root";
		CreateSequenceFormIds(game.getRoot(), new TIntHashSet(), new TIntHashSet());
		assert(numSequencesP1 == game.getNumSequencesP1()); // Ensure that our recursive function agrees with the game reader on how many sequences there are
		assert(numSequencesP2 == game.getNumSequencesP2());
		
		// create root sequence var
        GRBVar rootSequence = model.addVar(1, 1, 0, GRB.CONTINUOUS, "Xroot");
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
	 * @throws GRBException
	 */
	private void CreateSequenceFormVariablesAndConstraints(int currentNodeId, GRBVar parentSequence, TIntSet visited, double probability) throws GRBException{
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) {
			double value = playerToSolveFor == player1 ? node.getValue() : -node.getValue();
			objective.addTerm(probability * value, parentSequence);
			return;
		}
		
		if (node.getPlayer() == playerToSolveFor && !visited.contains(node.getInformationSet())) {
			visited.add(node.getInformationSet());
            GRBLinExpr sum = new GRBLinExpr();
			//sum.addTerm(-1, parentSequence);
			for (Action action : node.getActions()) {
				// real-valued variable in (0,1)
                GRBVar v = model.addVar(0, 1, 0, GRB.CONTINUOUS, "X" + node.getInformationSet() + action.getName());
				strategyVarsByInformationSet[node.getInformationSet()].put(action.getName(), v);
				int sequenceId = getSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName());
				strategyVarsBySequenceId[sequenceId] = v;
				// add 1*v to the sum over all the sequences at the information set
				sum.addTerm(1, v);
				CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited, probability);
			}
			// sum_{sequences} = parent_sequence. gurobi.addEq returns a reference to the range object describing the constraint. This is useful for dynamically modifying the model in derived classes.
            primalConstraints.put(node.getInformationSet(), model.addConstr(sum, '=', parentSequence,"Primal"+node.getInformationSet()));
		} else {
			for (int actionId = 0; actionId < node.getActions().length; actionId++) {
			Action action = node.getActions()[actionId];
				if (node.getPlayer() == playerToSolveFor) {
					// update parentSequence to be the current sequence
					GRBVar v = strategyVarsByInformationSet[node.getInformationSet()].get(action.getName());
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

	private void setObjective() throws GRBException {
		model.setObjective(objective, GRB.MAXIMIZE);
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
