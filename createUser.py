import os
import subprocess
import pipes
import sys

user = "loader@quip"
if(len(sys.argv) > 1):
	user = sys.argv[1]



os.system("java -jar trusted-app-client-0.0.1-jar-with-dependencies.jar -action a -username "+user+"  -id camicSignup -secret 9002eaf56-90a5-4257-8665-6341a5f77107 -comments loader -expires 01/01/2050  -url http://quip-data:9099/trustedApplication > userinfo")
output = open('userinfo', 'r').read()

#output='Digest of (camicSignup,9002eaf56-90a5-4257-8665-6341a5f77107,225dcb41-0de1-46ca-8f8f-048e99179d18,admin@localhost,14938360)=BQLD3fLlecAd2iv9zb9y866+ZnQ= Server Returned : {"value":"ac3b5744-5753-4477-b506-0597eeb2ab6d","expires":"Wed Apr 18 07:54:18 UTC 2018","applicationName":"caMic Signup App","applicationID":"camicSignup"}'

s = output.split("value")
key = s[1].split("expires")[0]
key =(key[3:len(key)-3])
#print(key)

#os.environ['api_key'] = key
#print os.environ['api_key']

#Hacky way of setting a persistent environment variable using python
#print("export api_key=%s" % (pipes.quote(str(key))))

print(key)
