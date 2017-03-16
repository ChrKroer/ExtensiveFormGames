package extensive_form_game_abstraction;

import java.util.ArrayList;
import java.util.Arrays;

import gurobi.GurobiSolver;
import gurobi.*;
import extensive_form_game.Game;
import extensive_form_game.GameGenerator;


/**
 * This die-roll poker class assumes that the Game instance contains a DRP game where each player rolls one private die, betting happens, and then they each roll another private die, followed by betting
 * A symmetric abstraction is computed, satisfying the constraints specified in Kroer & Sandholm 16: Extensive-Form Game Imperfect-Recall Abstractions with Bounds, such that an abstraction with bounded solution quality is found 
 * @author Christian Kroer
 * TODO: currently computes abstractions only for the second level
 */
public class DieRollPokerAbstractor extends GurobiSolver implements Abstractor {

	GameGenerator game;
	int numSides;
	int numLevelsPerRoll = 3; // DRP always has at most six information levels for Player 1, no matter the number of possible rolls
	int numAbstractionInformationSets;
	double[] sideProbabilities;
	double rollDistanceError = 0; // default value is that there is no correlation between rolls 
	

	
	// Indexed as [roll value][bucket]
	GRBVar[][] firstRollAbstractionVariables;
	// Indexed as [first roll ][second roll][bucket]
	GRBVar[][][] secondRollAbstractionVariables;
	// boolean variables designating whether bucket i is a bucket for private cards (bucketLevelSwitch[i] == 0) or public cards (bucketLevelSwitch[i] == 1) 
	GRBVar[] bucketLevelSwitch;
	GRBVar costVariablesSecondRoll[][];
	
	int[][] informationSetMapping;
	int[][][] actionMapping;
	
	public DieRollPokerAbstractor(GameGenerator game, int numSides, int numAbstractInformationSets) {
		this(game, numSides, numAbstractInformationSets, 0);
	}
	
	public DieRollPokerAbstractor(GameGenerator game, int numSides, int numAbstractInformationSets, double rollDistanceError) {
		super();
		this.game = game;
		this.numSides= numSides;
		this.numAbstractionInformationSets = numAbstractInformationSets;
		this.rollDistanceError = rollDistanceError;
		
		sideProbabilities = new double[numSides+1];
		for (int side = 0; side < numSides+1; side++) {
			sideProbabilities[side] = 1.0 / numSides;
		}
		
		try {
			initializeDataStructures();
			//createBucketLevelSwitchVars();
			createSecondRollAbstractionVars();
			//createFirstRollAbstractionVars();
			//createFirstRollImplicationConstraints();
			addObjectiveCostSecondRoll();
			addCostsToObjective();
			//createTieBreakingConstraints();
		} catch (GRBException e) {
			e.printStackTrace();
		}
		
		try {
			model.setObjective(objective, GRB.MINIMIZE);
		} catch (GRBException e) {
			e.printStackTrace();
		}
	}

	public DieRollPokerAbstractor(Game game, int numSides, int numAbstractInformationSets, double[] sideProbabilities) {
		this(game, numSides, numAbstractInformationSets);
		
		this.sideProbabilities = sideProbabilities;
		
		double sum = 0;
		for (double probability : sideProbabilities) {
			sum += probability;
		}
		assert(sum > 0.999 && sum < 1.001);
	}
	

	public SignalAbstraction getAbstraction() {
		String[] signalNames = new String[numSides];
		for (int side = 1; side <= numSides; side++) {
			signalNames[side-1] = Integer.toString(side);
		}
		SignalAbstraction abstraction = new SignalAbstraction(signalNames);
		@SuppressWarnings("unchecked")
		ArrayList<ArrayList<Integer> >[] buckets = new ArrayList[numAbstractionInformationSets];
		// Construct a list containing the buckets, where each item in the bucket is a List of the integer indices of the signals (e.g.  {0,3} would mean rolling 1 followed by 4)
		for (int bucket = 0; bucket < numAbstractionInformationSets; bucket++) {
			buckets[bucket] = new ArrayList<ArrayList<Integer>>();
			for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
			for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
				try {
					if (secondRollAbstractionVariables[firstRoll][secondRoll][bucket].get(GRB.DoubleAttr.X) > 1-cplexEpsilon) {
						ArrayList<Integer> signals = new ArrayList<Integer>(Arrays.asList(firstRoll-1, secondRoll-1));
						buckets[bucket].add(signals);
					}
				} catch (GRBException e) {
					e.printStackTrace();
				}
			}}
		}	
		// We are going to map everything to the first item in the bucket. That means that we end up using the information set IDs belonging to items at index 0 in buckets.
		for (int bucket = 0; bucket < numAbstractionInformationSets; bucket++) {
			if (buckets[bucket].size() > 0) {
				for (int i = 0; i < buckets[bucket].size(); i++) {
					abstraction.addAbstraction(buckets[bucket].get(0), buckets[bucket].get(i));
				}
			}
		}
		// TODO
		return abstraction;
	}
	
	private void initializeDataStructures() throws GRBException {
		firstRollAbstractionVariables = new GRBVar[numSides+1][numAbstractionInformationSets];
		secondRollAbstractionVariables = new GRBVar[numSides+1][numSides+1][numAbstractionInformationSets];
		costVariablesSecondRoll = new GRBVar[numSides+1][numSides+1];
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
			costVariablesSecondRoll[firstRoll][secondRoll] = model.addVar(0.0, 2*game.getLargestPayoff(), 0.0, GRB.CONTINUOUS, "Cost("+firstRoll+";"+secondRoll+")");
		}}
		bucketLevelSwitch = new GRBVar[numAbstractionInformationSets];
	}

	private void createBucketLevelSwitchVars() throws GRBException {
		for (int bucket = 0; bucket < numAbstractionInformationSets; bucket++) {
			bucketLevelSwitch[bucket] = model.addVar(0, 1, 0, GRB.BINARY, "S("+bucket+")");
		}
		
	}

	private void createSecondRollAbstractionVars() throws GRBException {
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
			GRBLinExpr expr = new GRBLinExpr();
			for (int bucket = 0; bucket < numAbstractionInformationSets; bucket++) {
				secondRollAbstractionVariables[firstRoll][secondRoll][bucket] = model.addVar(0,1,0, GRB.BINARY,"B("+firstRoll+";"+secondRoll+";"+bucket+")");
				// Ensure that the variable is only added to one bucket
				expr.addTerm(1, secondRollAbstractionVariables[firstRoll][secondRoll][bucket]);
				//addBucketLevelSwitchConstraint(secondRollAbstractionVariables[firstRoll][secondRoll][bucket], bucket, true);
			}
			model.addConstr(expr, '=', 1, "");
		}}
	}

	private void createTieBreakingConstraints() throws GRBException{
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 2; secondRoll <= numSides; secondRoll++) { // start at 2 so previous roll exists
			for (int bucket = 1; bucket < numAbstractionInformationSets; bucket++) { // only constrain second bucket and higher
				GRBLinExpr expr = new GRBLinExpr();
				expr.addTerm(1, secondRollAbstractionVariables[firstRoll][secondRoll][bucket]);
				for (int previousFirstRoll = 1; previousFirstRoll < firstRoll; previousFirstRoll++) {
				for (int previousSecondRoll = 1; previousSecondRoll < secondRoll; previousSecondRoll++) {
					expr.addTerm(-1, secondRollAbstractionVariables[previousFirstRoll][previousSecondRoll][bucket-1]);
				}}
				model.addConstr(expr, GRB.LESS_EQUAL, 0, "");
			}
		}}
	}
	
	private void createFirstRollAbstractionVars() throws GRBException {
		for (int side = 1; side <= numSides; side++) {
			for (int bucket = 0; bucket < numAbstractionInformationSets; bucket++) {
				firstRollAbstractionVariables[side][bucket] = model.addVar(0,1,0, GRB.BINARY,"B("+side+";"+bucket+")");
				addBucketLevelSwitchConstraint(firstRollAbstractionVariables[side][bucket], bucket, true);
			}
		}
	}

		
//	private void createFirstRollImplicationConstraints() throws GRBException {
//		for (int firstRoll1 = 1; firstRoll1 <= numSides; firstRoll1++) {
//		for (int firstRoll2 = 1; firstRoll2 <= numSides; firstRoll2++) {
//			GRBLinExpr expr = new GRBLinExpr();
//			//expr.addTerm(arg0, arg1);
//		}}
//
//	}

	// firstRoll/secondRoll denotes the first and second die roll for the same info set. The 1 or 2 after denotes different info sets
	private void addObjectiveCostSecondRoll() throws GRBException {
		for (int firstRoll1 = 1; firstRoll1 <= numSides; firstRoll1++) {
		for (int secondRoll1 = 1; secondRoll1 <= numSides; secondRoll1++) {
		for (int firstRoll2 = 1; firstRoll2 <= numSides; firstRoll2++) {
		for (int secondRoll2 = 1; secondRoll2 <= numSides; secondRoll2++) {
			double cost = computeCostOfAbstractingSecondRollPair(firstRoll1, firstRoll2, secondRoll1, secondRoll2);
			for (int bucket = 0; bucket < numAbstractionInformationSets; bucket++) {
			
				//objective.addTerm(cost, secondRollAbstractionVariables[firstRoll1][secondRoll1][bucket]);
				GRBLinExpr expr = new GRBLinExpr();
				expr.addTerm(cost, secondRollAbstractionVariables[firstRoll1][secondRoll1][bucket]);
				expr.addTerm(cost, secondRollAbstractionVariables[firstRoll2][secondRoll2][bucket]);
				expr.addConstant(-cost);
				model.addConstr(expr, GRB.LESS_EQUAL, costVariablesSecondRoll[firstRoll1][secondRoll1], "");
				model.addConstr(expr, GRB.LESS_EQUAL, costVariablesSecondRoll[firstRoll2][secondRoll2], "");
			}
		}}}}		
	}
	
	private double computeCostOfAbstractingSecondRollPair(int firstRoll1, int firstRoll2, int secondRoll1, int secondRoll2) {
		return computePayoffErrorCostOfAbstractingSecondRollPair(firstRoll1, firstRoll2, secondRoll1, secondRoll2) + computeNatureErrorCostOfAbstractingSecondRollPair(firstRoll1, firstRoll2, secondRoll1, secondRoll2);
	}
	
	private double computePayoffErrorCostOfAbstractingSecondRollPair(int firstRoll1, int firstRoll2, int secondRoll1, int secondRoll2) {
		double sum1 = firstRoll1 + secondRoll1;
		double sum2 = firstRoll2 + secondRoll2;
		if (sum1 == sum2) { // if the information sets have the same sum, then abstracting them is lossless from a payoff error perspective
			return 0;
		}
		
		double lower = Math.min(sum1, sum2);
		double higher = Math.max(sum1, sum2);		
		double error = 0;
		
		for (int opponentFirstRoll = 1; opponentFirstRoll <= numSides; opponentFirstRoll++) {
		for (int opponentSecondRoll = 1; opponentSecondRoll <= numSides; opponentSecondRoll++) {
			double probabilityOfSingleRoll = sideProbabilities[firstRoll1] * sideProbabilities[secondRoll1]; 
			int sum = opponentFirstRoll + opponentSecondRoll;
			// if the opponent hand draws with one and wins or loses against the other, we have a swing of largestPayoff, if it beats one and loses to the other, we have a swing of 2 * largestPayoff
			double offValue = (sum == lower || sum == higher ? 1 : 0) + (sum > lower && sum < higher ? 2: 0); 
			error += probabilityOfSingleRoll * 2 * offValue * game.getLargestPayoff();
			error += 2 * offValue * Math.abs(sideProbabilities[firstRoll1] * sideProbabilities[secondRoll1] - sideProbabilities[firstRoll2] * sideProbabilities[secondRoll2]);
		}}
		// this multiplies by node probability
		return error * sideProbabilities[firstRoll1] * sideProbabilities[secondRoll1];
	}
	
	private double computeNatureErrorCostOfAbstractingSecondRollPair(int firstRollInfoSet1, int firstRollInfoSet2, int secondRollInfoSet1, int secondRollInfoSet2) {
		double error = 0;
		for (int opponentFirstRoll = 1; opponentFirstRoll <= numSides; opponentFirstRoll++) {
		for (int opponentSecondRoll = 1; opponentSecondRoll <= numSides; opponentSecondRoll++) {
			double probabilityFirstRollInformationSet1 = (1.0 / numSides - rollDistanceError * Math.abs(firstRollInfoSet1 - opponentFirstRoll)) / computeNormalizationFactor(firstRollInfoSet1); 
			double probabilityFirstRollInformationSet2 = (1.0 / numSides - rollDistanceError * Math.abs(firstRollInfoSet2 - opponentFirstRoll)) / computeNormalizationFactor(firstRollInfoSet2); 
			double probabilitySecondRollInformationSet1 = (1.0 / numSides - rollDistanceError * Math.abs(secondRollInfoSet1 - opponentSecondRoll)) / computeNormalizationFactor(secondRollInfoSet1); 
			double probabilitySecondRollInformationSet2 = (1.0 / numSides - rollDistanceError * Math.abs(secondRollInfoSet2 - opponentSecondRoll)) / computeNormalizationFactor(secondRollInfoSet2);
			
			double nodeProbabilityInfoSet1 = probabilityFirstRollInformationSet1 * probabilitySecondRollInformationSet1;
			double nodeProbabilityInfoSet2 = probabilityFirstRollInformationSet2 * probabilitySecondRollInformationSet2;
			
			error += Math.abs(probabilityFirstRollInformationSet1 * probabilitySecondRollInformationSet1 - probabilityFirstRollInformationSet2 * probabilitySecondRollInformationSet2);
		}}
		// this multiplies by node probability
		return sideProbabilities[firstRollInfoSet1] * sideProbabilities[secondRollInfoSet1] * error * 2*game.getLargestPayoff() * numLevelsPerRoll;
	}

	private double computeNormalizationFactor(int roll) {
		double factor = 1;
		for (int side = 1; side <= numSides; side++) {
			factor -= rollDistanceError * Math.abs(roll - side);
		}
		return factor;
	}
	
	private void addCostsToObjective() throws GRBException {
		for (int firstRoll = 1; firstRoll <= numSides; firstRoll++) {
		for (int secondRoll = 1; secondRoll <= numSides; secondRoll++) {
		//for (int bucket = 0; bucket <= numAbstractionInformationSets; bucket++) {
			objective.addTerm(1.0 / (numSides*numSides), costVariablesSecondRoll[firstRoll][secondRoll]);
		}}
	}
	
	private void addBucketLevelSwitchConstraint(GRBVar var, int bucket, boolean isPublic) throws GRBException {
		GRBLinExpr expr = new GRBLinExpr();
		if (isPublic) {
			expr.addTerm(2, var);
			expr.addTerm(-1, bucketLevelSwitch[bucket]);
		} else {
			expr.addTerm(1, var);
			expr.addTerm(1, bucketLevelSwitch[bucket]);
		}
		model.addConstr(expr, GRB.LESS_EQUAL, 1, "");
	}
	
	@Override
	public int[][] informationSetMapping() {
		return informationSetMapping;
	}

	@Override
	public int[][][] actionMapping() {
		return actionMapping;
	}

}
