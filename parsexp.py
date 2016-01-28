#! /usr/bin/env python

import sys
import argparse
import matplotlib.pyplot as plt
import subprocess



def get_cmdline():
    
    parser = argparse.ArgumentParser(description='Parse experiments')

    parser.add_argument('--type',type=str,default="anticorrelation",help='type of relation',choices=["uncorrelation","correlation","anticorrelation","batchscheduling","projectscheduling"])
    
    args = parser.parse_args()
    
    return args.type


def read_data(typeexp):
    
    getfile = subprocess.Popen(['ls', 'experiments/'], stdout=subprocess.PIPE);
    allfiles = [line[:-1] for line in getfile.stdout if line[:len(typeexp)]==typeexp]
    getfile.wait();
        
    return allfiles

    
def plot_data(resfiles):
    
    rawdata = {}
    
    methods = set([])
    lengths = set([])
    
    for filename in resfiles:
        l = filename.split('.')
        if l[-1] == 'res':
        
            m = l[0].split('_')
            method = m[1]
            length = int(m[2])
        
            methods.add(method)
            lengths.add(length)
        
        
            if not rawdata.has_key(method):
                rawdata[method] = {}
            rawdata[method][length] = dict([(d.split()[1],d.split()[2]) for d in open('experiments/'+filename) if d[0]=='d'])
            
            print 'd['+method+']['+str(length)+'] =', rawdata[method][length]
            
    X = sorted(list(lengths))
    T = {}.fromkeys(methods)
    
    for method in methods:
        T[method] = [float(rawdata[method][length]['RUNTIME']) for length in lengths if rawdata[method].has_key(length)]
        #if len(T[method]) < len(X):
            

    
    plt.tick_params(axis='both', which='both', bottom='off', top='off',
                    labelbottom='on', left='off', right='off', labelleft='on')
                    
    #plt.yscale('log')
    
    plt.plot(X[:len(T['no'])],T['no'])
    plt.plot(X[:len(T['gcc'])],T['gcc'])
    plt.plot(X[:len(T['sort'])],T['sort'])

    
    plt.savefig('runtime.png', bbox_inches='tight')
        


if __name__ == '__main__':
    type_exp = get_cmdline()
    
    plot_data(read_data(type_exp))






