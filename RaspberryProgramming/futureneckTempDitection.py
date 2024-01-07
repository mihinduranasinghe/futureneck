
from tellcore.constants import TELLSTICK_TEMPERATURE
from tellcore.telldus import TelldusCore
# Initialize Tellstick
core = TelldusCore()
# Get sensor data
sensors = core.sensors()
#print("Sensor ID:",sensors)
for sensor_id in sensors:
 print("Sensor ID:",sensor_id.id)
 print("Temperature:",sensor_id.value(TELLSTICK_TEMPERATURE).value)
