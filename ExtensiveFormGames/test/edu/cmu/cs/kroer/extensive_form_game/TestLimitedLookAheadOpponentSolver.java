package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.UnknownObjectException;

import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.solver.LimitedLookAheadOpponentSolver;
import edu.cmu.cs.kroer.extensive_form_game.solver.SequenceFormLPSolver;
import gnu.trove.map.TObjectDoubleMap;

public class TestLimitedLookAheadOpponentSolver {

	@Test
	public void testSimpleGame() {
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

		assertEquals(3, solver.getValueOfGame(), TestConfiguration.epsilon);
	}
	
	@Test
	public void testEquilibriumEvaluationMiniKuhn() {
		Game miniKuhnGame = new Game();
		miniKuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(miniKuhnGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(miniKuhnGame, 2);
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		double[] nodeEvaluationTable = miniKuhnGame.getExpectedValuesForNodes(strategyP1, strategyP2);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(miniKuhnGame, 1, nodeEvaluationTable, 1);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.solveGame();
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
		
	}

	@Test
	public void testEquilibriumEvaluationLeduc() {
		Game leducGame = new Game();
		leducGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc.txt");

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(leducGame, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(leducGame, 2);
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();
		
		double[] nodeEvaluationTable = leducGame.getExpectedValuesForNodes(strategyP1, strategyP2);

		// Compute the best strategy to commit to when the limited look-ahead player knows how much can be achieved from a node in (some) equilibrium
		LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(leducGame, 1, nodeEvaluationTable, 1);
		//solver.writeModelToFile(TestConfiguration.lpModelsFolder + "kuhnp1-limited-look-ahead.lp");
		solver.solveGame();
		assertEquals(solverP1.getValueOfGame(), solver.getValueOfGame(), TestConfiguration.epsilon);
	}

}
