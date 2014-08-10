package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.*;

import edu.cmu.cs.kroer.extensive_form_game.solver.SequenceFormLPSolver;

public class TestSequenceFormLPSolver {
	private static final double epsilon = 0.001;
	private static final String gamesFolder = "/Users/ckroer/Documents/research/zerosum/games/";
	private static final String zerosumGamesFolder = "/Users/ckroer/Documents/research/zerosum/original_games/";

	Game miniKuhnGame;
	Game kuhnGame;
	Game coinGame;
	Game prslGame;
	Game leducKJGame;
	Game leducKj1RaiseGame;
	Game leducGame;
	Game leducUnabstractedGame;
	
	@Before
	public void setUp() {
		miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "mini_kuhn.txt");		

		kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "kuhn.txt");		

		coinGame = new Game();
		coinGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "coin.txt");		

		prslGame = new Game();
		prslGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "prsl.txt");		

		leducKJGame = new Game();
		leducKJGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "leduc_KJ.txt");

		leducKj1RaiseGame = new Game();
		leducKj1RaiseGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "leduc_Kj1Raise.txt");

		leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "leduc.txt");

		leducUnabstractedGame = new Game();
		leducUnabstractedGame.createGameFromFile(gamesFolder + "leduc.txt");
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
	public void testSolveLeducKJP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKJGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducKjP1.lp");
		solver.solveGame();
		assertEquals(3.312e-17, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveLeducKj1RaiseP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKj1RaiseGame, 1);
		solver.writeModelToFile("/Users/ckroer/Documents/research/lp-models/leducKj1RaiseP1.lp");
		solver.solveGame();
		assertEquals(-1.85e-17, solver.getValueOfGame(), epsilon);
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
