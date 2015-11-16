package extensive_form_game_abstraction;

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
	
	/**
	 * Returns a list of signals as string
	 * @param signals The list of signal strings that one wishes to know the mapping of
	 * @return
	 */
	public List<String> getAbstractSignalsByName(List<String> signals) {
		List<Integer> signalsList = new ArrayList<Integer>();
		for (String name : signals) {
			signalsList.add(namesToSignalId.get(name));
		}
		if (!abstraction.containsKey(signalsList)) {
			return signals;
		}
		List<String> abstractSignalsList = new ArrayList<String>();
		for (Integer id : abstraction.get(signalsList)) {
			abstractSignalsList.add(signalNames[id]);
		}
		
		return abstractSignalsList;
	}
	
	/**
	 * Returns a list of Integer objects, referring to the  indices of the signals into signalNames.
	 * @param signals The list of signal indices that one wishes to know the mapping of
	 * @return
	 */
	public List<Integer> getAbstractSignalsById(List<Integer> signals) {
		return abstraction.get(signals);
	}
	
}
