import os
import subprocess
import pipes
import sys
import json

user = "loader@quip"
if(len(sys.argv) > 1):
        user = sys.argv[1]

bindaashost = "quip-data:9099"
trusted_secret = "9002eaf56-90a5-4257-8665-6341a5f77107"
        
list_user_cmd = "java -jar trusted-app-client-0.0.1-jar-with-dependencies2.jar -action l -id camicSignup -secret " + trusted_secret + " -url http://" + bindaashost + "/trustedApplication"
create_user_cmd = "java -jar trusted-app-client-0.0.1-jar-with-dependencies.jar -action a -username "+user+"  -id camicSignup -secret " + trusted_secret + " -comments loader -expires 01/01/2050  -url http://" + bindaashost + "/trustedApplication"

user_list = os.popen(list_user_cmd).read()
# NOTE we assume that the format is unchanged, if this string from the jar changes, this method does too
user_list = json.loads(user_list.split("Server Returned :\n")[1])
keys = [u['value'] for u in user_list if u['username'] == user]
if len(keys) > 0:
    # the user exists, return the api key
    print(keys[0])
    exit(1)
else:
    # the user doesn't exist, so make it
    user_info = os.popen(create_user_cmd).read()
    # NOTE we assume that the format is unchanged, if this string from the jar changes, this method does too
    user_info = json.loads(user_info.split("Server Returned :\n")[1])
    print(user_info['value'])
    exit(1)
