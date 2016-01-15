/**
* Copyright (c) 2014,
*       Emmanuel Hebrard
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the <organization> nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package constraint;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.solver.variables.events.PropagatorEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.objects.setDataStructures.ISet;
import org.chocosolver.util.objects.setDataStructures.SetFactory;
import org.chocosolver.util.objects.setDataStructures.SetType;
import org.chocosolver.util.tools.ArrayUtils;

import util.BinaryHeap;
import util.Tuple;

/**
* Propagator for the Ranking Constraint (BC or RC) 
*
* @author Emmanuel Hebrard, George Katsirelos
*/
public class PropRanking extends Propagator<IntVar> {
	
	private static boolean verbose = false;

	protected boolean enforceRC;
	
	protected IntVar[] increasingUpperBoundVars;
	protected IntVar[] increasingLowerBoundVars;
	
	protected int[] count;
	
	protected int[][] rule;
	protected int num_rule;
	
	protected int[][] pruning;
	protected int num_pruning;
	
	protected BinaryHeap< Tuple< Integer, IntVar > > sortedVars;
	
	
	private void sortByIncreasingUpperBound() {

		for(int i=0; i<vars.length; i++) {
			count[vars[i].getUB()-1]++;
		}
		int total = 0;
		for(int i=0; i<vars.length; i++) {
			int oldcount = count[i];
			count[i] = total;
			total += oldcount;
		}
		for(int i=0; i<vars.length; i++) {
			increasingUpperBoundVars[count[vars[i].getUB()-1]++] = vars[i];
		}
		for(int i=0; i<vars.length; i++) {
			count[i] = 0;
		}
		
	}
	
	private void sortByIncreasingLowerBound() {
	
		for(int i=0; i<vars.length; i++) {
			count[vars[i].getLB()-1]++;
		}
		int total = 0;
		for(int i=0; i<vars.length; i++) {
			int oldcount = count[i];
			count[i] = total;
			total += oldcount;
		}
		
		for(int i=0; i<vars.length; i++) {
			increasingLowerBoundVars[count[vars[i].getLB()-1]++] = vars[i];
		}
		for(int i=0; i<vars.length; i++) {
			count[i] = 0;
		}

	}
	
	
	
	public PropRanking(IntVar[] vars, boolean rc) {
		super(vars, PropagatorPriority.LINEAR, false);
		enforceRC = rc;
		
		count = new int[vars.length];
		rule = new int[vars.length][2];
		pruning = new int[vars.length][2];
		increasingUpperBoundVars = new IntVar[vars.length];
		increasingLowerBoundVars = new IntVar[vars.length];
		for(int i=0; i<vars.length; ++i) {
			increasingUpperBoundVars[i] = vars[i];
			increasingLowerBoundVars[i] = vars[i];
		}
		sortedVars = new BinaryHeap< Tuple< Integer, IntVar > >();
	}
	
	@Override
	public ESat isEntailed() {
		return ESat.FALSE;
	}
	
	
	private void upperBoundPruning() throws ContradictionException {
		
		if(verbose) {
			System.out.println("UB pruning");
		}
		
		sortByIncreasingLowerBound();
		int last = 0;
		for(int j=0; j<vars.length; j++) {
			int lb = increasingLowerBoundVars[j].getLB()-1;
			if(lb > j) this.contradiction(null, "impossible");
			if(lb == j) {
				while(last++ < j) {
					
					if(verbose) {
						System.out.println("upper bound pruning: " + increasingUpperBoundVars[last].toString() + " <= " + j);
					}
					
					increasingLowerBoundVars[last].updateUpperBound(j, aCause);
				}
			}
		}
	}
	
	private void lowerBoundPruning() throws ContradictionException {
		
		if(verbose) {
			System.out.println("LB pruning");
		}
		
		sortByIncreasingLowerBound();
		sortByIncreasingUpperBound();
		
		int n = vars.length;
		
		int last = n-1;
		int l = num_rule-1;
		int u = num_rule-1;
		
		for(int j=vars.length-1; j>=0; j--) {
			
			// check if the variable with j-th highest lower bound is a culprit w.r.t. some rules
			int maxXj = increasingLowerBoundVars[j].getUB();
			int minXj = increasingLowerBoundVars[j].getLB();
						
			// decrease the pointer to the first rule 
			while( rule[u][1] >= maxXj && l>0 && rule[l][0] > minXj ) {
				l--;
				if(rule[l][1] < rule[l+1][0]-1) u = l; // jump if there is a gap
			}
			
			if(verbose) {
				System.out.println("Check if " + increasingLowerBoundVars[j].toString() + " is culprit for rules [" + rule[l][0] + "," + rule[u][1] + "]");
			}
			
			// Xj is a culprit for the rules from l to u
			if( minXj>=rule[l][0] && maxXj<=rule[u][1] ) {
			
				// tightens u
				while( u>0 && rule[u-1][1] >= maxXj ) u--;
				
				// enforce pruning
				while( last >=  rule[u][1] ) {
					
					if(verbose) {
						System.out.println("lower bound pruning: " + increasingUpperBoundVars[last].toString() + " >= " + rule[l][0]);
					}
					
					increasingUpperBoundVars[last--].updateLowerBound(rule[l][0], aCause);
				}
				
			}
			
		}
	}
	
	private void disentailmentAndPruning() throws ContradictionException {
		
		if(verbose) {
			System.out.println("Disentailment");
		}
		
		int k = 1, nxt_k;
		int ilb_ptr = 0;
		
		num_rule = 0;
		num_pruning = 0;
		sortByIncreasingLowerBound();
		
		
		if(verbose) {
			System.out.println("greedy:");
		}
		
		while(k <= vars.length) {
			if(verbose) {
				System.out.println("assign " + k + " to:");
			}
			
			nxt_k = k+1;
			
			// add variables whose domain contains k in the binary heap
			while( ilb_ptr < vars.length && increasingLowerBoundVars[ilb_ptr].getLB()<=k ) {	
				Tuple< Integer, IntVar > t = new Tuple< Integer, IntVar >( increasingLowerBoundVars[ilb_ptr].getUB(), increasingLowerBoundVars[ilb_ptr] );
				sortedVars.add(t);
				ilb_ptr++;
			}
				
			
			if(sortedVars.isEmpty()) {
				this.contradiction(null, "impossible");
			}
			
			Tuple< Integer, IntVar > Xi = sortedVars.remove();

			if(verbose) {
				System.out.println("  " + Xi.second.toString());
			}
			
			// compute the set M of variables which we won't be able to assign to a new "k" value 
			while( !sortedVars.isEmpty()	&& sortedVars.peek().first < nxt_k ) {
				Tuple< Integer, IntVar > Xj = sortedVars.remove();

				if(Xj.first < k) this.contradiction(null, "impossible");
				nxt_k++;
				
				if(verbose) {
					System.out.println("  " + Xj.second.toString() + "(" + nxt_k + ")");
				}

			}
			
			
			if(Xi.first == k) { // rule and possible pruning
					
				if(verbose) {
					System.out.println("learn a rule <" + (k+1) + "," + nxt_k + ">");
				}
				
				// the rule
				rule[num_rule][0] = k+1;
				rule[num_rule][1] = nxt_k;
				
				// actual pruning if the interval [k+1, nxt_k-1] is not empty
				if(k+1<nxt_k) {
					
					// store the pruning AND concatenate pruned intervals when possible
					if( num_pruning>0 && pruning[num_pruning-1][1]==k ) {
						pruning[num_pruning-1][1] = nxt_k;
					} else {
						pruning[num_pruning][0] = k+1;
						pruning[num_pruning][1] = nxt_k-1;
						num_pruning++;
					}
				}
				num_rule++;
			}
			
			k = nxt_k;
		}

		if(num_pruning>0) {
			
			if(verbose) {
				System.out.print("pruning:");
				for(int j=0; j<num_pruning; j++) {
					System.out.print(" [" + pruning[j][0] + "," + pruning[j][1] + "]");
				}
				System.out.println();
			}
			
			// enforce pruning
			for(int i=0; i<vars.length; i++) {
				for(int j=0; j<num_pruning; j++) {
					vars[i].removeInterval(pruning[j][0], pruning[j][1], aCause);
				}
			} 
		}

	}
	
	@Override
	public void propagate(int evtmask) throws ContradictionException {
		
		if(verbose) {
			System.out.println("propagate Ranking");
		}
		
		upperBoundPruning();
		disentailmentAndPruning();
		lowerBoundPruning();
	}
	
	

}

