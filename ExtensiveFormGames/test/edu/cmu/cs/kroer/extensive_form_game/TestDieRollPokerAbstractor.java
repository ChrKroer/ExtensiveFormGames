package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import extensive_form_game.Game;
import extensive_form_game_abstraction.DieRollPokerAbstractor;
import extensive_form_game_abstraction.SignalAbstraction;


/*
 * Right now this class only tests for correctness on the coarsest abstraction and the lossy abstraction
 */
public class TestDieRollPokerAbstractor {
	Game dieRollPoker3Private;
	Game dieRollPoker6Private;
	Game correlatedDieRollPoker3Private;
	Game correlatedDieRollPoker6Private;
	
	@Before
	public void setUp() throws Exception {
		dieRollPoker3Private = new Game();
		dieRollPoker3Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-3_private.txt");

		dieRollPoker6Private = new Game();
		dieRollPoker6Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-6_private.txt");

		correlatedDieRollPoker3Private = new Game();
		correlatedDieRollPoker3Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "correlated_drp_private_3sided_point1error.txt");

		correlatedDieRollPoker6Private = new Game();
		correlatedDieRollPoker6Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "correlated_drp_games/correlated_drp_6sided_point01error.txt");
	
	}

	

	
	@Test
	public void testDRP3LosslessAbstraction() {
		testDRPLosslessAbstraction(dieRollPoker3Private, 3);
	}

	@Test
	public void testDRP6LosslessAbstraction() {
		testDRPLosslessAbstraction(dieRollPoker6Private, 6);
	}
	
	private void testDRPLosslessAbstraction(Game game, int numSides) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, 2*numSides - 1);
		abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		
		SignalAbstraction abstraction = abstractor.getAbstraction();
		
		assertEquals(value, 0, TestConfiguration.epsilon);
		
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
			List<Integer> signals = new ArrayList<Integer>(Arrays.asList(firstRoll-1, secondRoll-1));
			List<Integer> abstractSignals = abstraction.getAbstractSignalsById(signals);
			assertEquals(firstRoll+secondRoll-2, abstractSignals.get(0) + abstractSignals.get(1));
		}}
	}
	
	@Test
	public void testDRP3LossyAbstractionSize1() {
		int numSides = 3;
		SignalAbstraction abstraction = getDRPLossyAbstraction(dieRollPoker3Private, numSides, 1);
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
			List<Integer> signals = new ArrayList<Integer>(Arrays.asList(firstRoll-1, secondRoll-1));
			List<Integer> abstractSignals = abstraction.getAbstractSignalsById(signals);
			assertEquals(0, abstractSignals.get(0) + abstractSignals.get(1));
		}}
	}

	@Test
	public void testDRP3LossyAbstractionSize3() {
		int numSides = 3;
		SignalAbstraction abstraction = getDRPLossyAbstraction(dieRollPoker3Private, numSides, 3);
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
			List<Integer> signals = new ArrayList<Integer>(Arrays.asList(firstRoll-1, secondRoll-1));
			List<Integer> abstractSignals = abstraction.getAbstractSignalsById(signals);
			//assertEquals(0, abstractSignals.get(0) + abstractSignals.get(1)); // this is not testing correctly. Could potentially add a test here for more thorough testing
		}}
	}

	private SignalAbstraction getDRPLossyAbstraction(Game game, int numSides, int numBuckets) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, numBuckets);
		abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		
		SignalAbstraction abstraction = abstractor.getAbstraction();
		return abstraction;
		
	}


	//@Test
	public void testCorrelatedDRP6LossyAbstractionSize6() {
		int numSides = 6;
		testCorrelatedDRPLossyAbstraction(correlatedDieRollPoker6Private, numSides, 0.01);
	}

	@Test
	public void testCorrelatedDRP3LossyAbstractionSize3() {
		int numSides = 3;
		testCorrelatedDRPLossyAbstraction(correlatedDieRollPoker3Private, numSides, 0.1);
	}

	@Test
	public void testCorrelatedDRP3LossyAbstractionSize2() {
		int numSides = 2;
		testCorrelatedDRPLossyAbstraction(correlatedDieRollPoker3Private, numSides, 0.1);
	}

	@Test
	public void testCorrelatedDRP3LossyAbstractionSize2Monotonicity() {
		int numSides = 2;
		double[] rollDistanceErrors = {0, 0.01, 0.02, 0.05, 0.08, 0.1, 0.2};
		double previous = -1;
		for (int i = 0; i < 7; i++) {
			double value = getCorrelatedDRPLossyAbstractionValue(correlatedDieRollPoker3Private, numSides, rollDistanceErrors[i]);
			System.out.println(value);
			assertTrue(value > previous);
			previous = value;
		}
		
	}

	private double getCorrelatedDRPLossyAbstractionValue(Game game, int numSides, double rollDistanceError) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, 2*numSides - 1, rollDistanceError); // lossless size if there were no nature error
		abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		return value;
	}
	
	
	private void testCorrelatedDRPLossyAbstraction(Game game, int numSides, double rollDistanceError) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, 2*numSides - 1, rollDistanceError); // lossless size if there were no nature error
		abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		
		SignalAbstraction abstraction = abstractor.getAbstraction();
		
		assertEquals(value, 0, TestConfiguration.epsilon);
		
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
			List<Integer> signals = new ArrayList<Integer>(Arrays.asList(firstRoll-1, secondRoll-1));
			List<Integer> abstractSignals = abstraction.getAbstractSignalsById(signals);
			assertEquals(firstRoll+secondRoll-2, abstractSignals.get(0) + abstractSignals.get(1));
		}}
	}

}
