#!/usr/bin/python
# -*- coding: utf-8 -*-
from __future__ import print_function
import json
from pprint import pprint
import simplejson
import sys

def read(filename): # data file 'result.json'
    with open(filename) as data_file:
        data = json.load(data_file)
    return data

def convert_topic(topic_num, origin_dict):
    dict_topics = {}
    for key,val in origin_dict.items(): # key - timestamp, k-topic, v- topicCount
        for k, v in val.items():
            if k in dict_topics.keys():
                dict_topics.get(k).update({key:v})
            else:
                dict_topics.update({k:{key:v}}) 
    topic_dict = dict_topics.get(topic_num)
    
    return_dict = []
    for item in topic_dict.items():
        item_dict = {}
        item_dict.update({"date":item[0]})
        item_dict.update({"value":item[1]})
        return_dict.append(item_dict)
    
    return json.dumps(return_dict)
   

def put(data, filename):
    try:
        #jsondata = simplejson.dumps(data, indent=4, skipkeys=True, sort_keys=True)
        jsondata = data 
        fd = open(filename, 'w')
        fd.write(jsondata)
        fd.close()
    except:
        print ('Error Writing')
        pass

if __name__ == '__main__':

    ## python result_topic.py result.json '4' '4.json'
    # filename = sys.argv[1]
    # topic = sys.argv[2]
    # output_name = sys.argv[3]
    #
    # # read the original dat
    # data = read(filename)
    # topic_json = convert_topic(topic,data)
    # put(topic_json, output_name)

    filename = '/Users/pangjac/Downloads/cm/result.json'


    for i in xrange(1,50):
        topic  = str(i)
        output_name = topic+str('.json')

        data = read(filename)
        topic_json = convert_topic(topic,data)
        put(topic_json, output_name)

