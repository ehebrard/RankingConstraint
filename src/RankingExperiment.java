
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



import constraint.Ranking;
import constraint.PropRanking;

import util.Tuple;
import util.BinaryHeap;
import util.MyDecisionMessage;

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
                int type = 0;
                if(args[2].equals("anticorrelation")) type = -1;
                else if(args[2].equals("correlation")) type = 1;
                boolean decomp = (args[3].equals("True"));

								re.footRule(length, perm, type, decomp);


								// /// SCHEDULING STUFF
								//
								// int[][] dur = new int[2][5];
								// int[][] dem = new int[2][5];
								//
								// dur[0][0] = 2;
								// dur[0][1] = 3;
								// dur[0][2] = 1;
								// dur[0][3] = 4;
								// dur[0][4] = 2;
								//
								// dur[1][0] = 4;
								// dur[1][1] = 1;
								// dur[1][2] = 3;
								// dur[1][3] = 2;
								// dur[1][4] = 3;
								//
								// dem[0][0] = 2;
								// dem[0][1] = 1;
								// dem[0][2] = 2;
								// dem[0][3] = 3;
								// dem[0][4] = 1;
								//
								// dem[1][0] = 3;
								// dem[1][1] = 3;
								// dem[1][2] = 2;
								// dem[1][3] = 1;
								// dem[1][4] = 2;
								//
								//
								//
								// re.watScheduling(dur.length, dur[0].length, dur, dem, 4, decomp);

        }
				
				
				public void watScheduling(int num_type, int num_task, int[][] duration, int[][] demand, int capacity, boolean decomp) {
					Solver solver = new Solver("Scheduling");
					
					int horizon = 0;
					for(int t=0; t<num_type; t++) {
						for(int i=0; i<num_task; ++i) {
							horizon += duration[t][i];
						}
					}
					
					
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
					
					IntVar[][] position = VF.integerMatrix("R", num_type, num_task, 1, num_task, solver);
					
					IntVar cap = VF.fixed("capacity", capacity, solver);
					
					IntVar makespan = VF.bounded("Makespan", 0, horizon, solver);
					
					
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
					
					// implied rankings
					for(int t=0; t<num_type; t++) {
						if(decomp) {
							solver.post( Ranking.reformulateGcc( position[t], solver ) );
						} else {
							solver.post( new Ranking( position[t] ) );
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
					
					System.out.println( solver.toString() );
					
					
          Chatterbox.showSolutions(solver);
          //Chatterbox.showDecisions(solver);
					
					//Chatterbox.showDecisions(solver, new MyDecisionMessage(solver, Y));

          //solver.set(new StrategiesSequencer(ISF.domOverWDeg(X, 123), ISF.domOverWDeg(Y, 123))); //, ISF.lexico_LB(Objective)));

          solver.set(new StrategiesSequencer(ISF.lexico_LB(starts[0]), ISF.lexico_LB(ends[0]), ISF.lexico_LB(starts[1]), ISF.lexico_LB(ends[1])));


          solver.findOptimalSolution(ResolutionPolicy.MINIMIZE, makespan);
					
				}


        public void footRule(int N, boolean perm, int type, boolean decomp) {
                Solver solver = new Solver("Correlation");

                IntVar[] X = VF.integerArray("X", N, 1, N, solver);
                IntVar[] Y = VF.integerArray("Y", N, 1, N, solver);

                if(perm) {
                        solver.post(ICF.alldifferent(X));
                        solver.post(ICF.alldifferent(Y));
                } else {
                    if( decomp ) {
                        // IntVar[] XS = VF.integerArray("XS", N, 1, N, solver);
                        // IntVar[] YS = VF.integerArray("YS", N, 1, N, solver);
                        //
                        // solver.post(ICF.sort(X, XS));
                        // solver.post(ICF.sort(Y, YS));
                        //
                        // solver.post(ICF.arithm(XS[0], "=", 1));
                        // solver.post(ICF.arithm(YS[0], "=", 1));
                        //
                        // for(int i = 0; i < N-1; ++i) {
                        //     solver.post(LCF.or(ICF.arithm(XS[i+1], "=", XS[i]),
                        //                        ICF.arithm(XS[i+1], "=", i+2)));
                        //     solver.post(LCF.or(ICF.arithm(YS[i+1], "=", YS[i]),
                        //                        ICF.arithm(YS[i+1], "=", i+2)));
                        // }
												
												
												
												// int[] values = new int[N];
												// for(int i=0; i<N; i++) values[i] = (i+1);
												//
												// IntVar[] XO = VF.integerArray("XOcc", N, 0, N, solver);
												// IntVar[] XCO = VF.integerArray("XCumulOcc", N, 1, N, solver);
												//
												// solver.post( ICF.global_cardinality( X, values, XO, true ) );
												//
												// solver.post( ICF.arithm( XO[0], "=", XCO[0] ) );
												// for(int i=1; i<N; ++i) {
												// 	IntVar[] scope = new IntVar[2];
												// 	scope[0] = XO[i];
												// 	scope[1] = XCO[i-1];
												//
												// 	solver.post( ICF.sum(scope, XCO[i]) );
												// }
												//
												//
												// for(int i=0; i<N-1; ++i) {
												// 	// Occ(1) + ... + Occ(i) >= i
												// 	solver.post( ICF.arithm( XCO[i], ">", i ) );
												//
												// 	// if Occ(1) + ... + Occ(i) > i+1 iff Occ(i+1) = 0
												//                           solver.post(
												// 		LCF.or(ICF.arithm(XCO[i] , "=", (i+1)),
												//                            				 ICF.arithm(XO[i+1], "=", 0)));
												//
												//                           solver.post(
												// 		LCF.or(ICF.arithm(XCO[i] , ">", (i+1)),
												//                            				 ICF.arithm(XO[i+1], ">", 0)));
												// }
												//
												//
												// IntVar[] YO = VF.integerArray("YOcc", N, 0, N, solver);
												// IntVar[] YCO = VF.integerArray("XCumulOcc", N, 1, N, solver);
												//
												// solver.post( ICF.global_cardinality( Y, values, YO, true ) );
												//
												// solver.post( ICF.arithm( YO[0], "=", YCO[0] ) );
												// for(int i=1; i<N; ++i) {
												// 	IntVar[] scope = new IntVar[2];
												// 	scope[0] = YO[i];
												// 	scope[1] = YCO[i-1];
												//
												// 	solver.post( ICF.sum(scope, YCO[i]) );
												// }
												//
												//
												// for(int i=0; i<N-1; ++i) {
												// 	// Occ(1) + ... + Occ(i) >= i
												// 	solver.post( ICF.arithm( YCO[i], ">", i ) );
												//
												// 	// if Occ(1) + ... + Occ(i) > i+1 iff Occ(i+1) = 0
												//                           solver.post(
												// 		LCF.or(ICF.arithm(YCO[i] , "=", (i+1)),
												//                            				 ICF.arithm(YO[i+1], "=", 0)));
												//
												//                           solver.post(
												// 		LCF.or(ICF.arithm(YCO[i] , ">", (i+1)),
												//                            				 ICF.arithm(YO[i+1], ">", 0)));
												// }
												
												
												// IS IT WHAT WAS NOT WORKING FOR GEORGE ?
												 // solver.post( Ranking.reformulateSort( X, solver ) );
												 // solver.post( Ranking.reformulateSort( Y, solver ) );
												 //
												 // solver.post( Ranking.reformulateGcc( X, solver ) );
												 // solver.post( Ranking.reformulateGcc( Y, solver ) );
												
												 // Constraint[] decomposition = Ranking.reformulateSort( X, solver );
												 // for(int i=0; i<decomposition.length; i++) {
												 // 													 solver.post(decomposition[i]);
												 // }
												 // decomposition = Ranking.reformulateSort( Y, solver );
												 //  												 for(int i=0; i<decomposition.length; i++) {
												 //  													 solver.post(decomposition[i]);
												 //  												 }
												 
												 Constraint[] decomposition = Ranking.reformulateGcc( X, solver );
												 for(int i=0; i<decomposition.length; i++) {
													 solver.post(decomposition[i]);
												 }
												 decomposition = Ranking.reformulateGcc( Y, solver );
 												 for(int i=0; i<decomposition.length; i++) {
 													 solver.post(decomposition[i]);
 												 }
												
                    } else {
                        solver.post(new Ranking(X));
                        solver.post(new Ranking(Y));
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
								//solver.post( ICF.arithm( Distance, "=", maxD ) );

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

                Chatterbox.showSolutions(solver);
                //Chatterbox.showDecisions(solver);
								
								//Chatterbox.showDecisions(solver, new MyDecisionMessage(solver, Y));

                //solver.set(new StrategiesSequencer(ISF.domOverWDeg(X, 123), ISF.domOverWDeg(Y, 123))); //, ISF.lexico_LB(Objective)));

                solver.set(new StrategiesSequencer(ISF.lexico_LB(X), ISF.lexico_LB(Y)));


                solver.findOptimalSolution((type<0 ? ResolutionPolicy.MAXIMIZE : ResolutionPolicy.MINIMIZE), Objective);
								
								// int solcount = 0;
								// if(solver.findSolution()){
								//    do{
								//
								// 		 System.out.println(solcount);
								//                      System.out.print("X:");
								//                      for(int i=0; i<N; i++) {
								//                              System.out.print(" "+X[i].getValue());
								//                      }
								//                      System.out.println();
								//                      System.out.print("Y:");
								//                      for(int i=0; i<N; i++) {
								//                              System.out.print(" "+Y[i].getValue());
								//                      }
								//                      System.out.println();
								//                      System.out.print("D:");
								//                      for(int i=0; i<N; i++) {
								//                              System.out.print(" "+D[i].getValue());
								//                      }
								//                      System.out.println();
								//                      System.out.print("Objective: = ");
								//                      System.out.println(Objective.getValue() + "\n");
								//
								// 		 ++solcount;
								//
								// 		 //if(solcount>54) System.exit(1);
								//        // do something, e.g. print out variables' value
								//    } while(solver.nextSolution());
								// }
								// System.out.println(solcount);
								
								// System.exit(1);


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
                        System.out.print("Objective: = ");
                        if(type==0)
                                System.out.print("|" + Distance.getValue() + " - " + maxD/2 + "| = ");
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
