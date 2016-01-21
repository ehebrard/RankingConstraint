/**

 */
package constraint.algo;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.sort.ArraySort;

import java.io.Serializable;
import java.util.Comparator;

public class AlgoRankingBC {

	int[] t; // Tree links
	int[] d; // Diffs between critical capacities
	int[] h; // Hall interval links
	int[] bounds;

	int nbBounds;

	Interval[] intervals;
	Interval[] minsorted;
	Interval[] maxsorted;

	private Propagator aCause;
	private IntVar[] vars;

	ArraySort sorter;

	public AlgoRankingBC(Propagator cause) {
		this.aCause = cause;
	}

	public void reset(IntVar[] variables) {
		this.vars = variables;
		int n = vars.length;
		if(intervals==null || intervals.length<n){
			t = new int[2 * n + 2];
			d = new int[2 * n + 2];
			h = new int[2 * n + 2];
			bounds = new int[2 * n + 2];
			intervals = new Interval[n];
			minsorted = new Interval[n];
			maxsorted = new Interval[n];
			for (int i = 0; i < n; i++) {
				intervals[i] = new Interval();
			}
			sorter = new ArraySort(n,true,false);
		}
		for (int i = 0; i < n; i++) {
			Interval interval = intervals[i];
			interval.idx = i;
			interval.var = vars[i];
			minsorted[i] = interval;
			maxsorted[i] = interval;
		}
	}

	//****************************************************************************************************************//
	//****************************************************************************************************************//
	//****************************************************************************************************************//

	static enum SORT implements Comparator<Interval> {
		MAX {
			@Override
			public final int compare(Interval o1, Interval o2) {
				return o1.ub - o2.ub;
			}
		},
		MIN {
			@Override
			public final int compare(Interval o1, Interval o2) {
				return o1.lb - o2.lb;
			}
		},;
	}

	public void filter() throws ContradictionException {
		boolean again;
		do {
			sortIt();
			again = filterLower();
			again |= filterUpper();
		} while (again);
	}

	public void sortIt() {
		int n = vars.length;
		IntVar vt;
		for (int i = 0; i < n; i++) {
			vt = intervals[i].var;
			intervals[i].lb = vt.getLB();
			intervals[i].ub = vt.getUB();
		}
		sorter.sort(minsorted,n,SORT.MIN);
		sorter.sort(maxsorted,n,SORT.MAX);
		int min = minsorted[0].lb;
		int max = maxsorted[0].ub + 1;
		int last = min - 2;
		int nb = 0;
		bounds[0] = last;
		int i = 0, j = 0;
		
		print_structure();
		
		
		while (true) {
			
			//System.out.print("\ni: "+i+", j: "+j+", min: "+min+", max: "+max+", last: "+last);
			
			if (i < this.vars.length && min <= max) {
				if (min != last) {
					
					//System.out.print(" +bound (min) "+min);
					
					bounds[++nb] = last = min;
				}
				minsorted[i].minrank = nb;
				if (++i < this.vars.length) {
					min = minsorted[i].lb;
				}
			} else {
				if (max != last) {
					
					//System.out.print(" +bound (max) "+max);
					
					bounds[++nb] = last = max;
				}
				maxsorted[j].maxrank = nb;
				if (++j == this.vars.length) {
					break;
				}
				max = maxsorted[j].ub + 1;
			}
		}
		
		
		
		this.nbBounds = nb;
		bounds[nb + 1] = bounds[nb] + 2;


		
		System.out.println("\n"+nbBounds);
		for (i = 0; i <= nbBounds; i++) {
			System.out.print(" "+bounds[i]);
		}
		System.out.println();

	}

	protected void pathset(int[] tab, int start, int end, int to) {
		int next = start;
		int prev = next;
		while (prev != end) {
			next = tab[prev];
			tab[prev] = to;
			prev = next;
		}
	}

	protected int pathmin(int[] tab, int i) {
		while (tab[i] < i) {
			i = tab[i];
		}
		return i;
	}

	protected int pathmax(int[] tab, int i) {
		while (tab[i] > i) {
			i = tab[i];
		}
		return i;
	}

	public boolean filterLower() throws ContradictionException {
		boolean filter = false;
		for (int i = 1; i <= nbBounds + 1; i++) {
			t[i] = h[i] = i - 1;
			d[i] = bounds[i] - bounds[i - 1];
		}
		
		// System.out.print("   ");
		// for (int i = 1; i <= nbBounds+1; i++) {
		// 	System.out.print(" "+t[i]);
		// }
		// System.out.println();
		
		for (int i = 0; i < this.vars.length; i++) {
			
			int _bi_ = nbBounds+1;
			System.out.print( "\n" + bounds[_bi_] );
			while( _bi_ > 1) {
				System.out.print( " -(" + d[_bi_] + ")-> " + bounds[t[_bi_]] );
				_bi_ = t[_bi_];
			}
			System.out.println();
			
			
			int x = maxsorted[i].minrank;
			int y = maxsorted[i].maxrank;
			int z = pathmax(t, x + 1);
			int j = t[z];
			
			System.out.println( "Explore [" + bounds[x] + ", " + (bounds[y]-1) + "]" );  
			

			if (--d[z] == 0) {
				System.out.println( "remove " + bounds[z] + " (dominated)");  
				
				t[z] = z + 1;
				z = pathmax(t, t[z]);
				t[z] = j;
			}
			pathset(t, x + 1, z, z);
	
			if (d[z] <= bounds[z] - bounds[y]) {
	
				System.out.println(x + " " + h[x]);
				if(h[x] > x)
					System.out.println( "new Hall(1): [" + bounds[x] + ", " + (bounds[y]-1) + "]" );
				else
					System.out.println( "new Hall(2): [" + bounds[h[x]] + ", " + (bounds[y]-1) + "]" );
				
	
				//aCause.contradiction(null, "");
			}
			if (h[x] > x) {
				int w = pathmax(h, h[x]);
				
				pathset(h, x, w, w);
			}
			if (d[z] == bounds[z] - bounds[y]) {
				
				pathset(h, h[y], j - 1, y);
				h[y] = j - 1;
			}
		}
		return filter;
	}

	public boolean filterUpper() throws ContradictionException {
		boolean filter = false;
		for (int i = 0; i <= nbBounds; i++) {
			t[i] = h[i] = i + 1;
			d[i] = bounds[i + 1] - bounds[i];
		}
		for (int i = this.vars.length - 1; i >= 0; i--) {
			int x = minsorted[i].maxrank;
			int y = minsorted[i].minrank;
			int z = pathmin(t, x - 1);
			int j = t[z];
			if (--d[z] == 0) {
				t[z] = z - 1;
				z = pathmin(t, t[z]);
				t[z] = j;
			}
			pathset(t, x - 1, z, z);
			if (d[z] < bounds[y] - bounds[z]) {
				aCause.contradiction(null, "");
			}
			if (h[x] < x) {
				int w = pathmin(h, h[x]);
				if (minsorted[i].var.updateUpperBound(bounds[w] - 1, aCause)) {
					filter |= true;
					minsorted[i].ub = minsorted[i].var.getUB();//bounds[w] - 1;
				}
				pathset(h, x, w, w);
			}
			if (d[z] == bounds[y] - bounds[z]) {
				pathset(h, h[y], j + 1, y);
				h[y] = j + 1;
			}
		}
		return filter;
	}
	
	public void print_structure() {
		for(int i=0; i<vars.length; i++) {
			System.out.println( "[" + maxsorted[i].lb + ", " + maxsorted[i].ub + "] [" + minsorted[i].lb + ", " + minsorted[i].ub + "]" );
		}
	}
	

	private static class Interval implements Serializable {
		int minrank, maxrank;
		IntVar var;
		int idx;
		int lb, ub;
	}
}

