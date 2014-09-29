

package edu.cmu.cs.kroer.extensive_form_game;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;

import edu.cmu.cs.kroer.extensive_form_game.solver.LimitedLookAheadOpponentSolver;
import edu.cmu.cs.kroer.extensive_form_game.solver.SequenceFormLPSolver;
import gnu.trove.map.TObjectDoubleMap;

public class Experiments {

	public static void main(String[] args) {
		//runIncreasingLookAheadGameValues(TestConfiguration.zerosumGamesFolder + "mini_kuhn.txt", 100, 2, 0.1, 1);
		runIncreasingLookAheadGameValues(TestConfiguration.zerosumGamesFolder + "kuhn.txt", 10, 2, 11, 10, 2, true, true);
		//runIncreasingLookAheadGameValues(TestConfiguration.zerosumGamesFolder + "leduc_Kj1Raise.txt", 5, 3, 21, 10, 1, true, false);
		//runIncreasingLookAheadGameValues(TestConfiguration.zerosumGamesFolder + "leduc.txt", 1, 1, 1, 10);
		//runIncreasingLookAheadGameValues(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), false, args[6].equalsIgnoreCase("cumulative"));
	}
	
	private static void addGaussianNoise(NormalDistribution distribution, double[] array) {
		// Add Gaussian noise to evaluations
		for (int i = 0; i < array.length; i++) {
			array[i] += distribution.sample();
		}
	}

	public static void runIncreasingLookAheadGameValues(String gameFile, int iterations, int lookAhead, int noiseUpperBound, int noiseDivisor, int playerToSolveFor, boolean printAverage, boolean cumulativeNoise) {
		Game game = new Game();
		game.createGameFromFileZerosumPackageFormat(gameFile);
		

		SequenceFormLPSolver solverP1 = new SequenceFormLPSolver(game, 1);
		SequenceFormLPSolver solverP2 = new SequenceFormLPSolver(game, 2);

		solverP1.solveGame();
		solverP2.solveGame();
		
		TObjectDoubleMap<String>[] strategyP1 = solverP1.getInformationSetActionProbabilities();
		TObjectDoubleMap<String>[] strategyP2 = solverP2.getInformationSetActionProbabilities();


		
		double[][] gameValues = new double[iterations][noiseUpperBound];
		MersenneTwister twister = new MersenneTwister();
		//int[][] iterationsBookKeeper = new int[lookAheadUpperBound][noiseUpperBound / noiseIncrement];
		for (int noise = 0; noise < noiseUpperBound; noise++) {
			NormalDistribution distribution;
			if (noise > 0) distribution = new NormalDistribution(twister, 0, (double) noise / noiseDivisor);
			else distribution = new NormalDistribution(twister, 0, 1);
			for (int iteration = 0; iteration < iterations; iteration++) {
				double[] nodeEvaluationTable;
				if (cumulativeNoise && noise > 0) {
					//System.out.println("Adding cumulative");
					nodeEvaluationTable = game.getExpectedValuesForNodes(strategyP1, strategyP2, playerToSolveFor == 1, distribution);
				} else {
					nodeEvaluationTable = game.getExpectedValuesForNodes(strategyP1, strategyP2, playerToSolveFor == 1);
				}
				if (noise > 0) addGaussianNoise(distribution, nodeEvaluationTable);
				// Compute the best strategy to commit to
				LimitedLookAheadOpponentSolver solver = new LimitedLookAheadOpponentSolver(game, playerToSolveFor, nodeEvaluationTable, lookAhead);
				solver.solveGame();
				//System.out.println((int) (noise/noiseIncrement)+"\t\t" + noise + " - " + noiseIncrement);
				gameValues[iteration][noise] = solver.getValueOfGame();
				//iterationsBookKeeper[lookAhead-1][noise / noiseIncrement] ++;

			}
		}
		
		
		DecimalFormat df = new DecimalFormat("#.####");
		// Output computed values
		//System.out.println("-------------------------------------------------------\n\n");
		//System.out.println("Value of game: " + df.format(solverP1.getValueOfGame()) + "\n");
		//System.out.print("Noise");
		/*for (double noise = 0; noise < noiseUpperBound-1; noise++) {
			System.out.print(df.format((double) noise / noiseDivisor)+"\t");
		}
		System.out.print(df.format((double) (noiseUpperBound-1) / noiseDivisor)+"\n");*/
		double[] averages = new double[noiseUpperBound];
		Arrays.fill(averages, 0);
		for (int iteration = 0; iteration < iterations; iteration++) {
			//System.out.print("Look ahead " + lookAhead);
			for (int noise = 0; noise < noiseUpperBound-1; noise++) {
				System.out.print(df.format(gameValues[iteration][noise]) + "\t");
				averages[noise] += gameValues[iteration][noise] / iterations;
			}
			System.out.print(df.format(gameValues[iteration][noiseUpperBound-1]) + "\n");
			averages[noiseUpperBound-1] += gameValues[iteration][noiseUpperBound-1] / iterations;
		}
		
		if (printAverage) {
			System.out.println("Averages:");
			for (int noise = 0; noise < noiseUpperBound; noise++) {
				System.out.print(df.format(averages[noise]) + "\t");
			}
		}
	}
}
