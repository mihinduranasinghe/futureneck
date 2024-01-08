from tellcore.constants import TELLSTICK_TEMPERATURE, TELLSTICK_HUMIDITY
from tellcore.telldus import TelldusCore

# Initialize Tellstick
core = TelldusCore()

# Get sensor data
sensors = core.sensors()

# Dictionary to hold the latest readings
latest_readings = {}

for sensor in sensors:
    sensor_id = sensor.id

    # Retrieve temperature and humidity values
    temperature = sensor.value(TELLSTICK_TEMPERATURE)
    humidity = sensor.value(TELLSTICK_HUMIDITY)

    # Update the dictionary with the latest readings
    if temperature and humidity:
        latest_readings[sensor_id] = {"Temperature": temperature.value, "Humidity": humidity.value}

# Print the latest readings for each sensor
for sensor_id, readings in latest_readings.items():
    print(f"FutureNeck detects Environment Sensing around you as the, Temperature: {readings['Temperature']}, Humidity: {readings['Humidity']}, Values provided by Sensor ID: {sensor_id}")