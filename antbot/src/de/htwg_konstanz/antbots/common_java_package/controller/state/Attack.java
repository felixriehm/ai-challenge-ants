package de.htwg_konstanz.antbots.common_java_package.controller.state;

import java.util.LinkedList;
import java.util.List;

import de.htwg_konstanz.antbots.bots.AntBot;
import de.htwg_konstanz.antbots.common_java_package.controller.Ant;
import de.htwg_konstanz.antbots.common_java_package.controller.GameInformations;
import de.htwg_konstanz.antbots.common_java_package.controller.boarder.BuildBoarder;
import de.htwg_konstanz.antbots.common_java_package.model.Configuration;
import de.htwg_konstanz.antbots.common_java_package.model.Tile;

public class Attack implements State{

	Ant ant;
	private StateName stateName;

	public Attack(Ant a) {
		this.ant = a;
		stateName = StateName.Attack;
	}
	
	@Override
	public void changeState() {
		if(AntBot.getEnemyHillManager().getAntsToHill().containsKey(ant)) {
			ant.setState(new AttackEnemyHill(ant));
			return;
		}
		if(AntBot.getAttackManager().getMarkedAnts().containsKey(ant)){
			return;
		}
		if(GameInformations.getFoodManager().getMarkedAnts().containsKey(ant)){
			ant.setState(new CollectFood(ant));
			return;
		}
		if( !GameInformations.getFoodManager().getMarkedAnts().containsKey(ant) && AntBot.getGameI().getExplorerAnts() >= Configuration.getExplorerAntsLimit() && BuildBoarder.marktAnts().contains(ant)){
			ant.setState(new GoToBoarder(ant));
			return;
		}
		if( !GameInformations.getFoodManager().getMarkedAnts().containsKey(ant) && AntBot.getGameI().getExplorerAnts() < Configuration.getExplorerAntsLimit()){
			ant.setState(new Exploration(ant));
			return;
		}
	}

	@Override
	public void execute() {
		List<Tile> order = new LinkedList<Tile>();
		order.add(AntBot.getAttackManager().getMarkedAnts().get(ant).getNewPosition());
		ant.setRoute(order);
	}

	@Override
	public String toString() {
		return "Attack State";
	}

	public StateName getStateName() {
		return stateName;
	}
	
	@Override
	public void stateEnter() {
		
	}

	@Override
	public void stateExit() {
		
	}
	
}
