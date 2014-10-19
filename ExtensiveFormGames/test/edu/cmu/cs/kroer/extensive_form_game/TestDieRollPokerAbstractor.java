package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.abstraction.DieRollPokerAbstractor;
import edu.cmu.cs.kroer.extensive_form_game.abstraction.SignalAbstraction;

public class TestDieRollPokerAbstractor {
	Game dieRollPoker3Private;
	Game dieRollPoker6Private;

	@Before
	public void setUp() throws Exception {
		dieRollPoker3Private = new Game();
		dieRollPoker3Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-3_private.txt");

		dieRollPoker6Private = new Game();
		dieRollPoker6Private.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "drp-6_private.txt");

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
}
