
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.Solver;

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
		
		IntVar[] vars = new IntVar[5];
		vars[0] = VF.integer("x1", 0, 100, solver);
		vars[1] = VF.integer("x2", 5, 56, solver);
		vars[2] = VF.integer("x3", 3, 10, solver);
		vars[3] = VF.integer("x4", 10, 1000, solver);
		vars[4] = VF.integer("x5", 1, 10, solver);
		
		//Tuple< Integer, IntVar >[] cVars = new Tuple< Integer, IntVar >[5];
		
		
		BinaryHeap< Tuple< Integer, IntVar > > varsByIncreasingLowerBound = new BinaryHeap< Tuple< Integer, IntVar > >();
		
		for(int i=0; i<5; i++) {
			//Tuple< Integer, IntVar > cVars = 
				
			varsByIncreasingLowerBound.add( new Tuple(vars[i].getLB(), vars[i]) );
		}
		
		while(!varsByIncreasingLowerBound.isEmpty()) {
			Tuple< Integer, IntVar > var = varsByIncreasingLowerBound.remove();
			
			System.out.println(var.y.toString());
		}
		
		
		
		
	}

}
