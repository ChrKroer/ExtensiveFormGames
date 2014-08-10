package edu.cmu.cs.kroer.extensive_form_game;
import static org.junit.Assert.*;

import org.junit.Test;

import edu.cmu.cs.kroer.extensive_form_game.Game;


public class TestGameClass {
	private static final String gamesFolder = "/Users/ckroer/Documents/research/zerosum/games/";
	private static final String zerosumGamesFolder = "/Users/ckroer/Documents/research/zerosum/original_games/";

	@Test
	public void readMiniDeckQLeducFromFile() {
		Game game = new Game(gamesFolder + "minideckq_leduc.txt");
		assertEquals(106, game.getNumChanceHistories());
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

	@Test
	public void testZerosumPackageFormatFileReaderMiniKuhn() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "mini_kuhn.txt");
		assertEquals(1, game.getNumChanceHistories());
		assertEquals(11, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[10]);
	}

	@Test
	public void testZerosumPackageFormatFileReaderLeducKj() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "leduc_KJ.txt");
		assertEquals(21, game.getNumChanceHistories());
		assertEquals(511, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[10]);
		assertEquals(66, game.getNumInformationSetsPlayer1());
		assertEquals(66, game.getNumInformationSetsPlayer2());
	}

	@Test
	public void testZerosumPackageFormatFileReaderLeducKj1Raise() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "leduc_Kj1Raise.txt");
		assertEquals(13, game.getNumChanceHistories());
		assertEquals(199, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[10]);
		assertEquals(28, game.getNumInformationSetsPlayer1());
		assertEquals(28, game.getNumInformationSetsPlayer2());
	}
	
	@Test
	public void testZerosumPackageFormatFileReader() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "kuhn.txt");
		assertEquals(1, game.getNumChanceHistories());
		assertEquals(55, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

	@Test
	public void testZerosumPackageFormatFileReaderCoin() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(zerosumGamesFolder + "coin.txt");
		assertEquals(0, game.getNumChanceHistories());
		assertEquals(23, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[22]);
	}

	@Test
	public void testFileReaderLeducWithSignals() {
		Game game = new Game();
		game.createGameFromFile(gamesFolder + "leduc.txt");
		assertEquals(157, game.getNumChanceHistories());
		assertEquals(9457, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

}
