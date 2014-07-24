package edu.cmu.cs.kroer.extensive_form_game;
import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.Game;


public class TestGameClass {

	@Test
	public void readMiniDeckQLeducFromFile() {
		Game game = new Game("/Users/ckroer/Documents/research/zerosum/games/minideckq_leduc.txt");
		assertEquals(106, game.getNumChanceHistories());
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

	@Test
	public void testZerosumPackageFormatFileReader() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/kuhn.txt");
		assertEquals(1, game.getNumChanceHistories());
		assertEquals(55, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

}
