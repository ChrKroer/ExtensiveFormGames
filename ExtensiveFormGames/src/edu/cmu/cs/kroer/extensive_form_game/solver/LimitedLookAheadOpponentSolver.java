package edu.cmu.cs.kroer.extensive_form_game.solver;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import edu.cmu.cs.kroer.extensive_form_game.Game;
import edu.cmu.cs.kroer.extensive_form_game.Game.Action;
import edu.cmu.cs.kroer.extensive_form_game.Game.Node;

public class LimitedLookAheadOpponentSolver extends SequenceFormLPSolver {
	
	double[] nodeEvaluationTable;
	int lookAhead;
	
	IloNumVar[] sequenceDeactivationVars; // Variables denoting whether a given sequence is deactivated
	IloNumVar[] sequenceLookAheadVars;  // Variables denoting the choice of descendant sequences at depth i + k, for some non-deactivated sequence at depth i
	IloNumVar[] informationSetValueVars; // Variables denoting the maximum utility achieved at an information set for the limited look ahead player
	
	IloLinearNumExpr[] evaluationSum; // indexed as [dualSequenceId]. [dualSequenceId] contains a weighted sum over primal sequences that lead to to nodes k levels below the dual sequence, where the weights are the evaluation of the node(s) times the chance probability.
	
	double[] dualUpperBounds; // TODO: compute these, diagonal matrix M for deactivating dual constraints
	double[] primalUpperBounds; // TODO: compute these, diagonal matrix M for deactivating primal incentive constraints
	double maxPayoff;
	
	

	
	public LimitedLookAheadOpponentSolver(Game game, int playerToSolveFor, double[] nodeEvaluationTable, int lookAhead) {
		super(game, playerToSolveFor);
		this.nodeEvaluationTable = nodeEvaluationTable;
		this.lookAhead = lookAhead;
		try {
			setUpModel();
		} catch (IloException e) {
			System.out.println("LimitedLookAheadOpponentSolver error, setUpModel() exception");
			e.printStackTrace();
		}
	}
	
	private void setUpModel() throws IloException {
		initializeDataStructures();
		CreateInformationSetValueVars();
		CreateInformationSetValueConstraints();
		CreateBooleanDualVars();
		ComputeMaxPayoff(game.getRoot());
		AddDualConstraintRemoval();
		AddPrimalIncentiveConstraints();
	}
	
	private void initializeDataStructures() {
		//sequenceDeactivationVars = new IloNumVar[getNumDualSequences()];
		//sequenceLookAheadVars = new IloNumVar[getNumDualSequences()];		
		dualUpperBounds = new double[getNumDualSequences()];
	}
	
	private void CreateInformationSetValueVars() throws IloException {
		String[] names = new String[numDualInformationSets];
		for (int i = 0; i < numDualInformationSets; i++) names[i] = "V" + i;
		informationSetValueVars = cplex.numVarArray(numDualInformationSets, -Double.MAX_VALUE, Double.MAX_VALUE, names);
	}
	
	private void CreateInformationSetValueConstraints() {
		CreateInformationSetValueConstraintsRecursive(game.getRoot(), new TIntHashSet(), new TIntArrayList(), 0);
	}
	
	private void CreateInformationSetValueConstraintsRecursive(int currentNodeId, TIntSet visited, TIntList dualParentSequences, int primalParentSequence) {
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) return;
		
		for (Action action : node.getActions()) {
			if (node.getPlayer() == playerNotToSolveFor && !visited.contains(node.getInformationSet())) {
				int newSequenceId = GetSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
				dualParentSequences.add(newSequenceId);
				CreateInformationSetValueConstraintsRecursive(action.getChildId(), visited, dualParentSequences, primalParentSequence);
				dualParentSequences.removeAt(dualParentSequences.size()-1);
			} else if (node.getPlayer() == playerToSolveFor){
				CreateInformationSetValueConstraintsRecursive(action.getChildId(), visited, dualParentSequences, GetSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName()));
			}			
		}		
		if (node.getPlayer() == playerNotToSolveFor) {
			visited.add(node.getInformationSet());
		}
	}
	
	private void CreateBooleanDualVars() throws IloException {
		String[] namesDeactivationVars = new String[numDualSequences];
		String[] namesLookAheadVars = new String[numDualSequences];
		for (int i = 0; i < numDualSequences; i++) {
			namesDeactivationVars[i] = "D" + i;
			namesDeactivationVars[i] = "I" + i;
		}
		sequenceDeactivationVars = cplex.numVarArray(numDualSequences, -Double.MAX_VALUE, Double.MAX_VALUE, namesDeactivationVars);
		sequenceLookAheadVars = cplex.numVarArray(numDualSequences, -Double.MAX_VALUE, Double.MAX_VALUE, namesLookAheadVars);
	}
	
	private void AddDualConstraintRemoval() throws IloException {
		for (int sequenceId = 0; sequenceId < numDualSequences; sequenceId++) {
			// expr represents the term (-maxPayoff * sequenceDeactivationsVars[sequenceId])
			IloLinearNumExpr expr = cplex.linearNumExpr();
			expr.addTerm(-maxPayoff, sequenceDeactivationVars[sequenceId]);
			// adds the term to the existing dual constraint representing the sequence
			cplex.addToExpr(dualConstraints.get(sequenceId), expr);
		}
	}
	
	/**
	 * Outer method that loops over all informationSetIds and action pairs for the limited look-ahead player.
	 * @throws IloException
	 */
	private void AddPrimalIncentiveConstraints() throws IloException {
		for (int informationSetId = game.getSmallestInformationSetId(playerNotToSolveFor); informationSetId < (numDualInformationSets + game.getSmallestInformationSetId(playerNotToSolveFor)); informationSetId++) {
			// We need to access the set of actions available at the information set. To do this, we loop over node.actions on the (arbitrarily picked) first node in the information set.
			int idOfFirstNodeInSet = game.getInformationSet(playerNotToSolveFor, informationSetId).get(0);
			Node firstNodeInSet = game.getNodeById(idOfFirstNodeInSet);
			// Dynamic programming table for expressions representing dominated actions
			TCustomHashMap<Action, IloLinearNumExpr> dominatedActionExpressionTable = new TCustomHashMap<Action, IloLinearNumExpr>();
			for (Action incentivizedAction : firstNodeInSet.getActions()) {
				IloLinearNumExpr incentiveExpr = GetIncentivizedActionExpression(informationSetId, incentivizedAction);
				for (Action dominatedAction : firstNodeInSet.getActions()) {
					if (!incentivizedAction.equals(dominatedAction)) {
						IloLinearNumExpr dominatedExpr = GetDominatedActionExpression(informationSetId, dominatedAction, dominatedActionExpressionTable);
						// TODO: Add cplex constraint
					}
				}
			}
		}
	}
	
	/**
	 * Using dynamic programming, this method computes an expression representing the value of a given action not chosen to be made optimal. This is done by creating an expression whose value is the sum of the value of descendant information sets under the action, according to the evaulation function and strategy for the rational player
	 * @param informationSetId
	 * @param dominatedAction the action to compute a value expression for
	 * @param dominatedActionExpressionTable dynamic programming table
	 * @return expression that represents the value of the action
	 * @throws IloException
	 */
	private IloLinearNumExpr GetDominatedActionExpression(int informationSetId, Action dominatedAction, TCustomHashMap<Action,IloLinearNumExpr> dominatedActionExpressionTable) throws IloException {
		if (dominatedActionExpressionTable.containsKey(dominatedAction)) {
			return dominatedActionExpressionTable.get(dominatedAction);
		} else {
			IloLinearNumExpr expr = cplex.linearNumExpr();
			TIntObjectMap<IloNumVar> informationSetToVariableMap = new TIntObjectHashMap<IloNumVar>();
			// Iterate over nodeIds in the List of nodes returned by getInformationSet 
			for (int i = 0; i < game.getInformationSet(playerNotToSolveFor, informationSetId).size(); i++) {
				// find the descendant information sets and add their values to the expression
				fillDominatedActionExpr(expr, game.getInformationSet(playerNotToSolveFor, informationSetId).get(i), informationSetToVariableMap);
			}
			// remember the expression for future method calls
			dominatedActionExpressionTable.put(dominatedAction, expr);
			return expr;
		}
	}
	
	/**
	 * Recursive algorithm for filling out value-expressions. Whenever an information set belonging to the limited look-ahead player is hit, an expr is created for each action at the information set, and the value of the information set is set to the max. The action expressions are then recursed on.
	 * @param expr
	 * @param currentNodeId
	 * @param informationSetToVariableMap
	 * @throws IloException
	 */
	private void fillDominatedActionExpr(IloLinearNumExpr expr, int currentNodeId, TIntObjectMap<IloNumVar> informationSetToVariableMap, TIntObjectMap<TCustomHashMap<String, IloLinearNumExpr>> exprMap) throws IloException {
		Node node = game.getNodeById(currentNodeId);
		if (node.getPlayer() == playerNotToSolveFor) {
			if (!informationSetToVariableMap.containsKey(node.getInformationSet())) {
				IloNumVar v = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, "V"+node.getInformationSet());
				informationSetToVariableMap.put(node.getInformationSet(), v);
			} else {
				// TODO
			}
		} else {
			for (Action action : node.getActions()) {
				fillDominatedActionExpr(expr, action.getChildId(), informationSetToVariableMap, exprMap);
			}
		}
	}
	
	private IloLinearNumExpr GetIncentivizedActionExpression(int informationSetId, Action incentivizedAction) throws IloException {
		return null; // TODO
	}
	
	private void ComputeMaxPayoff(int nodeId) {
		Node node = game.getNodeById(nodeId);
		if (node.isLeaf()) {
			if (node.getValue() > maxPayoff) maxPayoff = node.getValue();
			return;
		}

		for (Action action : node.getActions()) {
			ComputeMaxPayoff(action.getChildId());
		}
	}
}
