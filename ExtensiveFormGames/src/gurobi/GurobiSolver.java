package gurobi;

public abstract class GurobiSolver {
	protected GRBEnv env;
	protected GRBModel model;
	protected GRBLinExpr objective;
	// The amount of error we allow Cplex in how to interpret values of boolean variables
	protected double cplexEpsilon = 0.001;
	
	public GurobiSolver() {
		try {
			env = new GRBEnv("mip.log");
			model = new GRBModel(env);
			objective = new GRBLinExpr();
			setGRBParameters();
		} catch (GRBException e) {
            e.printStackTrace();
            System.out.println("Error GurobiSolver(): Gurobi setup failed");
        }

    }

	/**
	 * Solves the model. 
	 */
	public void solveModel() {
		try {
			model.optimize();
		} catch (GRBException e) {
            e.printStackTrace();
        }
    }

	
	/**
	 * Writes the current model to a file. CPLEX throws an exception if the model is faulty or the path does not exist.
	 * @param filename the absolute path to the file being written to
	 */
	public void writeModelToFile(String filename) {
		try {
			model.write(filename);
		} catch (GRBException e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * Returns the objective value of the CPLEX object
	 * @return
	 */
	public double getObjectiveValue() {
		try {
			return model.get(GRB.DoubleAttr.ObjVal);
		} catch (GRBException e) {
            e.printStackTrace();
            return -1;
        }
    }

	private void setGRBParameters() throws GRBException {
//		cplex.setParam(IloCplex.IntParam.SimDisplay, 0);
//		cplex.setParam(IloCplex.IntParam.MIPDisplay, 0);
//		cplex.setParam(IloCplex.IntParam.MIPInterval, -1);
//		cplex.setParam(IloCplex.IntParam.TuningDisplay, 0);
//		cplex.setParam(IloCplex.IntParam.BarDisplay, 0);
//		cplex.setParam(IloCplex.IntParam.SiftDisplay, 0);
//		cplex.setParam(IloCplex.IntParam.ConflictDisplay, 0);
//		cplex.setParam(IloCplex.IntParam.NetDisplay, 0);
//		cplex.setParam(IloCplex.DoubleParam.TiLim, 1e+75);
	}

}
