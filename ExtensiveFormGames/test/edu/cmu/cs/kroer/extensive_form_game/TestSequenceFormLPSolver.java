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
	Game leducKJGame;
	Game leducKj1RaiseGame;
	Game leducGame;
	Game leducUnabstractedGame;
	
	@Before
	public void setUp() {
		miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");		

		kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");		

		coinGame = new Game();
		coinGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "coin.txt");		

		prslGame = new Game();
		prslGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "prsl.txt");		

		leducKJGame = new Game();
		leducKJGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc_KJ.txt");

		leducKj1RaiseGame = new Game();
		leducKj1RaiseGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc_Kj1Raise.txt");

		leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc.txt");

		leducUnabstractedGame = new Game();
		leducUnabstractedGame.createGameFromFile(TestConfiguration.gamesFolder + "leduc.txt");
	}

	@Test
	public void testSolveMiniKuhnP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(miniKuhnGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1.lp");
		solver.solveGame();
		assertEquals(0.5, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveMiniKuhnP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(miniKuhnGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp2.lp");
		solver.solveGame();
		assertEquals(-0.5, solver.getValueOfGame(), epsilon);
	}

	
	@Test
	public void testSolveKuhnP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(kuhnGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1.lp");
		solver.solveGame();
		assertEquals(-1.0/18.0, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveKuhnP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(kuhnGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp2.lp");
		solver.solveGame();
		assertEquals(1.0/18.0, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveCoinP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(coinGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "coinp1.lp");
		solver.solveGame();
		assertEquals(0.375, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveCoinP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(coinGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "coinp2.lp");
		solver.solveGame();
		assertEquals(-0.375, solver.getValueOfGame(), 0.001);
	}

	@Test
	public void testSolvePrslP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(prslGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "prslp1.lp");
		solver.solveGame();
		assertEquals(0, solver.getValueOfGame(), 0.001);
	}

	@Test
	public void testSolvePrslP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(prslGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "prslp1.lp");
		solver.solveGame();
		assertEquals(0, solver.getValueOfGame(), 0.001);
	}
	
	@Test
	public void testSolveLeducKJP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKJGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducKjP1.lp");
		solver.solveGame();
		assertEquals(3.312e-17, solver.getValueOfGame(), epsilon);
	}

	@Test
	public void testSolveLeducKj1RaiseP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKj1RaiseGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducKj1RaiseP1.lp");
		solver.solveGame();
		assertEquals(-1.85e-17, solver.getValueOfGame(), epsilon);
	}
	
	@Test
	public void testPrimalConstraintsLeducP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		assertEquals(144, solver.getPrimalConstraints().size());
		assertEquals(337, solver.getStrategyVarsBySequenceId().length);
	}
	
	@Test
	public void testDualConstraintsLeducP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		assertEquals(337, solver.getDualConstraints().size());
		assertEquals(145, solver.getDualVars().length);
	}

	@Test
	public void testSolveLeducP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		solver.solveGame();
		assertEquals(-0.0856064, solver.getValueOfGame(), epsilon);
	}
	
	@Test
	public void testSolveLeducUnabstractedP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducUnabstractedGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		solver.solveGame();
		assertEquals(-0.0856064, solver.getValueOfGame(), epsilon);
	}
	
}
