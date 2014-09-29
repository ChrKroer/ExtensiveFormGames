package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.solver.CounterFactualRegretSolver;
import gnu.trove.map.TIntDoubleMap;

public class TestCounterFactualRegretSolver {

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

	@Test
	public void testKuhnConvergence() {
		Game kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");
		
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(kuhnGame);
		solver.solveGame(10);
		
		int iterations = 10;
		
		for (int iteration = 1; iteration < iterations; iteration++) {
			System.out.println("Starting Kuhn convergence test no " + iteration);
			solver.runCFR(20);
			
		}
	}

	@Test
	public void testStengelConvergence() {
		Game kuhnGame = new Game();
		kuhnGame.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "stengel.txt");
		
		CounterFactualRegretSolver solver = new CounterFactualRegretSolver(kuhnGame);
		//solver.solveGame(10);
		
		int iterations = 1;
		
		for (int iteration = 1; iteration <= iterations; iteration++) {
			System.out.print("Starting Kuhn convergence test no " + iteration + ": ");
			solver.runCFR(1000000);
			System.out.print(Arrays.toString(solver.getInformationSetActionProbabilitiesByActionId(1)) + " + ");
			System.out.println(Arrays.toString(solver.getInformationSetActionProbabilitiesByActionId(2)));
		}
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
		
		int iterations = 1;
		
		for (int iteration = 1; iteration <= iterations; iteration++) {
			System.out.print("Starting Kuhn convergence test no " + iteration + ": ");
			solver.runCFR(1000000);
			System.out.print(Arrays.toString(solver.getInformationSetActionProbabilitiesByActionId(1)) + " + ");
			System.out.println(Arrays.toString(solver.getInformationSetActionProbabilitiesByActionId(2)));
		}
	}
	
}













