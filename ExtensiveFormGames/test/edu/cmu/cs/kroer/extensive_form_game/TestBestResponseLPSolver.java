package edu.cmu.cs.kroer.extensive_form_game;


import extensive_form_game.Game;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.SequenceFormLPSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestBestResponseLPSolver {
	Game miniKuhnGame;
	Game kuhnGame;
	Game coinGame;
	Game prslGame;
	Game leducKJGame;
	Game leducKj1RaiseGame;
	Game leducGame;
	Game leducUnabstractedGame;
	Game stengelGame;
	Game dieRollPoker3;
	Game dieRollPoker6;
	
	@BeforeEach
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

		stengelGame = new Game();
		stengelGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "stengel.txt");

		dieRollPoker3 = new Game();
		dieRollPoker3.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-3.txt");
		
		dieRollPoker6 = new Game();
		dieRollPoker6.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-6.txt");
	}

	@Test
	public void testStengelGame() {
		// Inoptimal strategy for player2
		double[][] p2Strategy = new double[][] { new double[] {1, 0} };
		
		BestResponseLPSolver brSolver = new BestResponseLPSolver(stengelGame, 1, p2Strategy);
		brSolver.solveGame();
		double[][][] strategyProfile = brSolver.getStrategyProfile();
		
		assertEquals(0, strategyProfile[1][0][0], TestConfiguration.epsilon);
		assertEquals(1, strategyProfile[1][0][1], TestConfiguration.epsilon);

		assertEquals(0, strategyProfile[1][1][0], TestConfiguration.epsilon);
		assertEquals(1, strategyProfile[1][1][1], TestConfiguration.epsilon);
		
		// Try a different inoptimal strategy for player2
		p2Strategy = new double[][] { new double[] {0, 1} };
		brSolver = new BestResponseLPSolver(stengelGame, 1, p2Strategy);
		brSolver.solveGame();
		strategyProfile = brSolver.getStrategyProfile();
		
		assertEquals(0, strategyProfile[1][0][1], TestConfiguration.epsilon);
		assertEquals(1, strategyProfile[1][0][0], TestConfiguration.epsilon);

		assertEquals(0, strategyProfile[1][1][1], TestConfiguration.epsilon);
		assertEquals(1, strategyProfile[1][1][0], TestConfiguration.epsilon);

	}

	@Test
	public void testMiniKuhnEquilibrium() {
		SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(miniKuhnGame, 1);
		equilibriumSolver.solveGame();

		double[][] p1Strategy = equilibriumSolver.getStrategyProfile()[1];
		
		BestResponseLPSolver brSolver = new BestResponseLPSolver(miniKuhnGame, 2, p1Strategy);
		brSolver.solveGame();
		
		double[][][] strategyProfile = brSolver.getStrategyProfile();
		
		assertEquals(equilibriumSolver.getValueOfGame(), -brSolver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testDRP3Equilibrium() {
		SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(dieRollPoker3, 1);
		equilibriumSolver.solveGame();

		double[][] p1Strategy = equilibriumSolver.getStrategyProfile()[1];
		
		BestResponseLPSolver brSolver = new BestResponseLPSolver(dieRollPoker3, 2, p1Strategy);
		brSolver.solveGame();
		
		double[][][] strategyProfile = brSolver.getStrategyProfile();
		
		assertEquals(equilibriumSolver.getValueOfGame(), -brSolver.getValueOfGame(), TestConfiguration.epsilon);
	}

}
