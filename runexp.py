#! /usr/bin/env python

import argparse
import subprocess
import sys
from time import gmtime, strftime
date = strftime("%y%m%d%H%M%S", gmtime())



parser = argparse.ArgumentParser(description='Compute a sequence of tests')

#parser.add_argument('file',type=str,help='path to instance file')
#parser.add_argument('--show',type=str,default="nothing",help='level of verbosity',choices=["nothing","decision","solution","model"])
#parser.add_argument('--time',type=int,default=100000,help='time limit in ms')
#parser.add_argument('--node',type=int,default=0,help='node limit')
#parser.add_argument('--seed',type=int,default=1,help='random seed')
parser.add_argument('--length',type=int,default=5,help='seq length')
parser.add_argument('--rank',action='store_false',help='use ranking (default permutation)')
parser.add_argument('--type',type=str,default="uncorrelation",help='type of relation',choices=["uncorrelation","correlation","anticorrelation"])
parser.add_argument('--decomp',action='store_true',help='decompose ranking')

args = parser.parse_args()
    

cmdargs = [str(args.length), str(args.rank), args.type, str(args.decomp)] #, args.show, str(args.time), str(args.node), str(args.seed)]
    
chococmd = ['java', '-cp', 'lib/choco-solver-3.3.1.jar:lib/trove-3.0.0.jar:lib/slf4j-1.7.13/slf4j-simple-1.7.13.jar:lib/slf4j-1.7.13/slf4j-api-1.7.13.jar:out:.', 'RankingExperiment']

chococmd.extend(cmdargs)

print 'run Choco [', ' '.join(chococmd), ']'
choco = subprocess.Popen(chococmd)
choco.wait()
