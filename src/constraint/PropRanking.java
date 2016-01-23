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

import constraint.algo.AlgoRankingBC;

/**
* Propagator for the Ranking Constraint (BC or RC) 
*
* @author Emmanuel Hebrard, George Katsirelos
*/
public class PropRanking extends Propagator<IntVar> {
	
	private static boolean verbose = false;
	private static boolean trace = false;
	private int ncalls = 0;


	protected boolean enforceRC;

	protected int[] count;	
	protected IntVar[] increasingUpperBoundVars;
	protected IntVar[] increasingLowerBoundVars;


	AlgoRankingBC filter;




	// protected int[][] rule;
	// protected int num_rule;
	//
	// protected int[][] pruning;
	// protected int num_pruning;
	//
	// protected BinaryHeap< Tuple< Integer, IntVar > > sortedVars;
	//
	
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
		increasingUpperBoundVars = new IntVar[vars.length];
		increasingLowerBoundVars = new IntVar[vars.length];
		for(int i=0; i<vars.length; ++i) {
			increasingUpperBoundVars[i] = vars[i];
			increasingLowerBoundVars[i] = vars[i];
		}
		
		filter = new AlgoRankingBC(aCause);
		filter.reset(vars);
	}
	
	@Override
	public ESat isEntailed() {
		return ESat.FALSE;
	}
	
	
	public void upperBoundPruning() throws ContradictionException {
		
		if(verbose) {
			System.out.println("UB pruning");
		}
		
		sortByIncreasingLowerBound();
		
		if(trace) {
			for(int j=0; j<vars.length; j++) {
				System.out.print( " " + increasingLowerBoundVars[j].toString() );
			}
			System.out.println();
		}
		
		
		int last = 0;
		for(int j=0; j<vars.length; j++) {
			int lb = increasingLowerBoundVars[j].getLB()-1;
			if(lb > j) this.contradiction(null, "impossible");
			if(lb == j) {
				while(last < j) {
					
					if(verbose) {
						System.out.println("upper bound pruning: " + increasingUpperBoundVars[last].toString() + " <= " + j);
					}
					
					increasingLowerBoundVars[last].updateUpperBound(j, aCause);
					
					last++;
				}
			}
		}
	}
	

	
	@Override
	public void propagate(int evtmask) throws ContradictionException {
	
		ncalls++;
		
		if(verbose) {
			System.out.println("propagate Ranking " + ncalls);
		}
		
		upperBoundPruning();
		filter.filter();
		
		if(trace) {
			for(int j=0; j<vars.length; j++) {
				System.out.print( " " + increasingLowerBoundVars[j].toString() );
			}
			System.out.println();
		}
		
	}
	
	

}

