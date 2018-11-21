package aqua.blatt1.client;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import aqua.blatt1.common.Direction;
import aqua.blatt1.common.FishModel;
import aqua.blatt1.common.msgtypes.NeighborUpdate;
import messaging.Message;

public class TankModel extends Observable implements Iterable<FishModel> {

	public static final int WIDTH = 600;
	public static final int HEIGHT = 350;
	protected static final int MAX_FISHIES = 5;
	protected static final Random rand = new Random();
	protected volatile String id;
	protected final Set<FishModel> fishies;
	protected int fishCounter = 0;
	protected final ClientCommunicator.ClientForwarder forwarder;
	protected NeighborUpdate.Neighbors neighbors;
	protected volatile Boolean token = false;
	protected Timer timer = new Timer();
	protected Mode mode = Mode.IDLE;
	protected Save backup;
	protected List<Message> rightSaveList;
	protected List<Message> leftSaveList;

    enum Mode {
        IDLE, LEFT, RIGHT, BOTH
    }

    class Save {
        private int fishCounterBackup;
        protected List<Message> rightSaveList;
        protected List<Message> leftSaveList;

        private void addFish() {
            fishCounterBackup++;
        }

        Save(int fishCounter) {
            this.fishCounterBackup = fishCounter;
        }
    }

    void initiateSnapshot() {
        backup = new Save(fishCounter);
        mode = Mode.BOTH;
        forwarder.sendMarkers(neighbors);
    }

    void createLocalSnapshot(InetSocketAddress sender) {
        if (mode == Mode.IDLE) {
            backup = new Save(fishCounter);
            if (neighbors.getRightNeighbor() == sender) {
                rightSaveList = new ArrayList<>();
                mode = Mode.LEFT;
            } else {
                leftSaveList = new ArrayList<>();
                mode = Mode.RIGHT;
            }
            forwarder.sendMarkers(neighbors);
        } else if (mode == Mode.RIGHT) {
            if (neighbors.getRightNeighbor() == sender)
                mode = Mode.BOTH;
            else
                mode = Mode.IDLE;
        } else if (mode == Mode.LEFT) {
            if (neighbors.getRightNeighbor() == sender)
                mode = Mode.IDLE;
            else
                mode = Mode.BOTH;
        } else {
            if (neighbors.getRightNeighbor() == sender)
                mode = Mode.LEFT;
            else
                mode = Mode.RIGHT;
            //quit if mode == IDLE
        }

    }

	synchronized void receiveToken() {
		token = true;
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				token = false;
				forwarder.sendToken(neighbors);
			}
		}, 2000);
	}

	synchronized boolean hasToken() {
			return token;
	}

	public TankModel(ClientCommunicator.ClientForwarder forwarder) {
		this.fishies = Collections.newSetFromMap(new ConcurrentHashMap<FishModel, Boolean>());
		this.forwarder = forwarder;
	}

	synchronized void onRegistration(String id) {
		this.id = id;
		newFish(WIDTH - FishModel.getXSize(), rand.nextInt(HEIGHT - FishModel.getYSize()));
	}

	public synchronized void newFish(int x, int y) {
		if (fishies.size() < MAX_FISHIES) {
			x = x > WIDTH - FishModel.getXSize() - 1 ? WIDTH - FishModel.getXSize() - 1 : x;
			y = y > HEIGHT - FishModel.getYSize() ? HEIGHT - FishModel.getYSize() : y;

			FishModel fish = new FishModel("fish" + (++fishCounter) + "@" + getId(), x, y,
					rand.nextBoolean() ? Direction.LEFT : Direction.RIGHT);

			fishies.add(fish);
		}
	}

	synchronized void receiveFish(InetSocketAddress sender, FishModel fish) {
        if (mode != Mode.IDLE) {
            if (mode == Mode.BOTH)
                backup.addFish();
            else if (mode == Mode.RIGHT && sender == neighbors.getRightNeighbor())
                backup.addFish();
            else if (mode == Mode.LEFT && sender == neighbors.getLeftNeighbor())
                backup.addFish();

        }
        fish.setToStart();
        fishies.add(fish);
	}

	synchronized void receiveNeighbors(NeighborUpdate.Neighbors neighbors) {
		this.neighbors = neighbors;
	}

	public String getId() {
		return id;
	}

	public synchronized int getFishCounter() {
		return fishCounter;
	}

	public synchronized Iterator<FishModel> iterator() {
		return fishies.iterator();
	}

	private synchronized void updateFishies() {
		for (Iterator<FishModel> it = iterator(); it.hasNext();) {
			FishModel fish = it.next();

			fish.update();

			if (fish.hitsEdge())
				if (token)
					forwarder.handOff(fish, neighbors);
				else
					fish.reverse();
			if (fish.disappears())
				it.remove();
		}
	}

	private synchronized void update() {
		updateFishies();
		setChanged();
		notifyObservers();
	}

	protected void run() {
		forwarder.register();

		try {
			while (!Thread.currentThread().isInterrupted()) {
				update();
				TimeUnit.MILLISECONDS.sleep(10);
			}
		} catch (InterruptedException consumed) {
			// allow method to terminate
		}
	}

	public synchronized void finish() {
		forwarder.deregister(id);
	}

}