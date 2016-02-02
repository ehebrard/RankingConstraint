/**
 * Copyright (c) 2014,
 *       Charles Prud'homme (TASC, INRIA Rennes, LINA CNRS UMR 6241),
 *       Jean-Guillaume Fages (COSLING S.A.S.).
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

import gnu.trove.map.hash.TIntIntHashMap;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.unary.PropEqualXC;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranking constraint
 *
 * @author Emmanuel Hebrard, George Katsirelos
 * @since 16/06/11
 */
public class Ranking extends Constraint {
	
		public static enum Type {
			RC, BC
		}

    public Ranking(IntVar[] vars, boolean type) {
        super("Ranking", createProp(vars, type));
    }
		
    public Ranking(IntVar[] vars) {
        super("Ranking", createProp(vars, false));
    }

	private static Propagator createProp(IntVar[] vars, boolean rc) {
		if(vars.length==1){
			return new PropEqualXC(vars[0], 1);
		}
		
		return new PropRanking(vars, rc);
		// switch (Ranking.Type.valueOf(consistency)) {
		//
		// 	default: return new PropRanking(vars, true);
		// }
	}

    public static Constraint[] reformulateSort(IntVar[] vars, Solver solver) {
        List<Constraint> cstrs = new ArrayList<>();
				
				int N = vars.length;
				
        IntVar[] XS = VF.integerArray("XS", N, 1, N, solver);
        cstrs.add( ICF.sort(vars, XS) );
        cstrs.add( ICF.arithm(XS[0], "=", 1) );
        for(int i = 0; i < N-1; ++i) {
            cstrs.add( LCF.or(ICF.arithm(XS[i+1], "=", XS[i]) ,
 														  ICF.arithm(XS[i+1], "=", i+2)) );
        }
				
        return cstrs.toArray(new Constraint[cstrs.size()]);
    }
		
    public static Constraint[] reformulateGcc(IntVar[] vars, Solver solver) {
        List<Constraint> cstrs = new ArrayList<>();
				
				int N = vars.length;
				
				int[] values = new int[N];
				for(int i=0; i<N; i++) values[i] = (i+1);
				
				IntVar[] XO = VF.integerArray("XOcc", N, 0, N, solver);
				IntVar[] XCO = VF.integerArray("XCumulOcc", N, 1, N, solver);
				
				cstrs.add( ICF.global_cardinality( vars, values, XO, true ) );
				
				cstrs.add( ICF.arithm( XO[0], "=", XCO[0] ) );
				for(int i=1; i<N; ++i) {
					IntVar[] scope = new IntVar[2];
					scope[0] = XO[i];
					scope[1] = XCO[i-1];
					
					cstrs.add( ICF.sum(scope, XCO[i]) );
				}
				
				
				for(int i=0; i<N-1; ++i) {
					// Occ(1) + ... + Occ(i) >= i
					cstrs.add( ICF.arithm( XCO[i], ">", i ) );

					// if Occ(1) + ... + Occ(i) > i+1 iff Occ(i+1) = 0
				  cstrs.add(
						LCF.or(ICF.arithm(XCO[i] , "=", (i+1)),
				           ICF.arithm(XO[i+1], "=", 0)));

				  cstrs.add(
						LCF.or(ICF.arithm(XCO[i] , ">", (i+1)),
				           ICF.arithm(XO[i+1], ">", 0)));
				}
				
        return cstrs.toArray(new Constraint[cstrs.size()]);
    }
}
