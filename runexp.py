#! /usr/bin/env python

import argparse
import subprocess
import sys
import time


#from time import gmtime, strftime
date = time.strftime("%y%m%d%H%M%S", time.gmtime())

chocoprefix = ['java', '-cp', 'lib/choco-solver-3.3.1.jar:lib/trove-3.0.0.jar:lib/slf4j-1.7.13/slf4j-simple-1.7.13.jar:lib/slf4j-1.7.13/slf4j-api-1.7.13.jar:out:.', 'RankingExperiment']


def get_cmdline():
    
    parser = argparse.ArgumentParser(description='Compute a sequence of tests')

    #parser.add_argument('file',type=str,help='path to instance file')
    parser.add_argument('--show',type=str,help='level of verbosity',nargs='*',choices=["decision","solution","model","statistics","improvement","outcome"])
    parser.add_argument('--time',type=int,default=-1,help='time limit in ms')
    #parser.add_argument('--node',type=int,default=0,help='node limit')
    parser.add_argument('--seed',type=int,default=12345,help='random seed')
    parser.add_argument('--length',type=int,default=5,help='seq length')
    parser.add_argument('--rank',action='store_false',help='use ranking (default permutation)')
    parser.add_argument('--type',type=str,default="uncorrelation",help='type of relation',choices=["uncorrelation","correlation","anticorrelation"])
    parser.add_argument('--decomp',type=str, default='no',help='decompose ranking',choices=['no','gcc','sort'])
    parser.add_argument('--schedule',action='store_true',help='scheduling problem (default correlation)')
    parser.add_argument('--restart',action='store_true',help='use restart')
    parser.add_argument('--runs',type=int,default=1,help='number of runs')
    parser.add_argument('--prune',type=float,default=0,help='randomly prune bounds (value controls embedded solution type)')
    parser.add_argument('--aligned',action='store_true',help='whether embedded solutions should be aligned')

    args = parser.parse_args()


    showopt = 0;
    
    if args.show != None:
        things_to_show = set(args.show)
        if "decision" in things_to_show:
            showopt += 1
        if "solution" in things_to_show:
            showopt += 2
        if "model" in things_to_show:
            showopt += 4
        if "statistics" in things_to_show:
            showopt += 8
        if "improvement" in things_to_show:
            showopt += 16
        if "outcome" in things_to_show:
            showopt += 32
    
    cmdargs = [str(args.length), str(args.rank), args.type, str(args.decomp), str(args.schedule), str(args.time), str(args.restart), str(args.seed), str(showopt), str(args.runs), str(args.prune), str(args.aligned)] #, args.show, str(args.time), str(args.node), str(args.seed)]
    
    #chococmd = ['java', '-ea', '-cp', 'lib/choco-solver-3.3.1.jar:lib/trove-3.0.0.jar:lib/slf4j-1.7.13/slf4j-simple-1.7.13.jar:lib/slf4j-1.7.13/slf4j-api-1.7.13.jar:out:.', 'RankingExperiment']
    
    chococmd = [cmd for cmd in chocoprefix]

    chococmd.extend(cmdargs)
    
    return chococmd


def run_cmdline(chococmd):
    print 'c run Choco [', ' '.join(chococmd), ']'
    choco = subprocess.Popen(chococmd)
    choco.wait()
    
def run_cmdline_and_store(chococmd, filename):
    #print 'c run Choco [', ' '.join(chococmd), ']'
    
    #fout = open(filename+'.res', 'w')
    fout = open(filename+'.res', 'w')
    ferr = open(filename+'.err', 'w')
    
    fout.write('c run Choco ['+' '.join(chococmd)+']\n')
    
    choco = subprocess.Popen(chococmd, stdout=fout, stderr=ferr)
    choco.wait()
    
    fout.close()
    ferr.close()


        
def run_experiments(typecor, methods, cutoff, init_length, nruns):
    
    stop = False
    
    #methods = ['no', 'gcc', 'sort']
    
    length = init_length
    
    runtimes = dict(zip(methods, [0 for i in range(len(methods))]))
    
    while not stop:
        
        print 'run for n =', length
        
        stop = True
        for method in methods:
            
            if 1000*runtimes[method] < cutoff-1000:
                stop = False
                
                print method, 
                sys.stdout.flush()
            
                cmdline = [cmd for cmd in chocoprefix]
                
                if typecor == 'schedule':
                    cmdargs = [str(length), str(False), typecor, method, str(True), str(cutoff), str(True), '12345', '8', str(nruns), '0', str(False)]
                else:
                    cmdargs = [str(length), str(False), typecor, method, str(False), str(cutoff), str(False), '12345', '8', str(nruns), '1.0', str(False)]
                
                #cmdline.extend(['--rank', '--time', str(cutoff), '--length', str(length), '--type', typecor, '--show', 'statistics', '--decomp', method])
                cmdline.extend( cmdargs )
            
                key = 'experiments/'+typecor+'_'+method+'_'+str(length)
            
            
                start = time.time() 
                print ' '.join(cmdline)
                run_cmdline_and_store(cmdline, key)
                end = time.time() 

                runtimes[method] = (end-start)
                
                print runtimes[method]
          
        length += 1
        
        
def generate_command_lines(typecor, methods, cutoff, init_length, limit, nruns, suffix):
    
    red_type = 0.5
    if suffix == 'rand':
        red_type = 1.0
    
    length = init_length
    for length in range(init_length, limit+1):
        for method in methods:
            cmdline = [cmd for cmd in chocoprefix]
            
            if typecor == 'schedule':
                cmdargs = [str(length), str(False), typecor, method, str(True), str(cutoff), str(True), '12345', '8', str(nruns), str(red_type), str(False)]
            else:
                cmdargs = [str(length), str(False), typecor, method, str(False), str(cutoff), str(False), '12345', '8', str(nruns), str(red_type), str(True)]

            cmdline.extend( cmdargs )
            outfile = 'experiments/'+typecor+'_'+method+'_'+str(length)+'_'+suffix+'_.res'            
    
            print ' '.join(cmdline), '>', outfile, ';'
        


if __name__ == '__main__':
    if len(sys.argv)>1:
        run_cmdline(get_cmdline())
    else:
        #print "run experiments"
        #run_experiments('schedule', ['no', 'gcc', 'sort'], 3600000, 7, 20)
        
        #generate_command_lines('schedule', ['no', 'gcc', 'sort'], 10800000, 5, 15, 50, 'batch')
        generate_command_lines('uncorrelation', ['no', 'gcc', 'sort'], 10800000, 5, 20, 1000, 'sat')
        #generate_command_lines('uncorrelation', ['no', 'gcc', 'sort'], 10800000, 5, 20, 1000, 'rand')





