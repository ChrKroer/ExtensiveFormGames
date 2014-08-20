package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.solver.LimitedLookAheadOpponentSolver;

public class TestLimitedLookAheadOpponentSolver {

	@Test
	public void test() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,0,0,0,1,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 1, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.solveGame();
		assertEquals(3, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

}
