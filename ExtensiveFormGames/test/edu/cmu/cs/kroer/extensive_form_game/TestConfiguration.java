package edu.cmu.cs.kroer.extensive_form_game;

public final class TestConfiguration {
	// Specifies where game instance files in the new format are located
	static final String gamesFolder = "/Users/ckroer/Documents/research/zerosum/games/";
	// Specifies where game instance files in the zerosum package format are located
	static final String zerosumGamesFolder = "/Users/ckroer/Documents/research/zerosum/original_games/";
	// Specifies where game instances for correlated drp are located
	static final String correlatedDrpGamesFolder = zerosumGamesFolder + "correlated_drp_games/";	
	// Specifies where to save LP models. Each test case dumps its LP model to a file in the specified folder
	static final String lpModelsFolder = "/Users/ckroer/Documents/research/lp-models/";
	// Epsilon value for precision of CPLEX
	static double epsilon = 0.02;
	
	static final double miniKuhnValueOfGame = 0.5;
	static final double kuhnValueOfGame = -0.0555556;
	static final double coinValueOfGame = 0.375;
	static final double prslValueOfGame = 0;
	static final double leducKj1RaiseValueOfGame = -1.85037e-17;
	static final double stengelValueOfGame = 1;
	static final double drp3ValueOfGame = -0.158025;
	static final double drp6ValueOfGame = -0.0395062;
	
	static final double drp1PrivateValueOfGame = 0;
	static final double drp2PrivateValueOfGame = -0.05144101876675597;
	static final double drp3PrivateValueOfGame = -0.0384817;
	static final double drp6PrivateValueOfGame = -0.0773881;
}
