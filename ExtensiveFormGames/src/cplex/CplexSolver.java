package cplex;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.cplex.IloCplex;

public abstract class CplexSolver {
	protected IloCplex cplex;
	protected IloLinearNumExpr objective;
	// The amount of error we allow Cplex in how to interpret values of boolean variables
	protected double cplexEpsilon = 0.001;
	
	public CplexSolver() {
		try {
			cplex = new IloCplex();
			objective = cplex.linearNumExpr();
		} catch (IloException e) {
			System.out.println("Error CplexSolver(): CPLEX setup failed");
		}

	}

	/**
	 * Solves the model. 
	 */
	public void solveModel() {
		try {
			cplex.solve();
		} catch (IloException e) {
			e.printStackTrace();
		}
	}

	
	/**
	 * Writes the current model to a file. CPLEX throws an exception if the model is faulty or the path does not exist.
	 * @param filename the absolute path to the file being written to
	 */
	public void writeModelToFile(String filename) {
		try {
			cplex.exportModel(filename);
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Returns the objective value of the CPLEX object
	 * @return
	 */
	public double getObjectiveValue() {
		try {
			return cplex.getObjValue();
		} catch (IloException e) {
			e.printStackTrace();
			return -1;
		}
	}

}
