from websocket_server import WebsocketServer
import re
from dronekit import connect, VehicleMode, LocationGlobalRelative
import time
from pymavlink import mavutil
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
				print ('Arming vehicle')
				vehicle.armed = True
				while not vehicle.armed:
					time.sleep(1)
				server.send_message_to_all("armed_success")
				print ('Arming Success')
		if "takeOffDrone" in message:
			print ('Take Off vehicle')
			pos = message.find("takeOffDrone")
			aTargetAltitude = int(message[pos+13:])
			print "Target Altitude = ", aTargetAltitude
			vehicle.simple_takeoff(aTargetAltitude)
			while True:
				print " Altitude: ", vehicle.location.global_relative_frame.alt
				#Break and return from function just below target altitude.        
				if vehicle.location.global_relative_frame.alt>=aTargetAltitude*0.95: 
					print "Reached target altitude"
					break
				time.sleep(1)
			server.send_message_to_all("altitude_success")
			print ('Take Off Success')
		if "Velocity" in message:
			posvx = message.find("_vx_")
			posvy = message.find("_vy_")
			posvz = message.find("_vz_")
			totaltime = message.find("_time_")
			totalspeed = message.find("_speed_")
			vend = message.find("_end")
			vx = float(message[posvx+4:posvy])
			vy = float(message[posvy+4:posvz])
			vz = float(message[posvz+4:totaltime])
			ttime = float(message[totaltime+6:totalspeed])
			tspeed = float(message[totalspeed+7:vend])
			print "Velocity VX ", vx
			print "Velocity VY ", vy
			print "Velocity VZ ", vz
			print "Total Time ", ttime
			print "Total Speed ", tspeed
			msg = vehicle.message_factory.set_position_target_local_ned_encode(0,0,0,mavutil.mavlink.MAV_FRAME_BODY_NED, 0b0000111111000111, 0, 0, 0, vx, vz, vy, 0, 0, 0, 0, 0)
			vehicle.send_mavlink(msg)
			vehicle.flush()
			server.send_message_to_all("velocity_processed")
			print ('Velocity Processed')
		if "RTLDrone" in message:
			print ('RTL Request')
			vehicle.mode = VehicleMode("RTL")
			while not vehicle.landed:
					time.sleep(1)
			server.send_message_to_all("rtl_success")
			print ('RTL Success')
		if "landDrone" in message:
			print ('Now Landing Drone')
			vehicle.mode = VehicleMode("LAND")
			while not vehicle.landed:
					time.sleep(1)
			server.send_message_to_all("landed_success")
			print ('Landing Success')
		if "disconnectDrone" in message:
			print ('Disarming')
			vehicle.armed = False
			while vehicle.armed:
				time.sleep(1)
			server.send_message_to_all("disconnected_success")
			print ('Disarming Success')
		if "operationDrone" in message:
			pos = message.find("operationDrone")
			newVehicleMode = message[pos+15:]
			print ('New Operation Mode=' + newVehicleMode)
			vehicle.mode = VehicleMode(newVehicleMode)
			server.send_message_to_all("opdrone_success")
			print ('Unconfirmed changed vehicle mode')

server = WebsocketServer(8080, '0.0.0.0')
server.set_fn_new_client(new_client)
server.set_fn_message_received(message_received)
server.run_forever()