package edu.cmu.cs.kroer.extensive_form_game.solver;

import edu.cmu.cs.kroer.extensive_form_game.GameGenerator;
import edu.cmu.cs.kroer.extensive_form_game.GameState;

public class BestResponseTreeTraversalSolver extends ZeroSumGameSolver {
	
	private int nature = 0;
	private int player1 = 1;
	private int player2 = 2;
	
	private double[][] opponentStrategy;
	private int[] bestResponseActionIndices;
	
	private double[][] actionExpectedValues;
	
	private int playerToSolverFor;
	
	public BestResponseTreeTraversalSolver(GameGenerator game, int playerToSolveFor, double[][] opponentStrategy) {
		super(game);
		this.opponentStrategy = opponentStrategy;
		this.playerToSolverFor = playerToSolveFor;
		initializeDataStructures();
	}

	private void initializeDataStructures() {
		bestResponseActionIndices = new int[game.getNumInformationSets(playerToSolverFor)];
		actionExpectedValues = new double[game.getNumInformationSets(playerToSolverFor)][];
		for (int informationSetId = 0; informationSetId < game.getNumInformationSets(playerToSolverFor); informationSetId++) {
			actionExpectedValues[informationSetId] = new double[game.getNumActionsAtInformationSet(playerToSolverFor, informationSetId)];
		}
	}


	@Override
	public void solveGame() {
		// TODO Auto-generated method stub

	}

	private void computeNodeProbabilities(GameState gs, double probabilityOverOtherAgents) {
//		
//		int numActions =  game.getNumActions(gs);
//		for (int actionId = 0; actionId < numActions; actionId++) {
//			double probability = 1;
//			if (gs.getCurrentPlayer() == nature) {
//				probability = game.getProbabilityOfNatureAction(gs, actionId);
//			} else if (playerToSolverFor != gs.getCurrentPlayer()){
//				probability = opponentStrategy[gs.getCurrentInformationSetId()][actionId];
//			}
//			try {
//				double 
//				game.updateGameStateWithAction(gs, actionId, probability);
//				nodeValue += probability * computeExpectedValueOfActions(gs);
//				game.removeActionFromGameState(gs, actionId, nature);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}			
//
//
//		for (int actionId = 0; actionId < numActions; actionId++) {
//			double probability = 0; 
//			game.updateGameStateWithAction(gs, actionId, probability);
//			nodeValue += probability * computeExpectedValueOfActions(gs);
//			game.removeActionFromGameState(gs, actionId, nature);
//		}
//
		
	}
	
	private double computeExpectedValueOfActions(GameState gs) {
		double nodeValue = 0;
		if (gs.getCurrentPlayer() == nature) {
			int numActions = game.getNumActionsForNature(gs);
			for (int actionId = 0; actionId < numActions; actionId++) {
				try {
					double probability = game.getProbabilityOfNatureAction(gs, actionId);
					game.updateGameStateWithAction(gs, actionId, probability);
					nodeValue += probability * computeExpectedValueOfActions(gs);
					game.removeActionFromGameState(gs, actionId, nature);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}			
		} else {
			int numActions = game.getNumActionsAtInformationSet(gs);
//			for (int actionId = 0; actionId < numActions; actionId++) {
//				double probability = 
//				game.updateGameStateWithAction(gs, actionId, probability);
//				nodeValue += probability * computeExpectedValueOfActions(gs);
//				game.removeActionFromGameState(gs, actionId, nature);
//			}
		}
		
		return nodeValue;
	}
	
	@Override
	public void printStrategyVarsAndGameValue() {
		// TODO Auto-generated method stub

	}

	@Override
	public void printGameValue() {
		// TODO Auto-generated method stub

	}

	@Override
	public double[][][] getStrategyProfile() {
		double[][] bestResponseStrategy = new double[game.getNumInformationSets(playerToSolverFor)][];
		for (int informationSetId = 0; informationSetId < game.getNumInformationSets(playerToSolverFor); informationSetId++) {
			for (int actionId = 0; actionId < game.getNumActionsAtInformationSet(playerToSolverFor, informationSetId); actionId++) {
				bestResponseStrategy[informationSetId][actionId] = bestResponseActionIndices[informationSetId] == actionId ? 1 : 0;
			}			
		}
		
		if (playerToSolverFor == player1) {
			return new double[][][] { new double[0][], bestResponseStrategy, opponentStrategy };  
		} else {
			return new double[][][] { new double[0][], opponentStrategy, bestResponseStrategy };  
		}

		
	}

}
