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
	public void testZerosumPackageFormatFileReaderMiniKuhn() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/mini_kuhn.txt");
		assertEquals(1, game.getNumChanceHistories());
		assertEquals(11, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[10]);
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

	@Test
	public void testZerosumPackageFormatFileReaderCoin() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat("/Users/ckroer/Documents/research/zerosum/original_games/coin.txt");
		assertEquals(0, game.getNumChanceHistories());
		assertEquals(23, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[22]);
	}

	@Test
	public void testFileReaderLeducWithSignals() {
		Game game = new Game();
		game.createGameFromFile("/Users/ckroer/Documents/research/zerosum/games/leduc.txt");
		assertEquals(157, game.getNumChanceHistories());
		assertEquals(9457, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

}
