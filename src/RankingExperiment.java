
import org.chocosolver.solver.variables.VF;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.search.strategy.ISF;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;
import org.chocosolver.solver.search.measure.IMeasures;
import org.chocosolver.solver.trace.Chatterbox;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.search.loop.monitors.SMF;
import org.chocosolver.solver.search.limits.FailCounter;
import org.chocosolver.solver.exception.ContradictionException;


import constraint.Ranking;
import constraint.PropRanking;
import constraint.algo.AlgoRankingBC;

import util.Tuple;
import util.BinaryHeap;
import util.MyDecisionMessage;

/**
*
* RankingExperiment
*/

public class RankingExperiment {
	
	
	public int objective;
	public long num_node;
	public long num_backtrack;
	public long num_fail;
	public long num_restart;
	public long num_solution;
	public boolean optimal;
	public boolean limitout;
	public double runtime;
	

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
								
		RankingExperiment re = new RankingExperiment();
		
		//
		// re.test();
		// System.exit(1);
		//

		int length = Integer.parseInt(args[0]);
		boolean perm = (args[1].equals("True"));
		int type = 0;
		if(args[2].equals("anticorrelation")) type = -1;
		else if(args[2].equals("correlation")) type = 1;
		int decomp = 0;
		if(args[3].equals("gcc")) decomp = 1;
		else if(args[3].equals("sort")) decomp = 2;
		boolean schedule = (args[4].equals("True"));
		int time_cutoff = Integer.parseInt(args[5]);
		boolean use_restarts = (args[6].equals("True"));
		int seed = Integer.parseInt(args[7]);
		int showopt = Integer.parseInt(args[8]);
		int num_exp = Integer.parseInt(args[9]);
		//boolean dom_red = (args[10].equals("True"));
		double esolt = Double.parseDouble(args[10]);
		boolean dom_red = false;
		if(esolt>0)
			dom_red = true;
		boolean aligned = (args[11].equals("True"));
		boolean use_probing = (args[12].equals("True"));


		//int halfanour = 1800000;
		//int halfanhour = 1800000;
		int halfanhour = 	time_cutoff;

		if(!schedule) {
									
			//System.out.println(num_exp);
									
			if(num_exp>1) {
				int[] objectives = new int[num_exp];
				long[] nodes = new long[num_exp];
				long[] backtracks = new long[num_exp];
				long[] fails = new long[num_exp];
				long[] restarts = new long[num_exp];
				long[] nsols = new long[num_exp];
				boolean[] optimals = new boolean[num_exp];
				boolean[] limitouts = new boolean[num_exp];
				double[] runtimes = new double[num_exp];
									
				System.out.println( "d NUMINSTANCE    " +   num_exp );	
									
				double avg_runtime = 0;
				int num_launch = 0;
				while(num_launch < num_exp) {
																						
					int i=num_launch;
											
					
											
											
					// int limit = time_cutoff-(int)(1000*avg_runtime);
					// if(limit>halfanhour)
					int	limit = halfanhour;
					
					if(limit>1000) {
						
						System.out.println("run " + (i+1) + " " + esolt + " " + aligned + " for " + limit + "ms");
						
						re.footRule(length, perm, type, decomp, limit, 0, false, true, esolt, aligned, seed+i, use_probing);
						objectives[i] = re.objective;
						nodes[i] = re.num_node;
						backtracks[i] = re.num_backtrack;
						fails[i] = re.num_fail;
						restarts[i] = re.num_restart;
						nsols[i] = re.num_solution;
						nsols[i] = re.num_solution;
						optimals[i] = re.optimal;
						limitouts[i] = re.limitout;
						runtimes[i] = re.runtime;
											
						avg_runtime += runtimes[i];
						
						
						//for(int i=0; i<num_launch; i++) {
							System.out.println( "x LIMITOUT     " +   limitouts[i] );
							System.out.println( "x OBJECTIVE    " +   objectives[i] );
							System.out.println( "x RUNTIME      " +   runtimes[i] );
							System.out.println( "x NODES        " +   nodes[i] );
							//}
										
											
						num_launch++;
					} else break;
					//if(avg_runtime>time_cutoff) break;
											
				}
										
										
										
				int avg_obj = 0;
				int avg_node = 0;
				int avg_fail = 0;
				int avg_backtrack = 0;
				int avg_restart = 0;
				int num_satisfiable = 0;
										
				int num_limit = 0;
				double avg_optimal = 0;
										
				for(int i=0; i<num_launch; i++) {
					avg_obj += objectives[i];
					avg_node += nodes[i];
					avg_fail += fails[i];
					avg_backtrack += backtracks[i];
					avg_restart += restarts[i];
											
					if(optimals[i])	
						avg_optimal += 1.0;
					if(limitouts[i])
						num_limit++;
					if(nsols[i]>0)
						num_satisfiable++;
				}
										
				avg_obj /= num_launch;
				avg_node /= num_launch;
				avg_fail /= num_launch;
				avg_backtrack /= num_launch;
				avg_restart /= num_launch;
				//avg_runtime /= num_launch;
				avg_optimal /= num_launch;
									
									
											
				// for(int i=0; i<num_launch; i++) {
				// 	System.out.println( "x LIMITOUT     " +   limitouts[i] );
				// 	System.out.println( "x OBJECTIVE    " +   objectives[i] );
				// 	System.out.println( "x RUNTIME      " +   runtimes[i] );
				// 	System.out.println( "x NODES        " +   nodes[i] );
				// }

				System.out.println( "d OBJECTIVE    " +   avg_obj );
					
				System.out.println( "d OPTIMAL      " +   avg_optimal );
					

				System.out.println( "d RUNTIME      " +   avg_runtime );
				System.out.println( "d NODES        " +   avg_node );
				System.out.println( "d BACKTRACKS   " +   avg_backtrack );
				System.out.println( "d FAILS        " +   avg_fail );
				System.out.println( "d RESTARTS     " +   avg_restart);
											
				System.out.println( "d NUMSAT       " +   num_satisfiable );
				System.out.println( "d NUMFINISHED  " +   num_launch );
				System.out.println( "d NUMLIMIT     " +   num_limit );
										
										
									
			} else {
				re.footRule(length, perm, type, decomp, time_cutoff, showopt, false, dom_red, esolt, aligned, seed, use_probing);
			}
		} else {

		
								  
			int M=1;
									
			int[] objectives = new int[num_exp];
			long[] nodes = new long[num_exp];
			long[] backtracks = new long[num_exp];
			long[] fails = new long[num_exp];
			long[] restarts = new long[num_exp];
			long[] nsols = new long[num_exp];
			boolean[] optimals = new boolean[num_exp];
			boolean[] limitouts = new boolean[num_exp];
			double[] runtimes = new double[num_exp];
									
			int[][] dur = new int[M][length];
			int[][] dem = new int[M][length];


			double avg_runtime = 0;
			int num_launch = 0;
			
			System.out.println( "d NUMINSTANCE    " +   num_exp );

			while(num_launch < num_exp) {
										
										
				int i=num_launch;
										
				//System.out.println("run " + (i+1));
										
				re.generate_tasks(dur, dem, seed+i);
										
				// int limit = time_cutoff-(int)(1000*avg_runtime);
				// if(limit>halfanhour)
				int	limit = halfanhour;
				
				if(limit > 1000) {
					re.watScheduling(1, length, dur, dem, 4, decomp, limit, use_restarts, dom_red, esolt, seed+i, 0, use_probing);
					//re.footRule(length, perm, type, decomp, time_cutoff, 0, false, true, esolt, aligned, seed+i);
					objectives[i] = re.objective;
					nodes[i] = re.num_node;
					backtracks[i] = re.num_backtrack;
					fails[i] = re.num_fail;
					restarts[i] = re.num_restart;
					nsols[i] = re.num_solution;
					nsols[i] = re.num_solution;
					optimals[i] = re.optimal;
					limitouts[i] = re.limitout;
					runtimes[i] = re.runtime;
										
					avg_runtime += runtimes[i];
									
					System.out.println( "x LIMITOUT     " +   limitouts[i] );
					System.out.println( "x OBJECTIVE    " +   objectives[i] );
					System.out.println( "x RUNTIME      " +   runtimes[i] );
					System.out.println( "x NODES        " +   nodes[i] );
										
					num_launch++;
				} else break;
				//if(avg_runtime>time_cutoff) break;
										
			}
									
									
									
			int avg_obj = 0;
			int avg_node = 0;
			int avg_fail = 0;
			int avg_backtrack = 0;
			int avg_restart = 0;
			int num_satisfiable = 0;
									
			double avg_optimal = 0;
			int num_limit = 0;
									
			for(int i=0; i<num_launch; i++) {
				avg_obj += objectives[i];
				avg_node += nodes[i];
				avg_fail += fails[i];
				avg_backtrack += backtracks[i];
				avg_restart += restarts[i];
										
				if(optimals[i])	
					avg_optimal += 1.0;
				if(limitouts[i])	
					num_limit++;
				if(nsols[i]>0)
					num_satisfiable++;
			}
									

			avg_obj /= num_launch;
			avg_node /= num_launch;
			avg_fail /= num_launch;
			avg_backtrack /= num_launch;
			avg_restart /= num_launch;
			//avg_runtime /= num_launch;
			avg_optimal /= num_launch;
											
										
			// for(int i=0; i<num_launch; i++) {
			// 	System.out.println( "x LIMITOUT     " +   limitouts[i] );
			// 	System.out.println( "x OBJECTIVE    " +   objectives[i] );
			// 	System.out.println( "x RUNTIME      " +   runtimes[i] );
			// 	System.out.println( "x NODES        " +   nodes[i] );
			// }
			
			System.out.println( "d OBJECTIVE    " +   avg_obj );
				
			System.out.println( "d OPTIMAL      " +   avg_optimal );
				
			System.out.println( "d RUNTIME      " +   avg_runtime );
			System.out.println( "d NODES        " +   avg_node );
			System.out.println( "d BACKTRACKS   " +   avg_backtrack );
			System.out.println( "d FAILS        " +   avg_fail );
			System.out.println( "d RESTARTS     " +   avg_restart);
										
			System.out.println( "d NUMSAT       " +   num_satisfiable );
			System.out.println( "d NUMFINISHED  " +   num_launch );		
			System.out.println( "d NUMLIMIT     " +   num_limit );
			
		}
									
	}
						

									

				
	public void watScheduling(int num_type, int num_task, int[][] duration, int[][] demand, int capacity, int decomp, int time_cutoff, boolean restarts, boolean dom_red, double esolt, int seed, int showopt, boolean use_probing) {
		Solver solver = new Solver("Scheduling");
					
		int horizon = 0;
		for(int t=0; t<num_type; t++) {
			for(int i=0; i<num_task; ++i) {
				horizon += num_task * duration[t][i];
			}
		}
		
		//horizon = 200;
					
					
		IntVar[][] starts = VF.boundedMatrix("s", num_type, num_task, 0, horizon, solver);
		IntVar[][] durs   = VF.boundedMatrix("p", num_type, num_task, 0, horizon, solver);
		IntVar[][] ends   = VF.boundedMatrix("e", num_type, num_task, 0, horizon, solver);
					
					
					
					
		IntVar[] demands = new IntVar[num_type*num_task];
					
		Task[] tasks = new Task[num_type*num_task];
		for(int t=0; t<num_type; t++) {
			for(int i=0; i<num_task; ++i) {
				tasks[t*num_task+i] = new Task(starts[t][i], durs[t][i], ends[t][i]);
				demands[t*num_task+i] = VF.fixed("d_"+t+","+i, demand[t][i], solver);
			}
		}
					
		IntVar[][] position = VF.enumeratedMatrix("R", num_type, num_task, 1, num_task, solver);
					
					
		if(dom_red) {
						
			System.out.println( "reduce intervals" );
						
			for(int t=0; t<num_type; t++) {
				//post_random_domain_reduction(starts[t], 0, horizon, solver, seed+t);
				sat_random_intervals(position[t], solver, esolt, seed+t, false);
			}
		}
					
					
		IntVar cap = VF.fixed("capacity", capacity, solver);
					
		IntVar makespan = VF.bounded("Makespan", 0, horizon, solver);
					
		IntVar[] allvars = new IntVar[2*num_type*num_task];
		int k=0;
		for(int t=0; t<num_type; t++) {
			for(int i=0; i<num_task; i++) {
				allvars[k++] = position[t][i];
				allvars[k++] = starts[t][i];
			}
		}
					
		
		//channelling position <-> time
		for(int t=0; t<num_type; t++) {
			for(int i=0; i<num_task; i++) {
				for(int j=i+1; j<num_task; j++) {
					solver.post( LCF.or( ICF.arithm(ends[t][i], ">", starts[t][j] ), ICF.arithm(position[t][i], "<", position[t][j]) ) );
					solver.post( LCF.or( ICF.arithm(starts[t][i], "<", ends[t][j] ), ICF.arithm(position[t][i], ">", position[t][j]) ) );
				}
			}
		}

					
		// channelling duration <-> position
		for(int t=0; t<num_type; t++) {
			for(int i=0; i<num_task; i++) {
				// IntVar dur_const = VF.fixed("d", duration[t][i], solver);
				solver.post( ICF.times( position[t][i], duration[t][i], durs[t][i]) );
			}
		}
					
		// ranking
		for(int t=0; t<num_type; t++) {
			if(decomp==1) {
				solver.post( Ranking.reformulateGcc( position[t], solver ) );
			} else if(decomp==2) {
				solver.post( Ranking.reformulateSort( position[t], solver ) );
			} else {
				solver.post( new Ranking( position[t], use_probing ) );
			}
		}

					
		// cumulative
		solver.post( ICF.cumulative(tasks, demands, cap) );
					
		// objective
		for(int t=0; t<num_type; t++) {
			for(int i=0; i<num_task; i++) {
				solver.post( ICF.arithm( ends[t][i], "<=", makespan ) );
			}
		}
					
				
					
		set_display(showopt, solver);
        

		solver.set( ISF.sequencer(ISF.activity(allvars, seed), ISF.lexico_LB(makespan)) );
					
		if(restarts) {
			SMF.luby(solver, 2, 2, new FailCounter(2), 25000);
		}
					
		if(time_cutoff > 0) {
			SMF.limitTime(solver, time_cutoff);
		}

		solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, makespan);
					
					
		if(solver.getMeasures().getSolutionCount()>0) {		
			if((showopt&2)>0) {				
				int fmakespan = makespan.getValue();
				for(int t=0; t<num_type; t++) {
					for(int i=0; i<num_task; i++) {
						for(int q=0; q<demand[t][i]; q++) {
							for(int j=0; j<starts[t][i].getValue(); j++) {
								System.out.print(" ");
							}
							for(int j=starts[t][i].getValue(); j<ends[t][i].getValue(); j++) {
								System.out.print(duration[t][i]);
							}
							System.out.println();
						}
					}
					System.out.println();
				}
			}
						
			if((showopt&32)>0) {
				if(solver.getMeasures().isObjectiveOptimal()) {
					System.out.println("s OPTIMAL!");
				} else {
					System.out.println("s SUBOPTIMAL");
				}
			}		
		} else if((showopt&32)>0) {
			if(solver.getMeasures().isObjectiveOptimal()) {
				System.out.println("s NO SOLUTION!");
			} else {
				System.out.println("s NO SOLUTION FOUND");
			}
		}
	
		print_statistics(solver, ((showopt&8)>0));
						
	}

	public void test() {
		Solver solver = new Solver("test");
					
		// IntVar[] X = new IntVar[7];
		// X[0] = VF.enumerated("x1", 1, 7, solver);
		// X[1] = VF.enumerated("x2", 1, 7, solver);
		// X[2] = VF.enumerated("x3", 2, 4, solver);
		// X[3] = VF.enumerated("x4", 2, 3, solver);
		// X[4] = VF.enumerated("x5", 3, 4, solver);
		// X[5] = VF.enumerated("x6", 1, 7, solver);
		// X[6] = VF.enumerated("x7", 3, 4, solver);
					
		// IntVar[] X = new IntVar[6];
		// X[0] = VF.enumerated("x1", 3, 4, solver);
		// X[1] = VF.enumerated("x2", 2, 4, solver);
		// X[2] = VF.enumerated("x3", 3, 4, solver);
		// X[3] = VF.enumerated("x4", 2, 5, solver);
		// X[4] = VF.enumerated("x5", 3, 6, solver);
		// X[5] = VF.enumerated("x6", 1, 6, solver);
					
		// IntVar[] X = new IntVar[9];
		// X[0] = VF.enumerated("x1", 1, 2, solver);
		// X[1] = VF.enumerated("x2", 1, 2, solver);
		// X[2] = VF.enumerated("x3", 1, 3, solver);
		// X[3] = VF.enumerated("x4", 2, 3, solver);
		// X[4] = VF.enumerated("x5", 1, 4, solver);
		// X[5] = VF.enumerated("x6", 3, 6, solver);
		// X[6] = VF.enumerated("x7", 2, 7, solver);
		// X[7] = VF.enumerated("x8", 4, 7, solver);
		// X[8] = VF.enumerated("x9", 4, 7, solver);
		
		// IntVar[] X = new IntVar[8];
		// X[0] = VF.enumerated("x1", 1, 2, solver);
		// X[1] = VF.enumerated("x2", 1, 2, solver);
		// X[2] = VF.enumerated("x3", 2, 3, solver);
		// X[3] = VF.enumerated("x4", 1, 20, solver);
		// X[4] = VF.enumerated("x5", 1, 20, solver);
		// X[5] = VF.enumerated("x6", 1, 20, solver);
		// X[6] = VF.enumerated("x7", 1, 2, solver);
		// X[7] = VF.enumerated("x8", 1, 2, solver);
		// //X[8] = VF.enumerated("x9", 1, 2, solver);
		
		IntVar[] X = new IntVar[4];
		X[0] = VF.enumerated("x1", 1, 2, solver);
		X[1] = VF.enumerated("x2", 2, 3, solver);
		X[2] = VF.enumerated("x3", 2, 3, solver);
		X[3] = VF.enumerated("x4", 4, 4, solver);



					
		//PropRanking propagator = new PropRanking( X, true );
					
							
					
		AlgoRankingBC cg_algo = new AlgoRankingBC(null);
					
		cg_algo.reset(X);
					
					
		cg_algo.computeHallInt();
					
		// cg_algo.sortIt();
		//
		// //cg_algo.print_structure();
		//
		// try {
		// 	cg_algo.computeHall();
		// } catch(ContradictionException e) {
		// 	System.out.println("wipe out!");
		// }
		//
		// try {
		// 	cg_algo.filterFromRules();
		// } catch(ContradictionException e) {
		// 	System.out.println("wipe out!");
		// }
					
					
	}				
					
					
									
				
	private void generate_tasks(int[][] dur, int[][] dem, int seed) {
					
		java.util.Random random = new java.util.Random(seed);
					
		int M = dur.length;
		int N = dur[0].length;
					
		for(int t=0; t<M; t++) {
			for(int i=0; i<N; i++) {
				dur[t][i] = 1+random.nextInt(4);
				dem[t][i] = 1+random.nextInt(3);
				
				//System.out.print(" " + dur[t][i]);
			}
			//System.out.println();
		}
		//System.out.println();
		// for(int t=0; t<M; t++) {
		// 	for(int i=0; i<N; i++) {
		//
		// 		System.out.print(" " + dem[t][i]);
		// 	}
		// 	System.out.println();
		// }
		// System.out.println();
					
					
	}
				
				
				
	private void sat_random_intervals(IntVar[] X, Solver solver, double p, int seed, boolean aligned) {
		java.util.Random random = new java.util.Random(seed);
					
		int N = X.length;
					
		int[] solution = new int[N];

		solution[0] = 1;
					
		//System.out.print("sol: 1");
					
		for(int i=1; i<N; ++i) {
						
			double rd = random.nextFloat();
						
			if(rd<p) solution[i] = solution[i-1];
			else solution[i] = (i+1);
						
			//System.out.print( " " + solution[i] );
						
		}
					
		//System.out.println();
						
		for(int i=0; i<N; ++i) {
			int v = solution[i];
						
			if(!aligned) {
				int r = i+random.nextInt(N-i);
				v = solution[r];
				solution[r] = solution[i];
				solution[i] = v;
			}
						
			int lb = 1;
			if(v>1)
				lb = 1+random.nextInt(v-1);
			int ub = N;
			if(v<N)
				ub = v+random.nextInt(N-v);
						
			//System.out.println(  "[" + lb + " - " + v + " - " + ub + "]");
						
			solver.post( ICF.arithm(X[i], ">=", lb ) );
			solver.post( ICF.arithm(X[i], "<=", ub ) );
		}
					
	}
				
				
				
	private void random_intervals(IntVar[] X, Solver solver, int seed) {
		java.util.Random random = new java.util.Random(seed);
					
		int N = X.length;
				
					
		//System.out.println();
		for(int i=0; i<N; ++i) {
						
			int lb = 1+random.nextInt(N);
			int ub = 1+random.nextInt(N);
						
			if(lb>ub) {
				int aux = lb;
				lb = ub;
				ub = aux;
			}
						
			//System.out.println(  "[" + lb + ", " + ub + "]");
						
			solver.post( ICF.arithm(X[i], ">=", lb ) );
			solver.post( ICF.arithm(X[i], "<=", ub ) );
		}
					
	}
				
				
				
	private void post_random_domain_reduction_old(IntVar[] X, Solver solver, int seed) {
		java.util.Random random = new java.util.Random(seed);
					
		int N = X.length;
					
		System.out.println();
					
		for(int i=0; i<N; ++i) {
						
			int l = 2;
			int b = 5;
			int u = 1;
			int n = 2;
						
			int btype = random.nextInt(5);
						
			
						
			if(btype<l) {
				int ub = 1+random.nextInt(N);
							
				System.out.println(  "[" + 1 + ", " + ub + "]");
							
				solver.post( ICF.arithm(X[i], "<=", ub ) );
			} else if(btype<l+b) {
				int lb = 1+random.nextInt(N);
				int ub = 1+random.nextInt(N);
							
				if(lb>ub) {
					int aux = lb;
					lb = ub;
					ub = aux;
				}
							
				System.out.println(  "[" + lb + ", " + ub + "]");
							
				solver.post( ICF.arithm(X[i], ">=", lb ) );
				solver.post( ICF.arithm(X[i], "<=", ub ) );
			} else if(btype<l+b+u) {
				int lb = 1+random.nextInt(N);
							
				System.out.println(  "[" + lb + ", " + N + "]");
							
				solver.post( ICF.arithm(X[i], ">=", lb ) );
			} else {
							
				System.out.println(  "[" + 1 + ", " + N + "]");
							
			}
						
		}
					
	}
				
				
				
	private void post_random_domain_reduction(IntVar[] X, int lb, int ub, Solver solver, int seed) {
		java.util.Random random = new java.util.Random(seed);
					
		int N = X.length;
					
					
		for(int i=0; i<N; ++i) {

			int bound = 0;
						
			if(random.nextInt(3)>0) {
						
				double d = random.nextGaussian()/2.0;
				while(d<-1 || d>1) d = random.nextGaussian()/2.0;

				if(d<0)	d = -d;


				bound = lb+(int)(d * (ub-lb+1)); 
						
				System.out.println( d + ": [" + bound + ", " + ub + "]");
							
				solver.post( ICF.arithm(X[i], ">=", bound ) );
										
			}
					
		}

	}
				


	public void footRule(int N, boolean perm, int type, int decomp, int time_cutoff, int showopt, boolean clue, boolean dom_red, double esolt, boolean aligned, int seed, boolean use_probing) {
									
		Solver solver = new Solver("Correlation");

		IntVar[] X = VF.enumeratedArray("X", N, 1, N, solver);
		IntVar[] Y = VF.enumeratedArray("Y", N, 1, N, solver);
								

		if(dom_red) {
			if(esolt<1) {

				sat_random_intervals(X, solver, esolt, seed, aligned);
				sat_random_intervals(Y, solver, esolt, seed+1, aligned);
			} else {

				random_intervals(X, solver, seed);
				random_intervals(Y, solver, seed+1);
			}
									
			// post_random_domain_reduction_old(X, solver, seed);
			// post_random_domain_reduction_old(Y, solver, seed+1);
		}


		if(perm) {
			solver.post(ICF.alldifferent(X));
			solver.post(ICF.alldifferent(Y));
		} else {
									
			// ranking
				
			if(decomp==1) {
				solver.post( Ranking.reformulateGcc( X, solver ) );
				solver.post( Ranking.reformulateGcc( Y, solver ) );
			} else if(decomp==2) {
				solver.post( Ranking.reformulateSort( X, solver ) );
				solver.post( Ranking.reformulateSort( Y, solver ) );
			} else {
				solver.post( new Ranking( X, use_probing ) );
				solver.post( new Ranking( Y, use_probing ) );
			}
									
	
		}

		// distance
		IntVar[] D = VF.integerArray("D", N, 0, N-1, solver);

		for(int i=0; i<N; ++i) {
			solver.post(ICF.distance(X[i], Y[i], "=", D[i]));
		}

		int maxD = N*N;

		if(!perm) {
			if((N%2)==0)
				maxD = (3*N/2-1)*N/2;
			else {
				maxD = 2*(N/2)*(N/2) + (N/2+1)*(N/2) + N/2;
			}
		} else {
			if((maxD%2)>0) maxD++;
			maxD /= 2;
		}

		IntVar Distance = VF.bounded("TotalDistance", 0, maxD, solver);
		solver.post(ICF.sum(D, Distance));
			
		IntVar Objective = null;


		if(type == 0) {
			// UNCORRELATION

			IntVar Ref = VF.fixed("max/2", maxD/2, solver);
			Objective = VF.bounded("Correlation", 0, maxD/2, solver);
			solver.post(ICF.distance(Distance, Ref, "=", Objective));

		} else if(type!=0) {
			// CORRELATION

			Objective = Distance;

		}
								
								
								
		// if(example)
		// 	showopt += 3;
		set_display(showopt, solver);

	
		//solver.set(new StrategiesSequencer(ISF.domOverWDeg(X, 123), ISF.domOverWDeg(Y, 123))); //, ISF.lexico_LB(Objective)));

		solver.set( ISF.sequencer(ISF.lexico_LB(X), ISF.lexico_LB(Y)) );

		if(time_cutoff > 0) {
			SMF.limitTime(solver, time_cutoff);
		}

		solver.findOptimalSolution((type<0 ? ResolutionPolicy.MAXIMIZE : ResolutionPolicy.MINIMIZE), Objective);
								


		if(solver.getMeasures().getSolutionCount()>0) {
			if((showopt&2)>0) {
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
				System.out.print("Objective: = ");
				if(type==0)
					System.out.print("|" + Distance.getValue() + " - " + maxD/2 + "| = ");
				System.out.println(Objective.getValue());
			}
			if((showopt&32)>0) {
				if(solver.getMeasures().isObjectiveOptimal()) {
					System.out.println("s OPTIMAL!");
				} else {
					System.out.println("s SUBOPTIMAL");
				}
			}		
		} else if((showopt&32)>0) {
			if(solver.getMeasures().isObjectiveOptimal()) {
				System.out.println("s NO SOLUTION!");
			} else {
				System.out.println("s NO SOLUTION FOUND");
			}
		}

		print_statistics(solver, ((showopt&8)>0));

	}
				
				
	public void set_display(int showopt, Solver solver) {
		if((showopt&1)>0)
			Chatterbox.showDecisions(solver);
		if((showopt&16)>0)
			Chatterbox.showSolutions(solver);
		if((showopt&4)>0) {
			System.out.println(solver.toString());
		}
	}
				
				
	public void print_statistics(Solver solver, boolean doprint) {
		IMeasures stats = solver.getMeasures();
					
					
		objective = (int)(stats.getBestSolutionValue());
		num_node = stats.getNodeCount();
		num_backtrack = stats.getBackTrackCount();
		num_fail = stats.getFailCount();
		num_restart = stats.getRestartCount();
		optimal = stats.isObjectiveOptimal();
		runtime = stats.getTimeCount();
		num_solution = stats.getSolutionCount();
		limitout = solver.hasReachedLimit();
					
		if(doprint)
			print_stored_statistics();
	}
					
	public void print_stored_statistics() {
		System.out.println( "d OBJECTIVE    " +   objective );
					
		System.out.println( "d OPTIMAL      " +   optimal );
		System.out.println( "d LIMITOUT     " +   limitout );
					
		System.out.println( "d RUNTIME      " +   runtime );
		System.out.println( "d NODES        " +   num_node );
		System.out.println( "d BACKTRACKS   " +   num_backtrack );
		System.out.println( "d FAILS        " +   num_fail );
		System.out.println( "d RESTARTS     " +   num_restart );

		System.out.println( "d NUMSOLUTIONS " +  num_solution );
										
					
	}

				
}
