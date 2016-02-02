#! /usr/bin/env python

import sys
import argparse
import matplotlib.pyplot as plt
import subprocess


def get_results(exptype, subtype, seeds, lengths, methods):
    
    prefix = 'experiments/'+exptype+'_'
    
    average = {}.fromkeys(methods)
    
    mf = 0
    for method in methods:
        average[method] = {}
        for length in lengths:
            total = {}
            for seed in seeds:
                filename = prefix+method+'_'+str(length)+'_'+subtype+'_'+str(seed)+'.res'
                infile = None
                try: 
                    infile = open(filename)
                    for line in infile:
                        if line[0] == 'd':
                            sl = line.split()
                            val = None
                            if sl[1] == 'RUNTIME' or sl[1] == 'OPTIMAL':
                                val = float(sl[2])
                            else:
                                val = int(sl[2])
                            if not total.has_key(sl[1]):
                                total[sl[1]] = 0
                            total[sl[1]] += val
                except IOError:
                    mf += 1
                    #print filename, 'not found'
            for stat in total.keys():
                if not average[method].has_key(stat):
                    average[method][stat] = []
                average[method][stat].append(total[stat])
        
        print method
        print average[method]['RUNTIME']
        print average[method]['OBJECTIVE']
        print average[method]['NUMLIMIT']
        print average[method]['NUMFINISHED']
        print
        
    return average
    
    
def plot_sched(T, methods, X, stat):   
    minY = None
    maxY = None

    for method in methods:
         
        for i in range(len(T[method][stat])):
            if minY != None:
                if minY > T[method][stat][i]:
                    minY = T[method][stat][i]
            else:
                minY = T[method][stat][i]

            if maxY != None:
                if maxY < T[method][stat][i]:
                    maxY = T[method][stat][i]
            else:
                maxY = T[method][stat][i]
                
    
    plt.tick_params(axis='both', which='both', bottom='off', top='off',
                    labelbottom='on', left='off', right='off', labelleft='on')
    
    


    ax = plt.subplot() 

    ax.spines['top'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['left'].set_visible(False)


    #X2 = range(X[0], X[-1]+1, 2)
    
    gaps = [1, 10, 100, 250, 500, 1000, 2500, 10000, 25000, 100000]
    ogap = (maxY - minY)/10
    for g in gaps:
        if g>ogap:
            ogap = g
            break
            
    print int(minY), '-', (int(minY)%ogap), '=', (int(minY) - (int(minY)%ogap)), ogap
    
    Y = range(int(minY) - (int(minY)%ogap), int(maxY), ogap)
    
    #Y = [10**x for x in range(0,10)]
    plt.xticks(X)
    plt.yticks(Y) 
    plt.ylabel('runtime (s)')
    plt.xlabel('problem size')
    
    #plt.yscale('log')

    for y in Y:
        plt.plot(X, [y] * len(X), '--', lw=0.5, color='black', alpha=0.3)   
        

    # plt.text(12.3, 20000, 'Sortedness', color='red', rotation=51)
    # plt.text(13.75, 8000, 'Gcc', color='green', rotation=53)
    # plt.text(13.67, 6000, 'propagator', color='blue', rotation=52)
 
    # plt.text(14.15, 6000, 'Sortedness', color='red', rotation=68)
    # plt.text(10.2, 8000, 'Gcc', color='green', rotation=80)
    # plt.text(16.85, 3000, 'propagator', color='blue', rotation=70)
    #

    
    
    #ax2 = plt.twinx()
    #ax2.plot(range(1, 10001, 20), range(500))
    #ax2.set_ylabel('#solved')


    colors = ['blue', 'green', 'red']
    
    # for i in range(len(methods)):
    #     plt.plot(X[:len(T[method[i]][stat])],T['no'][stat],lw=2)
    #     #plt.plot(X[:len(T[method[i]]['NUMLIMIT'])],T['no']['NUMLIMIT'],lw=2,linestyle='--',color=colors[i])
    #   
    plt.plot(X[:len(T['no'][stat])],T['no'][stat],lw=2)
    plt.plot(X[:len(T['gcc'][stat])],T['gcc'][stat],lw=2)
    plt.plot(X[:len(T['sort'][stat])],T['sort'][stat],lw=2)
    #
    # plt.plot(X[:len(T['no'][stat])],T['no'][stat],lw=2)
    # plt.plot(X[:len(T['gcc'][stat])],T['gcc'][stat],lw=2)
    # plt.plot(X[:len(T['sort'][stat])],T['sort'][stat],lw=2)
    
    
    mult = 1000;
    if stat == 'OBJECTIVE':
        mult = 100
    
    # plt.plot(X[:len(T['no']['NUMLIMIT'])],[mult*x for x in T['no']['NUMLIMIT']],lw=2,linestyle='--',color='blue')
    # plt.plot(X[:len(T['gcc']['NUMLIMIT'])],[mult*x for x in T['gcc']['NUMLIMIT']],lw=2,linestyle='--',color='green')
    # plt.plot(X[:len(T['sort']['NUMLIMIT'])],[mult*x for x in T['sort']['NUMLIMIT']],lw=2,linestyle='--',color='red')
    # #
    
    x1,x2,y1,y2 = plt.axis()


    print x1,x2,y1,y2,maxY

    plt.axis((x1,x2,y1,maxY+ogap))
    
    
    return plt
    
            
def plot_uncor(T, methods, X, stat):

    minY = None
    maxY = None

    for method in methods:
         
        for i in range(len(T[method][stat])):
            if minY != None:
                if minY > T[method][stat][i]:
                    minY = T[method][stat][i]
            else:
                minY = T[method][stat][i]

            if maxY != None:
                if maxY < T[method][stat][i]:
                    maxY = T[method][stat][i]
            else:
                maxY = T[method][stat][i]
                
    
    plt.tick_params(axis='both', which='both', bottom='off', top='off',
                    labelbottom='on', left='off', right='off', labelleft='on')
    
    


    ax = plt.subplot() 

    ax.spines['top'].set_visible(False)
    ax.spines['bottom'].set_visible(False)
    ax.spines['right'].set_visible(False)
    ax.spines['left'].set_visible(False)


    X2 = range(X[0], X[-1]+1, 2)
    Y = [10**x for x in range(0,10)]
    plt.xticks(X2)
    plt.yticks(Y) 
    plt.ylabel('runtime (s)')
    plt.xlabel('problem size')
    
    plt.yscale('log')

    for y in Y:
        plt.plot(X, [y] * len(X), '--', lw=0.5, color='black', alpha=0.3)   

    # plt.text(12.3, 20000, 'Sortedness', color='red', rotation=51)
    # plt.text(13.75, 8000, 'Gcc', color='green', rotation=53)
    # plt.text(13.67, 6000, 'propagator', color='blue', rotation=52)
 
    # plt.text(14.15, 6000, 'Sortedness', color='red', rotation=68)
    # plt.text(10.2, 8000, 'Gcc', color='green', rotation=80)
    # plt.text(16.85, 3000, 'propagator', color='blue', rotation=70)
    #

    
    
    #ax2 = plt.twinx()
    #ax2.plot(range(1, 10001, 20), range(500))
    #ax2.set_ylabel('#solved')


    colors = ['blue', 'green', 'red']
    
    # for i in range(len(methods)):
    #     plt.plot(X[:len(T[method[i]][stat])],T['no'][stat],lw=2)
    #     #plt.plot(X[:len(T[method[i]]['NUMLIMIT'])],T['no']['NUMLIMIT'],lw=2,linestyle='--',color=colors[i])
    #   
    plt.plot(X[:len(T['no'][stat])],T['no'][stat],lw=2)
    plt.plot(X[:len(T['gcc'][stat])],T['gcc'][stat],lw=2)
    plt.plot(X[:len(T['sort'][stat])],T['sort'][stat],lw=2)
    #
    # plt.plot(X[:len(T['no'][stat])],T['no'][stat],lw=2)
    # plt.plot(X[:len(T['gcc'][stat])],T['gcc'][stat],lw=2)
    # plt.plot(X[:len(T['sort'][stat])],T['sort'][stat],lw=2)
    
    plt.plot(X[-len(T['no']['NUMLIMIT']):],T['no']['NUMLIMIT'],lw=2,linestyle='--',color='blue')
    plt.plot(X[-len(T['gcc']['NUMLIMIT']):],T['gcc']['NUMLIMIT'],lw=2,linestyle='--',color='green')
    plt.plot(X[-len(T['sort']['NUMLIMIT']):],T['sort']['NUMLIMIT'],lw=2,linestyle='--',color='red')
    # 
    
    x1,x2,y1,y2 = plt.axis()


    print x1,x2,y1,y2,maxY

    plt.axis((x1,x2,2,maxY*2))
    
    
    return plt
    
    #
    
    
    #plt.show()
    
    
            
    



def get_cmdline():
    
    parser = argparse.ArgumentParser(description='Parse experiments')

    parser.add_argument('--type',type=str,default="anticorrelation",help='type of relation',choices=["sat","rand","schedule"])
    parser.add_argument('--stat',type=str,default="RUNTIME",help='statistic',choices=['RUNTIME','NODES','BACKTRACKS','FAILS','RESTARTS','OPTIMAL','OBJECTIVE','NUMFINISHED','NUMLIMIT'])
    
    args = parser.parse_args()
    
    return args.type, args.stat


               
    # gaps = [1, 10, 100, 250, 500, 1000, 2500, 10000, 25000, 100000]
    #
    # if type_exp == 'schedule':
    #
    #     ogap = (maxval - minval)/10
    #     for g in gaps:
    #         if g>ogap:
    #             ogap = g
    #             break
    #
    #     print int(minval), '-', (int(minval)%ogap), '=', (int(minval) - (int(minval)%ogap)), ogap
    #
    #     Y = range(int(minval) - (int(minval)%ogap), int(maxval), ogap)
    #     plt.xticks(X)
    #     plt.yticks(Y)
    #     plt.ylabel('runtime (s)')
    #     plt.xlabel('problem size')
    #
    #     ax = plt.subplot()
    #
    #     ax.spines['top'].set_visible(False)
    #     ax.spines['bottom'].set_visible(False)
    #     ax.spines['right'].set_visible(False)
    #     ax.spines['left'].set_visible(False)
    #
    #     for y in Y:
    #         plt.plot(range(5, maxlength+1), [y] * len(range(5, maxlength+1)), '--',
    #                  lw=0.5, color='black', alpha=0.3)
    #
    #     plt.text(7.27, 700, 'Sortedness', color='red', rotation=63)
    #     plt.text(7.37, 430, 'Gcc', color='green', rotation=43)
    #     plt.text(7.27, 350, 'propagator', color='blue', rotation=38)
    #
    #
    #     x1,x2,y1,y2 = plt.axis()
    #     plt.axis((x1,maxlength,minval,maxval))
        


if __name__ == '__main__':
    
    
    type_exp, stat = get_cmdline()
    
    methods = ['no', 'gcc', 'sort']
    lengths = range(5,21)
    
    X = range(100, 1100, 100)
    subtype = 'batch'
    if type_exp != 'schedule':
        if type_exp == 'sat':
            subtype = 'sat'
        elif type_exp == 'rand':
            subtype = 'rand'
        type_exp = 'uncorrelation'
        X = range(100, 5100, 100)
    
    
    average = get_results(type_exp, subtype, X, lengths, methods)

    theplot = None
    
    if type_exp != 'schedule':
        theplot = plot_uncor(average, methods, lengths, stat)
    else:
        theplot = plot_sched(average, methods, lengths, stat)
    
    if subtype == 'sat' and stat == 'RUNTIME':
        theplot.text(17, 1000000, 'Sortedness', color='red', rotation=22)
        theplot.text(18, 330000, 'Gcc', color='green', rotation=23)
        theplot.text(17.4, 110000, 'propagator', color='blue', rotation=24)
    elif subtype == 'rand' and stat == 'RUNTIME':
        theplot.text(17.4, 250000, 'Sortedness', color='red', rotation=22)
        theplot.text(18.8, 75000, 'Gcc', color='green', rotation=18)
        theplot.text(17.6, 15000, 'propagator', color='blue', rotation=20)    
    
    theplot.savefig(stat+'_'+type_exp+'_'+subtype+'.png', bbox_inches='tight')
 




