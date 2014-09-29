package edu.cmu.cs.kroer.extensive_form_game.solver;

import java.util.Arrays;

import org.apache.commons.math3.distribution.UniformRealDistribution;

import edu.cmu.cs.kroer.extensive_form_game.Game;
import edu.cmu.cs.kroer.extensive_form_game.GameGenerator;
import edu.cmu.cs.kroer.extensive_form_game.GameState;
import edu.cmu.cs.kroer.extensive_form_game.TestConfiguration;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

public class CounterFactualRegretSolver extends ZeroSumGameSolver {
	int nature = 0;
	int player1 = 1;
	int player2 = 2;
	
	int totalIterationsRun = 0;
	
	double[][][] averagedStrategy;
	double[][][] currentStrategy;
	double[][][] regretTable;
	double[][] informationSetProbabilityForPlayer;
	final UniformRealDistribution distribution = new UniformRealDistribution(0, 1);

	public CounterFactualRegretSolver (GameGenerator game) {
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
			int abstractInformationSetId = game.getAbstractInformationSetId(player, informationSetId);
			
			map[informationSetId] = new TIntDoubleHashMap();
			double sum = 0;
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(player, informationSetId); actionId++) {
				int abstractActionId = game.getAbstractActionMapping(player, informationSetId, actionId);
				sum += averagedStrategy[player][abstractInformationSetId][abstractActionId]; 
			}
			
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(player, informationSetId); actionId++) {
				int abstractActionId = game.getAbstractActionMapping(player, informationSetId, actionId);
				if (sum > 0) {
					map[informationSetId].put(actionId, averagedStrategy[player][game.getAbstractInformationSetId(player, informationSetId)][abstractActionId] / sum);
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
		informationSetProbabilityForPlayer = new double[3][];

		
		// Initialize over information sets for each player
		int numInfoSetsP1 = game.getNumInformationSets(1);
		int numInfoSetsP2 = game.getNumInformationSets(2);
		averagedStrategy[player1] = new double[numInfoSetsP1][];
		averagedStrategy[player2] = new double[numInfoSetsP2][];
		currentStrategy[player1] = new double[numInfoSetsP1][];
		currentStrategy[player2] = new double[numInfoSetsP2][];
		regretTable[player1] = new double[numInfoSetsP1][];
		regretTable[player2] = new double[numInfoSetsP2][];
		informationSetProbabilityForPlayer[player1] = new double[numInfoSetsP1];
		informationSetProbabilityForPlayer[player2] = new double[numInfoSetsP2];
		
		// This currently assumes that information set IDs are consecutively numbered starting from 0
		// Initialize each information set for Player 1
		for (int informationSetId = 0; informationSetId < numInfoSetsP1; informationSetId++) {
			if (game.informationSetAbstracted(player1, informationSetId)) {
				continue;
			}
			int numActions = game.getNumActionsAtInformationSet(1, informationSetId);
			averagedStrategy[1][informationSetId] = new double[numActions]; 
			//Arrays.fill(averagedStrategy[1][informationSetId], 1.0 / numActions);
			currentStrategy[1][informationSetId] = new double[numActions];
			Arrays.fill(currentStrategy[1][informationSetId], 1.0 / numActions);
			regretTable[1][informationSetId] = new double[numActions]; 
		}
		// Initialize each information set for Player 2
		for (int informationSetId = 0; informationSetId < numInfoSetsP2; informationSetId++) {
			if (game.informationSetAbstracted(player2, informationSetId)) {
				continue;
			}
			int numActions = game.getNumActionsAtInformationSet(2, informationSetId);
			averagedStrategy[2][informationSetId] = new double[numActions]; 
			//Arrays.fill(averagedStrategy[2][informationSetId], 1.0 / numActions);
			currentStrategy[2][informationSetId] = new double[numActions]; 
			Arrays.fill(currentStrategy[2][informationSetId], 1.0 / numActions);
			regretTable[2][informationSetId] = new double[numActions]; 
		}
	}
	
	
	public void runCFR(int iterations) {
		totalIterationsRun += iterations;
		// TODO update existing averagedStrategy
		for (int iteration = 1; iteration < iterations; iteration++) {
			GameState gs = game.getInitialGameState();
			traverseGameState(gs);
			regretMatch();
		}
	}

	/**
	 * Dispatch method for performing an iteration. 
	 * @param player
	 * @param gs
	 * @param iteration
	 * @return
	 */
	private double traverseGameState(GameState gs) {
		if (gs.isLeaf()) {
			return gs.getValue();
		} else if (gs.getCurrentPlayer() == nature){
			double value = 0;
			for (int action = 0; action < game.getNumActionsForNature(gs); action++) {
				double probabilityOfAction = getProbabilityOfAction(gs, action);
				game.updateGameStateWithAction(gs, action, probabilityOfAction);
				value = probabilityOfAction * traverseGameState(gs);
				game.removeActionFromGameState(gs, action, nature);
			}
			return value;
		} else {
			return traversePlayerGameState(gs);
		}
	}

	/**
	 * Perform iteration for updating player. This updates regrets.
	 * @param player
	 * @param gs
	 * @param iteration
	 * @return
	 */
	private double traversePlayerGameState(GameState gs) {
		int numActions = game.getNumActionsAtInformationSet(gs);
		int currentPlayer = gs.getCurrentPlayer();
		
		double sumOfUtilities = 0;
		double[] actionUtilities = new double[numActions];
		
		for (int originalAction = 0; originalAction < numActions; originalAction++) {
			int abstractAction = game.getAbstractActionMapping(gs, originalAction);
			// use the abstract action probability
			double probabilityOfAction = getProbabilityOfAction(gs, abstractAction);
			// take original action in game tree
			game.updateGameStateWithAction(gs, originalAction, probabilityOfAction);
			// treat as abstract action when calculating regrets
			actionUtilities[abstractAction] = traverseGameState(gs);
			// remove original action from game tree
			game.removeActionFromGameState(gs, originalAction, currentPlayer);
			// treat as abstract action when calculating regrets
			sumOfUtilities += probabilityOfAction * actionUtilities[abstractAction];
		}
		// Perform second loop to update regret table
		if (gs.getCurrentPlayer() != nature) {
			int utilityMultiplier = currentPlayer == player1 ? 1 : -1;
			for (int originalAction = 0; originalAction < numActions; originalAction++) {
				// treat as abstract action when calculating regrets
				int action = game.getAbstractActionMapping(gs, originalAction);
				regretTable[currentPlayer][gs.getCurrentInformationSetId()][action] += utilityMultiplier * gs.getProbabilityWithoutPlayer(currentPlayer) * (actionUtilities[action] - sumOfUtilities);
				
				informationSetProbabilityForPlayer[currentPlayer][gs.getCurrentInformationSetId()] = gs.getProbabilityWithPlayer(currentPlayer);
			}
		}

		return sumOfUtilities;
	}

	/**
	 * Updates the current strategy for gs.getCurrentPlayer() based on the regret tables
	 * @param gs
	 */
	private void regretMatch() {
		int t = 1;
		for (int player = 1; player < 3; player++) {
			for (int informationSetId = 0; informationSetId < game.getNumInformationSets(player); informationSetId++) {
				// We only need to update regret for abstracted information sets
				if (game.informationSetAbstracted(player, informationSetId)) {
					continue;
				}
				
				double regretSum = 0;
				int numActions = game.getNumActionsAtInformationSet(player, informationSetId);
				
				for (int action = 0; action < numActions; action++) {
					regretSum += Math.max(0, regretTable[player][informationSetId][action]);
					averagedStrategy[player][informationSetId][action] += informationSetProbabilityForPlayer[player][informationSetId] * currentStrategy[player][informationSetId][action];
				}
				
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
		}
	}

	/**
	 * Uses gs.getCurrentInformationSetId() to look up the information set id, which will pull the abstracted information set if an abstraction is used.
	 * However, action is assumed to be the correct one, so no abstraction map lookup is done. Thus, calling methods should performing mapping if needed.
	 * Consider changing this in the future.
	 * @param gs
	 * @param action The action for which the probability is desired, explicitly looked up, with no abstraction mapping performed.
	 * @return
	 */
	// 
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

}






















