import requests
import json

url = "https://pa-api.telldus.com/json/sensors/list"

try:
    response = requests.get(url)

    # Check if the request was successful (status code 200)
    if response.status_code == 200:
        # Try to parse the JSON data
        try:
            responseData = response.json()
            # Now you can work with the parsed JSON data
            print(responseData)
        except json.JSONDecodeError as e:
            print(f"Error decoding JSON: {e}")
    else:
        # Print an error message if the request was not successful
        print(f"Request failed with status code {response.status_code}")
except requests.RequestException as e:
    # Handle other request exceptions
    print(f"Request exception: {e}")
