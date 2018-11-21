package aqua.blatt1.common.msgtypes;
import java.net.InetSocketAddress;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class NeighborUpdate implements Serializable {

	public static final class Neighbors implements Serializable {
		public Neighbors(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
			this.leftNeighbor = leftNeighbor;
			this.rightNeighbor = rightNeighbor;
		}
		private final InetSocketAddress leftNeighbor;
		private final InetSocketAddress rightNeighbor;

		public InetSocketAddress getLeftNeighbor(){
			return leftNeighbor;
		}
		public InetSocketAddress getRightNeighbor(){
			return rightNeighbor;
		}
	}

	private final Neighbors neighbors;

	public NeighborUpdate(InetSocketAddress leftNeighbor, InetSocketAddress rightNeighbor) {
		this.neighbors = new Neighbors(leftNeighbor, rightNeighbor);
	}

	public Neighbors getNeighbors() {
		return neighbors;
	}
}
