package edu.cmu.cs.kroer.extensive_form_game.abstraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignalAbstraction {
	String[] signalNames;
	Map<String, Integer> namesToSignalId = new HashMap<String, Integer>();
	Map<List<Integer>, List<Integer>> abstraction = new HashMap<List<Integer>, List<Integer>>();
	
	public SignalAbstraction(String[] signalNames) {
		this.signalNames = signalNames;
		for (int i = 0; i < signalNames.length; i++) {
			namesToSignalId.put(signalNames[i], i);
		}
	}
	
	public void addAbstraction(List<Integer> keep, List<Integer> abstracted) {
		abstraction.put(abstracted, keep);
	}

	public void addAbstractionByName(List<String> keep, List<String> abstracted) {
		List<Integer> keepList = new ArrayList<Integer>();
		List<Integer> abstractList = new ArrayList<Integer>();
		for (String name : keep) {
			keepList.add(namesToSignalId.get(name));
		}
		for (String name : abstracted) {
			abstractList.add(namesToSignalId.get(name));
		}
		abstraction.put(abstractList, keepList);
	}
	
	public List<String> getAbstractSignalsByName(List<String> signals) {
		List<Integer> signalsList = new ArrayList<Integer>();
		for (String name : signals) {
			signalsList.add(namesToSignalId.get(name));
		}
		
		List<String> abstractSignalsList = new ArrayList<String>();
		for (Integer id : abstraction.get(signals)) {
			abstractSignalsList.add(signalNames[id]);
		}
		
		return abstractSignalsList;
	}
	
}
