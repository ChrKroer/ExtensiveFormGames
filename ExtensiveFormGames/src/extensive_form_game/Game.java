package extensive_form_game;

import au.com.bytecode.opencsv.CSVReader;
import extensive_form_game_abstraction.SignalAbstraction;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Game implements GameGenerator {
	public class Action {
		private String name;
		private int childId; // id of the node lead to by taking this action
		private double probability;
		private int rem; // Legacy from zerosum package. Denotes the probability as an integer. We only use this to set the probability field
		public String getName() {
			return name;
		}
		public int getChildId() {
			return childId;
		}
		public double getProbability() {
			return probability;
		}
		
		public boolean equals(Action action) {
			if (name.equals(action.name)) return true;
			else return false;
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
		@Override
		public String toString() {
			return name;
		}
	}
	public class Node {
		private int nodeId;
		private String name;
		private int player; // -2 is leaf, 0 is nature, positive integers are actual players
		private boolean publicSignal; // TODO, useful for abstraction algorithms
		private int playerReceivingSignal; // TODO, useful for abstraction algorithms
		private int informationSet;
		private int abstractInformationSet;
		private int signalGroupPlayer1; // TODO, useful for abstraction algorithms
		private int signalGroupPlayer2; // TODO, useful for abstraction algorithms
		private Action[] actions; 
		private double value; // payoff at node, if node is a leaf node (player == -2) 
		public int getNodeId() {
			return nodeId;
		}
		public String getName() {
			return name;
		}
		public int getPlayer() {
			return player;
		}
		public boolean isPublicSignal() {
			return publicSignal;
		}
		public int getPlayerReceivingSignal() {
			return playerReceivingSignal;
		}
		public int getInformationSet() {
			return informationSet;
		}
		public int getRealInformationSet() {
			return informationSet;
		}
		public int getAbstractInformationSet() {
			return abstractInformationSet;
		}
		public void setAbstractInformationSet(int abstractInformationSet) {
			this.abstractInformationSet = abstractInformationSet;
		}
		public int getSignalGroupPlayer1() {
			return signalGroupPlayer1;
		}
		public int getSignalGroupPlayer2() {
			return signalGroupPlayer2;
		}
		public Action[] getActions() {
			return actions;
		}
		public double getValue() {
			return value;
		}
		public boolean isLeaf() {
			return player == -2;
		}
		@Override
		public String toString() {
			return name;
		}
	}
	
	int player1 = 0;
	int player2 = 1;
	
	private TIntArrayList [] [] informationSets; // indexed as [player][information set]
	private boolean [] [] informationSetsSeen; // indexed as [player]
	private Node [] nodes;
	private TIntIntMap [] childNodeIdBySignalId; // indexed as [nodeId][signalId], returns the child node reached when nature selects the signal
	private TIntIntMap [] actionIdBySignalId;// indexed as [nodeId][signalId], returns the index of the signal in the action vector at the node
	@SuppressWarnings("unchecked")
	private HashMap<List<String>, Integer>[] observedActionsToInformationSetId = new HashMap[3];
	
	private int root;
	private int numChanceHistories;
	private int numCombinedPlayerHistories;
	private int numTerminalHistories;
	private int numNodes;
	private int numInformationSetsPlayer1;
	private int numInformationSetsPlayer2;
	private int smallestInformationSetId[]; // indexed as [player], keeps track of the base index for the information sets
	private int [] numSequences;
	
	private String [] signals; // TODO
	private TObjectIntMap<String> signalNameToId; // TODO signalName refers to a unique name for each signal in the set S of all signals dealt by nature. 
	private int numRounds;
	private int depth;
	private int numPrivateSignals;
	
	private double smallestPayoff;
	private double biggestPayoff;
	
	private boolean hasAbstraction;
	private int[][] abstraction; 
	private int[][][] actionAbstractionMapping;
	private boolean useIdentityActionMap;

	SignalAbstraction signalAbstraction;
	
	public Game() {
		informationSets = new TIntArrayList [2] [];
		informationSetsSeen = new boolean [2] [];
		signalNameToId = new TObjectIntHashMap<String>();
		numSequences = new int[2];
		numSequences[0] = 1;
		numSequences[1] = 1;
		smallestInformationSetId = new int[2];
		smallestInformationSetId[0] = Integer.MAX_VALUE;
		smallestInformationSetId[1] = Integer.MAX_VALUE;
		smallestPayoff = Double.MAX_VALUE;
		biggestPayoff = -Double.MAX_VALUE;
		observedActionsToInformationSetId[1] = new HashMap<List<String>, Integer>();
		observedActionsToInformationSetId[2] = new HashMap<List<String>, Integer>();
	}
	
	public Game(String filename) {
		this();
		createGameFromFile(filename);
	}
	
	public void applySignalAbstraction(SignalAbstraction signalAbstraction) {
		for (int nodeId = 0; nodeId < getNumNodes(); nodeId++) {
			Node node = getNodeById(nodeId);
			if (node.getPlayer() == 1 || node.getPlayer() == 2) {
				// Get observed nature actions, then concatenate observed player actions
				List<String> observedActions = extractObservedNatureActionsFromNodeName(node.getName(), node.getPlayer());
				observedActions.addAll(extractObservedPlayerActionsFromNodeName(node.getName(), node.getPlayer()));
				// Uniquely identify each information set by the (out of order) list of observed actions. This works for signal-decomposable games.
				observedActionsToInformationSetId[node.getPlayer()].put(observedActions, node.getInformationSet());
			}
		}
		this.signalAbstraction = signalAbstraction;
		abstraction = new int[3][];
		abstraction[1] = new int[getNumInformationSetsPlayer1()];
		abstraction[2]= new int[getNumInformationSetsPlayer2()];
		applySignalAbstractionRecursive(getRoot(), new ArrayList<Integer>());
		hasAbstraction = true;
		useIdentityActionMap = true;
	}
	
	private void applySignalAbstractionRecursive(int currentNodeId, List<Integer> natureIndices) {
		Node node = getNodeById(currentNodeId);
		if (node.isLeaf()) {
			return;
		}
		
		if (node.getPlayer() == 1 || node.getPlayer() ==2) {
			List<String> natureSignals = extractObservedNatureActionsFromNodeName(node.getName(), node.getPlayer());
			List<String> abstractNatureSignals = signalAbstraction.getAbstractSignalsByName(natureSignals);
			List<String> observedPlayerActions = extractObservedPlayerActionsFromNodeName(node.getName(), node.getPlayer());
			// Make abstract observed list
			List<String> abstractActions = new ArrayList<String>(abstractNatureSignals);
			abstractActions.addAll(observedPlayerActions);
			int abstractInformationSetId = observedActionsToInformationSetId[node.getPlayer()].get(abstractActions);
			node.setAbstractInformationSet(abstractInformationSetId);
			if (node.getInformationSet() != abstractInformationSetId) {
				int t = 1;
			}
			abstraction[node.getPlayer()][node.getInformationSet()] = abstractInformationSetId;
		}
		for (Action action : node.getActions()) {
			if (node.getPlayer() == 0) {
				natureIndices.add(depth);
				applySignalAbstractionRecursive(action.getChildId(), natureIndices);
				natureIndices.remove(natureIndices.size()-1);
			} else {
				applySignalAbstractionRecursive(action.getChildId(), natureIndices);
			}			
		}
	}
	
	public void createGameFromFileZerosumPackageFormat(String filename) {
		//BufferedReader in = null;
		CSVReader in = null;
		try {
			in= new CSVReader(new FileReader(filename), ' ', '\'');
			//in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("Game::CreateGameFromFile: File not found");
			System.out.println("filename: " + filename);
			System.exit(0);
		}
		
		String[] splitLine;
		try {
			while ((splitLine = in.readNext()) != null) {
				// splitLine may contain empty strings. We filter these out here. This should preferably be handled by a library function. 
				List<String> filtered = new ArrayList<String>();
				for(String s : splitLine) {
					if(!s.equals("")) {
						filtered.add(s);
					}
				}
				splitLine = filtered.toArray(new String[0]);
				
				if (splitLine.length == 0) {
					continue;
				} else if (splitLine[0].equals("#")) {
					continue;
				} else if (!StringUtils.isNumeric(splitLine[0])) {
					readGameInfoLine(splitLine);
				} else {
					if (splitLine.length == 3) {
						CreateLeafNode(splitLine);
					} else if (Integer.parseInt(splitLine[0]) < numChanceHistories) {
						CreateZeroSumPackageStyleNatureNode(splitLine);
					} else {
						CreatePlayerNode(splitLine);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Game::CreateGameFromFile: Read exception");
		}
		
	}
	
	
	public void createGameFromFile(String filename) {
		//BufferedReader in = null;
		CSVReader in = null;
		try {
			in= new CSVReader(new FileReader(filename), ' ', '\'');
			//in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("Game::CreateGameFromFile: File not found");
			System.out.println("filename: " + filename);
			System.exit(0);
		}
		
		String[] splitLine;
		try {
			while ((splitLine = in.readNext()) != null) {
				if (splitLine[0].equals("#")) {
					continue;
				} else if (splitLine[0].equals("game info")) {
					readGameInfoLine(splitLine);
				} else if (splitLine[0].equals("signals")) {
					readSignalsLine(splitLine);
				} else if (splitLine[0].equals("signal data")) {
					readSignalTreeLine(splitLine);
				} else {
					if (splitLine.length == 3 || splitLine.length == 5) {
						CreateLeafNode(splitLine);
					} else if (splitLine[5].charAt(0) == 'n') {
						CreateNatureNode(splitLine);
					} else {
						CreatePlayerNode(splitLine);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("Game::CreateGameFromFile: Read exception");
		}
		
	}
	
	private void readGameInfoLine(String [] split_line) {
		numChanceHistories = Integer.parseInt(split_line[1]);
		numCombinedPlayerHistories = Integer.parseInt(split_line[2]);
		numTerminalHistories = Integer.parseInt(split_line[3]);
		numNodes = Integer.parseInt(split_line[5])+1;
		numInformationSetsPlayer1 = Integer.parseInt(split_line[7]);
		numInformationSetsPlayer2 = Integer.parseInt(split_line[8]);
		
		informationSets[0] = new TIntArrayList [numInformationSetsPlayer1];
		informationSets[1] = new TIntArrayList [numInformationSetsPlayer2];
		
		informationSetsSeen[0] = new boolean[numInformationSetsPlayer1];
		informationSetsSeen[1] = new boolean[numInformationSetsPlayer2];
		
		for (int i = 0; i < numInformationSetsPlayer1; i++) {
			informationSets[0][i] = new TIntArrayList();
		}
		for (int i = 0; i < numInformationSetsPlayer2; i++) {
			informationSets[1][i] = new TIntArrayList();
		}
		
		nodes = new Node[numNodes];
		childNodeIdBySignalId = new TIntIntMap [numNodes];
		actionIdBySignalId = new TIntIntMap [numNodes];
		for (int i = 0; i < numNodes; i++) {
			childNodeIdBySignalId[i] = new TIntIntHashMap();
			actionIdBySignalId[i] = new TIntIntHashMap();
		}
	}
	
	private void readSignalTreeLine(String[] split_line) {
		numRounds = Integer.parseInt(split_line[1]);
		depth = 2 * Integer.parseInt(split_line[2]) + Integer.parseInt(split_line[3]);
		numPrivateSignals = Integer.parseInt(split_line[2]);
	}
	
	private void readSignalsLine(String[] split_line) {
		signals = new String[split_line.length-1];
		for (int i = 1; i < split_line.length; i++) {
			signals[i-1] = split_line[i];
			signalNameToId.put(split_line[i], i-1);
		}
	}
	
	// CreateLeafNode handles both Zerosum format files, and the more heavily annotated files of this package
	private void CreateLeafNode(String[] line) {
		Node node = new Node();
		node.nodeId= Integer.parseInt(line[0]);
		node.name = line[1];
		node.player = -2;
		node.value = Double.parseDouble(line[2]);
		if (node.value < smallestPayoff) {
			smallestPayoff = node.value;
		}
		if (node.value > biggestPayoff) {
			biggestPayoff = node.value;
		}
		if (line.length == 5) {
			node.signalGroupPlayer1 = Integer.parseInt(line[3]);
			node.signalGroupPlayer2 = Integer.parseInt(line[4]);
		}
		nodes[node.nodeId] = node;
	}

	// The format is the same for player nodes in the Zerosum package and our format 
	private void CreatePlayerNode(String[] line) {
		Node node = new Node();
		node.nodeId = Integer.parseInt(line[0]);
		node.player = Integer.parseInt(line[2]) + 1;
		node.informationSet = Integer.parseInt(line[3]);
		node.setAbstractInformationSet(Integer.parseInt(line[3]));
		if (node.informationSet < smallestInformationSetId[node.player-1]) {
			smallestInformationSetId[node.player-1] = node.informationSet;
		}
		
		node.name = line[1];
		//System.out.println("Player: " + (node.player-1) + ", info set: " + node.informationSet);
		informationSets[node.player-1][node.informationSet].add(node.nodeId);
		
		int numActions = Integer.parseInt(line[4]);
		node.actions = new Action[numActions];
		for (int i = 0; i < numActions; i++) {
			if (!informationSetsSeen[node.player-1][node.informationSet]) {
				numSequences[node.player-1]++;
			}
			Action action = new Action();
			action.name = line[5+2*i];
			action.childId = Integer.parseInt(line[6+2*i]);
			node.actions[i] = action;
		}
		
		informationSetsSeen[node.player-1][node.informationSet] = true;
		if (node.name.equals("/")) {
			this.root = node.nodeId;
		}
		nodes[node.nodeId] = node;
	}
	
	
	/**
	 * Assumes that the name of a node is the string of actions performed to reach the node. Returns the subset of actions that are observed by player i.
	 * Assumes that actions are split by '/' and values are written [01a];actionName, where [01a] indicates whether the observer is 0,1, or a
	 * @param node
	 * @param player observing player
	 * @return
	 */
	public static List<String> extractObservedActionsFromNodeName(String name, int player) {
		List<String> observed = new ArrayList<String>();
		String[] actions = name.split("/");
		// start at 1, since the first action is the empty action at the root
		for (int i = 1; i < actions.length; i++) {
			String[] splitAction = actions[i].split(";");
			if (splitAction[1].equals("a") || Integer.parseInt(splitAction[1]) == player-1) {
				observed.add(splitAction[2]);
			}
		}
		
		return observed;
	}
	
	/**
	 * Assumes that the name of a node is the string of actions performed to reach the node. Returns the subset of actions that are observed by player i.
	 * Assumes that actions are split by '/' and values are written [01a];actionName, where [01a] indicates whether the observer is 0,1, or a
	 * @param node
	 * @param player observing player
	 * @return
	 */
	public static List<String> extractObservedNatureActionsFromNodeName(String name, int player) {
		List<String> observed = new ArrayList<String>();
		String[] actions = name.split("/");
		// start at 1, since the first action is the empty action at the root
		for (int i = 1; i < actions.length; i++) {
			String[] splitAction = actions[i].split(";");
			if (splitAction[0].equals("n") && (splitAction[1].equals("a") || Integer.parseInt(splitAction[1]) == player-1)) {
				observed.add(splitAction[2]);
			}
		}
		
		return observed;
	}

	/**
	 * Assumes that the name of a node is the string of actions performed to reach the node. Returns the subset of actions that are observed by player i.
	 * Assumes that actions are split by '/' and values are written [01a];actionName, where [01a] indicates whether the observer is 0,1, or a
	 * @param node
	 * @param player observing player
	 * @return
	 */
	public static List<String> extractObservedPlayerActionsFromNodeName(String name, int player) {
		List<String> observed = new ArrayList<String>();
		String[] actions = name.split("/");
		// start at 1, since the first action is the empty action at the root
		for (int i = 1; i < actions.length; i++) {
			String[] splitAction = actions[i].split(";");
			if (!splitAction[0].equals("n") && (splitAction[1].equals("a") || Integer.parseInt(splitAction[1]) == player-1)) {
				observed.add(splitAction[2]);
			}
		}
		
		return observed;
	}

	
	private void CreateZeroSumPackageStyleNatureNode(String[] line) {
		Node node = new Node();
		node.nodeId = Integer.parseInt(line[0]);
		node.name = line[1];
		node.player = 0;
		int numActions = Integer.parseInt(line[2]);
		node.actions = new Action[numActions];
		double sum = 0;
		for (int i = 0; i < numActions; i++) {
			Action action = new Action();
			action.name = line[3+3*i];
			action.childId = Integer.parseInt(line[4+3*i]);
			action.probability = Double.parseDouble(line[5+3*i]);
			sum += action.probability;
			node.actions[i] = action;			
		}
		for (int i = 0; i < numActions; i++) {
			node.actions[i].probability = (double) node.actions[i].probability / sum;
		}
		// the root node is the empty history
		if (node.name.equals("/")) {
			root = node.nodeId;
		}
		nodes[node.nodeId] = node;
	}

	// There is some code duplication between this and the method above for handling the Zerosum package format
	private void CreateNatureNode(String[] line) {
		Node node = new Node();
		node.nodeId = Integer.parseInt(line[0]);
		node.name = line[1];
		node.player = 0;
		String[] splitName = line[5].split(":");
		if (splitName[1].equals("a")) {
			node.publicSignal = true;
		} else {
			node.publicSignal = false;
			node.playerReceivingSignal = Integer.parseInt(splitName[1])+1;
		}
		
		node.signalGroupPlayer1 = Integer.parseInt(line[2]);
		node.signalGroupPlayer2 = Integer.parseInt(line[3]);
		
		int numActions = Integer.parseInt(line[4]);
		node.actions = new Action[numActions];
		double sum = 0;
		for (int i = 0; i < numActions; i++) {
			Action action = new Action();
			action.name = line[5+3*i];
			String signalName = action.name.split(":")[2];
			action.childId = Integer.parseInt(line[6+3*i]);
			action.rem = Integer.parseInt(line[7+3*i]);
			sum += action.rem;
			childNodeIdBySignalId[node.nodeId].put(signalNameToId.get(signalName), action.childId);
			actionIdBySignalId[node.nodeId].put(signalNameToId.get(signalName), i);
			node.actions[i] = action;
		}
		
		for (int i = 0; i < numActions; i++) {
			node.actions[i].probability = (double) node.actions[i].rem / sum;
		}
		
		if (node.name.equals("/")) {
			root = node.nodeId;
		}
		nodes[node.nodeId] = node;
	}

	/**
	 * Computes an array that represents the expected value for each node
	 * @param strategyP1
	 * @param strategyP2
	 * @param invertValues if true, all values are negated, representing the utility for Player 2 when considered as a maximizing player
	 * @return
	 */
	public double[] getExpectedValuesForNodes(TObjectDoubleMap<String>[] strategyP1, TObjectDoubleMap<String>[] strategyP2, boolean negateValues) {
		/*double[] expectedValue = new double[numNodes];
		fillExpectedValueArrayRecursive(expectedValue, root, strategyP1, strategyP2, negateValues, ZeroBranchOption.ZERO, false);
		return expectedValue;*/
		return getExpectedValuesForNodes(strategyP1, strategyP2, negateValues, null);
	}
	
	/**
	 * Computes an array that represents the expected value for each node
	 * @param strategyP1
	 * @param strategyP2
	 * @return
	 */
	public double[] getExpectedValuesForNodes(TObjectDoubleMap<String>[] strategyP1, TObjectDoubleMap<String>[] strategyP2) {
		return getExpectedValuesForNodes(strategyP1, strategyP2, false);
	}

		/**
	 * Computes an array that represents the expected value for each node
	 * @param strategyP1
	 * @param strategyP2
	 * @return
	 */
	public double[] getExpectedValuesForNodes(TObjectDoubleMap<String>[] strategyP1, TObjectDoubleMap<String>[] strategyP2, boolean negateValues, NormalDistribution distribution) {
		double[] expectedValue = new double[numNodes];
		fillExpectedValueArrayRecursive(expectedValue, root, strategyP1, strategyP2, negateValues, ZeroBranchOption.ZERO, false, distribution);
		return expectedValue;
	}

	// Enum specifies how to handle the expected value of branches with probability zero of being reached.
	// ZERO: This option places expected value of zero on all nodes in a probability 0 subtree
	// UNIFORM: This option uses uniform probabilities
	private enum ZeroBranchOption {ZERO, UNIFORM} // TODO: implement UNIFORM option
	/*private double fillExpectedValueArrayRecursive(double[] array, int currentNode, TObjectDoubleMap<String>[] strategyP1, TObjectDoubleMap<String>[] strategyP2, boolean negateValues, ZeroBranchOption zeroBranchOption, boolean inZeroBranch) {
		Node node = nodes[currentNode];
		//biggestPayoff = 0;
		//smallestPayoff = 0;
		if (node.isLeaf()) {
			if (inZeroBranch) {
				//array[currentNode] = 0;
				array[currentNode] = negateValues ? -node.getValue() + biggestPayoff: node.getValue() - smallestPayoff;
			}
			else {
				array[currentNode] = negateValues ? -node.getValue() + biggestPayoff: node.getValue() - smallestPayoff;
			}
			return array[currentNode];
		}

		
		array[currentNode] = 0;
		for(Action action : node.actions) {
			double probability = 0;
			if (node.getPlayer() == 0) {
				probability = action.getProbability();
			} else if (node.getPlayer() == 1){
				probability = strategyP1[node.getInformationSet()].get(action.getName());
			} else {
				probability = strategyP2[node.getInformationSet()].get(action.getName());
			}
			probability = inZeroBranch ? 0 : probability;
			array[currentNode] += probability * fillExpectedValueArrayRecursive(array, action.childId, strategyP1, strategyP2, negateValues, zeroBranchOption, probability == 0);
		}
		return array[currentNode];
	}*/

	private double fillExpectedValueArrayRecursive(double[] array, int currentNode, TObjectDoubleMap<String>[] strategyP1, TObjectDoubleMap<String>[] strategyP2, boolean negateValues, ZeroBranchOption zeroBranchOption, boolean inZeroBranch, NormalDistribution distribution) {
		Node node = nodes[currentNode];
		//biggestPayoff = 0;
		//smallestPayoff = 0;
		if (node.isLeaf()) {
			if (inZeroBranch) {
				//array[currentNode] = 0;
				array[currentNode] = negateValues ? -node.getValue() + biggestPayoff: node.getValue() - smallestPayoff;
			}
			else {
				array[currentNode] = negateValues ? -node.getValue() + biggestPayoff: node.getValue() - smallestPayoff;
			}
			return array[currentNode];
		}

		
		array[currentNode] = 0;
		for(Action action : node.actions) {
			double probability = 0;
			if (node.getPlayer() == 0) {
				probability = action.getProbability();
			} else if (node.getPlayer() == 1){
				probability = strategyP1[node.getInformationSet()].get(action.getName());
			} else {
				probability = strategyP2[node.getInformationSet()].get(action.getName());
			}
			
			if (null == distribution) {
				probability = inZeroBranch ? 0 : probability;
				array[currentNode] += probability * (fillExpectedValueArrayRecursive(array, action.childId, strategyP1, strategyP2, negateValues, zeroBranchOption, probability == 0, distribution));
			} else {
				array[currentNode] += probability * fillExpectedValueArrayRecursive(array, action.childId, strategyP1, strategyP2, negateValues, zeroBranchOption, probability == 0, distribution) + distribution.sample();
			}
		}
		return array[currentNode];
	}
	
	
	public TIntArrayList getInformationSet(int player, int informationSetId) {
		if (hasAbstraction) {
			System.out.println("Warning: abstraction exists, getInformationSet not updated to reflect this");
		}
		return informationSets[player-1][informationSetId];
	}

	public int getNumActionsAtInformationSet(int player, int informationSetId) {
		return nodes[informationSets[player-1][informationSetId].get(0)].getActions().length;
	}

	public Action[] getActionsAtInformationSet(int player, int informationSetId) {
		return nodes[informationSets[player-1][informationSetId].get(0)].getActions();
	}
	
	public int getNumActionsForNature(GameState gs) {
		return nodes[gs.getCurrentNodeId()].getActions().length;
	}

	
	public boolean[][] getInformationSetsSeen() {
		return informationSetsSeen;
	}

	public Node[] getNodes() {
		return nodes;
	}

	public TIntIntMap[] getChildNodeIdBySignalId() {
		return childNodeIdBySignalId;
	}

	public TIntIntMap[] getActionIdBySignalId() {
		return actionIdBySignalId;
	}

	public int getRoot() {
		return root;
	}

	public int getNumChanceHistories() {
		return numChanceHistories;
	}

	public int getNumCombinedPlayerHistories() {
		return numCombinedPlayerHistories;
	}

	public int getNumTerminalHistories() {
		return numTerminalHistories;
	}

	public int getNumNodes() {
		return numNodes;
	}

	public int getNumInformationSetsPlayer1() {
		return numInformationSetsPlayer1;
	}

	public int getNumInformationSetsPlayer2() {
		return numInformationSetsPlayer2;
	}

	public int[] getNumSequences() {
		return numSequences;
	}
	
	public int getNumSequencesP1() {
		return numSequences[0];
	}

	public int getNumSequencesP2() {
		return numSequences[1];
	}

	public String[] getSignals() {
		return signals;
	}

	public TObjectIntMap<String> getSignalNameToId() {
		return signalNameToId;
	}

	public int getNumRounds() {
		return numRounds;
	}

	public int getDepth() {
		return depth;
	}

	public int getNumPrivateSignals() {
		return numPrivateSignals;
	}

	public Node getNodeById(int currentNodeId) {
		return nodes[currentNodeId];
	}

	public int getSmallestInformationSetIdPlayer1() {
		return smallestInformationSetId[0];
	}

	public int getSmallestInformationSetIdPlayer2() {
		return smallestInformationSetId[1];
	}
	
	public int getSmallestInformationSetId(int player) {
		return smallestInformationSetId[player-1];
	}


	@Override
	public GameState getInitialGameState() {
		GameState gs = new GameState();
//		gs.setCurrentNodeId(getRoot());
//		gs.setCurrentPlayer(nodes[getRoot()].getPlayer());
//		if (hasAbstraction) {
//			gs.setCurrentInformationSetId(abstraction[nodes[getRoot()].getInformationSet()]);
//		} else {
//			gs.setCurrentInformationSetId(nodes[getRoot()].getInformationSet());
//		}
		gs.nodeIdHistory.add(getRoot());
		updateGameStateInfo(gs);
		return gs;
	}

	@Override
	public int getNumActions(GameState gs) {
		if (gs.getCurrentPlayer() == 0) {
			return getNumActionsForNature(gs);
		} else {
			return getNumActionsAtInformationSet(gs.getCurrentPlayer(), gs.getCurrentInformationSetId());
		}
	}

	@Override
	public int getNumActionsAtInformationSet(GameState gs) {
		return getNumActionsAtInformationSet(gs.getCurrentPlayer(), gs.getCurrentInformationSetId());
	}

	@Override
	public void updateGameStateWithAction(GameState gs, int actionId, double probability) {
		gs.addHistory(gs.getCurrentPlayer(), actionId);
		gs.addProbability(gs.getCurrentPlayer(), probability);
		int childNodeId = nodes[gs.nodeIdHistory.get(gs.nodeIdHistory.size()-1)].actions[actionId].getChildId();
		gs.nodeIdHistory.add(childNodeId);
		updateGameStateInfo(gs);
	}

	@Override
	public void removeActionFromGameState(GameState gs, int action, int player) {
		gs.popAction(player);
		gs.popProbability(player);
		gs.nodeIdHistory.remove(gs.nodeIdHistory.size()-1, 1);
		updateGameStateInfo(gs);
	}

	private void updateGameStateInfo(GameState gs) {
		Node newNode = nodes[gs.getCurrentNodeId()];
		
		//gs.nodeIdHistory.add(newNode.getNodeId());
		if (!newNode.isLeaf() && hasAbstraction && newNode.player != 0 && abstraction[newNode.getPlayer()][newNode.getInformationSet()] != newNode.getInformationSet()) {
			gs.setCurrentInformationSetId(abstraction[newNode.getPlayer()][newNode.getInformationSet()]);
			gs.setOriginalInformationSetId(newNode.getInformationSet());
		} else if (!newNode.isLeaf() && newNode.player != 0){
			gs.setCurrentInformationSetId(newNode.getInformationSet());
			gs.setOriginalInformationSetId(newNode.getInformationSet());
		}
		//gs.setCurrentInformationSetId(newNode.getInformationSet());
		
		gs.setCurrentPlayer(newNode.getPlayer());
		
		if (newNode.isLeaf()) {
			gs.setIsLeaf(true);
			gs.setValue(newNode.getValue());
		} else {
			gs.setIsLeaf(false);
		}
	}
	
	@Override
	public double getProbabilityOfNatureAction(GameState gs, int action) throws Exception {
		if (gs.getCurrentPlayer() != 0) {
			throw new Exception("Not a nature state");
		}
		return this.nodes[gs.getCurrentNodeId()].actions[action].getProbability();
	}

	@Override
	public int getNumInformationSets(int player) {
		if (player == 1) {
			return numInformationSetsPlayer1;
		} else if (player == 2) {
			return numInformationSetsPlayer2;
		}
		return 0;
	}


	@Override
	public boolean informationSetAbstracted(int player, int informationSetId) {
		if (hasAbstraction) {
			return informationSetId != abstraction[player][informationSetId];
		} else {
			return false;
		}
	}

	@Override
	public int getAbstractInformationSetId(int player, int informationSetId) {
		if (hasAbstraction) {
			return abstraction[player][informationSetId];
		} else {
			return informationSetId;
		}
	}

	@Override
	public void addInformationSetAbstraction(int[][] informationSetAbstraction,	int[][][] actionMapping) {
		this.hasAbstraction = true;
		this.useIdentityActionMap = false;
		this.abstraction = informationSetAbstraction;
		this.actionAbstractionMapping = actionMapping;
	}

	@Override
	public int getAbstractActionMapping(int player, int originalInformationSetId, int originalAction) {
		if (informationSetAbstracted(player, originalInformationSetId) && !useIdentityActionMap) {
			return actionAbstractionMapping[player][originalInformationSetId][originalAction];
		} else {
			return originalAction;
		}
	}
	
	@Override
	public int getAbstractActionMapping(GameState gs, int action) {
		if (informationSetAbstracted(gs.getCurrentPlayer(), gs.getOriginalInformationSetId()) && !useIdentityActionMap) {
			return actionAbstractionMapping[gs.getCurrentPlayer()][gs.getOriginalInformationSetId()][action];
		} else {
			return action;
		}
	}

	@Override
	public double computeGameValueForStrategies(double[][][] strategyProfile) {
		return computeGameValueRecursive(getRoot(), strategyProfile);
	}

	private double computeGameValueRecursive(int currentNodeId, double[][][] strategyProfile) {
		Node currentNode = getNodeById(currentNodeId);
		if (currentNode.isLeaf()) {
			return currentNode.getValue();
		}
		
		double value = 0;
		for (int actionId = 0; actionId < currentNode.getActions().length; actionId++) {
			Action action = currentNode.actions[actionId];
			double probability = currentNode.player == 0 ? action.getProbability() : strategyProfile[currentNode.getPlayer()][currentNode.getInformationSet()][actionId];
			value += probability * computeGameValueRecursive(action.getChildId(), strategyProfile);
		}
		
		return value;
	}

	@Override
	public double getLargestPayoff() {
		return biggestPayoff;
	}
	
	@Override
	public String toString(){
		return recursiveToString(getRoot(), "");
	}
	
	private String recursiveToString(int nodeId, String prefix) {
		Node node = getNodeById(nodeId);
		String stringRep = prefix + node.getName();
		if (node.isLeaf()) return stringRep;
		for (Action action : node.getActions()) {
			stringRep += " " + action;
		}
		stringRep += "\n";
		for (Action action : node.getActions()) {
			stringRep += recursiveToString(action.getChildId(), prefix+" ");
		}
		return stringRep;
	}
}