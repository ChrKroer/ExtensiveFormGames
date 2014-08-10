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
	IloNumVar[] dualVars;
	HashMap<String, IloNumVar>[] strategyVarsByInformationSet;

	TIntList[] sequenceFormDualMatrix; // indexed as [sequence id][information set]
	TIntDoubleMap[] dualPayoffMatrix; // indexed by [dual sequence][primal sequence]
	
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP1;
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP2;
	IloNumVar[] strategyVarsBySequenceId;
	int numSequencesP1;
	int numSequencesP2;
	int numPrimalSequences;
	int numDualSequences;
	
	TIntObjectMap<IloConstraint> primalConstraints;
	TIntObjectMap<IloRange> dualConstraints;

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
	}
	
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

	public void writeStrategyToFile(String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for (IloNumVar v : strategyVarsBySequenceId) {
				fw.write(v.getName() + ": \t" + cplex.getValue(v) + "\n");
			}
			fw.close();
		} catch (IOException | IloException e) {
			e.printStackTrace();
		}
	}
	
	public void writeModelToFile(String filename) {
		try {
			cplex.exportModel(filename);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
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
	
	private void setUpModel() throws IloException {
		setCplexParameters();

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

	private void CreateSequenceFormVariablesAndConstraints(int currentNodeId, IloNumVar parentSequence, TIntSet visited) throws IloException{
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) return;
		
		if (node.getPlayer() == playerToSolveFor && !visited.contains(node.getInformationSet())) {
			visited.add(node.getInformationSet());
			IloLinearNumExpr sum = cplex.linearNumExpr();
			//sum.addTerm(-1, parentSequence);
			for (Action action : node.getActions()) {
				IloNumVar v = cplex.numVar(0, 1, "X" + node.getInformationSet() + action.getName());
				strategyVarsByInformationSet[node.getInformationSet()].put(action.getName(), v);
				int sequenceId = GetSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName()); //= playerToSolveFor == 1 ? sequenceIdByInformationSetAndActionP1[node.getInformationSet()].get(action.getName()) : sequenceIdByInformationSetAndActionP2[node.getInformationSet()].get(action.getName()); 
				strategyVarsBySequenceId[sequenceId] = v;
				sum.addTerm(1, v);
				CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited);
			}
			primalConstraints.put(node.getInformationSet(), cplex.addEq(sum, parentSequence,"Primal"+node.getInformationSet()));
		} else {
			for (Action action : node.getActions()) {
				if (node.getPlayer() == playerToSolveFor) {
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
				int newSequenceId = GetSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
				sequenceFormDualMatrix[newSequenceId].add(informationSetMatrixId);
				InitializeDualSequenceMatrixRecursive(action.getChildId(), visited, newSequenceId);
			}
		} else {
			for (Action action : node.getActions()) {
				int newSequenceId = playerNotToSolveFor == node.getPlayer()? GetSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName()) : parentSequenceId;
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
				int newPrimalSequence = node.getPlayer() == playerToSolveFor? GetSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName()) : primalSequence;
				int newDualSequence = node.getPlayer() == playerNotToSolveFor? GetSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName()) : dualSequence;
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
	
	private int GetSequenceIdForPlayerToSolveFor(int informationSet, String actionName) {
		if (playerToSolveFor == 1) {
			return sequenceIdByInformationSetAndActionP1[informationSet].get(actionName);
		} else {
			return sequenceIdByInformationSetAndActionP2[informationSet].get(actionName);
		}
	}
	
	private int GetSequenceIdForPlayerNotToSolveFor(int informationSet, String actionName) {
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












