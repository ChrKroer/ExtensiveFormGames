package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import org.junit.*;

import extensive_form_game.Game;
import extensive_form_game.Game.Action;
import extensive_form_game.Game.Node;
import extensive_form_game_solver.SequenceFormLPSolver;
import gnu.trove.map.TObjectDoubleMap;

public class TestSequenceFormLPSolver {
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
	}

	@Test
	public void testSolveMiniKuhnP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(miniKuhnGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1.lp");
		solver.solveGame();
		assertEquals(0.5, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveMiniKuhnP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(miniKuhnGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp2.lp");
		solver.solveGame();
		assertEquals(-0.5, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	
	@Test
	public void testSolveKuhnP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(kuhnGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1.lp");
		solver.solveGame();
		assertEquals(-1.0/18.0, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveKuhnP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(kuhnGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp2.lp");
		solver.solveGame();
		assertEquals(1.0/18.0, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveCoinP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(coinGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "coinp1.lp");
		solver.solveGame();
		solver.printGameValue();
		assertEquals(0.375, solver.getValueOfGame(), TestConfiguration.epsilon);
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
		assertEquals(3.312e-17, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveLeducKJP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKJGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducKjP1.lp");
		solver.solveGame();
		assertEquals(-3.312e-17, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveLeducKj1RaiseP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKj1RaiseGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducKj1RaiseP1.lp");
		solver.solveGame();
		assertEquals(-1.85e-17, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveLeducKj1RaiseP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducKj1RaiseGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducKj1RaiseP1.lp");
		solver.solveGame();
		assertEquals(1.85e-17, solver.getValueOfGame(), TestConfiguration.epsilon);
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
		assertEquals(-0.0856064, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveLeducP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		solver.solveGame();
		assertEquals(0.0856064, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testSolveLeducUnabstractedP1() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducUnabstractedGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		solver.solveGame();
		assertEquals(-0.0856064, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveLeducUnabstractedP2() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(leducUnabstractedGame, 2);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "leducp1.lp");
		solver.solveGame();
		assertEquals(0.0856064, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveStengel() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(stengelGame, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "stengelP1.lp");
		solver.solveGame();
		assertEquals(1, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testSolveDieRollPoker3() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(dieRollPoker3, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "drp3p1.lp");
		solver.solveGame();
		assertEquals(-0.158025, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testSolveDieRollPoker6() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(dieRollPoker6, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "drp6p1.lp");
		solver.solveGame();
		assertEquals(-0.0395062, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveDieRollPoker1Private() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(dieRollPoker1Private, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "drp1Privatep1.lp");
		solver.solveGame();
		assertEquals(TestConfiguration.drp2PrivateValueOfGame, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveDieRollPoker2Private() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(dieRollPoker2Private, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "drp2Privatep1.lp");
		solver.solveGame();
		assertEquals(TestConfiguration.drp2PrivateValueOfGame, solver.getValueOfGame(), TestConfiguration.epsilon);
	}

	@Test
	public void testSolveDieRollPoker3Private() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(dieRollPoker3Private, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "drp3Privatep1.lp");
		solver.solveGame();
		assertEquals(TestConfiguration.drp3PrivateValueOfGame, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testSolveDieRollPoker6Private() {
		SequenceFormLPSolver solver = new SequenceFormLPSolver(dieRollPoker6Private, 1);
		solver.writeModelToFile(TestConfiguration.lpModelsFolder + "drp6Privatep1.lp");
		solver.solveGame();
		assertEquals(TestConfiguration.drp6PrivateValueOfGame, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	
	public void testForProbabilityZeroNonFoldActionsLeduc() {
		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(leducGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(leducGame, 2);
		
		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		for (int informationSetId = 0; informationSetId < leducGame.getNumInformationSetsPlayer1(); informationSetId++) {
			int idOfFirstNodeInSet = leducGame.getInformationSet(1, informationSetId).get(0);
			Node firstNodeInSet = leducGame.getNodeById(idOfFirstNodeInSet);
			boolean anyNonZero = false;
			boolean anyZero = false;
			for (Action action : firstNodeInSet.getActions()) {
				if (strategyP1[informationSetId].get(action.getName()) != 0) anyNonZero = true;
				if (strategyP1[informationSetId].get(action.getName()) == 0) anyZero = true;
			}
			if (!anyNonZero) continue;
			if (!anyZero) continue;
			System.out.println();
			for (Action action : firstNodeInSet.getActions()) {
				//if (!(action.getName().equals("f") || strategyP1[informationSetId].get(action.getName()) != 0)) {
					System.out.println(firstNodeInSet.getName() + ": " + action.getName() + "   ----   " + strategyP1[informationSetId].get(action.getName()));
				//}
			}
		}
		assertTrue(false);
		for (int informationSetId = 0; informationSetId < leducGame.getNumInformationSetsPlayer2(); informationSetId++) {
			int idOfFirstNodeInSet = leducGame.getInformationSet(1, informationSetId).get(0);
			Node firstNodeInSet = leducGame.getNodeById(idOfFirstNodeInSet);
			for (Action action : firstNodeInSet.getActions()) {
				assertTrue(action.getName().equals("f") || strategyP2[informationSetId].get(action.getName()) != 0);
			}
		}
		
	}

	@Test
	public void testComputeGameValueForStrategies() {
		SequenceFormLPSolver lpsolverP1 = new SequenceFormLPSolver(dieRollPoker3, 1);
		SequenceFormLPSolver lpsolverP2 = new SequenceFormLPSolver(dieRollPoker3, 2);
		lpsolverP1.solveGame();
		lpsolverP2.solveGame();

		double[][][] lpstrat = new double[][][] {new double[0][0], lpsolverP1.getStrategyProfile()[1], lpsolverP2.getStrategyProfile()[2] };

		assertEquals(TestConfiguration.drp3ValueOfGame, dieRollPoker3.computeGameValueForStrategies(lpstrat) , 0.01);
	}
}


