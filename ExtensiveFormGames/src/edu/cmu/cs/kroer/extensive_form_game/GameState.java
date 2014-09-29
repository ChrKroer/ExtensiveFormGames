package edu.cmu.cs.kroer.extensive_form_game;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public class GameState {
	
	TDoubleList[] playerProbabilities;
	TIntList[] playerHistories;
	// The sample histories are used to remember what was sampled at each information set across iterations for the players
	TIntIntMap[] playerSampleHistories;

	TIntList nodeIdHistory;
	
	boolean isLeaf;
	double value;
	int nodesTraversed;
	int currentNodeId;
	int currentInformationSetId;
	int currentPlayer;
	
	public GameState() {
		playerProbabilities = new TDoubleList[3];
		playerHistories = new TIntList[3];
		playerSampleHistories = new TIntIntMap[3];
		for (int player = 0; player < 3; player++) {
			playerProbabilities[player] = new TDoubleArrayList();
			playerHistories[player] = new TIntArrayList();
			playerSampleHistories[player] = new TIntIntHashMap();
		}
		nodeIdHistory = new TIntArrayList();
	}

	public double getProbabilityWithPlayer(int player) {
		double product = 1;
		for (int i = 0; i < playerProbabilities[player].size(); i++) { 
			product *= playerProbabilities[player].get(i); 
		}
		return product;
	}

	public double getProbabilityWithoutPlayer(int player) {
		double probability = 1;
		for (int currentPlayer = 0; currentPlayer < 3; currentPlayer++) {
			if (currentPlayer != player) {
				probability *= getProbabilityWithPlayer(currentPlayer);
			}
		}
		return probability;
	}
	
//	public double probabilityP1() {
//		double product = 1;
//		for (int i = 0; i < playerProbabilities[1].size(); i++) { 
//			product = product * playerProbabilities[1].get(i); 
//		}
//		return product;
//	}
//
//	public double probabilityP2() {
//		double product = 1;
//		for (int i = 0; i < playerProbabilities[2].size(); i++) { 
//			product = product * playerProbabilities[2].get(i); 
//		}
//		return product;
//	}
//
//	public double probabilityNature() {
//		double product = 1;
//		for (int i = 0; i < playerProbabilities[0].size(); i++) { 
//			product = product * playerProbabilities[0].get(i); 
//		}
//		return product;
//	}
	

	public void addProbability(int player, double val) { playerProbabilities[player].add(val);	}
//	public void addProbabilityP1(double val) { playerProbabilities[1].add(val);	}
//	public void addProbabilityP2(double val) { playerProbabilities[2].add(val);	}
//	public void addProbabilityNature(double val) { playerProbabilities[0].add(val);	}

	
	public void addHistory(int player, int val) { playerHistories[player].add(val);	}
//	public void addHistoryP1(int val) { playerHistories[1].add(val);	}
//	public void addHistoryP2(int val) { playerHistories[2].add(val);	}
//	public void addHistoryNature(int val) { playerHistories[0].add(val);	}
	
	
	public TIntList history(int player) { return playerHistories[player]; }
//	public TIntList p1History() { return playerHistories[1]; }
//	public TIntList p2History() { return playerHistories[2]; }
//	public TIntList natureHistory() { return playerHistories[0]; }
	
	
	public void popProbability(int player) { playerProbabilities[player].remove(playerProbabilities[player].size()-1, 1); }
	public void popAction(int player) { playerHistories[player].remove(playerHistories[player].size()-1, 1); }
	
	
	public boolean priorSampleExists(int player, int informationSetId) { return playerSampleHistories[player].containsKey(informationSetId);	}
	public int getPriorSample(int player, int informationSetId) { return playerSampleHistories[player].get(informationSetId); }
	public void addSample(int player, int informationSetId, int action) { playerSampleHistories[player].put(informationSetId, action); }
	
	
	public boolean isLeaf() {
		return isLeaf;
	}



	public void setLeaf(boolean isLeaf) {
		this.isLeaf = isLeaf;
	}



	public double getValue() {
		return value;
	}



	public void setValue(double value) {
		this.value = value;
	}



	public int getNodesTraversed() {
		return nodesTraversed;
	}



	public void setNodesTraversed(int nodesTraversed) {
		this.nodesTraversed = nodesTraversed;
	}


	public int getCurrentNodeId() {
		return currentNodeId;
	}


	public void setCurrentNodeId(int currentNodeId) {
		this.currentNodeId = currentNodeId;
	}


	public int getCurrentInformationSetId() {
		return currentInformationSetId;
	}


	public void setCurrentInformationSetId(int currentInformationSetId) {
		this.currentInformationSetId = currentInformationSetId;
	}


	public int getCurrentPlayer() {
		return currentPlayer;
	}


	public void setCurrentPlayer(int currentPlayer) {
		this.currentPlayer = currentPlayer;
	}
	
	

}
