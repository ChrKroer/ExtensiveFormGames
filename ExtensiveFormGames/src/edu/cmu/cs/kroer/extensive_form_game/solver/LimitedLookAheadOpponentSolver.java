package edu.cmu.cs.kroer.extensive_form_game.solver;

import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import edu.cmu.cs.kroer.extensive_form_game.Game;

public class LimitedLookAheadOpponentSolver extends SequenceFormLPSolver {
	
	IloNumVar[] booleanDualSequenceVars;
	
	/*public LimitedLookAheadOpponentSolver(Game game, int playerToSolveFor) {
		super(game, playerToSolveFor);
	}*/
	
	public LimitedLookAheadOpponentSolver(Game game, int playerToSolveFor, double[] nodeEvaluationTable) {
		super(game, playerToSolveFor);
		try {
			setUpModel();
		} catch (IloException e) {
			System.out.println("LimitedLookAheadOpponentSolver error, setUpModel() exception");
			e.printStackTrace();
		}
	}
	
	private void setUpModel() throws IloException {
		initializeDataStructures();
		CreateBooleanDualVars();
		AddDualConstraintRemoval();
	}
	
	private void initializeDataStructures() {
		booleanDualSequenceVars = new IloNumVar[this.getNumDualSequences()];
	}
	
	private void CreateBooleanDualVars() throws IloException {
		String[] names = new String[numDualSequences];
		for (int i = 0; i < numDualSequences; i++) names[i] = "B" + i;
		booleanDualSequenceVars = cplex.numVarArray(numDualSequences, -Double.MAX_VALUE, Double.MAX_VALUE, names);
	}
	
	private void AddDualConstraintRemoval() {
		
	}
}
