#!/usr/bin/python
# -*- coding: utf-8 -*-
import json
from pprint import pprint
import simplejson
import sys
import ast

def read(filename): # data file 'result.json'
    with open(filename) as data_file:
        #data = json.load(data_file)
        data = json.dumps(json.load(data_file))
    return data

def convert_topic(topic_num, origin_dict):

    dict_topics = {}
    for key,val in origin_dict.items(): # key - timestamp, k-topic, v- topicCount
        for k, v in val.items():
            if k in dict_topics.keys():
                dict_topics.get(k).update({key:v})
            else:
                dict_topics.update({k:{key:v}}) 


    if topic_num in dict_topics.keys():      
        topic_dict = dict_topics.get(topic_num)


        return_dict = []
        for item in topic_dict.items():
            item_dict = {}
            item_dict.update({"date":item[0]})
            item_dict.update({"value":item[1]})
            return_dict.append(item_dict)
        
        return json.dumps(return_dict)
    else:
        print 'Given topic_num' + topic_num+ ' does not exist in present topic dictionary'
   

def put(data, filename):
    try:
        #jsondata = simplejson.dumps(data, indent=4, skipkeys=True, sort_keys=True)
        jsondata = data 
        print 'start creating files'

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

    #python result_topic.py result.json 
    filename = '/Users/pangjac/Downloads/cm/result.json'


    for i in xrange(1,50):
        topic  = str(i)
        output_name = 'static/mgdata/' + topic + str('.json')
        #output_name = 'jsondata/' + topic + str('.json')
        #output_name = topic+str('.json')

        data = read(filename)
        data_dict = ast.literal_eval(data)

        topic_json = convert_topic(topic,data_dict)

        put(topic_json, output_name)


