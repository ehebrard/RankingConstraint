
package util;

public class Tuple<X extends Comparable<X>, Y> implements Comparable< Tuple <X, Y> > { 
  public final X first; 
  public final Y second; 
  public Tuple(X first, Y second) { 
    this.first = first; 
    this.second = second; 
  } 

	public int	compareTo(Tuple< X, Y> o) {
		// int f = this.x.compareTo(o2.x);
		// return (f!=0 ? f : this.y.compareTo(o2.y));
		return first.compareTo(o.first);
	}
}
