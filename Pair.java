/*
 * tclo, melissatan
 * 
 * This class used in Map.path
 * 
 */

public final class Pair<R, S> {
	
	public final R first;
	public final S second;
	
	public Pair(R first, S second) {
		this.first = first;
		this.second = second;
	}
	
	public R getFirst() {
		return first;
	}
	
	public S getSecond() {
		return second;
	}
}
