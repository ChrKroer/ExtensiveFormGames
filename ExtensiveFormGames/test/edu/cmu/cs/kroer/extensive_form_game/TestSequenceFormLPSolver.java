package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.*;

import edu.cmu.cs.kroer.extensive_form_game.solver.SequenceFormLPSolver;

public class TestSequenceFormLPSolver {
	private static final double epsilon = 0.001;
	Game miniKuhnGame;
	Game kuhnGame;
	Game coinGame;
	Game prslGame;
	Game leducGame;
	Game leducUnabstractedGame;
	
	@Before
	public void setUp() {
		miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/mini_kuhn.txt");		

		kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/kuhn.txt");		

		coinGame = new Game();
		coinGame.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/coin.txt");		

		prslGame = new Game();
		prslGame.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/prsl.txt");		

		leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/leduc.txt");

		leducUnabstractedGame = new Game();
		leducUnabstractedGame.createGameFromFile("/Users/ckroer/Documents/research/zerosum/games/leduc.txt");
	}

	@Test
	public void testSolveMiniKuhnP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(miniKuhnGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/kuhnp1.lp");
		solver.solveGame();
		assertEquals(0.5, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveMiniKuhnP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(miniKuhnGame, 2);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/kuhnp2.lp");
		solver.solveGame();
		assertEquals(-0.5, solver.getValueOfGame(), epsilon);
	}

	
	@Test
	public void testSolveKuhnP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(kuhnGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/kuhnp1.lp");
		solver.solveGame();
		assertEquals(-1.0/18.0, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveKuhnP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(kuhnGame, 2);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/kuhnp2.lp");
		solver.solveGame();
		assertEquals(1.0/18.0, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveCoinP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(coinGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/coinp1.lp");
		solver.solveGame();
		assertEquals(0.375, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveCoinP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(coinGame, 2);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/coinp2.lp");
		solver.solveGame();
		assertEquals(-0.375, solver.getValueOfGame(), 0.001);
	}

	@Test
	public void testSolvePrslP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(prslGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/prslp1.lp");
		solver.solveGame();
		assertEquals(0, solver.getValueOfGame(), 0.001);
	}

	@Test
	public void testSolvePrslP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(prslGame, 2);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/prslp1.lp");
		solver.solveGame();
		assertEquals(0, solver.getValueOfGame(), 0.001);
	}
	
	
	@Test
	public void testPrimalConstraintsLeducP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducp1.lp");
		assertEquals(144, solver.getPrimalConstraints().size());
		assertEquals(337, solver.getStrategyVarsBySequenceId().length);
	}
	
	@Test
	public void testDualConstraintsLeducP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducp1.lp");
		assertEquals(337, solver.getDualConstraints().size());
		assertEquals(145, solver.getDualVars().length);
	}

	@Test
	public void testSolveLeducP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducp1.lp");
		solver.solveGame();
		assertEquals(-0.0856064, solver.getValueOfGame(), epsilon);
	}
	
	@Test
	public void testSolveLeducUnabstractedP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducUnabstractedGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducp1.lp");
		solver.solveGame();
		assertEquals(-0.0856064, solver.getValueOfGame(), epsilon);
	}
	
}
