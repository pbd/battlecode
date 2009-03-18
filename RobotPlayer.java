package sourceminer;

import battlecode.common.*;

public class RobotPlayer implements Runnable {

	static int DUZO = 100000;

	static int WORKERS = 1;

	private final RobotController myRC;

	Direction dir = Direction.NONE;

	int runda = 0;

	Robot tab[];

	MapLocation mojArchon;

	RobotInfo robInfo;

	Message myBroadcast = new Message();

	Message myMail;

	int robots = 0;

	double transfer;

	int i;

	boolean set = false;

	boolean go = false;

	boolean naZlozu = false;

	boolean burning = false;

	int aktPoziom = 1;

	// ta strategia zaklada w miare plaski teren

	int ile = 0;

	private void myYield() {
		runda++;
		myRC.yield();
	}

	public RobotPlayer(RobotController rc) {
		myRC = rc;
	}

	private MapLocation najblizszy(MapLocation skad, MapLocation dokad[]) {
		int i;
		double ile = 0;
		double min = RobotPlayer.DUZO;
		MapLocation ktoryMin = null;
		for (i = 0; i < dokad.length; i++) {
			ile = skad.distanceSquaredTo(dokad[i]);
			if (ile < min) {
				min = skad.distanceSquaredTo(dokad[i]);
				ktoryMin = dokad[i];
			}
		}
		return ktoryMin;
	}

	public int mojaOdleglosc(MapLocation a, MapLocation b) {
		return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY()
				- b.getY()));
	}

	public MapLocation najblizszyArchon() {
		MapLocation archony[] = myRC.senseAlliedArchons();
		MapLocation najblizszy = najblizszy(myRC.getLocation(), archony);
		return najblizszy;
	}

	private boolean dojdzDo(MapLocation pole) {
		Direction dir;
		try {
			dir = myRC.getLocation().directionTo(pole);
			while (dir != Direction.OMNI) {
				myRC.setDirection(dir);
				myRC.yield();
				if (myRC.canMove(myRC.getDirection())) {
					myRC.moveForward();
					myRC.yield();
					while (myRC.isMovementActive()) {
						myRC.yield();
					}
				} else {
					myRC.setDirection(dir.rotateRight());
					myRC.yield();
					if (myRC.canMove(myRC.getDirection())) {
						myRC.moveForward();
						myRC.yield();
						while (myRC.isMovementActive()) {
							myRC.yield();
						}
					} else {
						return false;
					}
				}
				dir = myRC.getLocation().directionTo(pole);
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
		return true;
	}

	public MapLocation najblizszySasiadPola(MapLocation pole) {
		return pole.subtract(myRC.getLocation().directionTo(pole));
	}

	public boolean zbierzBlok() throws Exception {

		MapLocation bloki[] = myRC.senseNearbyBlocks();
		int i = 0;

		if (myRC.canSenseSquare(mojArchon))
			aktPoziom = myRC.senseNumBlocksAtLocation(mojArchon);

		while ((i < bloki.length)
				&& ((bloki[i].equals(myRC.getLocation())) || (bloki[i].equals(mojArchon))))/*(mojaOdleglosc(
						mojArchon, bloki[i])) <= aktPoziom)*/ {
			i++;
		}

		if (i < bloki.length) {
			if (!dojdzDo(najblizszySasiadPola(bloki[i])))
				return false;
			if (myRC.canLoadBlockFromLocation(bloki[i])
					&& (myRC.getNumBlocks() < GameConstants.WORKER_MAX_BLOCKS)) {
				myRC.loadBlockFromLocation(bloki[i]);
				myRC.yield();
				while (myRC.isMovementActive()) {
					myRC.yield();
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean wyladujBlok() {
		MapLocation loc;
		try {
			loc = mojArchon;
			if (!myRC.getLocation().isAdjacentTo(loc))
				if (!dojdzDo(najblizszySasiadPola(loc))) {
					loc = najblizszySasiadPola(mojArchon);
					if (!dojdzDo(najblizszySasiadPola(loc))) {
						return false;
					}
				}
			if (myRC.canUnloadBlockToLocation(loc) && myRC.getNumBlocks() > 0)
				myRC.unloadBlockToLocation(loc);
			else {
				return false;
			}
			myRC.yield();
			while (myRC.isMovementActive()) {
				myRC.yield();
			}
		} catch (Exception e) {
			System.out.println("caught exception:");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean wyladujTuBlok() {
		MapLocation start = myRC.getLocation();
		Direction i = Direction.NORTH;
		if (myRC.getNumBlocks() > 0) {
			do {
				i.rotateRight();
			} while ((i != Direction.NORTH) || !myRC.canMove(i));
			if (myRC.canMove(i)) {
				try {
					myRC.setDirection(i);
					myRC.yield();
					myRC.moveForward();
					myRC.yield();
					while (myRC.isMovementActive()) {
						myRC.yield();
					}
					if (myRC.canUnloadBlockToLocation(start)) {
						myRC.unloadBlockToLocation(start);
						myRC.yield();
						while (myRC.isMovementActive()){
							myRC.yield();
						}
					} else {
						return false;
					}
				} catch (Exception e) {

					System.out.println("caught exception:");
					e.printStackTrace();
					return false;
				}
			} else
				return false;
			return true;
		} else {
			return false;
		}
	}

	public void run() {

		if (myRC.getRobotType() == RobotType.ARCHON) {

			while (true) {
				try {

					if (!burning) {

						/** * beginning of main loop ** */
						while (myRC.isMovementActive()) {
							myYield();
						}

						dir = myRC.senseDirectionToUnownedFluxDeposit();
						if (dir == Direction.NONE) {
							myYield();
						} else {
							if (dir == Direction.OMNI) {
								naZlozu = true;
								burning = true;
							} else {
								myRC.setDirection(dir);
								myYield();
								if (myRC.canMove(dir)) {
									myRC.moveForward();
								}
								myYield();
							}
						}
					} else {
						if (robots < RobotPlayer.WORKERS) {
							while (myRC.hasActionSet())
								myYield();
							runda++;
							if (myRC.canMove(myRC.getDirection())
									&& (myRC.senseGroundRobotAtLocation(myRC
											.getLocation().add(
													myRC.getDirection())) == null)) {
								myRC.spawn(RobotType.WORKER);
								myRC.yield();
								myRC.setDirection(myRC.getDirection()
										.rotateLeft());
								robots++;
							}
							myYield();
						} else {
							tab = myRC.senseNearbyGroundRobots();
							if (tab.length < robots)
								robots--;
							for (i = 0; i < tab.length; i++) {
								if (myRC.canSenseObject(tab[i])) {
									try {
										robInfo = myRC.senseRobotInfo(tab[i]);
									} catch (Exception e) {
										// chcemy ignorowac sytuacje, gdy robot
										// zginie nam w miedzyczasie
									}
									if (robInfo.location.isAdjacentTo(myRC
											.getLocation())
											|| robInfo.location.equals(myRC
													.getLocation())) {
										if (robInfo.energonLevel < robInfo.maxEnergon
												+ GameConstants.ENERGON_RESERVE_SIZE
												- 0.2) {

											myRC.transferEnergon(1,
													robInfo.location, tab[i]
															.getRobotLevel());
											myYield();
										}

									}
								}
								myYield();
							}
							myYield();
						}

						/** * end of main loop ** */
					}
				} catch (Exception e) {
					System.out.println("caught exception:");
					e.printStackTrace();
				}
			}
		} else {
			// worker
			boolean juz = false;
			mojArchon = najblizszyArchon();
			while (true) {
				try {

					if (myRC.getEnergonLevel() + myRC.getEnergonReserve() > myRC
							.getMaxEnergonLevel()
							+ GameConstants.ENERGON_RESERVE_SIZE - 1
							&& !juz) {

						while (!juz) {
							if (zbierzBlok())
							if (!wyladujBlok())
								wyladujTuBlok();
						}
						juz = true;

					} else {
						myRC.yield();
					}

				} catch (Exception e) {
					System.out.println("caught exception:");
					e.printStackTrace();
				}

			}

		}
	}
}