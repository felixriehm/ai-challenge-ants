package de.htwg_konstanz.antbots.common_java_package.controller.attack;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.htwg_konstanz.antbots.common_java_package.controller.Ant;
import de.htwg_konstanz.antbots.common_java_package.controller.GameInformations;
import de.htwg_konstanz.antbots.common_java_package.model.Aim;
import de.htwg_konstanz.antbots.common_java_package.model.Ilk;
import de.htwg_konstanz.antbots.common_java_package.model.Order;
import de.htwg_konstanz.antbots.common_java_package.model.Tile;

public class MaxN {
	
	public enum Strategy{
		AGGRESSIVE,
		PASSIVE,
		NEUTRAL
	}
	
	LinkedList<Order> bestMove;
	GameInformations board;
	int startDepth;
	int boundSum;
	
	LinkedList<Aim> possibleDirections;
	LinkedList<Order> possibleMovesTmp;
	LinkedList<LinkedList<Ant>> playerAnts;
	
	CommandManager cm;
	Strategy gameStrategy;
	
	public MaxN(){
		// Liste die alle m�glichen Bewegungsm�glichkeiten beinhaltet
		possibleDirections = new LinkedList<Aim>();
		possibleDirections.add(Aim.DONTMOVE);
		possibleDirections.add(Aim.NORTH);
		possibleDirections.add(Aim.SOUTH);
		possibleDirections.add(Aim.EAST);
		possibleDirections.add(Aim.WEST);
	}
	
	public LinkedList<Order> attack(GameInformations board, int depth, Strategy st, List<Set<Ant>> playerAnts){
		// Variablen initialisieren
		bestMove = null;
		this.board = board;
		boundSum = 42;
		this.playerAnts = new LinkedList<>();
		
		// Ameisen in eine Liste einf�gen. Index 0 ist eigener Spieler
		for (Set<Ant> antList : playerAnts) {
			LinkedList<Ant> tmp = new LinkedList<Ant>();
			for (Ant ant : antList) {
				tmp.add(new Ant(new Tile(ant.getAntPosition().getRow(),ant.getAntPosition().getCol()),ant.getId()));
			}
			this.playerAnts.addLast(tmp);
		}
		
		// Weitere Variablen initialisieren
		startDepth = depth*this.playerAnts.size();
		cm = new CommandManager();
		gameStrategy = st;
		
		// Der eigentliche Algorithmus
		maxn(depth*this.playerAnts.size(), this.playerAnts.get(0), boundSum);
		
		return bestMove;
	}

	private LinkedList<Integer> maxn(int depth, LinkedList<Ant> ants, int bound) {
		// Rekursionsende erreicht
	    if (depth == 0 ){
	       return evaluation(gameStrategy);
	    }
	    // Liste mit erstellen in die die m�glichen Z�ge gespeichert werden
	    LinkedList<LinkedList<Order>> possibleMoves = new LinkedList<LinkedList<Order>>();
	    // Liste in die die Z�ge tempor�r in der Methode genratePossibleMoves gespeichert werden
	    // �ndert sich w�hrrend der Rekursion bei genratePossibleMoves
	    possibleMovesTmp = new LinkedList<Order>();
	    generatePossibleMoves(ants.size(), ants, possibleMoves);
	    
	    // Ersten Zug rausnehmen und ausf�hren
	    LinkedList<Order> firstChild = possibleMoves.poll();
    	ExecuteNextMove(firstChild,ants);
    	if (depth == startDepth){
			bestMove = firstChild;
    	}
    	// Aktuellen Spieler berechnen der in dieser Rekursionstiefe dran ist und rekursion fortsetzen
    	int player = Math.abs(depth-startDepth)%playerAnts.size();
	    LinkedList<Integer> best = maxn(depth-1, playerAnts.get((player+1)%playerAnts.size()),boundSum);
	    UndoMove(firstChild);
	    
	    // Die restlichen Z�ge werden hier bearbeitet
	    while (!possibleMoves.isEmpty()) {
	    	// hier findet das Shallow prunning statt
	    	if(best.get(player) >= bound){
	    		return best;
	    	}
	    	
	    	// N�chster Zug entnehmen und ausf�hren
	    	LinkedList<Order> childMove = possibleMoves.poll();
	    	ExecuteNextMove(childMove,ants);
	    	LinkedList<Integer> current = maxn(depth-1,playerAnts.get((player+1)%playerAnts.size()),boundSum-best.get(player));
	    	UndoMove(childMove);

	    	// Den bessere Evaluationsvektor nehmen und falls bei der Wurzel angekommen bestMove neu zuweisen
	    	// falls der neue besser ist
	    	if (current.get(player) > best.get(player)) {
	    		best = current;
	    		if (depth == startDepth){
	    			bestMove = childMove;
	    		}
	       }
	    }
	    return best;
	 }
	
	// Zug r�ckg�ngig machen
	private void UndoMove(LinkedList<Order> childMove) {
		cm.undo();
	}

	// Zug ausf�hren
	private void ExecuteNextMove(LinkedList<Order> childMove, LinkedList<Ant> antsToGo){
		//cm.executeCommand(new MoveCommand(childMove, antsToGo));
	}
	
	private void generatePossibleMoves(int depth, LinkedList<Ant> antsToGo, LinkedList<LinkedList<Order>> possibleMoves) {
		// Rekursionstiefe erreicht
		if (depth <= 0){
			// Hier werden die Z�ge die tempor�r in possibleMovesTmp (�ndert sich beim zur�ckspringen) gespeichert wurden
			// in eine neue Liste hinzugef�gt und diese dann der possibleMoves Liste
			LinkedList<Order> tmp = new LinkedList<Order>();
			for (Order order : possibleMovesTmp) {
				tmp.add(order);
			}
			possibleMoves.add(tmp);
		    return;
		}
		// Die erste Ameise aus der Liste nehmen
		Ant ant = antsToGo.get(depth-1); 
		
		// F�r jede Ameise alle Bewegungsm�glichkeiten durchgehen
		for (Aim aim : possibleDirections) {
			// Bedingungen f�r einen nciht g�ltigen Bewgungszug ansonsten zu possibleMovesTmp hinzuf�gen
			if(board.getTile(ant.getAntPosition(), aim).getType() == Ilk.WATER/* ||
					(board.getTile(ant.getAntPosition(), aim).getType() == Ilk.MY_ANT && !(ant.getAntPosition().getRow() == ant.getAntPosition().getRow()+aim.getRowDelta() && ant.getAntPosition().getCol() == ant.getAntPosition().getCol()+aim.getColDelta()))*/){
				continue;
			}else{
				boolean skip = false;
				for (Order order1 : possibleMovesTmp) {
					for (Order order2 : possibleMovesTmp) {
						if(order1.getNewPosition().equals(order2.getNewPosition()) && !order1.equals(order2)){
							skip = true;
						}
					}
				}
				if(skip){
					// nicht vollkommen sicher zb wenn eien ameise alle Wege blockiert bekommt, bzw kreuzen
					continue;
				}
				possibleMovesTmp.addFirst(new Order(ant.getAntPosition(), aim));
			}
			// Weiter in die Rekursion gehen d.h. n�chste Ameise betrachten
			generatePossibleMoves(depth-1, antsToGo, possibleMoves);
			// Beim zur�ckspringen muss der letzte Zug entfernt werden
			try {
				possibleMovesTmp.removeFirst();
			} catch (Exception e) {

			}
		}
	}
	
	private LinkedList<Integer> evaluation(Strategy strategy) {
		// Liste f�r die Anzahl toter Ameisen pro Spieler
		LinkedList<Integer> PlayersDeadAnts = new LinkedList<>();
		// Hier werden die Ameisen einzeln in eine Liste gespeichert
		LinkedList<Ant> allAnts = new LinkedList<>();
		for(LinkedList<Ant> ants : playerAnts){
			allAnts.addAll(ants);
		}
		// F�r jede Ameise wird deren Schw�che berechent mit der Methode enemies()
		// Dazu werden erst die eigenen Ameisen f�r den jeweiligen Spieler der am Zug ist
		// entfernt und die restlichen als gegnerischen Ameise der Methode �bergeben
		// danach werden die Ameisen wieder hinzugef�gt da f�r jeden Spieler das gleiche
		// getan werden muss
		for(LinkedList<Ant> ants : playerAnts){
			allAnts.removeAll(ants);
			enemies(ants,allAnts);
			PlayersDeadAnts.addLast(calculateDeadAnts(ants, allAnts));
			allAnts.addAll(ants);
		}
		
		// Erstellen des Evaltionsvektor
		LinkedList<Integer> result = new LinkedList<Integer>();
		
		switch (strategy) {
		case AGGRESSIVE:
			// F�r Spieler i wird der Evaluationswert bestimmt indem die Summe der toten Ameisen
			// der anderen Spieler berechnet wird. Das Resultat wird noch mit den directionPoints
			// addiert
			// Sagt also aus wieviel Ameisen der Spieler get�tet hat
			for (int i = 0; i < playerAnts.size(); i++) {
				int ownDeadAnts = PlayersDeadAnts.remove(i);
				int deadAntsSum = 0;
				for(int deadAnts : PlayersDeadAnts){
					deadAntsSum = deadAntsSum + deadAnts;
				}
				result.add(i,deadAntsSum+directionPoints(i));
				PlayersDeadAnts.add(i,ownDeadAnts);
			}
			break;
			
		case PASSIVE:
			// F�r Spieler i wird der Evaluationswert bestimmt indem die Summe der toten Ameisen
			// aller Spieler berechnet wird. Dieser Wert wird mit den eignen toten Ameisen subtrahiert
			// Das Resultat wird noch mit den directionPoints addiert
			// Sagt also aus wieviel Ameisen der jeweilige Spieler verloren hat
			int deadAntsSum = 0;
			for(int deadAnts : PlayersDeadAnts){
				deadAntsSum = deadAntsSum + deadAnts;
			}
			for (int i = 0; i < playerAnts.size(); i++) {
				result.add(i,deadAntsSum-PlayersDeadAnts.get(i)+directionPoints(i)); 
			}
			break;
			
		case NEUTRAL:
			// TODO Neutral Strategy
			break;

		default:
			for (int i = 0; i < playerAnts.size(); i++) {
				result.add(0);
			}
			break;
		}
		
		// Der Evaluationsvektor wird nun mit der SOS-MAtrix (Social Oriented Search) multiplizeirt
		LinkedList<Double> sosResult = sosDotProduct(result);
		
		// Der Evaluationsvektor wird normiert auf 42. Das muss sein da das Shallow Prunning
		// bei Mehrspieler mit "Bounds" arbeitet. 42 ist der globale bound. D.h. die Summe 
		// der Werte in dem Evaluationsvektor ergibt 42.
		LinkedList<Integer> resultNorm = new LinkedList<Integer>();
		double doubleSum = 0;
		for (Double value : sosResult) {
			doubleSum = doubleSum + value;
		}
		if(doubleSum != 0){
			for (Double value : sosResult) {
				resultNorm.addLast((int)Math.round(value*42/doubleSum));
			}
		}else{
			for (Double value : sosResult) {
				resultNorm.addLast(0);
			}
		}
		
		return resultNorm;
	}
	
	private LinkedList<Double> sosDotProduct(LinkedList<Integer> evaluation){
		// nur positive werte erlaubt wegen shallow prunning
		// Die SOS-Matrix baut sich w�hrrend dem Spiel dynamisch auf. Je mehr Spieler
		// im Kampf beteiligt sind desto gr��er wird die Matrix.
		LinkedList<LinkedList<Double>> sos = new LinkedList<>();
		for (int i = 0; i < playerAnts.size(); i++) {
			LinkedList<Double> row = new LinkedList<>();
			for (int j = 0; j < playerAnts.size(); j++) {
				if(i == j){
					if(i == 1){
						row.addLast(7.0);
					}else{
						row.addLast(1.0);
					}
				}else{
					if(i != j && j == 1){
						row.addLast(0.5);
					}else{
						row.addLast(0.0);
					}
				}
			}
			sos.add(row);
		}
		
		// Hier wird der Evaluationsvektor mit der Matrix multipliziert und in einen
		// neuen Vektor gespeichert
		LinkedList<Double> result = new LinkedList<>();
		for (LinkedList<Double> row : sos) {
			double vectorSum = 0;
			for (int i = 0; i < row.size(); i++) {
				vectorSum = vectorSum + row.get(i)* evaluation.get(i);
			}
			result.addLast(vectorSum);
		}
		
		// Der Evalutionsvektor ist ein n-Tupel die Zahl an dem Index i ist der gewonnene Wert f�r
		// Spieler i
		return result;
	}

	// Vielleicht kann man diese Methode irgenwann wiederverwenden. Berechent die gewonnen Distanz bei einem
	// Zug zu den gegnerischen Ameisen. DirectionPoints wird im moment daf�r verwenden.
	private int calculateDistanceGain(int player) {
		LinkedList<Ant> allEnemyAnts = new LinkedList<>();
		for (int i = 1; i < playerAnts.size(); i++) {
			allEnemyAnts.addAll(playerAnts.get(i));
		}
		double prevsum = 0;
		for (Ant ant : board.getMyAnts()) {
			double prev = 0;
			for (Ant enemy : allEnemyAnts) {
				prev = prev + board.getDistance(ant.getAntPosition(), enemy.getAntPosition());
			}
			prevsum = prevsum + prev/(double)allEnemyAnts.size();
		}
		
		
		double nowsum = 0;
		for (Ant ant : playerAnts.get(0)) {
			double now = 0;
			for (Ant enemy : allEnemyAnts) {
				now = now + board.getDistance(ant.getAntPosition(), enemy.getAntPosition());
			}
			nowsum = nowsum + now/(double)allEnemyAnts.size();
		}
		
		if(prevsum - nowsum < 0){
			return 0;
		}else{
			return (int)Math.ceil(prevsum - nowsum);
		}
		
	}
	
	// Berechent f�r einen Spieler Punkte. Indem f�r jede Ameise dieses Spielers gepr�ft wird
	// ob diese sich in Richtung des Gegners bewegt hat. Hat sie das getan erh�ht sich
	// die Punktzahl. F�rdert das die Ameisen zu den gegnerischen Ameisen hin laufen.
	private int directionPoints(int player){
		LinkedList<Ant> removedAnts = playerAnts.remove(player);
		int pointCounter = 0;
		for (Ant ant : removedAnts) {
			int enemyTeam = (int)((Math.random()) * playerAnts.size()-1 + 0);
			for (Aim aim : board.getDirections(ant.getAntPosition(), playerAnts.get(enemyTeam).get((int)((Math.random()) * playerAnts.get(enemyTeam).size()-1 + 0)).getAntPosition())) {
				if(ant.getexecutedDirection() == aim){
					pointCounter++;
				}
			}
			
		}
		playerAnts.add(player,removedAnts);
		return pointCounter;
	}

	// Hier werden die Anzahl der toten Ameisen berechent. Zuvor muss enemies() mit allen anderen Ameisen-Teams
	// aufgerufen werden. Der Algorithmus ist gleich wie aus der Simulation. Muss seperat in einer Methode berechnet werden
	// da die Schw�che aller Ameisen bekannt sein m�ssen. Bevor man die Todesbedingung pr�fen kann.
	private int calculateDeadAnts(LinkedList<Ant> myAntsToGo, LinkedList<Ant> enemyAntsToGo) {
		int myDeadAnts = 0;

		for (Ant ant : myAntsToGo) {
			LinkedList<Ant> enemiesInAttackRadius = ant.getEnemiesinAttackRadius();
			for (Ant enemy : enemiesInAttackRadius) {
				if(ant.getWeakness() >= enemy.getWeakness()){
					myDeadAnts++;
					break;
				}
			}
		}
		return myDeadAnts;
	}
	
	// Hier werden f�r Ameisen eines Spielers deren Schw�che berechnet. Der Algorithmus ist gleich wie 
	// vaus der Simulation. In der Methode werden vorerst nur aber nur die Schw�che erh�ht die
	// in dem Ameisen-Objekt gewspeichert wird.
	private void enemies(LinkedList<Ant> ants, LinkedList<Ant> enemyAnts){
		for (Ant ant : ants) {
			int weakness = 0;
			LinkedList<Ant> enemiesInAttackRadius = new LinkedList<Ant>();
			Set<Tile> attackRadiusTiles= board.getTilesInAttackRadius(ant.getAntPosition(), (int)Math.sqrt(board.getAttackRadius2()));
			for (Tile tile : attackRadiusTiles) {
				for (Ant enemy : enemyAnts) {
					if(tile.getRow() == enemy.getAntPosition().getRow() && tile.getCol() == enemy.getAntPosition().getCol()){
						weakness++;
						enemiesInAttackRadius.add(enemy);
					}
				}
				
			}
			ant.setWeakness(weakness);
			ant.setEnemiesinAttackRadius(enemiesInAttackRadius);
		}
	}
}

