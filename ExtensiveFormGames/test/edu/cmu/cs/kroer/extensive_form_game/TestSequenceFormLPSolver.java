package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.solver.SequenceFormLPSolver;

public class TestSequenceFormLPSolver {
	private static final double epsilon = 0.001;
	
	@Test
	public void testSolveKuhnP1() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/kuhn.txt");
		SequenceFormLPSolver solver = new SequenceFormLPSolver(game, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/kuhnp1.lp");
		solver.solveGame();
		assertEquals(-1.0/18.0, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveKuhnP2() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/kuhn.txt");
		SequenceFormLPSolver solver = new SequenceFormLPSolver(game, 2);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/kuhnp2.lp");
		solver.solveGame();
		assertEquals(1.0/18.0, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveLeducP1() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/leduc.txt");
		SequenceFormLPSolver solver = new SequenceFormLPSolver(game, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducp1.lp");
		solver.solveGame();
		assertEquals(-0.0856064, solver.getValueOfGame(), epsilon);
	}
	
}
