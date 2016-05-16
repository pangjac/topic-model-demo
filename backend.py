from __future__ import print_function
import json
from pprint import pprint
import simplejson
import result_topic
from flask import Flask
from gmail_credentials import get_credentials
import time
import os
import json
from pprint import pprint
import sys
import ast

app = Flask(__name__, static_url_path='')


@app.route("/")
def index():
    return app.send_static_file('index.html')


@app.route("/run")
def run():
    tm = time.time()
    os.system("java -cp './class:./lib/*' cc.mallet.topics.GmailQuickstartV2 topic-input.mallet")

    tm = time.time() - tm
    print(tm)

    # result.txt is previously last iteration topic result. Its format is as 
    # 0	155	Mean Time:2016-02-16T15:38:42.741-05:00	Dispersion:1.5749929447272642E-6 webinar audio organizer email registration cancel calendar 
    result_txt = os.path.exists('/Users/pangjac/Downloads/cm/result.txt')

    file_exist = os.path.exists('./result.json')

    if file_exist:
        print("result.json exists now. ")
        filename = './result.json'

    for i in xrange(1,50):
        topic = str(i)
        #output_name = 'jsondata/' + topic + str('.json')
        output_name = 'static/mgdata/' + topic + str('.json')
        
        data = result_topic.read(filename)
        data_dict = ast.literal_eval(data)
        
        topic_json = result_topic.convert_topic(topic,data_dict)
        
        result_topic.put(topic_json,output_name)

    return "Finish!"


if __name__ == "__main__":
    app.run(port=8000)
