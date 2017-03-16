package edu.cmu.cs.kroer.extensive_form_game;


import java.util.Arrays;

import extensive_form_game.Game;
import extensive_form_game_abstraction.DieRollPokerAbstractor;
import extensive_form_game_abstraction.SignalAbstraction;
import extensive_form_game_solver.BestResponseLPSolver;
import extensive_form_game_solver.CounterFactualRegretSolver;
import extensive_form_game_solver.SequenceFormLPSolver;
import gnu.trove.map.TIntDoubleMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestCounterFactualRegretSolver {
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
	Game dieRollPoker1Private;
	Game dieRollPoker2Private;
	Game dieRollPoker3Private;
	Game dieRollPoker6Private;
	Game correlatedDieRollPoker3Private; 

	
	@BeforeEach
	public void setUp() {
		//TestConfiguration.epsilon = 0;
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

		dieRollPoker1Private = new Game();
		dieRollPoker1Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-1_private.txt");

		dieRollPoker2Private = new Game();
		dieRollPoker2Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-2_private.txt");

		
		dieRollPoker3Private = new Game();
		dieRollPoker3Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-3_private.txt");

		dieRollPoker6Private = new Game();
		dieRollPoker6Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-6_private.txt");

//		correlatedDieRollPoker3Private = new Game();
//		correlatedDieRollPoker3Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "correlated_drp_private_3sided_point01error.txt");

	}

	@Test
	public void testSolveGame() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialGetStrategyVarMap() {
		Game kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");

		// This test checks that strategy vars are initialized correctly
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(kuhnGame);
		TIntDoubleMap[] map = solver.getInformationSetActionProbabilitiesByActionId();
		
		for (int informationSetId = 0; informationSetId < kuhnGame.getNumInformationSets(1); informationSetId++) {
			int numActions = kuhnGame.getNumActionsAtInformationSet(1, informationSetId);
			for (int actionId = 0; actionId < numActions; actionId++) {
				assertEquals(1.0 / numActions, map[informationSetId].get(actionId), TestConfiguration.epsilon);
			}
		}
	}

		public void testGameConvergence(Game game, double gameValue, int iterations) {
			testGameConvergence(game, gameValue, iterations, TestConfiguration.epsilon);
		}
	
	public void testGameConvergence(Game game, double gameValue, int iterations, double epsilon) {
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(game);
		//solver.solveGame(10);
		
		solver.runCFR(iterations);

		double[][][] strategyProfile = solver.getStrategyProfile();
		
		BestResponseLPSolver brSolver = new BestResponseLPSolver(game, 1, strategyProfile[2]);
		brSolver.solveGame();
		assertEquals(gameValue, brSolver.getValueOfGame(), epsilon);
		
		brSolver = new BestResponseLPSolver(game, 2, strategyProfile[1]);
		brSolver.solveGame();
		assertEquals(gameValue, -brSolver.getValueOfGame(), epsilon);

		
		assertEquals(gameValue, game.computeGameValueForStrategies(strategyProfile), epsilon);
	}

	
	
	public void testGameConvergenceWithExploitabilityPrints(Game game, double gameValue, int iterations, int printInterval) {
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(game);
		//solver.solveGame(10);
		
		for  (int numIterations = 0; numIterations < iterations; numIterations += printInterval) {
			solver.runCFR(printInterval);

			double[][][] strategyProfile = solver.getStrategyProfile();

			System.out.println("\n Iterations: " + numIterations);
			BestResponseLPSolver brSolver = new BestResponseLPSolver(game, 2, strategyProfile[1]);
			brSolver.solveGame();
			double exploitabilityPlayer1 = brSolver.getValueOfGame() + gameValue;
			System.out.println("Exploitability player1: " + exploitabilityPlayer1);
			
			
			brSolver = new BestResponseLPSolver(game, 1, strategyProfile[2]);
			brSolver.solveGame();
			double exploitabilityPlayer2 = brSolver.getValueOfGame() - gameValue;
			System.out.println("Exploitability player2: " + exploitabilityPlayer2);
		}
		
	}


	public void printCoinConvergence() {
		testGameConvergenceWithExploitabilityPrints(coinGame, TestConfiguration.coinValueOfGame, 100000, 10000);
	}
	

	public void printLeducKj1RaiseConvergence() {
		testGameConvergenceWithExploitabilityPrints(leducKj1RaiseGame, TestConfiguration.leducKj1RaiseValueOfGame, 1000000, 100000);
	}
	
	@Test
	public void testCoinConvergence() {
		testGameConvergence(coinGame, TestConfiguration.coinValueOfGame, 10000000);
	}

	@Test
	public void testPrslConvergence() {
		testGameConvergence(prslGame, TestConfiguration.prslValueOfGame, 1000000);
	}


	@Test
	public void testMiniKuhnConvergence() {
		testGameConvergence(miniKuhnGame, TestConfiguration.miniKuhnValueOfGame, 1000000);
	}
	
	
	@Test
	public void testKuhnConvergence() {
		testGameConvergence(kuhnGame, TestConfiguration.kuhnValueOfGame, 1000000);
	}

	@Test
	public void testStengelConvergence() {
		testGameConvergence(stengelGame, TestConfiguration.stengelValueOfGame, 10000000);
	}

	@Test
	public void testStengelStrategyConvergence() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "stengel.txt");
		
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(game);
		//solver.solveGame(10);
		
		solver.runCFR(1000000);
		System.out.print(Arrays.toString(solver.getInformationSetActionProbabilitiesByActionId(1)) + " + ");
		System.out.println(Arrays.toString(solver.getInformationSetActionProbabilitiesByActionId(2)));

		
		double[][][] strategyProfile = solver.getStrategyProfile();
		assertEquals(0.5, strategyProfile[2][0][0], TestConfiguration.epsilon);
		assertEquals(0.5, strategyProfile[2][0][1], TestConfiguration.epsilon);
		
		assertEquals(TestConfiguration.stengelValueOfGame, stengelGame.computeGameValueForStrategies(strategyProfile),TestConfiguration.epsilon);
	}

	@Test
	public void testStengelConvergenceWithAbstraction() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "stengel.txt");
		
		// Make information set abstraction
		int[][] abstraction = new int[3][];
		abstraction[1] = new int[] {0, 0};
		abstraction[2] = new int[] {0};
		// Make action mapping for above abstraction
		int[][][] actionMapping = new int[3][][];
		actionMapping[1] = new int[game.getNumInformationSets(1)][];
		actionMapping[2] = new int[game.getNumInformationSets(2)][];
		
		actionMapping[1][0] = new int[] {0, 1};
		actionMapping[1][1] = new int[] {1, 0};
		actionMapping[2][0] = new int[] {0, 1};
		
		game.addInformationSetAbstraction(abstraction, actionMapping);
		
		
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(game);
		//solver.solveGame(10);
		
		solver.runCFR(1000000);
		
		double[][][] strategyProfile = solver.getStrategyProfile();
		assertEquals(1, strategyProfile[2][0][0], TestConfiguration.epsilon);
		assertEquals(0, strategyProfile[2][0][1], TestConfiguration.epsilon);

		assertEquals(1, strategyProfile[1][0][0], TestConfiguration.epsilon);
		assertEquals(0, strategyProfile[1][0][1], TestConfiguration.epsilon);
	}

	
	@Test
	public void testLeducKj1RaiseConvergence() {
		testGameConvergence(leducKj1RaiseGame, TestConfiguration.leducKj1RaiseValueOfGame, 100000);
	}

	
	@Test
	public void testDRP3Convergence() {
		testGameConvergence(dieRollPoker3, TestConfiguration.drp3ValueOfGame, 10000);
	}

	@Test
	public void testDRP3WithSignalAbstractionConvergence() {
		testGameConvergence(dieRollPoker3, TestConfiguration.drp3ValueOfGame, 100000);
	}
	
	
	@Test
	public void testDRP6Convergence() {
		testGameConvergence(dieRollPoker6, TestConfiguration.drp6ValueOfGame, 10000);
	}

	@Test
	public void testDRP2PrivateConvergence() {
		testGameConvergence(dieRollPoker2Private, TestConfiguration.drp2PrivateValueOfGame, 10000);
	}

	@Test
	public void testDRP3PrivateConvergence() {
		testGameConvergence(dieRollPoker3Private, TestConfiguration.drp3PrivateValueOfGame, 10000);
	}


	public void testSolveDieRollPokerPrivateLosslessAbstraction(Game game, int numSides, int iterations, double valueOfGame, double convergenceBound) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, 2*numSides - 1);
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		assertEquals(0, value, TestConfiguration.epsilon);
		
		SignalAbstraction abstraction = abstractor.getAbstraction();
		game.applySignalAbstraction(abstraction);

		testGameConvergence(game, valueOfGame, iterations, convergenceBound);	
	}	
	
	@Test
	public void testSolveDieRollPoker3PrivateLosslessAbstraction() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-3_private.txt");
		
		testSolveDieRollPokerPrivateLosslessAbstraction(dieRollPoker3Private, 3, 10000, TestConfiguration.drp3PrivateValueOfGame, 0.01);
	}

	@Test
	public void testSolveDieRollPoker1PrivateLosslessAbstraction() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-1_private.txt");
		
		testSolveDieRollPokerPrivateLosslessAbstraction(game, 1, 10000, TestConfiguration.drp1PrivateValueOfGame, 0.001);
	}
	
	@Test
	public void testSolveDieRollPoker2PrivateLosslesstAbstraction() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-2_private.txt");
		
		testSolveDieRollPokerPrivateLosslessAbstraction(game, 2, 100000, TestConfiguration.drp2PrivateValueOfGame, 0.001);
	}



	@Test
	public void testSolveCorrelatedDieRollPokerPrivate2sides() {
		Game drpGame = new Game();
		drpGame.createGameFromFileZerosumPackageFormat(TestConfiguration.correlatedDrpGamesFolder + "correlated_drp_2sided_point01error.txt");

		testSolveCorrelatedDieRollPokerPrivate(drpGame, 2, 0.1, 100, 2000);
	}

	@Test
	public void testSolveCorrelatedDieRollPokerPrivate3sides01error() {
		Game drpGame = new Game();
		drpGame.createGameFromFileZerosumPackageFormat(TestConfiguration.correlatedDrpGamesFolder + "correlated_drp_3sided_point01error.txt");

		testSolveCorrelatedDieRollPokerPrivate(drpGame, 3, 0.1, 100, 2000);
	}
	
	@Test
	public void testSolveCorrelatedDieRollPokerPrivate3sides001error() {
		Game drpGame = new Game();
		drpGame.createGameFromFileZerosumPackageFormat(TestConfiguration.correlatedDrpGamesFolder + "correlated_drp_3sided_point01error.txt");

		testSolveCorrelatedDieRollPokerPrivate(drpGame, 3, 0.01, 100, 2000);
	}
	
	public void testSolveCorrelatedDieRollPokerPrivate(Game drpGame, int numSides, double error, int numRounds, int numCFRIterationsPerRound) {
		SignalAbstraction abstraction = getCorrelatedDRPAbstraction(drpGame, numSides, error);
		drpGame.applySignalAbstraction(abstraction);
		
		CounterFactualRegretSolver cfrSolver = new CounterFactualRegretSolver(drpGame);
		
		// TODO: LP solver strategy for debugging purposes
//		SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 1);
//		equilibriumSolver.solveGame();
//		double[][] p1Strategy = equilibriumSolver.getStrategyProfile()[1];

		
		for (int i = 0; i < numRounds; i++) {
			cfrSolver.runCFR(numCFRIterationsPerRound);
			double[][][] strategyProfile = cfrSolver.getStrategyProfile();

			BestResponseLPSolver brSolverP1 = new BestResponseLPSolver(drpGame, 1, strategyProfile[2]);
			brSolverP1.solveGame();
			
			BestResponseLPSolver brSolverP2 = new BestResponseLPSolver(drpGame, 2, strategyProfile[1]);
			brSolverP2.solveGame();
			
			
			System.out.printf("%d\t%d\t%.3f\t%.3f\t%.3f\n", cfrSolver.getNumNodesTouched(), numCFRIterationsPerRound * (i+1), cfrSolver.getValueOfGame(), brSolverP1.getValueOfGame(), brSolverP2.getValueOfGame());
		}
	}
	
	private SignalAbstraction getCorrelatedDRPAbstraction(Game game, int numSides, double rollDistanceError) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, 2*numSides - 1, rollDistanceError); // lossless size if there were no nature error
		abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		//System.out.println("Abstraction value: " + value);
		return abstractor.getAbstraction();
	}

	public void printGameConvergence(Game game, double gameValue, int iterations, double epsilon) {
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(game);
		//solver.solveGame(10);
		
		solver.runCFR(iterations);

		double[][][] strategyProfile = solver.getStrategyProfile();
		
		BestResponseLPSolver brSolver = new BestResponseLPSolver(game, 1, strategyProfile[2]);
		brSolver.solveGame();
		assertEquals(gameValue, brSolver.getValueOfGame(), epsilon);
		
		brSolver = new BestResponseLPSolver(game, 2, strategyProfile[1]);
		brSolver.solveGame();
		assertEquals(gameValue, -brSolver.getValueOfGame(), epsilon);

		
		assertEquals(gameValue, game.computeGameValueForStrategies(strategyProfile), epsilon);
	}
	
}













