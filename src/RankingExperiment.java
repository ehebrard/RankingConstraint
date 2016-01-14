
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;


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
		
		Solver solver = new Solver("Hello world!");
		
		IntVar[] vars = new IntVar[9];
		vars[3] = VF.integer("x3", 1, 2, solver);
		vars[1] = VF.integer("x1", 1, 2, solver);
		vars[5] = VF.integer("x5", 1, 3, solver);
		vars[8] = VF.integer("x8", 2, 3, solver);
		vars[4] = VF.integer("x4", 1, 4, solver);
		vars[7] = VF.integer("x7", 3, 6, solver);
		vars[6] = VF.integer("x6", 2, 7, solver);
		vars[2] = VF.integer("x2", 4, 7, solver);
		vars[0] = VF.integer("x0", 4, 7, solver);
										
		
		//Tuple< Integer, IntVar >[] cVars = new Tuple< Integer, IntVar >[5];
		
		
		BinaryHeap< Tuple< Integer, IntVar > > varsByIncreasingLowerBound = new BinaryHeap< Tuple< Integer, IntVar > >();
		
		for(int i=0; i<9; i++) {
			//Tuple< Integer, IntVar > cVars = 
				
			varsByIncreasingLowerBound.add( new Tuple(vars[i].getUB(), vars[i]) );
		}
		
		while(!varsByIncreasingLowerBound.isEmpty()) {
			Tuple< Integer, IntVar > var = varsByIncreasingLowerBound.remove();
			
			System.out.println(var.second.toString());
		}
		
		
		Propagator<IntVar> prop = new PropRanking(vars, true);
		
		//solver.post( ranking );
		
		try {
		prop.propagate(0);
	} catch(ContradictionException e) {
		System.out.println("INCONSISTENT");
		
	}
		
		
		
		
		
	}

}
