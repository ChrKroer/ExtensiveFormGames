package edu.cmu.cs.kroer.extensive_form_game.solver;

import edu.cmu.cs.kroer.extensive_form_game.GameGenerator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.TObjectDoubleMap;


public abstract class ZeroSumGameSolver {
	
	int nature = 0;
	int player1 = 1;
	int player2 = 2;
	
	double valueOfGame;
	double [] strategyVars;
	GameGenerator game;
	
	public ZeroSumGameSolver(GameGenerator game) {
		this.game = game;
	}
	
	public abstract void solveGame();
	
	public abstract void printStrategyVarsAndGameValue();
	public abstract void printGameValue();



	public double getValueOfGame() {
		return valueOfGame;
	}


	public double[] getStrategyVars() {
		return strategyVars;
	}

	//public abstract TObjectDoubleMap<String>[] getInformationSetActionProbabilities();
	public abstract double[][][] getStrategyProfile();
}
