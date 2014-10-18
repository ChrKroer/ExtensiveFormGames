package edu.cmu.cs.kroer.extensive_form_game;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.abstraction.DieRollPokerAbstractor;

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
	public void testDieRollPokerAbstractorGameGeneratorIntInt() {
		fail("Not yet implemented");
	}

	@Test
	public void testDieRollPokerAbstractorGameIntIntDoubleArray() {
		fail("Not yet implemented");
	}

	@Test
	public void testInformationSetMapping() {
		fail("Not yet implemented");
	}

	@Test
	public void testActionMapping() {
		fail("Not yet implemented");
	}

	
	@Test
	public void testDRP3Abstraction() {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(dieRollPoker3Private, 3, 5);
		abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp3-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		
		assertEquals(value, 0, TestConfiguration.epsilon);
	}
}
