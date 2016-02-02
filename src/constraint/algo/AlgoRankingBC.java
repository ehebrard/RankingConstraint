/**

 */
package constraint.algo;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.Solver;
import org.chocosolver.util.sort.ArraySort;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.search.strategy.ISF;

import java.io.Serializable;
import java.util.Comparator;

import util.BinaryHeap;
import util.Tuple;

import constraint.Ranking;

public class AlgoRankingBC {

	public static int verbose = 0;
	public static boolean check_consistency = false;
	public static boolean verify_properties = false;

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
	
	
	private int[][] rule; // list of a,b,k's 
	private int num_rule;
	
	
	private int [] cur_a;
	private boolean [] is_in;
	private int num_h;
	private int [] lbs;
	private int nlb;
	private int [] ck;
	
	protected BinaryHeap< Tuple< Integer, IntVar > > sortedVars;
	

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
		
		num_rule = 0;
		
		
		
		rule = new int[n][3];
		
		ck = new int[n];
		lbs = new int[n];
		cur_a = new int[n];
		is_in = new boolean[n];
		
		
		sortedVars = new BinaryHeap< Tuple< Integer, IntVar > >();
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
		sortIt();
		computeHall();
		filterFromRules();
		
		if(check_consistency)
			checkConsistency();
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

		if(verbose>0) {
			print_structure();
		}
		
		while (true) {
			if (i < this.vars.length && min <= max) {
				if (min != last) {
					bounds[++nb] = last = min;
				}
				minsorted[i].minrank = nb;
				if (++i < this.vars.length) {
					min = minsorted[i].lb;
				}
			} else {
				if (max != last) {
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
		bounds[nb + 1] = bounds[nb] * 2;

		// System.out.println("\n"+nbBounds);
		// for (i = 0; i <= nbBounds; i++) {
		// 	System.out.print(" "+bounds[i]);
		// }
		// System.out.println();

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

	public void computeHall() throws ContradictionException {
		num_rule = 0;
		boolean filter = false;
		for (int i = 1; i <= nbBounds + 1; i++) {
			t[i] = h[i] = i - 1;
			d[i] = bounds[i] - bounds[i - 1];
		}

		
		for (int i = 0; i < this.vars.length; i++) {
			
			int _bi_ = nbBounds+1;
			
			if(verbose>1) {
				System.out.print( "\n" + bounds[_bi_] );
				while( _bi_ > 1) {
					System.out.print( " -(" + d[_bi_] + ")-> " + bounds[t[_bi_]] );
					_bi_ = t[_bi_];
				}
				System.out.println();
				for(int _ti_ = nbBounds+1; _ti_ > 0; --_ti_) {
					System.out.print( " " + bounds[_ti_] );
				}
				System.out.print("\n ");
				for(int _ti_ = nbBounds+1; _ti_ > 0; --_ti_) {
					System.out.print( " " + h[_ti_] );
				}
				System.out.println();
			}
			
			
			int x = maxsorted[i].minrank;
			int y = maxsorted[i].maxrank;
			int z = pathmax(t, x + 1);
			int j = t[z];
			
			if(verbose>1) {
				System.out.println( "Explore [" + bounds[x] + ", " + (bounds[y]-1) + "] :" + z );  
			}

			if (--d[z] == 0) {
				if(verbose>1) {
					System.out.println( "remove " + bounds[z] + " (dominated)");  
				}
				
				t[z] = z + 1;
				z = pathmax(t, t[z]);
				t[z] = j;
				
			}
			pathset(t, x + 1, z, z);
	
			if (h[x] > x) {
				int w = pathmax(h, h[x]);			
				pathset(h, x, w, w);
			}
			
			if (d[z] <= bounds[z] - bounds[y]) {
				
				pathset(h, h[y], j - 1, y);
				h[y] = j - 1;

				if( y > h[y] ) {

					int nrb = num_rule;
					
			
					if(num_rule==0 || rule[num_rule-1][1] != bounds[y]) {
						rule[num_rule][1] = bounds[y];
						num_rule++;
					}
					rule[num_rule-1][0] = bounds[h[y]+1];
					//rule[num_rule-1][2] = (bounds[z] - d[z] - bounds[h[y]+1]);
					rule[num_rule-1][2] = (bounds[z] - d[z]);
					
					
					if(verbose>1) {	
						if(num_rule>nrb)				
							System.out.println( "new Hall: [" + bounds[h[y]+1] + ", " + (bounds[y]-1) + "] #= " + (bounds[z] - d[z] - bounds[h[y]+1]) );
						else
							System.out.println( "improve Hall: [" + bounds[h[y]+1] + ", " + (bounds[y]-1) + "] #= " + (bounds[z] - d[z] - bounds[h[y]+1]) );
					}
					
				}
			}
			
		}
		
		
		if(verbose>0) {
			for(int i=0; i<num_rule; i++) {
				System.out.println( "<" + rule[i][0] + ", " + rule[i][1] + ", " + rule[i][2] + ">");
			}
		}
		
		for(int i=0; i<num_rule; i++) {
			if(rule[i][1] < rule[i][2]) {
				for(int j=0; j<vars.length; j++) {
					
					vars[j].removeInterval(rule[i][1], rule[i][2]-1, aCause);

				}
			}
		}
		
		if(verbose>0) {
			for(int j=0; j<vars.length; j++) {
				System.out.println( maxsorted[j].var );
			}
		}
		
	}


	public void filterFromRules() throws ContradictionException {
		// rules are of the form <a,b,c> and should be read as:
		// if x is such that D(x) \not\subset [a, b[, then x \in [a,b[ => y \not\in [b,c], forall y
		
		// we use them as follows:
		// if there exists y and R such that D(y) \subset \bicup_{i \in R} [b_i,c_i] then forall x such that D(x) \not\subset \bigcap_{i \in R} [a_i, b_i[ we have x \not\in \bigcap_{i \in R} [a,b[
		
		// rules are ordered so that b_{i+1} > b_i, and therefore c_{i+1} > c_i otherwise [FIND A GOOD REASON, but in any case i+1 would be useless]
		// moreover, suppose that c_i >= b_{i+1}-1, then a_{i+1} <= a_i, because 1/ a_{i+1} <= b_i otherwise there would be a wipe out, and then, the interval [a_i, b_{i+1}] would dominates [a', b_{i+1}] for all a' \in [a_i, b_i]
		
		if(verify_properties) {
			for(int i=1; i<num_rule; i++) {
				if( rule[i][1] <= rule[i-1][1] ) { System.out.println("assert 1"); System.exit(1); };

				if( rule[i][2] <= rule[i-1][2] ) { System.out.println("assert 2"); System.exit(1); };

				if( rule[i-1][2] >= rule[i][1] && rule[i-1][0] < rule[i][0] ) { System.out.println("assert 3"); System.exit(1); };
			}
		}
		
		// since b_i's are in increasing order, there are at most n of them, and if we explore potential culprit ordered by non-decreasing lb, we do not have to look back to previous rules once we move to a new one
		int l=0; // pointer to the first potential rule
		int u=0; // pointer to the last rule for which 

		int n=vars.length;
		
		for(int i=0; i<n; i++) {
			
			int lb = minsorted[i].var.getLB(); //bounds[minsorted[i].minrank];
			int ub = minsorted[i].var.getUB(); //bounds[minsorted[i].maxrank]-1;
			
			
			// assert(lb == minsorted[i].var.getLB());
			// assert(ub == minsorted[i].var.getUB());
			
			
			if(verbose>0) {
				System.out.println("check if " + minsorted[i].var.toString() + " is culprit");
			}

			if(verbose>1) {
				if(l<num_rule-1) {
					if(rule[l+1][1]<=lb) {
						System.out.println("  rule [" + rule[l][1] + ", " + rule[l][2] + "] does not cover lb");
					}
				} else {
					System.out.println("  last rule");
				}
			}
			
			while(l<num_rule-1 && rule[l+1][1]<=lb) {
				
				l++; // find the tightest value for l
				
				if(verbose>1) {
					if(l<num_rule-1) {
						System.out.println("  move l-ptr from " + (l-1) + " = [" + rule[l-1][1] + ", " + rule[l-1][2] + "] to " + (l) + " = [" + rule[l][1] + ", " + rule[l][2] + "]");
					} else {
						System.out.println("  last rule");
					}
				}
				
			}
			
			if(verbose>1) {
				if(rule[l][1]>lb) {
					System.out.println("  could not cover lb");
				}
			}
			
			
			if(rule[l][1]<=lb) {
				
				if(verbose>1) {
					System.out.println(" lb covered, check ub ");

					if(u<n) {
						if(rule[u][2]<ub) {
							System.out.println("   rule [" + rule[u][1] + ", " + rule[u][2] + "] too small");
						} else {
							System.out.println("   rule [" + rule[u][1] + ", " + rule[u][2] + "] large enough");
						}

						if(rule[u][2]>=rule[u+1][1]-1) {
							System.out.println("   rules [" + rule[u][1] + ", " + rule[u][2] + "] and [" + rule[u+1][1] + ", " + rule[u+1][2] + "] are contiguous");
						} else {
							System.out.println("   rule [" + rule[u][1] + ", " + rule[u][2] + "] and [" + rule[u+1][1] + ", " + rule[u+1][2] + "] have a GAP");
						}

					} else {
						System.out.println("   last rule");
					}
				}
				
				// the rule might apply, check ub
				u = l;
				while(u<n && rule[u][2]<ub && rule[u][2]>=rule[u+1][1]-1) {
					u++;
					
					if(verbose>1) {
						if(u<n) {
							if(rule[u][2]<ub) {
								System.out.println("   rule [" + rule[u][1] + ", " + rule[u][2] + "] too small");
							} else {
								System.out.println("   rule [" + rule[u][1] + ", " + rule[u][2] + "] large enough");
							}

							if(rule[u][2]>=rule[u+1][1]-1) {
								System.out.println("   rules [" + rule[u][1] + ", " + rule[u][2] + "] and [" + rule[u+1][1] + ", " + rule[u+1][2] + "] are contiguous");
							} else {
								System.out.println("   rule [" + rule[u][1] + ", " + rule[u][2] + "] and [" + rule[u+1][1] + ", " + rule[u+1][2] + "] have a GAP");
							}

						} else {
							System.out.println("   last rule");
						}
					}
				}

				if(rule[u][2]>=ub) {
				
					if(verbose>1) {
						System.out.print("  yes, for rules");
					}
					//
					// we can prune with respect to the rules r[l],...,r[u]
					// - we prune the intersection of the [a_i, b_i], that is [r[l].a, r[l].b[ 
					// - from variables that are not contained in the union, that is [r[u].a, r[u].b[ 
				
					if(verify_properties) {
						for(int j=l; j<=u; j++) {

							if(verbose>1) {
								System.out.print(" [" + rule[j][1] + ", " + rule[j][2] + "]");
							}

							if( rule[j][0]<rule[j+1][0] ) { System.out.println("assert 4"); System.exit(1); };
							if( rule[j][1]>rule[j+1][1] ) { System.out.println("assert 5"); System.exit(1); };
						}
					}

					if(verbose>1) {
						System.out.println();
					}

					for(int j=0; j<n; j++) {
						int x_lb = vars[j].getLB();
						int x_ub = vars[j].getUB();
						
						
						boolean included = (x_lb >= rule[u][0] && x_ub < rule[u][1]);
						
						boolean disjoint = (rule[l][0] > x_ub || rule[l][1] <= x_lb);
						
						// System.out.print( "[" + x_lb + ", " + x_ub + "] is");
						// if(!included)
						// 	System.out.print(" not");
						// System.out.println(" a subset of [" + rule[u][0] + ", " + (rule[u][1]-1) + "]");
						//
						// System.out.print( "[" + x_lb + ", " + x_ub + "] is");
						// if(!disjoint)
						// 	System.out.print(" not");
						// System.out.println(" disjoint to [" + rule[l][0] + ", " + (rule[l][1]-1) + "]");


						if( !included && !disjoint ) {
	
							if(verbose>0) {
								System.out.println(" -> remove [" + rule[l][0] + ", " + (rule[l][1]-1) + "] from " + vars[j].toString());
							}


							if(rule[l][0]<=x_lb) {
								
								if(verbose>0) {
									System.out.println(" => " + vars[j].toString() + " >= " + rule[l][1]);
								}
								
								vars[j].updateLowerBound(rule[l][1], aCause);
							} else if(rule[l][1]>x_ub) {
								
								if(verbose>0) {
									System.out.println(" => " + vars[j].toString() + " <= " + (rule[l][0]-1));
								}
								
								vars[j].updateUpperBound(rule[l][0]-1, aCause);
							} else {									
								vars[j].removeInterval(rule[l][0], rule[l][1]-1, aCause);
							}
						}
					}
				}				
			}
		}
	}
	
	
	public void checkConsistency() {
		
		boolean[][] is_checked = new boolean[vars.length][vars.length+1];
		int[] ub = new int[vars.length];
		int[] lb = new int[vars.length];
		
    for (int i = 0; i < vars.length; i++) {
			lb[i] = vars[i].getLB();
			ub[i] = vars[i].getUB();
    }
		

		for (int i = 0; i < vars.length; i++) {
			for (int j = vars[i].getLB(); j <= vars[i].getUB(); j++) {
				if(vars[i].contains(j) && !is_checked[i][j]) {
					
					Solver checker = new Solver("checker");
		
					IntVar[] X = new IntVar[vars.length];
			    for (int k = 0; k < vars.length; k++) {
						if(i==k) {
							X[k] = VF.fixed("x"+k, j, checker);
						} else {
							X[k] = VF.bounded("x"+k, lb[k], ub[k], checker);
			    	}
					}
		
					checker.post( Ranking.reformulateGcc(X, checker) );
					checker.set( ISF.lexico_LB(X) );
		
					if(!checker.findSolution()) {
						System.out.println( "Missed pruning!" + X[i].toString() );
					
						for (int k = 0; k < vars.length; k++) {
							System.out.println( vars[k].toString() );
						}
					
						System.exit(1);
					} else {
						for(int k=0; k<vars.length; k++) {
							is_checked[k][X[k].getValue()] = true;
						}
					}
				}
			}
		}
		
		
		
	}

	
	public void computeHallInt() {
		int n = vars.length;
		IntVar vt;
		for (int i = 0; i < n; i++) {
			vt = intervals[i].var;
			intervals[i].lb = vt.getLB();
			intervals[i].ub = vt.getUB();
		}
		sorter.sort(minsorted,n,SORT.MIN);
		sorter.sort(maxsorted,n,SORT.MAX);
		
		int min, max, lb;
		int last=-1;
		nlb = 0;
		
		for(int i=0; i<n; i++) {
			min = minsorted[i].lb;
			
			// System.out.print( minsorted[i].var + ": " );
			// System.out.println( min );
			//	
			if(min != last) {
				lbs[nlb++] = min;
				last = min;
			}
		}

		last = -1;
		for(int i=0; i<n; i++) {
			max = maxsorted[i].ub;
			min = maxsorted[i].lb;
			
			
			//System.out.print( min + ".." + max + ": " );
			
			
			if(max>last) {
				//flush list of Halls
				for(int j=0; j<num_h; j++) {
					System.out.println("["+ cur_a[j] + "," + last + "] (" + (ck[cur_a[j]] - (last-cur_a[j]+1)) + ")");
					is_in[cur_a[j]] = false;
				}
				num_h = 0;
				last = max;
			}
			
			for(int j=0; j<nlb; j++) {
				lb = lbs[j];
				if(lb<=min) {
					if(++ck[lb] >= (max-lb+1)) {
						
						//System.out.println("?? ["+ cur_a[j] + "," + max + "] (" + (ck[lb] - (max-lb+1)) + ")");
						
						if(!is_in[lb]) {
							cur_a[num_h++] = lb;
							is_in[lb] = true;
						}
					}
				} else break;
			}
			
		}
		
		for(int j=0; j<num_h; j++) {
			System.out.println("["+ cur_a[j] + "," + last + "] (" + (ck[cur_a[j]] - (last-cur_a[j]+1)) + ")");
		}
		
		
	}
	
	
	
	
	public void probeRC() throws ContradictionException {

		int n = vars.length;
		IntVar vt;
		int old_lb;
		int old_ub;
		
		boolean debug = false;
		boolean pruning = false;
		

		for (int i = 0; i < n; i++) {
			vt = intervals[i].var;
			intervals[i].lb = vt.getLB();
			intervals[i].ub = vt.getUB();
		}
		
		for (int i = 0; i < n; i++) {
			old_lb = intervals[i].lb;
			old_ub = intervals[i].ub;
			for (int j = old_lb; j <= old_ub; j++) {	
				
				if(intervals[i].var.contains(j)) {
				
					intervals[i].lb = j;
					intervals[i].ub = j;
				
					if(debug)
						System.out.print(" " + i + ":" + j);
				
					if(!probe()) {
						
						if(j == intervals[i].var.getLB()) {
							intervals[i].var.updateLowerBound(j+1, aCause);
						} else if (j == intervals[i].var.getUB()) {
							intervals[i].var.updateUpperBound(j-1, aCause);
						} else {
							intervals[i].var.removeValue(j, aCause);
						}
						
						if(debug) {
							System.out.print("*");
							pruning = true;
						}
					}
				
				}
			}
			intervals[i].lb = old_lb;
			intervals[i].ub = old_ub;
			
			if(debug)
				System.out.println();
		}
		
		if(debug) {
			System.out.println();
			if(pruning) {
				System.exit(1);
			}
		
		}
	}	
	
	public boolean probe()  {
 	
		// if(verbose>2) {
	// 		System.out.println("Disentailment");
	// 	}
	

		//counting_sort(order, minsorted);
		//sorter.sort(maxsorted,n,SORT.MAX);
 	
		int k = 1, nxt_k;
		int ilb_ptr = 0;
		
		
		sorter.sort(minsorted,vars.length,SORT.MIN);
 	
		//num_rule = 0;
		//num_pruning = 0;
		//sortByIncreasingLowerBound();
 	
 	
		if(verbose>2) {
			System.out.println("greedy:");
		}
 	

		while(k <= vars.length) {
			if(verbose>2) {
				System.out.println("assign " + k + " to:");
			}
 		
			nxt_k = k+1;
 		
			// add variables whose domain contains k in the binary heap
			while( ilb_ptr < vars.length && minsorted[ilb_ptr].lb<=k && minsorted[ilb_ptr].ub>=k ) {	
				//Tuple< Integer, IntVar > t = new Tuple< Integer, IntVar >( increasingLowerBoundVars[ilb_ptr].getUB(), increasingLowerBoundVars[ilb_ptr] );
				Tuple< Integer, IntVar > t = new Tuple< Integer, IntVar >( minsorted[ilb_ptr].ub, minsorted[ilb_ptr].var );
				
				//increasingLowerBoundVars[ilb_ptr].getUB(), increasingLowerBoundVars[ilb_ptr] );
 			
				//System.out.print(" (+" + increasingLowerBoundVars[ilb_ptr].toString() + ")");
 			
 			
				sortedVars.add(t);
				ilb_ptr++;
			}
 			
 		
			if(sortedVars.isEmpty()) {
				return false;
			}
 		
			Tuple< Integer, IntVar > Xi = sortedVars.remove();
	
			//System.out.print(" (-" + Xi.second.toString() + ")");
	
			if(verbose>2) {
				System.out.println("  " + Xi.second.toString());
			}
 		
			// compute the set M of variables which we won't be able to assign to a new "k" value 
			while( !sortedVars.isEmpty()	&& sortedVars.peek().first < nxt_k ) {
				Tuple< Integer, IntVar > Xj = sortedVars.remove();
 			
				//System.out.println(" (-" + Xi.second.toString() + ")");
				if(Xj.first < k) return false;
				nxt_k++;
 			
				if(verbose>2) {
					System.out.println("  " + Xj.second.toString() + "(" + nxt_k + ")");
				}

			}
 		
			k = nxt_k;
		}

		return true;
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

