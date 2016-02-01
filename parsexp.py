#! /usr/bin/env python

import sys
import argparse
import matplotlib.pyplot as plt
import subprocess



def get_cmdline():
    
    parser = argparse.ArgumentParser(description='Parse experiments')

    parser.add_argument('--type',type=str,default="anticorrelation",help='type of relation',choices=["uncorrelation","correlation","anticorrelation","schedule"])
    parser.add_argument('--subtype',type=str,default="sat",help='subtype',choices=["sat","rand"])
    parser.add_argument('--stat',type=str,default="RUNTIME",help='statistic',choices=['RUNTIME','NODES','BACKTRACKS','FAILS','RESTARTS','OPTIMAL','OBJECTIVE'])
    
    args = parser.parse_args()
    
    return args.type, args.subtype, args.stat


def read_data(typeexp, st):
    
    getfile = subprocess.Popen(['ls', 'experiments/'], stdout=subprocess.PIPE);
 
    allfiles = None
    if typeexp == 'schedule':
        allfiles = [line[:-1] for line in getfile.stdout if line[:len(typeexp)]==typeexp]
    else:
        allfiles = [line[:-1] for line in getfile.stdout if line[:len(typeexp)]==typeexp and (line.find(st) == len(line)-(6+len(st)))]
    getfile.wait();
        
    return allfiles

    
def plot_data(resfiles, type_exp, stat):
    
    average = {}
    allpoints = {}
    num_instance = 0
    
    
    methods = set([])
    lengths = set([])
    
    maxlength = 0
    minval = None
    maxval = None
    
    for filename in resfiles:
        
        print 'read', filename
        
        count = 0
        l = filename.split('.')
        if l[-1] == 'res':
        
            m = l[0].split('_')
            method = m[1]
            length = int(m[2])
            
            print method, length
            
        
            methods.add(method)
            lengths.add(length)
        
        
            if not average.has_key(method):
                average[method] = {}
                
            if not allpoints.has_key(method):    
                allpoints[method] = {}
                # stats = ['RUNTIME', 'OBJECTIVE', 'NODES']
                # allpoints[method] = {}.fromkeys(stats)
                # for stat in stats:
                #     allpoints[method][stat] = []
                
            average[method][length] = {}
            
            empty = True
            for d in open('experiments/'+filename):
                sd = d.split()
                if d[0] == 'd':
                    # print d,
                    print 'total', sd[1], '=', sd[2], '\n'
                    average[method][length][sd[1]] = sd[2]
                    empty = False
                elif d[0] == 'x':
                    count += 1
                    if not allpoints[method].has_key(sd[1]):
                        allpoints[method][sd[1]] = []
                    allpoints[method][sd[1]].append(sd[2])
            num_instance = count/3
            
            # ### A SUPPRIMER
            # average[method][length]['NUMINSTANCE'] = 1000
            # if type_exp == 'schedule':
            #     average[method][length]['NUMINSTANCE'] = 50
            
            if not empty and maxlength<length:
                maxlength = length
                
                
            #average[method][length] = dict([(d.split()[1],d.split()[2]) for d in open('experiments/'+filename) if d[0]=='d'])
            
            #print 'd['+method+']['+str(length)+'] =', rawdata[method][length]
            
    X = sorted(list(lengths))
    T = {}.fromkeys(methods)
    S = {}.fromkeys(methods)
    
    
    for method in methods:
        
        #print method, 
        
        T[method] = []
        S[method] = []
        for length in X:
            t = None
            s = None
            if average[method].has_key(length):
                if average[method][length].has_key(stat):
                    
                    all_solved = False
                    if int(average[method][length]['NUMINSTANCE']) == int(average[method][length]['NUMFINISHED']):
                        all_solved = True
                    
                    if all_solved:
                        if stat == 'RUNTIME' or stat == 'OPTIMAL':
                            t = float(average[method][length][stat])
                        else:
                            t = int(average[method][length][stat])
                        s = None
                    else:
                        #s = (int(average[method][length]['NUMINSTANCE']) - int(average[method][length]['NUMFINISHED']))
                        s = int(average[method][length]['NUMFINISHED'])
                        t = None
                        
                    if t != None:    
                        if minval == None or minval > t:
                            minval = t
                        if maxval == None or maxval < t:
                            maxval = t
                    
            T[method].append(t)
            S[method].append(s)
        
        for i in range(len(T[method])):
            if T[method][i] == None:
                print T[method]
                T[method] = T[method][:i]
                print T[method]
                print
                break
        
        for i in range(len(S[method])):
            if S[method][-i-1] == None:
                print S[method]
                S[method] = S[method][-i:]
                print S[method]
                print
                break        
                
        
        #T[method] = [float(average[method][length]['RUNTIME']) for length in X if average[method].has_key(length)]
        #if len(T[method]) < len(X):
 
    
    plt.tick_params(axis='both', which='both', bottom='off', top='off',
                    labelbottom='on', left='off', right='off', labelleft='on')
               
               
    gaps = [1, 10, 100, 250, 500, 1000, 2500, 10000, 25000, 100000]
               
    if type_exp == 'schedule':
        
        ogap = (maxval - minval)/10
        for g in gaps:
            if g>ogap:
                ogap = g
                break
                
        print int(minval), '-', (int(minval)%ogap), '=', (int(minval) - (int(minval)%ogap)), ogap
        
        Y = range(int(minval) - (int(minval)%ogap), int(maxval), ogap)     
        plt.xticks(X)
        plt.yticks(Y) 
        plt.ylabel('runtime (s)')
        plt.xlabel('problem size')  
    
        ax = plt.subplot() 
    
        ax.spines['top'].set_visible(False)
        ax.spines['bottom'].set_visible(False)
        ax.spines['right'].set_visible(False)
        ax.spines['left'].set_visible(False)

        for y in Y:
            plt.plot(range(5, maxlength+1), [y] * len(range(5, maxlength+1)), '--',
                     lw=0.5, color='black', alpha=0.3) 

        plt.text(7.27, 700, 'Sortedness', color='red', rotation=63)
        plt.text(7.37, 430, 'Gcc', color='green', rotation=43)
        plt.text(7.27, 350, 'propagator', color='blue', rotation=38)
        
        
        x1,x2,y1,y2 = plt.axis()
        plt.axis((x1,maxlength,minval,maxval))
        
    else:
        print maxlength
        
        X2 = range(X[0], maxlength, 2)
        Y = [10**x for x in range(0,10)]
        plt.xticks(X2)
        plt.yticks(Y) 
        plt.ylabel('runtime (s)')
        plt.xlabel('problem size')  
        
        plt.yscale('log')
    
        ax = plt.subplot() 
    
        ax.spines['top'].set_visible(False)
        ax.spines['bottom'].set_visible(False)
        ax.spines['right'].set_visible(False)
        ax.spines['left'].set_visible(False)

        for y in Y:
            plt.plot(X, [y] * len(X), '--',
                     lw=0.5, color='black', alpha=0.3)   

        # plt.text(12.3, 20000, 'Sortedness', color='red', rotation=51)
        # plt.text(13.75, 8000, 'Gcc', color='green', rotation=53)
        # plt.text(13.67, 6000, 'propagator', color='blue', rotation=52)
        
        plt.text(14.15, 6000, 'Sortedness', color='red', rotation=68)
        plt.text(10.2, 8000, 'Gcc', color='green', rotation=80)
        plt.text(16.85, 3000, 'propagator', color='blue', rotation=70)
        
        x1,x2,y1,y2 = plt.axis()

        plt.axis((x1,x2,max(minval, 0.02),32400))
        
        
        #ax2 = plt.twinx()
        #ax2.plot(range(1, 10001, 20), range(500))
        #ax2.set_ylabel('#solved')

    
    
    
    plt.plot(X[:len(T['no'])],T['no'],lw=2)
    plt.plot(X[:len(T['gcc'])],T['gcc'],lw=2)
    plt.plot(X[:len(T['sort'])],T['sort'],lw=2)
    
    plt.plot(X[-len(S['no']):],S['no'],lw=2,linestyle='--',color='blue')
    plt.plot(X[-len(S['gcc']):],S['gcc'],lw=2,linestyle='--',color='green')
    plt.plot(X[-len(S['sort']):],S['sort'],lw=2,linestyle='--',color='red')
    #
    plt.savefig('runtime.png', bbox_inches='tight')
        


if __name__ == '__main__':
    type_exp, sub_type, stat = get_cmdline()
    
    plot_data(read_data(type_exp, sub_type), type_exp, stat)
    
    #plot_data(read_data(type_exp), type_exp)






