import math
import nltk
from itertools import islice,chain
from operator import itemgetter,attrgetter
from collections import defaultdict
from nltk.corpus.reader import CorpusReader
from nltk.util import binary_search_file as _binary_search_file
from nltk.probability import FreqDist
from nltk.corpus import wordnet as wn
from nltk.corpus import wordnet_ic
brown_ic = wordnet_ic.ic('ic-brown.dat')
semcor_ic = wordnet_ic.ic('ic-semcor.dat')
from nltk.corpus import genesis
genesis_ic = wn.ic(genesis, False, 0.0)

def shortest_path_distance(self, other, simulate_root=False):
        """
        Returns the distance of the shortest path linking the two synsets (if
        one exists). For each synset, all the ancestor nodes and their
        distances are recorded and compared. The ancestor node common to both
        synsets that can be reached with the minimum number of traversals is
        used. If no ancestor nodes are common, None is returned. If a node is
        compared with itself 0 is returned.

        :type other: Synset
        :param other: The Synset to which the shortest path will be found.
        :return: The number of edges in the shortest path connecting the two
            nodes, or None if no path exists.
        """
	
        if self == other:
            return 0

        path_distance = None
	
        dist_list1 = self.hypernym_distances(simulate_root=simulate_root)
        dist_dict1 = {}

        dist_list2 = other.hypernym_distances(simulate_root=simulate_root)
        dist_dict2 = {}

        # Transform each distance list into a dictionary. In cases where
        # there are duplicate nodes in the list (due to there being multiple
        # paths to the root) the duplicate with the shortest distance from
        # the original node is entered.

        for (l, d) in [(dist_list1, dist_dict1), (dist_list2, dist_dict2)]:
            for (key, value) in l:
                if key in d:
                    if value < d[key]:
                        d[key] = value
                else:
                    d[key] = value

        # For each ancestor synset common to both subject synsets, find the
        # connecting path length. Return the shortest of these.

        for synset1 in dist_dict1.keys():
            for synset2 in dist_dict2.keys():
                if synset1 == synset2:
                    new_distance = dist_dict1[synset1] + dist_dict2[synset2]
                    if path_distance is None or path_distance < 0 or new_distance < path_distance:
                        path_distance = new_distance

        return path_distance


def returnSynset(l1):
   	returnL=[]
	for a in l1:
	   	b=wn.synset(a+'.v.01')
     		returnL.append(b)
	return returnL

def returnSynsets(l1):
   	returnL=[]
	for a in l1:
	   	b=wn.synsets(a, pos='v')
		#lb = [];
		#lb.append(b[0]);
		#lb.append(b[1]);
     		returnL.append(b);
	return returnL
	

def returnSimList(lverb,l):
	ret = [];
	for el in l:
		#res
		#jcn
		#wup, lch
		#sim=verb.path_similarity(el,brown_ic)
		#sim=verb.path_similarity(el,semcor_ic)
		#sim = shortest_path_distance(verb, el, simulate_root=True);
		#sim = wn.lin_similarity(verb, el, brown_ic, verbose = False);
		#sim=verb.wup_similarity(el);
		#sim = similarity(lverb,el)
		sim = similarity(lverb,el);
		#print sim;
		ret.append(sim);
		#ret.append(sim[0]);
		#ret.append(sim[1]);
	return ret;

def similarity(lverb,el):
	msim = -1;
	i=1;
	for v1 in lverb:
		j = 1;
		for v2 in el:
			#sim = wn.lin_similarity(v1, v2, brown_ic, verbose = False)/(math.log(i+j**2));
			sim = v1.wup_similarity(v2)/(math.log(i+j**2));
			if sim>msim:
				msim = sim;
			j = j+1;
		i = i+1;
	
	return msim;

def similarity2(lverb,el):
	ret = [];
	for v1 in lverb:
		maxsim = -1;
		for v2 in el:
			sim = wn.lin_similarity(v1, v2, brown_ic, verbose = False);
			if (sim>maxsim):
				maxsim = sim;
			
		ret.append(maxsim);
	if (len(ret) != 2):
		ret.append(0);

	return ret;



	
def returnPlusMinusLists(verb, lplus, lminus, f):
	
	lverb = wn.synsets(verb, pos = 'v');
	str('here');
	#print lverb;
	simPlus=returnSimList(lverb,lplus)
	simMinus=returnSimList(lverb, lminus)
		
	f.write(verb);
	f.write("\n");
	print>>f, simPlus;
	f.write("\n");	
	print>>f, simMinus;
	f.write("\n");	
	f.write("\n");	
		
	return simPlus,simMinus	

def returnLabel(pscore,mscore):
	if pscore>mscore: 
		if pscore-mscore>0.03:return '+'
		else: return '0'
	
	if pscore<mscore: 
		if mscore-pscore>0.03:return '-'
		else: return '0'
	if pscore==mscore:
		return '0'

def main():
	
	str('salam')
	#lp=['have', 'be', 'grow', 'add', 'increase','generate','sum']
	lp=['grow', 'find', 'get']
	#lp=['grow', 'find', 'get'];
	lplus=returnSynsets(lp)
	#lplus.append(wn.synset('total.n.01'))
	#lose
	#lm=['spend', 'give', 'eat', 'sell', 'need', 'miss','leave', 'lose']
	lm=['spend', 'give', 'eat', 'sell', 'miss']
	#lm=['spend', 'give', 'eat', 'sell', 'miss', 'lose']
	#lm=['spend', 'give', 'eat', 'sell']
	lminus=returnSynsets(lm)
	#lminus.append(wn.synset('left.n.01'))	
	
	#verbList= ['have', 'do', 'be', 'buy', 'grow', 'pick', 'find', 'spend', 'give', 'go', 'get', 'play', 'pay', 'sell', 'finish', 'leave', 'plant', 'work', 'serve', 'place', 'wash', 'eat', 'need', 'miss', 'catch', 'bake', 'make', 'earn', 'cut', 'receive', 'love', 'save', 'attend', 'see', 'divide', 'clean', 'start', 'hold', 'plan', 'join', 'come', 'rent', 'rise', 'add', 'purchase', 'decide', 'borrow', 'take', 'transfer', 'lose', 'win', 'put', 'call', 'stack', 'split', 'damage', 'stock', 'break', 'agree', 'assume', 'stand', 'begin', 'sport', 'fill', 'check', 'end', 'defeat', 'mow', 'discover', 'crack', 'gather', 'search', 'travel', 'visit', 'shine']
	f=open("/Users/hosseini/Desktop/D/research_uw/data/verbs3.txt",'r')
	lines = f.readlines();
	verbList= [];
	for l in lines:
		ss = l.split(" ");
		ss = ss[0].split("\t")
		verbList.append(ss[0]);	
	for k in range(0,10):
		
		
		
		str('salam')
		base_str = "/Users/hosseini/Desktop/D/research_uw/data/sverbs4/";
	
		in_str = base_str + "sverbs" + `k` + ".txt";
		out_str = base_str + "dis" + `k` + ".txt";
		f=open(in_str,'r');
		fout=open(out_str,'w');
		selected = [];
		lines = f.readlines();
		i = 0;
		for l in lines:
			#if (i==21):
				#break;
		#	print l;
			ss = l.split(" ");
			ss = ss[0].split("\n")		
			selected.append(ss[0]);
			#print "selected:"
			#print selected
			i = i+1;
		lselected = returnSynsets(selected);
			
			#f=open("/Users/Javad/Desktop/D/verbsPolarity2.txt",'w')
		for verb in verbList:
			plusscore,minusscore=returnPlusMinusLists(verb, lselected, [], fout)
				#plusscore,minusscore=returnPlusMinusLists(verb, lplus, lminus)
				#f.write(verb+'   '+returnLabel(plusscore,minusscore)+'   '+str(plusscore)+'   '+str(minusscore)+'\n')
			#f.close()		
		
	
	return 0;

def main2():
	f=open("/Users/hosseini/Desktop/D/research_uw/data/verbs.txt",'r')
	lines = f.readlines();
	verbList= [];
	for l in lines:
		ss = l.split(" ");
		ss = ss[0].split("\t")
		verbList.append(ss[0]);	
	for v in verbList:
		l1 = wn.synsets(v, pos='v')
		for v2 in verbList:
			l2 = wn.synsets(v2, pos='v')
			print v + " " +v2 + " ";
			print similarity(l1, l2);
			print "\n";

