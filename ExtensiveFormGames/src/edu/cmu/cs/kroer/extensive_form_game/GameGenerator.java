package edu.cmu.cs.kroer.extensive_form_game;


public interface GameGenerator {
	public GameState getInitialGameState();
	public int getNumActionsAtInformationSet(GameState gs);

	/**
	 * Update the GameState object to have the input action. Note that this action is the  index into the action list, not the name of the action
	 * @param gs
	 * @param action index into action list at the current node/nodes in information set
	 * @param probability the probability of this action being taken in the current game state
	 */
	public void updateGameStateWithAction(GameState gs, int action, double probability);

	/**
	 * Remove the input action from the game state. Note that this action is the  index into the action list, not the name of the action
	 * @param gs
	 * @param action index into action list at the current node/nodes in information set
	 * @param player the player that took the action
	 */
	public void removeActionFromGameState(GameState gs, int action, int player);
	
	/**
	 * Returns the probability of the action at actionIndex is taken. Should check that gs is currently at a nature node.
	 * @param gs
	 * @param actionIndex index into action list at the current node/nodes in information set
	 * @return
	 */
	public double getProbabilityOfNatureAction(GameState gs, int actionIndex) throws Exception;
	
	public int getNumInformationSets(int player);
	public int getNumActionsAtInformationSet(int player, int informationSetId);
	public int getNumActionsForNature(GameState gs);
}















