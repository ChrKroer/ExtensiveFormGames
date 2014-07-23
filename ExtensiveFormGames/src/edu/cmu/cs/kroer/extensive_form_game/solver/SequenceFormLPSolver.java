package edu.cmu.cs.kroer.extensive_form_game.solver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import edu.cmu.cs.kroer.extensive_form_game.Game;
import edu.cmu.cs.kroer.extensive_form_game.Game.Action;
import edu.cmu.cs.kroer.extensive_form_game.Game.Node;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
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
	IloNumVar[] modelStrategyVars;
	IloNumVar[] dualVars;
	HashMap<String, IloNumVar>[] strategyVarsByInformationSet;

	TIntDoubleMap[] sequenceFormDualMatrix;
	
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP1;
	TObjectIntMap<String>[] sequenceIdByInformationSetAndActionP2;
	IloNumVar[] strategyVarsBySequenceId;
	int numSequencesP1;
	int numSequencesP2;
	
	@SuppressWarnings("unchecked")
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
	
	private void initializeDataStructures() {
		int numInformationSets = 0;
		if (playerToSolveFor == 1) {
			numInformationSets = game.getNumInformationSetsPlayer1();
		} else {
			numInformationSets = game.getNumInformationSetsPlayer2();
		}
		this.strategyVarsByInformationSet = (HashMap<String, IloNumVar>[]) new HashMap[numInformationSets+1];
		for (int i = 1; i <= numInformationSets; i++) {
			this.strategyVarsByInformationSet[i] = new HashMap<String, IloNumVar>();
		}

		for (int i = 1; i <= game.getNumInformationSetsPlayer1(); i++) {
			sequenceIdByInformationSetAndActionP1[i] = new TObjectIntHashMap<String>();
		}
		for (int i = 1; i <= game.getNumInformationSetsPlayer2(); i++) {
			sequenceIdByInformationSetAndActionP2[i] = new TObjectIntHashMap<String>();
		}

		if (playerToSolveFor == 1) {
			strategyVarsBySequenceId = new IloNumVar[game.getNumSequencesP1()];
		} else {
			strategyVarsBySequenceId = new IloNumVar[game.getNumSequencesP2()];
		}

	}
	
	@Override
	public void solveGame() {
		try {
			if (cplex.solve()) {
				strategyVars = cplex.getValues(modelStrategyVars);
				valueOfGame = cplex.getObjValue();
			}
		} catch (IloException e) {
			e.printStackTrace();
			System.out.println("Error SequenceFormLPSolver::solveGame: solve exception");
		}
	}

	@Override
	public TObjectDoubleMap<String> getStrategyVarMap() {
		TObjectDoubleMap<String> map = new TObjectDoubleHashMap<String>();
		for (IloNumVar v : modelStrategyVars) {
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
		for (IloNumVar v : modelStrategyVars) {
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

	public void writeToFile(String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for (IloNumVar v : modelStrategyVars) {
				fw.write(v.getName() + ": \t" + cplex.getValue(v) + "\n");
			}
			fw.close();
		} catch (IOException | IloException e) {
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
		IloNumVar rootSequence = cplex.numVar(1, 1, "root seq");
		CreateSequenceFormVariablesAndConstraints(game.getRoot(), rootSequence, new TIntHashSet());

		CreateDualVariablesAndConstraints();
		SetObjective();
	}

	private void CreateSequenceFormIds(int currentNodeId, TIntSet visitedP1, TIntSet visitedP2) {
		Node node = game.getNodeById(currentNodeId);
		for (Action action : node.getActions()) {
			if (node.getPlayer() == 1 && !visitedP1.contains(node.getInformationSet())) {
				visitedP1.add(node.getInformationSet());
				sequenceIdByInformationSetAndActionP1[node.getInformationSet()].put(action.getName(), numSequencesP1++);
			} else if (node.getPlayer() == 2 && !visitedP2.contains(node.getInformationSet())) {
				visitedP2.add(node.getInformationSet());
				sequenceIdByInformationSetAndActionP2[node.getInformationSet()].put(action.getName(), numSequencesP2++);
			}
			CreateSequenceFormIds(action.getChildId(), visitedP1, visitedP2);
		}		
	}

	private void CreateSequenceFormVariablesAndConstraints(int currentNodeId, IloNumVar parentSequence, TIntSet visited) throws IloException{
		Node node = this.game.getNodeById(currentNodeId);
		if (node.getPlayer() == playerToSolveFor && !visited.contains(node.getInformationSet())) {
			visited.add(node.getInformationSet());
			IloLinearNumExpr sum = cplex.linearNumExpr();
			for (Action action : node.getActions()) {
				IloNumVar v = cplex.numVar(0, 1, node.getInformationSet() + ";" + action.getName());
				strategyVarsByInformationSet[node.getInformationSet()].put(action.getName(), v);
				int sequenceId = playerToSolveFor == 1 ? sequenceIdByInformationSetAndActionP1[node.getInformationSet()].get(action.getName()) : sequenceIdByInformationSetAndActionP2[node.getInformationSet()].get(action.getName()); 
				strategyVarsBySequenceId[sequenceId] = v;
				sum.addTerm(1, v);
				CreateSequenceFormVariablesAndConstraints(action.getChildId(), v, visited);
			}
			cplex.addEq(sum, 1);
		} else {
			for (Action action : node.getActions()) {
				CreateSequenceFormVariablesAndConstraints(action.getChildId(), parentSequence, visited);
			}
		}
	}


	private void CreateDualVariablesAndConstraints() throws IloException {
		int numVars = 0;
		if (playerToSolveFor == 1) {
			numVars = game.getNumInformationSetsPlayer1() + 1;
		} else {
			numVars = game.getNumInformationSetsPlayer2() + 1;
		}
		this.dualVars = cplex.numVarArray(numVars, -Double.MAX_VALUE, Double.MAX_VALUE);
		
		sequenceFormDualMatrix = new TIntDoubleMap[numVars];
		for (int i = 0; i < numVars; i++) {
			sequenceFormDualMatrix[i] = new TIntDoubleHashMap();
		}
		InitializeDualSequenceMatrix();
	}

	private void InitializeDualSequenceMatrix() throws IloException {
		sequenceFormDualMatrix[0].put(0, 1);
		InitializeDualSequenceMatrixRecursive(game.getRoot(), new TIntHashSet(), 1);
	}
	
	private void InitializeDualSequenceMatrixRecursive(int currentNodeId, TIntSet visited, int numSequences) throws IloException {
		Node node = this.game.getNodeById(currentNodeId);
		for (Action action : node.getActions()) {
			if (playerNotToSolveFor == node.getPlayer() && !visited.contains(node.getInformationSet())) {
				visited.add(node.getInformationSet());
				
			}
		} 
	}
	
	private void SetObjective() {
		// TODO Auto-generated method stub
		
	}

}












