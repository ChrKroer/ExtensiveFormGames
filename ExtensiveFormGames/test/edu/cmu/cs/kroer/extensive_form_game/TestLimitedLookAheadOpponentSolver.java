package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import java.util.Arrays;

import ilog.concert.IloException;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Test;

import extensive_form_game.Game;
import extensive_form_game_solver.LimitedLookAheadOpponentSolver;
import extensive_form_game_solver.SequenceFormLPSolver;
import gnu.trove.map.TObjectDoubleMap;

public class TestLimitedLookAheadOpponentSolver {

	@Test
	public void testMiniKuhnP1Eval1() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,0,0,0,1,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 1, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp1-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(1, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testMiniKuhnP1Eval2() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,0,0,0,2,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 1, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp1-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(1, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testMiniKuhnP1Eval3() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {1,1,1,1,1,1,1,1,1,1,1};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp1-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(-0.5, solver.getValueOfGame(), TestConfiguration.epsilon);
	}


	
	@Test
	public void testMiniKuhnP2() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,0,0,0,1,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp2-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(-0.5, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testMiniKuhnP2FoldKing() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,1,0,0,0,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp2-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(1, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testMiniKuhnP2FoldKingBetQueen() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,1,0,0,1,0,0,0,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp2-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(2, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testMiniKuhnP2La2FoldKingLikesQueenShowdown() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,1,1,0,0,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp2-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(2, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testMiniKuhnP2La2FoldKingLikesRaiseFoldQueen() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		
		double[] nodeEvaluationTable = {0,0,0,0,0,0,1,1,0,0,0};

		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "minikuhnp2-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
			assertTrue(false);
		}

		assertEquals(2, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testEquilibriumEvalutationMiniKuhn() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(miniKuhnGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(miniKuhnGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = miniKuhnGame.getExpectedValuesForNodes(strategyP1, strategyP2, true);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 1, nodeEvaluationTable, 100);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-minikuhn-limited-look-ahead.lp");
		solver.solveGame();
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testEquilibriumEvalutationMiniKuhnP2() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(miniKuhnGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(miniKuhnGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = miniKuhnGame.getExpectedValuesForNodes(strategyP1, strategyP2, false);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 2, nodeEvaluationTable, 100);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibriump2-minikuhn-limited-look-ahead.lp");
		solver.solveGame();
		solver.printGameValue();
		try {
			solver.printSequenceActivationValues();
		} catch (IloException e) {
			e.printStackTrace();
		}
		assertEquals(solverP2.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	
	@Test
	public void testEquilibriumEvalutationKuhn() {
		Game kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(kuhnGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(kuhnGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = kuhnGame.getExpectedValuesForNodes(strategyP1, strategyP2, true);
		System.out.println(Arrays.toString(nodeEvaluationTable));
		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(kuhnGame, 1, nodeEvaluationTable, 1);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-kuhn-limited-look-ahead.lp");
		solver.solveGame();
		System.out.println(kuhnGame);
		solver.printStrategyVarsAndGameValue();
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}	

	@Test
	public void testEquilibriumEvalutationKuhnP2() {
		Game kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(kuhnGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(kuhnGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = kuhnGame.getExpectedValuesForNodes(strategyP1, strategyP2, false);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(kuhnGame, 2, nodeEvaluationTable, 1);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-kuhnp2-limited-look-ahead.lp");
		solver.solveGame();
		assertEquals(solverP2.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}	
	

	@Test
	public void testEquilibriumEvalutationPrsl() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "prsl.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(game, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(game, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = game.getExpectedValuesForNodes(strategyP1, strategyP2, true);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(game, 1, nodeEvaluationTable, 100);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-prsl-limited-look-ahead.lp");
		solver.solveGame();
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}	

	@Test
	public void testEquilibriumEvalutationCoin() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "coin.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(game, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(game, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = game.getExpectedValuesForNodes(strategyP1, strategyP2, true);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(game, 1, nodeEvaluationTable, 10);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-coin-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
			solver.printGameValue();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}	

	@Test
	public void testEquilibriumEvalutationCoinP2() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "coin.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(game, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(game, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = game.getExpectedValuesForNodes(strategyP1, strategyP2, false);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(game, 2, nodeEvaluationTable, 100);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibriump2-coin-limited-look-ahead.lp");
		solver.solveGame();
		try {
			solver.printSequenceActivationValues();
			solver.printGameValue();
		} catch (IloException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(solverP2.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}	
	
	
	@Test
	public void testEquilibriumEvalutationKuhnNoise() {
		Game kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(kuhnGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(kuhnGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		// get negated expected values
		double[] nodeEvaluationTable = kuhnGame.getExpectedValuesForNodes(strategyP1, strategyP2, true);
		for (double noise = 0.1; noise < 1; noise += 0.1) {
		// Add Gaussian noise to evaluations
		NormalDistribution distribution = new NormalDistribution(0, noise);
		for (int iteration = 0; iteration < 100; iteration++) {
			for (int i = 0; i < nodeEvaluationTable.length; i++) {
				nodeEvaluationTable[i] += distribution.sample();
			}

			// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
			LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(kuhnGame, 1, nodeEvaluationTable, 1);
			//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
			solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-kuhn-limited-look-ahead.lp");
			solver.solveGame();
			assertTrue(solverP1.getValueOfGame() <= solver.getValueOfGame());
		}}
	}	
	
	@Test
	public void testEquilibriumEvaluationLeducKJLa100() {
		Game leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc_KJ.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(leducGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(leducGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		double[] nodeEvaluationTable = leducGame.getExpectedValuesForNodes(strategyP1, strategyP2);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(leducGame, 1, nodeEvaluationTable, 100);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-leducKJp1la1-limited-look-ahead.lp");
		solver.solveGame();
		System.out.println("Value of game: " + solver.getValueOfGame());
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testEquilibriumEvaluationLeducKJ1RaiseLa100() {
		Game leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc_Kj1Raise.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(leducGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(leducGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		


		MersenneTwister twister = new MersenneTwister();
		NormalDistribution distribution = new NormalDistribution(twister, 0, (double) 0.1);
		double[] nodeEvaluationTable = leducGame.getExpectedValuesForNodes(strategyP1, strategyP2, true, distribution);
	
		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(leducGame, 1, nodeEvaluationTable, 100);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-leducKj1Raisep1la1-limited-look-ahead.lp");
		solver.solveGame();
		System.out.println("Value of game: " + solver.getValueOfGame());
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	public void testEquilibriumEvaluationLeducLa1() {
		Game leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(leducGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(leducGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		double[] nodeEvaluationTable = leducGame.getExpectedValuesForNodes(strategyP1, strategyP2);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(leducGame, 1, nodeEvaluationTable, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-leducp1la2-limited-look-ahead.lp");
		solver.solveGame();
		System.out.println("Value of game: " + solver.getValueOfGame());
		assertTrue(solverP1.getValueOfGame() <= solver.getValueOfGame());
	}

	
	public void testEquilibriumEvaluationLeducLa2() {
		Game leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(leducGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(leducGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		double[] nodeEvaluationTable = leducGame.getExpectedValuesForNodes(strategyP1, strategyP2);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(leducGame, 1, nodeEvaluationTable, 3);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "equilibrium-leducp1la2-limited-look-ahead.lp");
		solver.solveGame();
		System.out.println("Value of zero-sum game: " + solver.getValueOfGame());
		System.out.println("Value of limited look-ahead game: " + solver.getValueOfGame());
		assertTrue(solverP1.getValueOfGame() <= solver.getValueOfGame());
	}
	
}
