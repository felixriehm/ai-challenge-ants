package de.htwg_konstanz.antbots.common_java_package.controller;

import java.util.*;

import de.htwg_konstanz.antbots.common_java_package.model.Aim;
import de.htwg_konstanz.antbots.common_java_package.model.Configuration;
import de.htwg_konstanz.antbots.common_java_package.model.Ilk;
import de.htwg_konstanz.antbots.common_java_package.model.Order;
import de.htwg_konstanz.antbots.common_java_package.model.Tile;

/**
 * Holds all game data and current game state.
 */
public class GameInformations {
	/** Maximum map size. */
	public static final int MAX_MAP_SIZE = 256 * 2;

	private final int loadTime;

	private final int turnTime;

	private static int rows;

	private static int cols;

	// current turn
	private int turn;

	private final int turns;

	private final int viewRadius2;

	private final int attackRadius2;

	private final int spawnRadius2;

	private final Set<Tile> vOffsets;

	private long turnStartTime;

	private final boolean visible[][];

	private static Tile map[][];

	private Set<Ant> myAntsDangered = new HashSet<Ant>();

	private final List<Ant> myAnts = new LinkedList<Ant>();

	private final Set<Tile> myHills = new HashSet<Tile>();

	private final Set<Tile> enemyHills = new HashSet<Tile>();

	private static FoodManager foodManager = new FoodManager();

	private final Set<Order> orders = new HashSet<Order>();

	private Set<Ant> enemyAnts = new HashSet<Ant>();

	private int explorerAnts = 0;

	private int antIdCounter = 0;

	private int enemyIdCounter = 0;

	private static Logger logger;

	/**
	 * Creates new {@link GameInformations} object.
	 * 
	 * @param loadTime
	 *            timeout for initializing and setting up the bot on turn 0
	 * @param turnTime
	 *            timeout for a single game turn, starting with turn 1
	 * @param rows
	 *            game map height
	 * @param cols
	 *            game map width
	 * @param turns
	 *            maximum number of turns the game will be played
	 * @param viewRadius2
	 *            squared view radius of each ant
	 * @param attackRadius2
	 *            squared attack radius of each ant
	 * @param spawnRadius2
	 *            squared spawn radius of each ant
	 */
	public GameInformations(int loadTime, int turnTime, int rows, int cols,
			int turns, int viewRadius2, int attackRadius2, int spawnRadius2) {
		this.loadTime = loadTime;
		this.turnTime = turnTime;
		this.rows = rows;
		this.cols = cols;
		this.turn = 0;
		this.turns = turns;
		this.viewRadius2 = viewRadius2;
		this.attackRadius2 = attackRadius2;
		this.spawnRadius2 = spawnRadius2;

		// map
		map = new Tile[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				map[i][j] = new Tile(i, j);
			}
		}

		// visible
		visible = new boolean[rows][cols];
		for (boolean[] row : visible) {
			Arrays.fill(row, false);
		}

		// calc vision offsets
		vOffsets = new HashSet<Tile>();
		int mx = (int) Math.sqrt(viewRadius2);
		for (int row = -mx; row <= mx; ++row) {
			for (int col = -mx; col <= mx; ++col) {
				int d = row * row + col * col;
				if (d <= viewRadius2) {
					vOffsets.add(new Tile(row, col));
				}
			}
		}

	}

	public Tile[][] getMap() {
		return map;
	}

	/**
	 * @return the map as a set
	 */
	public Set<Tile> getMapSet() {
		Set<Tile> set = new HashSet<Tile>();
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				set.add(map[i][j]);
			}
		}
		return set;
	}

	public void increaseExplorerAnts() {
		explorerAnts++;
	}

	public void decreaseExplorerAnts() {
		explorerAnts--;
	}

	public int getExplorerAnts() {
		return explorerAnts;
	}

	public Tile getTileOfMap(Tile t) {
		return map[t.getRow()][t.getCol()];
	}

	/**
	 * returns all neighbourth of tile t
	 * 
	 * @param t
	 * @return
	 */
	public Map<Tile, Aim> getNeighbour(Tile t) {
		Map<Tile, Aim> list = new HashMap<Tile, Aim>();
		for (Aim a : Aim.values()) {
			if (a != Aim.DONTMOVE)
				list.put(getTile(t, a), a);
		}
		return list;
	}

	/**
	 * getNeighbour without Water
	 * 
	 * @param t
	 * @return
	 */
	public Map<Tile, Aim> getMoveAbleNeighbours(Tile t) {
		Map<Tile, Aim> list = new HashMap<Tile, Aim>();
		for (Aim a : Aim.values()) {
			if (a != Aim.DONTMOVE && getIlk(t) != Ilk.WATER) {
				Tile tile = getTile(t, a);
				if (getIlk(tile) != Ilk.WATER) {
					list.put(tile, a);
				}
			}
		}
		return list;
	}

	public LinkedList<Aim> getMoveAbleDirections(Tile t) {
		LinkedList<Aim> list = new LinkedList<Aim>();
		for (Aim a : Aim.values()) {
			Tile tile = getTile(t, a);
			if (getIlk(tile) != Ilk.WATER && getIlk(tile) != Ilk.HILL) {
				list.add(a);
			}
		}
		return list;
	}

	/**
	 * Returns timeout for initializing and setting up the bot on turn 0.
	 * 
	 * @return timeout for initializing and setting up the bot on turn 0
	 */
	public int getLoadTime() {
		return loadTime;
	}

	/**
	 * Returns timeout for a single game turn, starting with turn 1.
	 * 
	 * @return timeout for a single game turn, starting with turn 1
	 */
	public int getTurnTime() {
		return turnTime;
	}

	/**
	 * Returns game map height.
	 * 
	 * @return game map height
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * Returns game map width.
	 * 
	 * @return game map width
	 */
	public int getCols() {
		return cols;
	}

	/**
	 * Returns all Tiles in viewRadius range. The Pythagorean theorem is used.
	 * This Method dosn't uses breadth-first search.
	 * 
	 * @param tile
	 * @param radius
	 * @return
	 */
	public Set<Tile> getTilesInRadius(Tile tile, int radius) {
		Set<Tile> tiles = new HashSet<Tile>();

		for (Tile locOffset : getTilesOffsetRadius(radius)) {
			Tile newLoc = getTile(tile, locOffset);
			tiles.add(newLoc);
		}

		return tiles;
	}

	private Set<Tile> getTilesOffsetRadius(int radius) {
		Set<Tile> vOffsets = new HashSet<Tile>();
		int mx = radius;
		for (int row = -mx; row <= mx; ++row) {
			for (int col = -mx; col <= mx; ++col) {
				int d = row * row + col * col;
				if (d <= radius * radius) {
					vOffsets.add(new Tile(row, col));
				}
			}
		}
		return vOffsets;
	}

	/**
	 * Returns all Tiles in attackRadius range. The Pythagorean theorem is used.
	 * No breadth-first search is used.
	 * 
	 * @param tile
	 * @param radius
	 * @return
	 */
	public Set<Tile> getTilesInAttackRadius(Tile tile, int radius) {

		Set<Tile> vOffsets = new HashSet<Tile>();
		int mx = radius;
		for (int row = -mx; row <= mx; ++row) {
			for (int col = -mx; col <= mx; ++col) {
				int d = row * row + col * col;
				if (d <= radius * radius + 1) {
					vOffsets.add(new Tile(row, col));
				}
			}
		}

		Set<Tile> tiles = new HashSet<Tile>();

		for (Tile locOffset : vOffsets) {
			Tile newLoc = getTile(tile, locOffset);
			tiles.add(newLoc);
		}

		return tiles;
	}

	/**
	 * Next Turn. Is called by the bot itself.
	 */
	public void increaseTurn() {
		turn++;
	}

	/**
	 * A turn is the timeunit of an antbot. It goes from 1 (starting phase) to
	 * getTurns().
	 * 
	 * @return turn
	 */
	public int getCurrentTurn() {
		return turn;
	}

	public List<Tile> getUnknowTilesToExplore(Collection<Tile> c) {
		List<Tile> tilesToExplore = new LinkedList<>();

		for (Tile t : c) {
			if (t.getType() == Ilk.UNKNOWN) {
				tilesToExplore.add(t);
			}
		}
		return tilesToExplore;
	}

	public List<Tile> getValueTilesToExplore(Collection<Tile> c) {
		List<Tile> tilesToExplore = new LinkedList<>();

		for (Tile t : c) {
			tilesToExplore.add(t);
		}

		tilesToExplore.sort(new TilesComperator());

		return tilesToExplore;
	}

	class TilesComperator implements Comparator<Tile> {

		@Override
		public int compare(Tile o1, Tile o2) {

			return o1.getDiscoverdAtTurn() - o2.getDiscoverdAtTurn();
		}

	}

	/**
	 * True if the game is in the setup turn.
	 * 
	 * @author Chrisi
	 * @return
	 */
	public boolean isSetupTurn() {
		return getCurrentTurn() == 0;
	}

	/**
	 * Returns maximum number of turns the game will be played.
	 * 
	 * @return maximum number of turns the game will be played
	 */
	public int getTurns() {
		return turns;
	}

	/**
	 * Returns squared view radius of each ant.
	 * 
	 * @return squared view radius of each ant
	 */
	public int getViewRadius2() {
		return viewRadius2;
	}

	/**
	 * Returns squared attack radius of each ant.
	 * 
	 * @return squared attack radius of each ant
	 */
	public int getAttackRadius2() {
		return attackRadius2;
	}

	/**
	 * Returns squared spawn radius of each ant.
	 * 
	 * @return squared spawn radius of each ant
	 */
	public int getSpawnRadius2() {
		return spawnRadius2;
	}

	/**
	 * Sets turn start time.
	 * 
	 * @param turnStartTime
	 *            turn start time
	 */
	public void setTurnStartTime(long turnStartTime) {
		this.turnStartTime = turnStartTime;
	}

	/**
	 * Returns how much time the bot has still has to take its turn before
	 * timing out.
	 * 
	 * @return how much time the bot has still has to take its turn before
	 *         timing out
	 */
	public int getTimeRemaining() {
		return turnTime - (int) (System.currentTimeMillis() - turnStartTime);
	}

	/**
	 * Returns ilk at the specified location.
	 * 
	 * @param tile
	 *            location on the game map
	 * 
	 * @return ilk at the <cod>tile</code>
	 */
	public Ilk getIlk(Tile tile) {
		return map[tile.getRow()][tile.getCol()].getType();
	}

	/**
	 * Sets ilk at the specified location.
	 * 
	 * @param tile
	 *            location on the game map
	 * @param ilk
	 *            ilk to be set at <code>tile</code>
	 */
	public void setIlk(Tile tile, Ilk ilk) {
		map[tile.getRow()][tile.getCol()].setType(ilk);
	}

	/**
	 * Returns ilk at the location in the specified direction from the specified
	 * location.
	 * 
	 * @param tile
	 *            location on the game map
	 * @param direction
	 *            direction to look up
	 * 
	 * @return ilk at the location in <code>direction</code> from
	 *         <cod>tile</code>
	 */
	public Ilk getIlk(Tile tile, Aim direction) {
		Tile newTile = getTile(tile, direction);
		return map[newTile.getRow()][newTile.getCol()].getType();
	}

	/**
	 * Returns location in the specified direction from the specified location.
	 * 
	 * @param tile
	 *            location on the game map
	 * @param direction
	 *            direction to look up
	 * 
	 * @return location in <code>direction</code> from <cod>tile</code>
	 */
	public static Tile getTile(Tile tile, Aim direction) {
		int row = (tile.getRow() + direction.getRowDelta()) % rows;
		if (row < 0) {
			row += rows;
		}
		int col = (tile.getCol() + direction.getColDelta()) % cols;
		if (col < 0) {
			col += cols;
		}
		return map[row][col];
	}

	/**
	 * Returns location with the specified offset from the specified location.
	 * 
	 * @param tile
	 *            location on the game map
	 * @param offset
	 *            offset to look up
	 * 
	 * @return location with <code>offset</code> from <cod>tile</code>
	 */
	public Tile getTile(Tile tile, Tile offset) {
		int row = (tile.getRow() + offset.getRow()) % rows;
		if (row < 0) {
			row += rows;
		}
		int col = (tile.getCol() + offset.getCol()) % cols;
		if (col < 0) {
			col += cols;
		}
		return map[row][col];
	}

	/**
	 * Returns a set containing all my ants locations.
	 * 
	 * @return a set containing all my ants locations
	 */
	public List<Ant> getMyAnts() {
		return myAnts;
	}

	public List<Tile> getMyTiles() {
		List<Tile> myTiles = new LinkedList<>();

		for (Ant a : myAnts) {
			myTiles.add(a.getAntPosition());
		}

		return myTiles;
	}

	/**
	 * Returns a set containing all enemy ants locations.
	 * 
	 * @return a set containing all enemy ants locations
	 */
	public Set<Ant> getEnemyAnts() {
		return enemyAnts;
	}

	/**
	 * Returns a set containing all my hills locations.
	 * 
	 * @return a set containing all my hills locations
	 */
	public Set<Tile> getMyHills() {
		return myHills;
	}

	/**
	 * Returns a set containing all enemy hills locations.
	 * 
	 * @return a set containing all enemy hills locations
	 */
	public Set<Tile> getEnemyHills() {
		return enemyHills;
	}

	/**
	 * Returns all orders sent so far.
	 * 
	 * @return all orders sent so far
	 */
	public Set<Order> getOrders() {
		return orders;
	}

	public boolean[][] getVisibleTilesAsArray() {
		return visible;
	}

	/**
	 * Returns true id a location is unknown
	 * 
	 * @param tile
	 * @return
	 */
	public boolean isUnknown(Tile tile) {
		return tile.getType() == Ilk.UNKNOWN;
	}

	/**
	 * Calculates distance between two locations on the game map. Gives the
	 * ManhattanDistance!
	 * 
	 * @param t1
	 *            one location on the game map
	 * @param t2
	 *            another location on the game map
	 * 
	 * @return distance between <code>t1</code> and <code>t2</code>
	 */
	public int getDistance(Tile t1, Tile t2) {
		int rowDelta = Math.abs(t1.getRow() - t2.getRow());
		int colDelta = Math.abs(t1.getCol() - t2.getCol());
		rowDelta = Math.min(rowDelta, rows - rowDelta);
		colDelta = Math.min(colDelta, cols - colDelta);
		return rowDelta + colDelta;
	}

	/**
	 * Returns one or two orthogonal directions from one location to the
	 * another.
	 * 
	 * @param t1
	 *            one location on the game map
	 * @param t2
	 *            another location on the game map
	 * 
	 * @return orthogonal directions from <code>t1</code> to <code>t2</code>
	 */
	public List<Aim> getDirections(Tile t1, Tile t2) {
		List<Aim> directions = new ArrayList<Aim>();
		if (t1.getRow() < t2.getRow()) {
			if (t2.getRow() - t1.getRow() >= rows / 2) {
				directions.add(Aim.NORTH);
			} else {
				directions.add(Aim.SOUTH);
			}
		} else if (t1.getRow() > t2.getRow()) {
			if (t1.getRow() - t2.getRow() >= rows / 2) {
				directions.add(Aim.SOUTH);
			} else {
				directions.add(Aim.NORTH);
			}
		}
		if (t1.getCol() < t2.getCol()) {
			if (t2.getCol() - t1.getCol() >= cols / 2) {
				directions.add(Aim.WEST);
			} else {
				directions.add(Aim.EAST);
			}
		} else if (t1.getCol() > t2.getCol()) {
			if (t1.getCol() - t2.getCol() >= cols / 2) {
				directions.add(Aim.EAST);
			} else {
				directions.add(Aim.WEST);
			}
		}
		return directions;
	}

	/**
	 * Clears game state information about my ants locations.
	 */
	public void clearMyAnts() {
		for (Ant ant : myAnts) {
			Tile position = ant.getPosBefore();
			if (position != null) {
				map[position.getRow()][position.getCol()].setType(Ilk.LAND);
			}
		}
	}

	/**
	 * Clears game state information about enemy ants locations.
	 */
	public void clearEnemyAnts() {
		for (Ant enemyAnt : enemyAnts) {
			Tile position = enemyAnt.getAntPosition();
			map[position.getRow()][position.getCol()].setType(Ilk.LAND);
		}
		enemyAnts.clear();
	}

	/**
	 * Clears game state information about my hills locations.
	 */
	public void clearMyHills() {
		myHills.clear();
	}

	/**
	 * Clears game state information about enemy hills locations.
	 */
	public void clearEnemyHills() {
		enemyHills.clear();
	}

	/**
	 * Clears game state information about dead ants locations.
	 */
	public void clearDeadAnts() {
		// currently we do not have list of dead ants, so iterate over all map
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++) {
				if (map[row][col].getType() == Ilk.DEAD) {
					map[row][col].setType(Ilk.LAND);
				}
			}
		}
	}

	/**
	 * Clears visible information
	 */
	public void clearVision() {
		for (int row = 0; row < rows; ++row) {
			for (int col = 0; col < cols; ++col) {
				visible[row][col] = false;
			}
		}
	}

	/**
	 * Calculates visible information
	 */
	public void setVision() {
		for (Ant ant : myAnts) {
			for (Tile locOffset : vOffsets) {
				Tile newLoc = getTile(ant.getAntPosition(), locOffset);
				visible[newLoc.getRow()][newLoc.getCol()] = true;

				// for exploration
				newLoc.setDiscoverdAtTurn(turn);

				if (newLoc.getType() == Ilk.UNKNOWN) {
					newLoc.setType(Ilk.LAND);
				}
			}
		}
	}

	public static FoodManager getFoodManager() {
		return foodManager;
	}

	/**
	 * Updates game state information about new ants and food locations.
	 * 
	 * @param ilk
	 *            ilk to be updated
	 * @param tile
	 *            location on the game map to be updated
	 */
	public void update(Ilk ilk, Tile tile) {

		map[tile.getRow()][tile.getCol()].setType(ilk);
		switch (ilk) {
		case FOOD:
			foodManager.update(tile);
			break;
		case MY_ANT:
			boolean set = false;

			if (turn == 1) {
				myAnts.add(new Ant(tile, antIdCounter));
			}
			for (Ant ant : myAnts) {
				if (ant.getAntPosition().equals(tile)) {
					set = true;
				}

			}
			if (set == true) {

			} else {
				myAnts.add(new Ant(tile, antIdCounter));

			}

			break;
		case ENEMY_ANT:
			enemyAnts.add(new Ant(tile, enemyIdCounter));
			break;
		}
		antIdCounter++;
		enemyIdCounter++;
	}

	/**
	 * Updates game state information about hills locations.
	 * 
	 * @param owner
	 *            owner of hill
	 * @param tile
	 *            location on the game map to be updated
	 */
	public void updateHills(int owner, Tile tile) {
		if (owner > 0){
			enemyHills.add(tile);
			map[tile.getRow()][tile.getCol()].setType(Ilk.ENEMY_HILL);
		}else{
			myHills.add(tile);
			map[tile.getRow()][tile.getCol()].setType(Ilk.HILL);
		}
	}

	/**
	 * Issues an order by sending it to the system output.
	 * 
	 * @param myAnt
	 *            map tile with my ant
	 * @param direction
	 *            direction in which to move my ant
	 */
	public void issueOrder(Tile myAnt, Aim direction) {
		Order order = new Order(myAnt, direction);
		orders.add(order);
		System.out.println(order);
	}

	/**
	 * Issues an order by sending it to the system output.
	 * 
	 * @param order
	 *            an order
	 */
	public void issueOrder(Order order) {
		orders.add(order);
		System.out.println(order);
	}

	public static void setLogger(Logger logger1) {
		logger = logger1;

	}

	public static Logger getLogger() {
		return logger;
	}

	public Set<Ant> getMyAntsDangered() {
		return myAntsDangered;
	}

	public void setMyAntDangered(Set<Ant> myAnts) {
		myAntsDangered = myAnts;
	}

	public Set<Ant> getOwnNotDangeredAnts() {
		Set<Ant> notDangerd = new HashSet<>();

		for (Ant ant : myAnts) {
			if (!ant.isDanger()) {
				notDangerd.add(ant);
			}
		}
		return notDangerd;
	}

	/**
	 * gibt alle eigenen Ameisen im SIchtfeld zur�ck
	 */
	public Set<Ant> getOwnAntsInViewRadiusNotDangered(Ant ant, int antSize) {
		Set<Ant> myAntsInViewRadius = new HashSet<>();
		Set<Tile> tilesInViewRadius = getTilesInRadius(ant.getAntPosition(),
				(int) Math.sqrt(getViewRadius2()));
		for (Ant myAnt : getMyAnts()) {
			if (!myAnt.isDanger()
					&& tilesInViewRadius.contains(myAnt.getAntPosition())) {
				if (antSize + myAntsInViewRadius.size() <= Configuration.GROUPSIZE) {
					myAntsInViewRadius.add(myAnt);
				}

			}
		}
		return myAntsInViewRadius;
	}
}
