package edu.cmu.cs.kroer.extensive_form_game.solver;


import java.util.Arrays;

import org.apache.commons.math3.distribution.UniformRealDistribution;

import edu.cmu.cs.kroer.extensive_form_game.Game;
import edu.cmu.cs.kroer.extensive_form_game.GameState;
import edu.cmu.cs.kroer.extensive_form_game.TestConfiguration;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class OpponentSamplingCFR extends ZeroSumGameSolver {
	int nature = 0;
	int player1 = 1;
	int player2 = 2;
	
	int totalIterationsRun = 0;
	
	double[][][] averagedStrategy;
	double[][][] currentStrategy;
	double[][][] regretTable;
	int[][] informationSetCounter;
	final UniformRealDistribution distribution = new UniformRealDistribution(0, 1);

	public OpponentSamplingCFR(Game game) {
		super(game);
		initializeDataStructures();
	}

	@Override
	public void solveGame() {
		solveGame(10);
	}

	public void solveGame(int numIterations) {
		runCFR(numIterations);
	}

	@Override
	public void printStrategyVarsAndGameValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void printGameValue() {
		// TODO Auto-generated method stub
		
	}

//	@Override
//	public TObjectDoubleMap<String>[] getInformationSetActionProbabilities() {
//		int numInformationSets = 1; 
//		TObjectDoubleMap<String>[] map = new TObjectDoubleHashMap[numInformationSets];
//		for (int informationSetId = 0; informationSetId < numInformationSets; informationSetId++) {
//			map[informationSetId] = new TObjectDoubleHashMap<>();
//			double sum = 0;
//			int numActions = game.getNumActionsAtInformationSet(1, informationSetId);
//			
//			for (int action = 0; action < numActions; action++) {
//				String actionName = game.
//				try {
//					sum += cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName));
//				} catch (IloException e) {
//					e.printStackTrace();
//				}
//			}
//			for (String actionName : strategyVarsByInformationSet[informationSetId].keySet()) {
//				if (sum > 0) {
//					map[informationSetId].put(actionName, cplex.getValue(strategyVarsByInformationSet[informationSetId].get(actionName)) / sum);
//				} else {
//					map[informationSetId].put(actionName, 0);
//				}
//			}
//		}
//		return map;
//	}
	
	@Override
	public TIntDoubleMap[] getInformationSetActionProbabilitiesByActionId() {
		return getInformationSetActionProbabilitiesByActionId(1);
	}

	public TIntDoubleMap[] getInformationSetActionProbabilitiesByActionId(int player) {
		int numInformationSets = game.getNumInformationSets(player);
		TIntDoubleMap[] map = new TIntDoubleHashMap[numInformationSets];
		for (int informationSetId = 0; informationSetId < numInformationSets; informationSetId++) {
			map[informationSetId] = new TIntDoubleHashMap();
			double sum = 0;
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(player, informationSetId); actionId++) {
				sum += averagedStrategy[player][informationSetId][actionId]; 
			}
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(player, informationSetId); actionId++) {
				if (sum > 0) {
					map[informationSetId].put(actionId, averagedStrategy[player][informationSetId][actionId] / sum);
				} else {
					map[informationSetId].put(actionId, 0);
				}
			}
		}
		return map;
	}
	
	private void initializeDataStructures() {
		// Initialize the tables for each player
		averagedStrategy = new double[3][][];
		currentStrategy = new double[3][][];
		regretTable = new double[3][][];
		informationSetCounter = new int[3][];
		
		// Initialize over information sets for each player
		int numInfoSetsP1 = game.getNumInformationSets(1);
		int numInfoSetsP2 = game.getNumInformationSets(2);
		averagedStrategy[player1] = new double[numInfoSetsP1][];
		averagedStrategy[player2] = new double[numInfoSetsP2][];
		currentStrategy[player1] = new double[numInfoSetsP1][];
		currentStrategy[player2] = new double[numInfoSetsP2][];
		regretTable[player1] = new double[numInfoSetsP1][];
		regretTable[player2] = new double[numInfoSetsP2][];
		informationSetCounter[player1] = new int[numInfoSetsP1];
		informationSetCounter[player2] = new int[numInfoSetsP2];
		
		// This currently assumes that information set IDs are consecutively numbered starting from 0
		// Initialize each information set for Player 1
		for (int informationSetId = 0; informationSetId < numInfoSetsP1; informationSetId++) {
			int numActions = game.getNumActionsAtInformationSet(1, informationSetId);
			averagedStrategy[1][informationSetId] = new double[numActions]; 
			Arrays.fill(averagedStrategy[1][informationSetId], 1.0 / numActions);
			currentStrategy[1][informationSetId] = new double[numActions];
			Arrays.fill(currentStrategy[1][informationSetId], 1.0 / numActions);
			regretTable[1][informationSetId] = new double[numActions]; 
		}
		// Initialize each information set for Player 2
		for (int informationSetId = 0; informationSetId < numInfoSetsP2; informationSetId++) {
			int numActions = game.getNumActionsAtInformationSet(2, informationSetId);
			averagedStrategy[2][informationSetId] = new double[numActions]; 
			Arrays.fill(averagedStrategy[2][informationSetId], 1.0 / numActions);
			currentStrategy[2][informationSetId] = new double[numActions]; 
			Arrays.fill(currentStrategy[2][informationSetId], 1.0 / numActions);
			regretTable[2][informationSetId] = new double[numActions]; 
		}
	}
	
	
	public void runCFR(int iterations) {

		int totalNodesTraversed = 0;
		
		for (int iteration = 1; iteration < iterations; iteration++) {
			totalIterationsRun++;
			if (iteration % 10 == 0) {
				//System.out.println("Starting iteartion " + iteration);
				
			}
			
			GameState gs = game.getInitialGameState();
			preSample(gs);
			traverseGameState(1, gs);
			traverseGameState(2, gs);
			
		}
	}

	/**
	 * Dispatch method for performing an iteration. 
	 * @param player
	 * @param gs
	 * @param iteration
	 * @return
	 */
	private double traverseGameState(int player, GameState gs) {
		if (gs.isLeaf()) {
			if (player == player1) {
				return gs.getValue();
			} else {
				return -gs.getValue();
			}
		}
		
		if (player == gs.getCurrentPlayer()) {
			return traverseGameStateForUpdatingPlayer(player, gs);
		} else {
			return traverseGameStateForPassivePlayer(player, gs);
		}
	}

	/**
	 * Perform iteration for updating player. This updates regrets.
	 * @param player
	 * @param gs
	 * @param iteration
	 * @return
	 */
	private double traverseGameStateForUpdatingPlayer(int player, GameState gs) {
		regretMatch(gs);
		int numActions = game.getNumActionsAtInformationSet(gs);
		double[] u = new double[numActions];
		double uSigma = 0;
		
		for (int action = 0; action < numActions; action++) {
			double probabilityOfAction = currentStrategy[player][gs.getCurrentInformationSetId()][action];
			game.updateGameStateWithAction(gs, action, probabilityOfAction);
			u[action] = traverseGameState(player, gs);
			game.removeActionFromGameState(gs, action, player);
			uSigma += probabilityOfAction * u[action];
		}
		// Perform second loop to update regret table
		for (int action = 0; action < numActions; action++) {
			regretTable[player][gs.getCurrentInformationSetId()][action] += u[action] - uSigma;
		}
		
		return uSigma;
	}

	/**
	 * Updates the current strategy for gs.getCurrentPlayer() based on the regret tables
	 * @param gs
	 */
	private void regretMatch(GameState gs) {
		int player = gs.getCurrentPlayer();
		int informationSetId = gs.getCurrentInformationSetId();
		int numActions = game.getNumActionsAtInformationSet(gs);
		
		// First, we sum up all the regrets at the information set
		double regretSum = 0;
		for (int action = 0; action < numActions; action++) {
			regretSum += Math.max(0, regretTable[player][informationSetId][action]);
		}
		
		// Second, we set the probability of each action to be regretOfAction / regretSum 
		double probabilitySum = 0;
		for (int action = 0; action < numActions; action++) {
			if (regretSum > 0) {
				currentStrategy[player][informationSetId][action] = Math.max(0, regretTable[player][informationSetId][action]) / regretSum;
			} else {
				currentStrategy[player][informationSetId][action] = 1.0 / numActions;
			}
			probabilitySum += currentStrategy[player][informationSetId][action];
		}
		assert probabilitySum > 0.99999999 && probabilitySum < 1.00000001;
	}

	/**
	 * Performs an iteration for the passive player. This method merely samples an action, updates the cumulative strategy, and recurses.
	 * @param player
	 * @param gs
	 * @param iteration
	 * @return
	 */
	private double traverseGameStateForPassivePlayer(int player, GameState gs) {
		if (gs.getCurrentPlayer() != nature) {
			//regretMatch(gs);
			for (int action = 0; action < game.getNumActionsAtInformationSet(gs); action++) {
				averagedStrategy[gs.getCurrentPlayer()][gs.getCurrentInformationSetId()][action] += currentStrategy[gs.getCurrentPlayer()][gs.getCurrentInformationSetId()][action];
			}
		}
		
		int oldPlayer = gs.getCurrentPlayer();
		int sampledAction = sampleAction(gs);
		double probabilityOfBranch = getProbabilityOfAction(gs, sampledAction);
		game.updateGameStateWithAction(gs, sampledAction, probabilityOfBranch);
		double branchValue = traverseGameState(player, gs);
		game.removeActionFromGameState(gs, sampledAction, oldPlayer);
		
		return branchValue;
	}

	private void preSample(GameState gs) {
		if (gs.isLeaf()) {
			return;
		}
		
		int oldPlayer = gs.getCurrentPlayer();
		int action = sampleAction(gs);
		double branchProbability = getProbabilityOfAction(gs, action);
		
		game.updateGameStateWithAction(gs, action, branchProbability);
		preSample(gs);
		game.removeActionFromGameState(gs, action, oldPlayer);
	}

	private double getProbabilityOfAction(GameState gs, int action) {
		if (gs.getCurrentPlayer() == nature) {
			try {
				return game.getProbabilityOfNatureAction(gs, action);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return currentStrategy[gs.getCurrentPlayer()][gs.getCurrentInformationSetId()][action];
		}
		return 0; // failure
	}

	private int sampleAction(GameState gs) {
		if (gs.priorSampleExists(gs.getCurrentPlayer(), gs.getCurrentInformationSetId())) {
			return gs.getPriorSample(gs.getCurrentPlayer(), gs.getCurrentInformationSetId());
		} else {
			double randomNumber = distribution.sample();
			int action = 0;
			double sum = 0;
			while (sum <= randomNumber) {
				if (gs.getCurrentPlayer() == nature) {
					try {
						sum += game.getProbabilityOfNatureAction(gs, action);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					sum += currentStrategy[gs.getCurrentPlayer()][gs.getCurrentInformationSetId()][action];
				}
				action++;
			}
			
			action -= 1;
			gs.addSample(gs.getCurrentPlayer(), gs.getCurrentInformationSetId(), action);
			return action;
		}
	}
}






















