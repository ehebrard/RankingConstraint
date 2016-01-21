package util;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.trace.IMessage;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.IntVar;

public class MyDecisionMessage implements IMessage{

	private Solver solver;
	private IntVar[] vars;

    public MyDecisionMessage(Solver solver, IntVar[] vars) {
        this.solver = solver;
				this.vars = vars;
    }

    @Override
    public String print() {
        String s ="";
        for (int i = 0; i < vars.length ; i++) {
        	s=s+vars[i]+" ";
        }
        return s.toString();
    }

	
}
