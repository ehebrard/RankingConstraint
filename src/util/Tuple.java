
package util;

public class Tuple<X extends Comparable<X>, Y> implements Comparable< Tuple <X, Y> > { 
  public final X x; 
  public final Y y; 
  public Tuple(X x, Y y) { 
    this.x = x; 
    this.y = y; 
  } 

	public int	compareTo(Tuple< X, Y> o) {
		// int f = this.x.compareTo(o2.x);
		// return (f!=0 ? f : this.y.compareTo(o2.y));
		return x.compareTo(o.x);
	}
}
