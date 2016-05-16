from flask import Flask
from gmail_credentials import get_credentials
import time
import os
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

    file_exist = os.path.exists('/Users/pangjac/Downloads/cm/result.txt')

    if file_exist:
    	print "file exists"
    	with open('/Users/pangjac/Downloads/cm/result.txt') as f:
    		data = f.readlines()

    return "Finish!"

if __name__ == "__main__":
    app.run(port=8000)
