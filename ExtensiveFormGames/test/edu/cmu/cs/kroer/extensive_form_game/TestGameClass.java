package edu.cmu.cs.kroer.extensive_form_game;

import java.util.List;


import extensive_form_game.Game;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class TestGameClass {

	@Test
	public void readMiniDeckQLeducFromFile() {
		Game game = new Game(TestConfiguration.gamesFolder + "minideckq_leduc.txt");
		assertEquals(106, game.getNumChanceHistories());
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

	@Test
	public void testZerosumPackageFormatFileReaderMiniKuhn() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt");
		assertEquals(1, game.getNumChanceHistories());
		assertEquals(11, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[10]);
	}

	@Test
	public void testZerosumPackageFormatFileReaderLeducKj() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc_KJ.txt");
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
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "leduc_Kj1Raise.txt");
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
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "kuhn.txt");
		assertEquals(1, game.getNumChanceHistories());
		assertEquals(55, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

	@Test
	public void testZerosumPackageFormatFileReaderCoin() {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(TestConfiguration.zerosumGamesFolder + "coin.txt");
		assertEquals(0, game.getNumChanceHistories());
		assertEquals(23, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[22]);
	}

	@Test
	public void testFileReaderLeducWithSignals() {
		Game game = new Game();
		game.createGameFromFile(TestConfiguration.gamesFolder + "leduc.txt");
		assertEquals(157, game.getNumChanceHistories());
		assertEquals(9457, game.getNodes().length);
		assertNotNull(game.getNodes()[0]);
		assertNotNull(game.getNodes()[50]);
	}

	@Test
	public void testExtractObservedActionsFromNodeName() {
		List<String> extracted = Game.extractObservedActionsFromNodeName("/n;0;3/n;1;3/0;a;c/1;a;c/n;0;2/n;1;2/", 0);
		assertEquals("3", extracted.get(0));
		assertEquals("c", extracted.get(1));
		assertEquals("c", extracted.get(2));
		assertEquals("2", extracted.get(3));

		extracted = Game.extractObservedActionsFromNodeName("/n;0;3/n;1;3/0;a;c/0;a;c/n;0;2/n;1;3/", 1);
		assertEquals("3", extracted.get(0));
		assertEquals("c", extracted.get(1));
		assertEquals("c", extracted.get(2));
		assertEquals("3", extracted.get(3));
	}

	@Test
	public void testExtractObservedNatureActionsFromNodeName() {
		List<String> extracted = Game.extractObservedNatureActionsFromNodeName("/n;0;3/n;1;3/0;a;c/1;a;c/n;0;2/n;1;2/", 0);
		assertEquals("3", extracted.get(0));
		assertEquals("2", extracted.get(1));

		extracted = Game.extractObservedNatureActionsFromNodeName("/n;0;3/n;1;3/0;a;c/0;a;c/n;0;2/n;1;3/", 1);
		assertEquals("3", extracted.get(0));
		assertEquals("3", extracted.get(1));
	}

	@Test
	public void testExtractObservedPlayerActionsFromNodeName() {
		List<String> extracted = Game.extractObservedPlayerActionsFromNodeName("/n;0;3/n;1;3/0;a;c/1;a;c/n;0;2/n;1;2/", 0);
		assertEquals("c", extracted.get(0));
		assertEquals("c", extracted.get(1));

		extracted = Game.extractObservedPlayerActionsFromNodeName("/n;0;3/n;1;3/0;a;c/0;a;c/n;0;2/n;1;3/", 1);
		assertEquals("c", extracted.get(0));
		assertEquals("c", extracted.get(1));
	}
	
	@Test
	public void testApplySignalAbstraction() {
		
	}
}
