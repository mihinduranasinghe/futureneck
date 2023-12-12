# -*- coding: utf-8 -*- 
#!/usr/bin/env python 
# This is where to insert your generated API keys (http://api.telldus.com/keys) 
pubkey = "FEHUVEW84RAFR5SP22RABURUPHAFRUNU" # Public Key 
privkey = "ZUXEVEGA9USTAZEWRETHAQUBUR69U6EF" # Private Key 
token = "72ce9da15f550ef07795e851c9a3f313065745f50" # Token 
secret = "9c6285b3e1dbfdd54d3b0d415a9fd3bc" # Token Secret  

import requests, json, hashlib, uuid, time 
localtime = time.localtime(time.time()) 
timestamp = str(time.mktime(localtime)) 
nonce = uuid.uuid4().hex 
oauthSignature = (privkey + "%26" + secret) 
# GET-request 
response = requests.get(
    url="https://api.telldus.com/sensors/list",
    params={
        "includeValues": "1",  
        
    },  
    headers={
        "Authorization": 'OAuth oauth_consumer_key="{pubkey}", oauth_nonce="{nonce}", oauth_signature="{oauthSignature}", oauth_signature_method="PLAINTEXT", oauth_timestamp="{timestamp}", oauth_token="{token}", oauth_version="1.0"'.format(pubkey=pubkey, nonce=nonce, oauthSignature=oauthSignature, timestamp=timestamp, token=token),   
        
    },  
    ) 
# Output/response from GET-request  
responseData = response.json() 
#print(responseData) 
print(json.dumps(responseData, indent=4, sort_keys=True)) 