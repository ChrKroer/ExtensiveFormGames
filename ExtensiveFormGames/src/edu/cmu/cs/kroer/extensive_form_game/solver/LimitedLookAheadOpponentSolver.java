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
	double [] maxEvaluationValueForSequence; // [sequenceId] returns the maximum value in the nodeEvaluationTable at depth k over all trees rooted at the sequence
	
	

	
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
		
		
		createInformationSetValueVars();
		createInformationSetValueConstraints();
		createBooleanDualVars();
		computeMaxPayoff(game.getRoot());
		computeMaxEvaluation();
		
		addDualConstraintRemoval();
		addPrimalIncentiveConstraints();
	}
	
	private void initializeDataStructures() {
		//sequenceDeactivationVars = new IloNumVar[getNumDualSequences()];
		//sequenceLookAheadVars = new IloNumVar[getNumDualSequences()];		
		dualUpperBounds = new double[getNumDualSequences()];
		maxEvaluationValueForSequence = new double[getNumDualSequences()];
		for (int dualSequence = 0; dualSequence < getNumDualSequences(); dualSequence++) maxEvaluationValueForSequence[dualSequence] = -Double.MAX_VALUE;
	}
	
	private void createInformationSetValueVars() throws IloException {
		String[] names = new String[numDualInformationSets];
		for (int i = 0; i < numDualInformationSets; i++) names[i] = "V" + i;
		informationSetValueVars = cplex.numVarArray(numDualInformationSets, -Double.MAX_VALUE, Double.MAX_VALUE, names);
	}
	
	private void createInformationSetValueConstraints() {
		createInformationSetValueConstraintsRecursive(game.getRoot(), new TIntHashSet(), new TIntArrayList(), 0);
	}
	
	private void createInformationSetValueConstraintsRecursive(int currentNodeId, TIntSet visited, TIntList dualParentSequences, int primalParentSequence) {
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) return;
		
		for (Action action : node.getActions()) {
			if (node.getPlayer() == playerNotToSolveFor && !visited.contains(node.getInformationSet())) {
				int newSequenceId = getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
				dualParentSequences.add(newSequenceId);
				createInformationSetValueConstraintsRecursive(action.getChildId(), visited, dualParentSequences, primalParentSequence);
				dualParentSequences.removeAt(dualParentSequences.size()-1);
			} else if (node.getPlayer() == playerToSolveFor){
				createInformationSetValueConstraintsRecursive(action.getChildId(), visited, dualParentSequences, getSequenceIdForPlayerToSolveFor(node.getInformationSet(), action.getName()));
			}			
		}		
		if (node.getPlayer() == playerNotToSolveFor) {
			visited.add(node.getInformationSet());
		}
	}
	
	private void createBooleanDualVars() throws IloException {
		String[] namesDeactivationVars = new String[numDualSequences];
		String[] namesLookAheadVars = new String[numDualSequences];
		for (int i = 0; i < numDualSequences; i++) {
			namesDeactivationVars[i] = "D" + i;
			namesDeactivationVars[i] = "I" + i;
		}
		sequenceDeactivationVars = cplex.numVarArray(numDualSequences, -Double.MAX_VALUE, Double.MAX_VALUE, namesDeactivationVars);
		sequenceLookAheadVars = cplex.numVarArray(numDualSequences, -Double.MAX_VALUE, Double.MAX_VALUE, namesLookAheadVars);
	}
	
	private void addDualConstraintRemoval() throws IloException {
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
	private void addPrimalIncentiveConstraints() throws IloException {
		for (int informationSetId = game.getSmallestInformationSetId(playerNotToSolveFor); informationSetId < (numDualInformationSets + game.getSmallestInformationSetId(playerNotToSolveFor)); informationSetId++) {
			// We need to access the set of actions available at the information set. To do this, we loop over node.actions on the (arbitrarily picked) first node in the information set.
			int idOfFirstNodeInSet = game.getInformationSet(playerNotToSolveFor, informationSetId).get(0);
			Node firstNodeInSet = game.getNodeById(idOfFirstNodeInSet);
			// Dynamic programming table for expressions representing dominated actions
			TCustomHashMap<Action, IloLinearNumExpr> dominatedActionExpressionTable = new TCustomHashMap<Action, IloLinearNumExpr>();
			for (Action incentivizedAction : firstNodeInSet.getActions()) {
				IloLinearNumExpr incentiveExpr = getIncentivizedActionExpression(informationSetId, incentivizedAction);
				for (Action dominatedAction : firstNodeInSet.getActions()) {
					if (!incentivizedAction.equals(dominatedAction)) {
						IloLinearNumExpr dominatedExpr = getDominatedActionExpression(informationSetId, dominatedAction, dominatedActionExpressionTable);
						// TODO: Add cplex constraint, incentiveExpr >= dominatedExpr
						cplex.addGe(incentiveExpr, dominatedExpr);
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
	private IloLinearNumExpr getDominatedActionExpression(int informationSetId, Action dominatedAction, TCustomHashMap<Action,IloLinearNumExpr> dominatedActionExpressionTable) throws IloException {
		if (dominatedActionExpressionTable.containsKey(dominatedAction)) {
			return dominatedActionExpressionTable.get(dominatedAction);
		} else {
			// this expression represents the value of dominatedAction
			IloLinearNumExpr expr = cplex.linearNumExpr();
			TIntObjectMap<IloNumVar> informationSetToVariableMap = new TIntObjectHashMap<IloNumVar>();
			TIntObjectMap<TCustomHashMap<String, IloLinearNumExpr>> exprMap = new TIntObjectHashMap<TCustomHashMap<String, IloLinearNumExpr>>();
			
			// Iterate over nodes in information set and add value of each node for dominatedAction 
			for (int i = 0; i < game.getInformationSet(playerNotToSolveFor, informationSetId).size(); i++) {
				// find the descendant information sets and add their values to the expression
				fillDominatedActionExpr(expr, game.getInformationSet(playerNotToSolveFor, informationSetId).get(i), informationSetToVariableMap, exprMap, 0);
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
	private void fillDominatedActionExpr(IloLinearNumExpr actionExpr, int currentNodeId, TIntObjectMap<IloNumVar> informationSetToVariableMap, TIntObjectMap<TCustomHashMap<String, IloLinearNumExpr>> exprMap, int depth) throws IloException {
		Node node = game.getNodeById(currentNodeId);
		if (depth == lookAhead || node.isLeaf()) {
			addLookAheadDepthEvaluationValueToExpression(actionExpr, currentNodeId);
			return;
		}
		
		

		if (node.getPlayer() == playerNotToSolveFor && !informationSetToVariableMap.containsKey(node.getInformationSet())) {
			// Create information set var and action expressions
			IloNumVar informationSetValueVar = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, "V"+node.getInformationSet());
			// Add information set value var to the expression describing the value of the parent action
			actionExpr.addTerm(1, informationSetValueVar);
			informationSetToVariableMap.put(node.getInformationSet(), informationSetValueVar);
			TCustomHashMap<String,IloLinearNumExpr> actionMap = new TCustomHashMap<String,IloLinearNumExpr>();
			for (Action action : node.getActions()) {
				IloLinearNumExpr newActionExpr = cplex.linearNumExpr();
				// Require information set value var to be >= value of each action
				cplex.addGe(informationSetValueVar, newActionExpr);
				actionMap.put(action.getName(), newActionExpr);
			}
			exprMap.put(node.getInformationSet(), actionMap);

		}

		for (Action action : node.getActions()) {
			IloLinearNumExpr newActionExpr = node.getPlayer() == playerNotToSolveFor ? exprMap.get(node.getInformationSet()).get(action.getName()) : actionExpr;
			fillDominatedActionExpr(newActionExpr, action.getChildId(), informationSetToVariableMap, exprMap, depth+1);
		}
	}
	
	/**
	 * For the given action, this method computes an IloLinearNumExpr representing the value of the action over all nodes in the information set, as a function of the rational player's strategy
	 * @param informationSetId
	 * @param incentivizedAction
	 * @return
	 * @throws IloException
	 */
	private IloLinearNumExpr getIncentivizedActionExpression(int informationSetId, Action incentivizedAction) throws IloException {
		IloLinearNumExpr actionExpr = cplex.linearNumExpr(); 
		// Iterate over nodes in information set
		TIntObjectMap<TCustomHashMap<String, IloLinearNumExpr>> exprMap = new TIntObjectHashMap<TCustomHashMap<String, IloLinearNumExpr>>();
		for (int i = 0; i < game.getInformationSet(playerNotToSolveFor, informationSetId).size(); i++) {
			fillIncentivizedActionExpr(actionExpr, game.getRoot(), exprMap, 0);
		}
		return actionExpr;
	}
	
	/**
	 * Recursive helper method for getIncentivizedActionExpression
	 * @param actionExpr
	 * @param currentNodeId
	 * @param exprMap
	 * @param depth
	 * @throws IloException
	 */
	private void fillIncentivizedActionExpr(IloLinearNumExpr actionExpr, int currentNodeId, TIntObjectMap<TCustomHashMap<String, IloLinearNumExpr>> exprMap, int depth) throws IloException {
		Node node = game.getNodeById(currentNodeId);
		if (depth == lookAhead || node.isLeaf()) {
			addLookAheadDepthEvaluationValueToExpression(actionExpr, currentNodeId);
			return;
		}
		
		if (node.getPlayer() == playerNotToSolveFor && !exprMap.containsKey(node.getInformationSet())) {
			TCustomHashMap<String,IloLinearNumExpr> actionMap = new TCustomHashMap<String,IloLinearNumExpr>();
			IloLinearNumExpr sum = cplex.linearNumExpr();
			for (Action action : node.getActions()) {
				IloNumVar actionActiveVar = cplex.boolVar();
				// actionActiveVars should sum to 1
				sum.addTerm(1, actionActiveVar);
				// actionValueVar represents the value of the action according to the heuristic evaluation k steps ahead
				IloNumVar actionValueVar = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, "V"+node.getInformationSet());
				actionExpr.addTerm(1, actionValueVar);
				
				// Expression that takes on value 0 or M, where M is a sufficiently large constant to enable the full value of the action when active
				IloLinearNumExpr valueExpr = cplex.linearNumExpr();
				valueExpr.addTerm(maxEvaluationValueForSequence[getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName())], actionActiveVar);
				
				// Force the real-valued actionValueVar to take on a non-zero value only if actionActiveVar == 1
				cplex.addLe(actionValueVar, valueExpr);
				
				// Create a new actionExpr to recurse on and let actionValueVar be bounded by its value  
				IloLinearNumExpr newActionExpr = cplex.linearNumExpr();
				cplex.addLe(actionValueVar, newActionExpr);
				actionMap.put(action.getName(), newActionExpr);
			}
			// actionActiveVars should sum to 1
			cplex.addEq(sum, 1);
			exprMap.put(node.getInformationSet(), actionMap);
		}
		for (Action action : node.getActions()) {
			IloLinearNumExpr newActionExpr = node.getPlayer() == playerNotToSolveFor ? exprMap.get(node.getInformationSet()).get(action.getName()) : actionExpr;
			fillIncentivizedActionExpr(newActionExpr, action.getChildId(), exprMap, depth+1);
		}
	}

	private void addLookAheadDepthEvaluationValueToExpression(IloLinearNumExpr expr, int currentNodeId) throws IloException {
			int sequenceIdForRationalPlayer = playerToSolveFor == 1? sequenceIdForNodeP1[currentNodeId] : sequenceIdForNodeP2[currentNodeId];
			// The heuristic value of a node for the limited look-ahead player is the evaluationTable value, weighted by probability of reaching the node, over both nature and the rational player 
			expr.addTerm(nodeNatureProbabilities[currentNodeId] * nodeEvaluationTable[currentNodeId], strategyVarsBySequenceId[sequenceIdForRationalPlayer]);
	}
	
	private void computeMaxPayoff(int nodeId) {
		Node node = game.getNodeById(nodeId);
		if (node.isLeaf()) {
			if (node.getValue() > maxPayoff) maxPayoff = node.getValue();
			return;
		}

		for (Action action : node.getActions()) {
			computeMaxPayoff(action.getChildId());
		}
	}

	private void computeMaxEvaluation() {
		for (int informationSetId = 0; informationSetId < this.numDualInformationSets; informationSetId++) {
			for (int i = 0; i < game.getInformationSet(playerNotToSolveFor, informationSetId).size(); i++) {
				Node node = game.getNodeById(game.getInformationSet(playerNotToSolveFor, informationSetId).get(i));
				for (Action action : node.getActions()) {
					computeMaxEvaluationForAction(node.getNodeId(), getSequenceIdForPlayerNotToSolveFor(informationSetId, action.getName()), 0);
				}
			}
		}
	}
	
	private void computeMaxEvaluationForAction(int nodeId, int sequenceId, int depth) {
		Node node = game.getNodeById(nodeId);
		if (depth == lookAhead || node.isLeaf()) {
			if (nodeEvaluationTable[nodeId] > maxEvaluationValueForSequence[sequenceId]) maxEvaluationValueForSequence[sequenceId] = nodeEvaluationTable[nodeId];
			return;
		}

		for (Action action : node.getActions()) {
			computeMaxEvaluationForAction(action.getChildId(), sequenceId, depth+1);
		}
	}

}
