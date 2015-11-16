package edu.cmu.cs.kroer.extensive_form_game;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;

import edu.cmu.cs.kroer.extensive_form_game.abstraction.DieRollPokerAbstractor;
import edu.cmu.cs.kroer.extensive_form_game.abstraction.SignalAbstraction;
import edu.cmu.cs.kroer.extensive_form_game.solver.BestResponseLPSolver;
import edu.cmu.cs.kroer.extensive_form_game.solver.CounterFactualRegretSolver;

public class CorrelatedDrpExperimentRunner {
	public static void main(String[] args) {
		JSAP jsap = new JSAP();
		
		FlaggedOption numSidesOption = new FlaggedOption("num_sides")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setDefault("3") 
                                .setRequired(true) 
                                .setShortFlag('s') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
		FlaggedOption distanceErrorOption = new FlaggedOption("distance_error")
                                .setStringParser(JSAP.DOUBLE_PARSER)
                                .setDefault("0") 
                                .setRequired(true) 
                                .setShortFlag('e') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
		FlaggedOption numRoundsOption = new FlaggedOption("num_rounds")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setDefault("2") 
                                .setRequired(true) 
                                .setShortFlag('r') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
		FlaggedOption numIterationsPerRoundOption = new FlaggedOption("num_iterations_per_round")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setDefault("100") 
                                .setRequired(true) 
                                .setShortFlag('i') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
		FlaggedOption gameFolder = new FlaggedOption("game_folder")
                                .setStringParser(JSAP.STRING_PARSER)
                                .setDefault(TestConfiguration.correlatedDrpGamesFolder) 
                                .setShortFlag('f') 
                                .setLongFlag(JSAP.NO_LONGFLAG);
        Switch computeAbstractionValueOption = new Switch("compute_abstraction_value")
                        .setShortFlag('v')
                        .setLongFlag(JSAP.NO_LONGFLAG);
        FlaggedOption numAbstractRollsOption = new FlaggedOption("num_abstract_rolls")
                                .setStringParser(JSAP.INTEGER_PARSER)
                                .setDefault("100") 
                                .setRequired(true) 
                                .setShortFlag('a') 
                                .setLongFlag(JSAP.NO_LONGFLAG);

        try {
			jsap.registerParameter(numSidesOption);
			jsap.registerParameter(distanceErrorOption);
			jsap.registerParameter(numRoundsOption);
			jsap.registerParameter(numIterationsPerRoundOption);
			jsap.registerParameter(gameFolder);
			jsap.registerParameter(computeAbstractionValueOption);
			jsap.registerParameter(numAbstractRollsOption);
		} catch (JSAPException e) {
			e.printStackTrace();
		}
        
        JSAPResult config = jsap.parse(args);    

		Game drpGame = new Game();
		drpGame.createGameFromFileZerosumPackageFormat(config.getString("game_folder")+ "correlated_drp_"+ config.getInt("num_sides") + "sided_point" + Double.toString(config.getDouble("distance_error")).split("\\.")[1] + "error.txt");

		if (config.getBoolean("compute_abstraction_value")) {
			double value = getCorrelatedDRPAbstractionValue(drpGame, config.getInt("num_sides"), config.getDouble("distance_error"), config.getInt("num_abstract_rolls"));
			System.out.printf("%.2f\t%.3f\n", config.getDouble("distance_error"), value);
		} else {
			solveCorrelatedDieRollPokerPrivate(drpGame, config.getInt("num_sides"), config.getDouble("distance_error"), config.getInt("num_rounds"), config.getInt("num_iterations_per_round"));
		}
	}
	
	public static void solveCorrelatedDieRollPokerPrivate(Game drpGame, int numSides, double error, int numRounds, int numCFRIterationsPerRound) {
		SignalAbstraction abstraction = getCorrelatedDRPAbstraction(drpGame, numSides, error);
		drpGame.applySignalAbstraction(abstraction);
		
		CounterFactualRegretSolver cfrSolver = new CounterFactualRegretSolver(drpGame);
		
		// TODO: LP solver strategy for debugging purposes
//		SequenceFormLPSolver equilibriumSolver = new SequenceFormLPSolver(drpGame, 1);
//		equilibriumSolver.solveGame();
//		double[][] p1Strategy = equilibriumSolver.getStrategyProfile()[1];

		int numIterations = 0;
		for (double i = 0.25; i <= numRounds; i+=0.25) {
			//System.out.println("NumIterations: " + ((int)Math.pow(10, i) - numIterations));
			cfrSolver.runCFR((int)Math.pow(10, i) - numIterations);
			
			numIterations += (int)Math.pow(10, i) - numIterations;
			
			double[][][] strategyProfile = cfrSolver.getStrategyProfile();

			BestResponseLPSolver brSolverP1 = new BestResponseLPSolver(drpGame, 1, strategyProfile[2]);
			brSolverP1.solveGame();
			
			BestResponseLPSolver brSolverP2 = new BestResponseLPSolver(drpGame, 2, strategyProfile[1]);
			brSolverP2.solveGame();
			
			double cfrGameValue = cfrSolver.getValueOfGame();
			double brP1Value = brSolverP1.getValueOfGame();
			double brP2Value = brSolverP2.getValueOfGame();
			double regretP1 = brP1Value - cfrGameValue;
			double regretP2 = cfrGameValue - brP2Value;
			
			System.out.printf("%d\t%d\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\t%.3f\n", cfrSolver.getNumNodesTouched(), numIterations, cfrGameValue, brP1Value, brP2Value, regretP1, regretP2, regretP1+regretP2);
		}
	}
	
	private static SignalAbstraction getCorrelatedDRPAbstraction(Game game, int numSides, double rollDistanceError) {
		return getCorrelatedDRPAbstraction(game, numSides, rollDistanceError, 2*numSides - 1);
	}
	
	private static SignalAbstraction getCorrelatedDRPAbstraction(Game game, int numSides, double rollDistanceError, int numAbstractRolls) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, numAbstractRolls, rollDistanceError); // lossless size if there were no nature error
		//abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		double value = abstractor.getObjectiveValue();
		//System.out.println("Abstraction value: " + value);
		return abstractor.getAbstraction();
	}

	private static double getCorrelatedDRPAbstractionValue(Game game, int numSides, double rollDistanceError, int numAbstractRolls) {
		DieRollPokerAbstractor abstractor = new DieRollPokerAbstractor(game, numSides, numAbstractRolls, rollDistanceError); // lossless size if there were no nature error
		//abstractor.writeModelToFile(TestConfiguration.lpModelsFolder + "drp" + numSides + "-private-abstraction.lp");
		
		abstractor.solveModel();
		return abstractor.getObjectiveValue();
	}

	
	
}
