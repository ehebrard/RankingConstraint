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
package org.chocosolver.solver.search.solution;

import gnu.trove.set.hash.TIntHashSet;
import org.chocosolver.solver.Cause;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.exception.SolverException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.RealVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Class which stores the value of each variable in a solution
 * <br/>
 *
 * @author Jean-Guillaume Fages
 * @author Charles Prud'homme
 * @since 05/06/2013
 */
public class Solution implements Serializable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Solution.class);

    HashMap<IntVar, Integer> intmap = new HashMap<>();
    HashMap<RealVar, double[]> realmap = new HashMap<>();
    HashMap<SetVar, int[]> setmap = new HashMap<>();
    TIntHashSet dvars = new TIntHashSet(16, .5f, -1);
    boolean empty = true;

    public Solution() {
    }

    /**
     * Records the current solution of the solver
     * clears all previous recordings
     *
     * @param solver a solver
     */
    public void record(Solver solver) {
        if (empty) {
            Variable[] _dvars = solver.getStrategy().getVariables();
            for (int i = 0; i < _dvars.length; i++) {
                dvars.add(_dvars[i].getId());
            }
            empty = false;
        }
        boolean warn = false;
        intmap.clear();
        realmap.clear();
        setmap.clear();
        Variable[] vars = solver.getVars();
        for (int i = 0; i < vars.length; i++) {
            int kind = vars[i].getTypeAndKind() & Variable.KIND;
            if (!vars[i].isInstantiated()) {
                if (dvars.contains(vars[i].getId())) {
                    throw new SolverException(vars[i] + " is not instantiated when recording a solution.");
                } else {
                    warn = true;
                }
            } else {
                switch (kind) {
                    case Variable.INT:
                    case Variable.BOOL:
                        IntVar v = (IntVar) vars[i];
                        intmap.put(v, v.getValue());
                        break;
                    case Variable.REAL:
                        RealVar r = (RealVar) vars[i];
                        realmap.put(r, new double[]{r.getLB(), r.getUB()});
                        break;
                    case Variable.SET:
                        SetVar s = (SetVar) vars[i];
                        setmap.put(s, s.getValues());
                        break;
                }
            }
        }
        if (warn && LOGGER.isWarnEnabled()) {
            LOGGER.warn("Some non decision variables are not instantiated in the current solution.");
        }
    }

    /**
     * Set all variables to their respective value in the solution
     * Throws an exception is this empties a domain (i.e. this domain does not contain
     * the solution value)
     * <p>
     * BEWARE: A restart might be required so that domains contain the solution values
     */
    public void restore() throws ContradictionException {
        if (empty) {
            throw new UnsupportedOperationException("Empty solution. No solution found");
        }
        for (IntVar i : intmap.keySet()) {
            i.instantiateTo(intmap.get(i), Cause.Null);
        }
        for (SetVar s : setmap.keySet()) {
            s.instantiateTo(setmap.get(s), Cause.Null);
        }
        for (RealVar r : realmap.keySet()) {
            double[] bounds = realmap.get(r);
            r.updateBounds(bounds[0], bounds[1], Cause.Null);
        }
    }

    @Override
    public String toString() {
        StringBuilder st = new StringBuilder("Solution: ");
        for (IntVar i : intmap.keySet()) {
            st.append(i.getName()).append("=").append(intmap.get(i)).append(", ");
        }
        for (SetVar s : setmap.keySet()) {
            st.append(s.getName()).append("=").append(Arrays.toString(setmap.get(s))).append(", ");
        }
        for (RealVar r : realmap.keySet()) {
            double[] bounds = realmap.get(r);
            st.append(r.getName()).append("=[").append(bounds[0]).append(",").append(bounds[1]).append("], ");
        }

        return st.toString();
    }

    /**
     * Get the value of variable v in this solution
     *
     * @param v IntVar (or BoolVar)
     * @return the value of variable v in this solution, or null if the variable is not instantiated in the solution
     */
    public Integer getIntVal(IntVar v) {
        if (empty) {
            throw new UnsupportedOperationException("Empty solution. No solution found");
        }
        if (intmap.containsKey(v)) {
            return intmap.get(v);
        } else {
            return null;
        }
    }

    /**
     * Get the value of variable s in this solution
     *
     * @param s SetVar
     * @return the value of variable s in this solution, or null if the variable is not instantiated in the solution
     */
    public int[] getSetVal(SetVar s) {
        if (empty) {
            throw new UnsupportedOperationException("Empty solution. No solution found");
        }
        if (setmap.containsKey(s)) {
            return setmap.get(s);
        } else
            return null;
    }

    /**
     * Get the bounds of r in this solution
     *
     * @param r RealVar
     * @return the bounds of r in this solution, or null if the variable is not instantiated in the solution
     */
    public double[] getRealBounds(RealVar r) {
        if (empty) {
            throw new UnsupportedOperationException("Empty solution. No solution found");
        }
        if (realmap.containsKey(r)) {
            return realmap.get(r);
        } else return null;
    }

    /**
     * @return true iff this is a valid solution
     */
    public boolean hasBeenFound() {
        return !empty;
    }
}
