package de.htwg_konstanz.antbots.common_java_package.controller.state;

import java.awt.Color;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.htwg_konstanz.antbots.bots.AntBot;
import de.htwg_konstanz.antbots.common_java_package.controller.Ant;
import de.htwg_konstanz.antbots.common_java_package.controller.GameInformations;
import de.htwg_konstanz.antbots.common_java_package.controller.boarder.BuildBoarder;
import de.htwg_konstanz.antbots.common_java_package.model.Configuration;
import de.htwg_konstanz.antbots.common_java_package.model.Ilk;
import de.htwg_konstanz.antbots.common_java_package.model.Tile;
import de.htwg_konstanz.antbots.visualizer.OverlayDrawer;
import de.htwg_konstanz.antbots.visualizer.OverlayDrawer.SubTile;

public class GoToBoarder implements State{

	private StateName stateName;
	private boolean goToBoarder = false;
	private Tile destination;
	
	Ant ant;
	
	public GoToBoarder(Ant a) {
		this.stateName = StateName.GoToBoarder;
		ant = a;
	}
boolean test = false;
	@Override
	public void execute() {
		if(destination != null && destination.getType() == Ilk.WATER) {
			goToBoarder = false;
		}
		if(destination != null && ant.getAntPosition().equals(destination)){
			goToBoarder = false;
			
		}
		if(!goToBoarder) {
			for(Entry<Set<Tile>, Set<Tile>> e : BuildBoarder.getAreaAndBoarder().entrySet()) {
				if(e.getKey().contains(ant.getAntPosition())) {
					Tile target = (Tile) e.getValue().toArray()[(int) (Math.random() * (e.getValue().size())-1)  ];
					
					List<Tile> route = AntBot.getPathfinding().aStar(ant.getAntPosition(), target);
					route.remove(0);
					if(route.size() == 0) {
						continue;
					}
					
					ant.setRoute(route);

					destination = route.get(route.size() - 1);
					test = true;
					
				} 
				
			} 
			if(test)
				AntBot.debug().log("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAANT " + ant.getAntPosition() +  " " + test+ " destination " + destination + " type " + destination.getType().name());
			
			goToBoarder = true;
		} else {
			//damit der weg jedes mal neu berechnet wird um zu verhindern, dass die Route �ber unentdecktes Land geht(k�nnte n�mlich Wasser sein)
			AntBot.debug().log("Ant " + ant.getAntPosition() + "Ziel " + destination + " type ");
			List<Tile> route = AntBot.getPathfinding().aStar(ant.getAntPosition(), destination);
			
			route.remove(0);
			ant.setRoute(route);
		}
		for (Tile rTile : ant.getRoute()) {
			OverlayDrawer.setFillColor(Color.ORANGE);
			OverlayDrawer.drawTileSubtile(rTile.getRow(), rTile.getCol(),
					SubTile.TL);
		}
	}

	@Override
	public void changeState() {
		if(AntBot.getAttackManager().getMarkedAnts().containsKey(ant)){
			ant.setState(new Attack(ant));
			return;
		}
		if(AntBot.getEnemyHillManager().getAntsToHill().containsKey(ant)) {
			ant.setState(new AttackEnemyHill(ant));
			return;
		}
		if(GameInformations.getFoodManager().getMarkedAnts().containsKey(ant)){
			ant.setState(new CollectFood(ant));
			return;
		}
		if(!GameInformations.getFoodManager().getMarkedAnts().containsKey(ant) && AntBot.getGameI().getExplorerAnts() >= Configuration.getExplorerAntsLimit() && BuildBoarder.marktAnts().contains(ant)){
			return;
		}
	}

	@Override
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
