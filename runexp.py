#! /usr/bin/env python

import argparse
import subprocess
import sys
from time import gmtime, strftime
date = strftime("%y%m%d%H%M%S", gmtime())



parser = argparse.ArgumentParser(description='Compute a sequence of tests')

parser.add_argument('file',type=str,help='path to instance file')
parser.add_argument('--show',type=str,default="nothing",help='level of verbosity',choices=["nothing","decision","solution","model"])
parser.add_argument('--time',type=int,default=100000,help='time limit in ms')
parser.add_argument('--node',type=int,default=0,help='node limit')
parser.add_argument('--seed',type=int,default=1,help='random seed')


args = parser.parse_args()
    
    
chococmd = ['java', '-cp', 'lib/choco-solver-3.3.1.jar:out:.', 'RankingExperiment', args.file, args.show, str(args.time), str(args.node), str(args.seed)]

print 'run Choco [', ' '.join(chococmd), ']'
choco = subprocess.Popen(chococmd)
choco.wait()