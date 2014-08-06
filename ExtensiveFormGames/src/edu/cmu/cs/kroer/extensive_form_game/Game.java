package edu.cmu.cs.kroer.extensive_form_game;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import au.com.bytecode.opencsv.CSVReader;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.*;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.apache.commons.lang3.*;

public class Game {
	public class Action {
		private String name;
		private int childId;
		private double probability;
		private int rem;
		public String getName() {
			return name;
		}
		public int getChildId() {
			return childId;
		}
		public double getProbability() {
			return probability;
		}
		public int getRem() {
			return rem;
		}
	}
	public class Node {
		private int nodeId;
		private String name;
		private int player;
		private boolean publicSignal;
		private int playerReceivingSignal;
		private int informationSet;
		private int signalGroupPlayer1; // Signal groups not yet implemented
		private int signalGroupPlayer2;
		private Action[] actions;
		private int value;
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
		public int getSignalGroupPlayer1() {
			return signalGroupPlayer1;
		}
		public int getSignalGroupPlayer2() {
			return signalGroupPlayer2;
		}
		public Action[] getActions() {
			return actions;
		}
		public int getValue() {
			return value;
		}
		public boolean isLeaf() {
			return player == -2;
		}
	}
	
	private TIntArrayList [] [] informationSets;
	private boolean [] [] informationSetsSeen;
	private Node [] nodes;
	private TIntIntMap [] childNodeIdBySignalId;
	private TIntIntMap [] actionIdBySignalId;
	
	private int root;
	private int numChanceHistories;
	private int numCombinedPlayerHistories;
	private int numTerminalHistories;
	private int numNodes;
	private int numInformationSetsPlayer1;
	private int numInformationSetsPlayer2;
	private int smallestInformationSetId[];
	private int [] numSequences;
	
	private String [] signals;
	private TObjectIntMap<String> signalNameToId;
	private int numRounds;
	private int depth;
	private int numPrivateSignals;
	
	
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
	}
	
	public Game(String filename) {
		this();
		createGameFromFile(filename);
	}
	
	public void createGameFromFileZerosumPackageFormat(String filename) {
		//BufferedReader in = null;
		CSVReader in = null;
		try {
			in= new CSVReader(new FileReader(filename), ' ', '\'');
			//in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("ExtensiveFormGame::CreateGameFromFile: File not found");
			System.exit(0);
		}
		
		String[] splitLine;
		try {
			while ((splitLine = in.readNext()) != null) {
				if (splitLine[0].equals("#")) {
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
			System.out.println("ExtensiveFormGame::CreateGameFromFile: Read exception");
		}
		
	}
	
	
	public void createGameFromFile(String filename) {
		//BufferedReader in = null;
		CSVReader in = null;
		try {
			in= new CSVReader(new FileReader(filename), ' ', '\'');
			//in = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			System.out.println("ExtensiveFormGame::CreateGameFromFile: File not found");
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
			System.out.println("ExtensiveFormGame::CreateGameFromFile: Read exception");
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
		node.value = Integer.parseInt(line[2]);
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
			action.rem = Integer.parseInt(line[5+3*i]);
			sum += action.rem;
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

	public TIntArrayList[][] getInformationSets() {
		return informationSets;
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
	
	
}














