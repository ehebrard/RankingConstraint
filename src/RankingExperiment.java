
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.ResolutionPolicy;



import constraint.Ranking;
import constraint.PropRanking;

import util.Tuple;
import util.BinaryHeap;

/**
* 
* RankingExperiment
*/

public class RankingExperiment {

	/**
		* constructeur
		*/
	public RankingExperiment(){

	}

	/**
		* fonction principale pour tester que la classe fonctionne bien
		* @param args
		*/
	public static void main(String[] args){
		//System.out.println();
		
		RankingExperiment re = new RankingExperiment();
		
		int length = Integer.parseInt(args[0]);
		boolean perm = (args[1].equals("True"));
		
		re.maxFootRuleUncorrelation(length, perm);
	}
		
		
	public void maxFootRuleUncorrelation(int N, boolean perm) {

		Solver solver = new Solver("Hello world!");
	
		IntVar[] X = VF.integerArray("X", N, 1, N, solver);
		IntVar[] Y = VF.integerArray("Y", N, 1, N, solver);
		
		if(perm) {
			solver.post(ICF.alldifferent(X));
			solver.post(ICF.alldifferent(Y));
		} else {
			solver.post(new Ranking(X));
			solver.post(new Ranking(Y));
		}
	
		// distance
		IntVar[] D = VF.integerArray("D", N, 0, N-1, solver);
	
		for(int i=0; i<N; ++i) {
			solver.post(ICF.distance(X[i], Y[i], "=", D[i]));
		}
		
		int maxD = N*N/2;
		if(!perm) {
			maxD = (3*N/2-1)*N/2;
		}
		
		IntVar Distance = VF.bounded("TotalDistance", 0, maxD, solver);
		solver.post(ICF.sum(D, Distance));
		
		IntVar Ref = VF.fixed("max/2", maxD/2, solver);
		IntVar Objective = VF.bounded("Correlation", 0, maxD/2, solver);
		solver.post(ICF.distance(Distance, Ref, "=", Objective));
		
		Chatterbox.showSolutions(solver);
		
		solver.set(new StrategiesSequencer(ISF.domOverWDeg(X, 123), ISF.domOverWDeg(Y, 123))); //, ISF.lexico_LB(Objective)));
		
		
		
		solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, Objective);
		
		
		if(solver.getMeasures().getSolutionCount()>0) {
			System.out.print("X:");
			for(int i=0; i<N; i++) {
				System.out.print(" "+X[i].getValue());
			}
			System.out.println();
			System.out.print("Y:");
			for(int i=0; i<N; i++) {
				System.out.print(" "+Y[i].getValue());
			}
			System.out.println();
			System.out.print("D:");
			for(int i=0; i<N; i++) {
				System.out.print(" "+D[i].getValue());
			}
			System.out.println();
			System.out.print("Objective: = |" + Distance.getValue() + " - " + maxD/2 + "| = ");
			System.out.println(Objective.getValue());
		} else {
			if(solver.getMeasures().isObjectiveOptimal()) {
				System.out.println("NO SOLUTION!");
			} else {
				System.out.println("NO SOLUTION FOUND");
			}	
		}
		System.out.println(solver.getMeasures().toOneShortLineString() + "\n");
		
	
	}

}
