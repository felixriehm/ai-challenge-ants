package de.htwg_konstanz.antbots.common_java_package.controller.state;


public interface State {
	public void execute();
	void changeState();
	void stateEnter();
	void stateExit();
	StateName getStateName();
}
