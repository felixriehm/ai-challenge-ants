package de.htwg_konstanz.antbots.common_java_package.controller.attack;


import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import de.htwg_konstanz.antbots.common_java_package.controller.Ant;
import de.htwg_konstanz.antbots.common_java_package.controller.GameInformations;
import de.htwg_konstanz.antbots.common_java_package.model.Aim;
import de.htwg_konstanz.antbots.common_java_package.model.Order;
import de.htwg_konstanz.antbots.common_java_package.model.Tile;


public class AlphaBeta {
	
	public enum Strategy{
		AGGRESSIVE,
		PASSIVE,
		NEUTRAL
	}
	
	LinkedList<Order> bestMove;
	GameInformations board;
	int startDepth;
	
	LinkedList<Order> possibleMovesTmp;
	LinkedList<Ant> myAntsToGo;
	LinkedList<Ant> enemyAntsToGo;

	
	int directionPoint = 0;
	int myDeadAnts = 0;
	int enemyDeadAnts = 0;
	
	CommandManager cm;
	Strategy gameStrategy;
	
	public AlphaBeta(){
	}
	
	public LinkedList<Order> alphaBeta(GameInformations board, int depth, Strategy st, List<Set<Ant>> beteiligteAmeisen){
		bestMove = null;
		this.board = board;
		startDepth = depth;
		
		myAntsToGo = new LinkedList<Ant>();
		enemyAntsToGo = new LinkedList<Ant>();
		
		for (Ant ant : beteiligteAmeisen.get(0)) {
			myAntsToGo.add(new Ant(new Tile(ant.getAntPosition().getRow(),ant.getAntPosition().getCol()),ant.getId()));
		}
		for (Ant ant : beteiligteAmeisen.get(1)) {
			enemyAntsToGo.add(new Ant(new Tile(ant.getAntPosition().getRow(),ant.getAntPosition().getCol()),ant.getId()));
		}
		
		cm = new CommandManager();
		gameStrategy = st;

		
		max(depth,Integer.MIN_VALUE , Integer.MAX_VALUE);
		
		return bestMove;
	}

	private int max(int depth, int alpha, int beta) {
	    if (depth == 0 || myAntsToGo.isEmpty() || enemyAntsToGo.isEmpty()){
	    	int result = evaluation(gameStrategy);

	       return result;
	    }
	    int maxValue = alpha;
	    LinkedList<LinkedList<Order>> possibleMoves = new LinkedList<LinkedList<Order>>();
	    possibleMovesTmp = new LinkedList<Order>();
	    generatePossibleMoves(myAntsToGo.size(), myAntsToGo, possibleMoves);

	    while (!possibleMoves.isEmpty()) {
	    	LinkedList<Order> childMove = possibleMoves.poll();
	    	ExecuteNextMove(childMove,myAntsToGo,enemyAntsToGo, false);
	    	int evaluation = min(depth-1, maxValue, beta);
	    	UndoMove(childMove);

	    	if (evaluation > maxValue) {
	    		maxValue = evaluation;
	    		if (maxValue >= beta)
	    			break;
	    		if (depth == startDepth)
	    			bestMove = childMove;
	       }
	    }
	    return maxValue;
	 }

	private int min(int depth, int alpha, int beta) {
	    if (depth == 0 || enemyAntsToGo.isEmpty() || myAntsToGo.isEmpty()){
	    	int result = evaluation(gameStrategy);

	       return result;
	    }
	       
	    int minValue = beta;
	    LinkedList<LinkedList<Order>> possibleMoves = new LinkedList<LinkedList<Order>>();
	    possibleMovesTmp = new LinkedList<Order>();
	    generatePossibleMoves(enemyAntsToGo.size(), enemyAntsToGo, possibleMoves);

	    while (!possibleMoves.isEmpty()) {
	    	LinkedList<Order> childMove = possibleMoves.poll();
	    	ExecuteNextMove(childMove,enemyAntsToGo,myAntsToGo,true);

	    	int evaluation = max(depth-1, 
	                      alpha, minValue);
	    	UndoMove(childMove);

	    	if (evaluation < minValue) {
	    		minValue = evaluation;
	    		if (minValue <= alpha)
	    			break;
	    		}
	    	}
	    return minValue;
	}
	
	private void UndoMove(LinkedList<Order> childMove) {
		cm.undo();
	}

	private void ExecuteNextMove(LinkedList<Order> childMove, LinkedList<Ant> antsToGo, LinkedList<Ant> enemysToGo, boolean min){
		cm.executeCommand(new MoveCommand(childMove, antsToGo, enemysToGo, this, min));
	}
	
	private void generatePossibleMoves(int depth, LinkedList<Ant> antsToGo, LinkedList<LinkedList<Order>> possibleMoves) {
		if (depth == 0){
			LinkedList<Order> tmp = new LinkedList<Order>();
			for (Order order : possibleMovesTmp) {
				tmp.add(order);
			}
			possibleMoves.add(tmp);
		    return;
		}
		Ant ant = antsToGo.get(depth-1); 
		for (Aim aim : board.getMoveAbleDirections(ant.getAntPosition())) {
			boolean skip = false;
			Order newOrder = new Order(ant.getAntPosition(), aim);
			for (Order order1 : possibleMovesTmp) {
				if(order1.getNewPosition().equals(newOrder.getNewPosition())){
					skip = true;
				}
			}
			if(skip){
				// nicht vollkommen sicher zb wenn eien ameise alle Wege blockiert bekommt, bzw kreuzen
				continue;
			}
			possibleMovesTmp.addFirst(newOrder);
			
			generatePossibleMoves(depth-1, antsToGo, possibleMoves);
			try {
				possibleMovesTmp.removeFirst();
			} catch (Exception e) {

			}
		}
	}
	
	private int evaluation(Strategy strategy) {
		int t1 = enemyDeadAnts;
		int t2 = myDeadAnts;
		int t3 = 1;
		
		int w1;
		int w2;
		int w3 = 0;
		
		switch (strategy) {
		case AGGRESSIVE:
			w1 = 100;
			w2 = -50; // um die haelfte auf die gegnerischen Toten Ameisen gewichtet
			w3 = 0;
			break;
			
		case PASSIVE:
			w1 = 50;
			w2 = -100; // um die haelfte auf die eigenen Toten Ameisen gewichtet
			w3 = 0;
			break;
			
		case NEUTRAL:
			w1 = 1;
			w2 = -1;
			w3 = 0;
			break;

		default:
			w1 = 1;
			w2 = -1;
			break;
		}
		if(!( w1 * t1 + w2 * t2 == 0 && t1!= 0 && t2!= 0)){
			w3 = directionPoint;
		}else{
			w3 = -1;
		}
		return w1 * t1 + w2 * t2 + w3 * t3;
	}

	private double calculateDistanceGain() {
		double prevsum = 0;
		for (Ant ant : board.getMyAnts()) {
			double prev = 0;
			for (Ant enemy : enemyAntsToGo) {
				prev = prev + board.getDistance(ant.getAntPosition(), enemy.getAntPosition());
			}
			prevsum = prevsum + prev/(double)enemyAntsToGo.size();
		}
		
		
		double nowsum = 0;
		for (Ant ant : myAntsToGo) {
			double now = 0;
			for (Ant enemy : enemyAntsToGo) {
				now = now + board.getDistance(ant.getAntPosition(), enemy.getAntPosition());
			}
			nowsum = nowsum + now/(double)enemyAntsToGo.size();
		}
		return prevsum - nowsum;
	}

	public LinkedList<Ant> getMyAntsToGo() {
		return myAntsToGo;
	}

	public LinkedList<Ant> getEnemyAntsToGo() {
		return enemyAntsToGo;
	}

	public GameInformations getBoard() {
		return board;
	}

	public void setMyDeadAnts(int deadAnts) {
		myDeadAnts = myDeadAnts + deadAnts;
		
	}

	public void setEnemyDeadAnts(int deadAnts) {
		enemyDeadAnts = enemyDeadAnts + deadAnts;
	}

	public void setDirectionPoint(int directionPoint) {
		this.directionPoint = this.directionPoint + directionPoint;
	}
	
	
	
	
}
