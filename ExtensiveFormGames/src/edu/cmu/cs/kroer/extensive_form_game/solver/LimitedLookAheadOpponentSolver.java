package edu.cmu.cs.kroer.extensive_form_game.solver;

import java.util.HashMap;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex.UnknownObjectException;
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
	double minPayoff;
	double [] maxEvaluationValueForSequence; // [sequenceId] returns the maximum value in the nodeEvaluationTable at depth k over all trees rooted at the sequence
	double epsilon = 0.001;
	
	

	
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
		
		computeAuxiliaryInformationForNodes();
		
		createInformationSetValueVars();
		createInformationSetValueConstraints();
		createBooleanDualVars();
		computeMaxMinPayoff(game.getRoot());
		computeMaxEvaluation();
		
		addDualConstraintRemoval();
		addPrimalIncentiveConstraints();
		cplex.addEq(sequenceDeactivationVars[0], 0);
		CreateDeactivationSequenceFormConstraints(game.getRoot(), sequenceDeactivationVars[0], new TIntHashSet());
	}
	
	private void initializeDataStructures() {
		maxPayoff = -Double.MAX_VALUE;
		minPayoff = Double.MAX_VALUE;
		//sequenceDeactivationVars = new IloNumVar[getNumDualSequences()];
		//sequenceLookAheadVars = new IloNumVar[getNumDualSequences()];		
		dualUpperBounds = new double[getNumDualSequences()];
		maxEvaluationValueForSequence = new double[getNumDualSequences()];
		for (int dualSequence = 0; dualSequence < getNumDualSequences(); dualSequence++) maxEvaluationValueForSequence[dualSequence] = -Double.MAX_VALUE;
	}
	
	public void printSequenceActivationValues() throws UnknownObjectException, IloException {
		for (int i = 1; i < numDualSequences; i++) {
			System.out.println("D" + i + " = " + cplex.getValue(sequenceDeactivationVars[i]));
		}
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
			namesLookAheadVars[i] = "T" + i;
		}
		sequenceDeactivationVars = cplex.boolVarArray(numDualSequences, namesDeactivationVars);
		sequenceLookAheadVars = cplex.boolVarArray(numDualSequences, namesLookAheadVars);
	}
	
	private void CreateDeactivationSequenceFormConstraints(int currentNodeId, IloNumVar parentSequence, TIntSet visited) throws IloException{
		Node node = game.getNodeById(currentNodeId);
		if (node.isLeaf()) return;
		
		if (node.getPlayer() == playerNotToSolveFor && !visited.contains(node.getInformationSet())) {
			visited.add(node.getInformationSet());
			IloLinearNumExpr sum = cplex.linearNumExpr();
			//sum.addTerm(-1, parentSequence);
			for (Action action : node.getActions()) {
				// real-valued variable in (0,1)
				int sequenceId = getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
				IloNumVar v = sequenceDeactivationVars[sequenceId];
				// add 1*v to the sum over all the sequences at the information set
				sum.addTerm(1, v);
				CreateDeactivationSequenceFormConstraints(action.getChildId(), v, visited);
			}
			// if parent sequence is deactived, it subtracts one from the sum
			sum.addTerm(-1, parentSequence);
			// if parent sequence is NOT deactivated, this requires at least one action to not be deactivated
			cplex.addLe(sum, node.getActions().length - 1);
		} else {
			for (Action action : node.getActions()) {
				if (node.getPlayer() == playerNotToSolveFor) {
					// update parentSequence to be the current sequence
					int sequenceId = getSequenceIdForPlayerNotToSolveFor(node.getInformationSet(), action.getName());
					IloNumVar v = sequenceDeactivationVars[sequenceId]; 
					CreateDeactivationSequenceFormConstraints(action.getChildId(), v, visited);
				} else {
					CreateDeactivationSequenceFormConstraints(action.getChildId(), parentSequence, visited);
				}
			}
		}
	}

	
	private void addDualConstraintRemoval() throws IloException {
		// Start at 1, we do not want to deactivate the empty sequence
		for (int sequenceId = 1; sequenceId < numDualSequences; sequenceId++) {
			// expr represents the term (-maxPayoff * sequenceDeactivationsVars[sequenceId])
			IloLinearNumExpr expr = cplex.linearNumExpr();
			double biggestDifferential = maxPayoff - minPayoff;
			expr.addTerm(biggestDifferential, sequenceDeactivationVars[sequenceId]);
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
			HashMap<Action, IloLinearNumExpr> dominatedActionExpressionTable = new HashMap<Action, IloLinearNumExpr>();
			for (Action incentivizedAction : firstNodeInSet.getActions()) {
				// get expr representing value of chosen action
				IloLinearNumExpr incentiveExpr = getIncentivizedActionExpression(informationSetId, incentivizedAction);
				
				IloLinearNumExpr incentiveConstraintLHS = cplex.linearNumExpr();
				incentiveConstraintLHS.add(incentiveExpr);
				
				int incentiveSequenceId = getSequenceIdForPlayerToSolveFor(informationSetId, incentivizedAction.getName());
				// ensure that expression is only active if the sequence is chosen
				incentiveConstraintLHS.addTerm(maxEvaluationValueForSequence[incentiveSequenceId]+this.epsilon, sequenceDeactivationVars[incentiveSequenceId]);
		
				
				for (Action dominatedAction : firstNodeInSet.getActions()) {
					if (!incentivizedAction.equals(dominatedAction)) {
						// get expr representing value of dominated action
						IloLinearNumExpr dominatedExpr = getDominatedActionExpression(informationSetId, dominatedAction, dominatedActionExpressionTable);
						IloLinearNumExpr incentiveConstraintRHS = cplex.linearNumExpr();
						incentiveConstraintRHS.add(dominatedExpr);
						
						int dominatedSequenceId = getSequenceIdForPlayerToSolveFor(informationSetId, dominatedAction.getName());
						// ensure that expression is only active is the sequence is deactivated
						incentiveConstraintRHS.addTerm(maxEvaluationValueForSequence[dominatedSequenceId], sequenceDeactivationVars[dominatedSequenceId]);
						// TODO: something needs to be flipped here. Perhaps it needs to be LB of other side
						incentiveConstraintRHS.setConstant(this.epsilon - maxEvaluationValueForSequence[dominatedSequenceId]);
						cplex.addGe(incentiveConstraintLHS, incentiveConstraintRHS, "PrimalIncentive("+informationSetId+";"+incentivizedAction.getName()+";"+dominatedAction.getName()+")");
						
						// add constraint requiring equality if both sequences are chosen to be incentivized, here we add one side of the two weak inequalities enforcing equality, since the other direction is also iterated over
						IloLinearNumExpr equalityRHS = cplex.linearNumExpr();
						equalityRHS.add(incentiveConstraintRHS);
						equalityRHS.addTerm(0.5*maxEvaluationValueForSequence[dominatedSequenceId], sequenceDeactivationVars[dominatedSequenceId]);
						equalityRHS.addTerm(0.5*maxEvaluationValueForSequence[dominatedSequenceId], sequenceDeactivationVars[incentiveSequenceId]);
						cplex.addGe(incentiveExpr, equalityRHS, "quality("+informationSetId+";"+incentivizedAction.getName()+";"+dominatedAction.getName()+")");
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
	private IloLinearNumExpr getDominatedActionExpression(int informationSetId, Action dominatedAction, HashMap<Action,IloLinearNumExpr> dominatedActionExpressionTable) throws IloException {
		if (dominatedActionExpressionTable.containsKey(dominatedAction)) {
			return dominatedActionExpressionTable.get(dominatedAction);
		} else {
			// this expression represents the value of dominatedAction
			IloLinearNumExpr expr = cplex.linearNumExpr();
			TIntObjectMap<IloNumVar> informationSetToVariableMap = new TIntObjectHashMap<IloNumVar>();
			TIntObjectMap<HashMap<String, IloLinearNumExpr>> exprMap = new TIntObjectHashMap<HashMap<String, IloLinearNumExpr>>();
			
			// Iterate over nodes in information set and add value of each node for dominatedAction
			TIntArrayList informationSet = game.getInformationSet(playerNotToSolveFor, informationSetId);
			for (int i = 0; i < informationSet.size(); i++) {
				Node node = game.getNodeById(informationSet.get(i));
				// we need to locate the action that corresponds to dominatedAction at the current node, so that we can pull the correct childId
				Action dominatedActionForNode = node.getActions()[0];
				for (Action action : node.getActions()) {
					if (action.getName().equals(dominatedAction.getName())) dominatedActionForNode = action;
				}

				// find the descendant information sets and add their values to the expression
				fillDominatedActionExpr(expr, dominatedActionForNode.getChildId(), informationSetToVariableMap, exprMap, 1, dominatedAction);
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
	private void fillDominatedActionExpr(IloLinearNumExpr actionExpr, int currentNodeId, TIntObjectMap<IloNumVar> informationSetToVariableMap, TIntObjectMap<HashMap<String, IloLinearNumExpr>> exprMap, int depth, Action dominatedAction) throws IloException {
		Node node = game.getNodeById(currentNodeId);
		if (depth == lookAhead || node.isLeaf()) {
			addLookAheadDepthEvaluationValueToExpression(actionExpr, currentNodeId);
			return;
		}
		
		

		if (node.getPlayer() == playerNotToSolveFor && !informationSetToVariableMap.containsKey(node.getInformationSet())) {
			// Create information set var and action expressions
			IloNumVar informationSetValueVar = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, "Dom;"+dominatedAction.getName()+"("+node.getInformationSet()+";"+currentNodeId+")");
			// Add information set value var to the expression describing the value of the parent action
			actionExpr.addTerm(1, informationSetValueVar);
			informationSetToVariableMap.put(node.getInformationSet(), informationSetValueVar);
			HashMap<String,IloLinearNumExpr> actionMap = new HashMap<String,IloLinearNumExpr>();
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
			fillDominatedActionExpr(newActionExpr, action.getChildId(), informationSetToVariableMap, exprMap, depth+1, dominatedAction);
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
		int sequenceId = getSequenceIdForPlayerNotToSolveFor(informationSetId, incentivizedAction.getName());
		
		// Iterate over nodes in information set
		double maxHeuristicAtNode = 0;
		TIntObjectMap<HashMap<String, IloLinearNumExpr>> exprMap = new TIntObjectHashMap<HashMap<String, IloLinearNumExpr>>();
		TIntArrayList informationSet = game.getInformationSet(playerNotToSolveFor, informationSetId); 
		for (int i = 0; i < informationSet.size(); i++) {
			Node node = game.getNodeById(informationSet.get(i));
			// ensure that we are using the correct Action at the node, so we can pull the correct childId
			Action incentiveActionForNode = node.getActions()[0];
			for (Action action : node.getActions()) {
				if (action.getName().equals(incentivizedAction.getName())) incentiveActionForNode = action;
			}
			fillIncentivizedActionExpr(actionExpr, incentiveActionForNode.getChildId(), exprMap, 1, incentivizedAction);
			if (maxEvaluationValueForSequence[sequenceId] > maxHeuristicAtNode) {
				maxHeuristicAtNode = maxEvaluationValueForSequence[sequenceId];
			}
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
	private void fillIncentivizedActionExpr(IloLinearNumExpr actionExpr, int currentNodeId, TIntObjectMap<HashMap<String, IloLinearNumExpr>> exprMap, int depth, Action incentivizedAction) throws IloException {
		Node node = game.getNodeById(currentNodeId);
		if (depth == lookAhead || node.isLeaf()) {
			addLookAheadDepthEvaluationValueToExpression(actionExpr, currentNodeId);
			return;
		}
		
		if (node.getPlayer() == playerNotToSolveFor && !exprMap.containsKey(node.getInformationSet())) {
			HashMap<String,IloLinearNumExpr> actionMap = new HashMap<String,IloLinearNumExpr>();
			IloLinearNumExpr sum = cplex.linearNumExpr();
			for (Action action : node.getActions()) {
				IloNumVar actionActiveVar = cplex.boolVar();
				// actionActiveVars should sum to 1
				sum.addTerm(1, actionActiveVar);
				// actionValueVar represents the value of the action according to the heuristic evaluation k steps ahead
				IloNumVar actionValueVar = cplex.numVar(-Double.MAX_VALUE, Double.MAX_VALUE, "Inc;"+incentivizedAction.getName()+"("+node.getInformationSet()+";"+action.getName()+")");
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
			fillIncentivizedActionExpr(newActionExpr, action.getChildId(), exprMap, depth+1, incentivizedAction);
		}
	}

	private void addLookAheadDepthEvaluationValueToExpression(IloLinearNumExpr expr, int currentNodeId) throws IloException {
			int sequenceIdForRationalPlayer = playerToSolveFor == 1? sequenceIdForNodeP1[currentNodeId] : sequenceIdForNodeP2[currentNodeId];
			// The heuristic value of a node for the limited look-ahead player is the evaluationTable value, weighted by probability of reaching the node, over both nature and the rational player 
			expr.addTerm(nodeNatureProbabilities[currentNodeId] * nodeEvaluationTable[currentNodeId], strategyVarsBySequenceId[sequenceIdForRationalPlayer]);
	}
	
	private void computeMaxMinPayoff(int nodeId) {
		Node node = game.getNodeById(nodeId);
		if (node.isLeaf()) {
			if (node.getValue() > maxPayoff) {
				maxPayoff = node.getValue();
			}
			if (node.getValue() < minPayoff) {
				minPayoff = node.getValue();
			}
			return;
		}

		for (Action action : node.getActions()) {
			computeMaxMinPayoff(action.getChildId());
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
