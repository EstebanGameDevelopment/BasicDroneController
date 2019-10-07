from websocket_server import WebsocketServer
import re
from dronekit import connect, VehicleMode, LocationGlobalRelative
import time
vehicle = connect('tcp:127.0.0.1:5760', wait_ready=True)
vehicle.mode = VehicleMode("GUIDED")
print('Flight Controller Connected')

def new_client(client, server):
		print("Client connected")
		server.send_message_to_all("Client connected")
def message_received(client, server, message):
		if len(message) > 200:
				message = message[:200] + ".."
		print ('(client message: ' + message + ')')
		if "armDrone" in message:
				vehicle.armed = True
				while not vehicle.armed:
					time.sleep(1)
				client.send(str("armed_success"))
		if "takeOffDrone" in message:
			pos = message.find("takeOffDrone")
			aTargetAltitude = int(message[pos+13:])
			print "Target Altitude = " + aTargetAltitude
			vehicle.simple_takeoff(aTargetAltitude)
			# Check that vehicle has reached takeoff altitude
			while True:
				print " Altitude: ", vehicle.location.global_relative_frame.alt 
				#Break and return from function just below target altitude.        
				if vehicle.location.global_relative_frame.alt>=aTargetAltitude*0.95: 
					print "Reached target altitude"
					break
				time.sleep(1)
			client.send(str("altitude_success"))
		if "landDrone" in message:
			vehicle.mode = VehicleMode("LAND")
			while not vehicle.landed:
					time.sleep(1)
			client.send(str("landed_success"))
		if "disconnectDrone" in message:
			print ('Disarming')
			vehicle.armed = False
			while vehicle.armed:
				time.sleep(1)
			client.send(str("disconnected_success"))

server = WebsocketServer(8080, '0.0.0.0')
server.set_fn_new_client(new_client)
server.set_fn_message_received(message_received)
server.run_forever()