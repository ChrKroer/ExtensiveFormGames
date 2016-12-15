package edu.cmu.cs.kroer.extensive_form_game;

public final class TestConfiguration {
	// Specifies where game instance files in the new format are located
	public static final String gamesFolder = "/Users/ckroer/Dropbox/research/equilibrium-finding/zerosum/games/";
	// Specifies where game instance files in the zerosum package format are located
	public static final String zerosumGamesFolder = "/Users/ckroer/Dropbox/research/equilibrium-finding/zerosum/original_games/";
	// Specifies where game instances for correlated drp are located
	public static final String correlatedDrpGamesFolder = zerosumGamesFolder + "correlated_drp_games/";	
	// Specifies where to save LP models. Each test case dumps its LP model to a file in the specified folder
	public static final String lpModelsFolder = "/Users/ckroer/Documents/research/lp-models/";
	// Epsilon value for precision of CPLEX
	public static double epsilon = 0.02;
	
	public static final double miniKuhnValueOfGame = 0.5;
	public static final double kuhnValueOfGame = -0.0555556;
	public static final double coinValueOfGame = 0.375;
	public static final double prslValueOfGame = 0;
	public static final double leducKj1RaiseValueOfGame = -1.85037e-17;
	public static final double stengelValueOfGame = 1;
	public static final double drp3ValueOfGame = -0.158025;
	public static final double drp6ValueOfGame = -0.0395062;
	
	public static final double drp1PrivateValueOfGame = 0;
	public static final double drp2PrivateValueOfGame = -0.05144101876675597;
	public static final double drp3PrivateValueOfGame = -0.0384817;
	public static final double drp6PrivateValueOfGame = -0.0773881;
}
