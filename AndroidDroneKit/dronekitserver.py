from websocket_server import WebsocketServer
import re
from dronekit import connect, VehicleMode, LocationGlobalRelative
import time
import math
from pymavlink import mavutil
vehicle = connect('tcp:127.0.0.1:5760', wait_ready=False)
vehicle.mode = VehicleMode("OFFBOARD")
vehicle.flush()
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
				vehicle.flush()
				while not vehicle.armed:
					time.sleep(1)
				server.send_message_to_all("armed_success")
				print ('Arming Success')
		if "takeOffDrone" in message:
			print ('Take Off vehicle')
			pos = message.find("takeOffDrone")
			aTargetAltitude = float(message[pos+13:])
			# aTargetAltitude = int(vehicle.location.global_relative_frame.alt) + aTargetAltitude
			print "Target Altitude = ", aTargetAltitude
			# vehicle.simple_takeoff(aTargetAltitude)
			# vehicle.flush()
			# counter = 0
			# while counter < 5:
			# 	counter = counter + 1
			# 	print " Altitude: ", vehicle.location.global_relative_frame.alt
			# 	#Break and return from function just below target altitude.        
			# 	if vehicle.location.global_relative_frame.alt>=aTargetAltitude: 
			# 		print "Reached target altitude"
			# 		break
			# 	time.sleep(1)
			# server.send_message_to_all("altitude_success")
			SMOOTH_TAKEOFF_THRUST = 0.6
			thrust = SMOOTH_TAKEOFF_THRUST
			counter = 0
			while counter < 25:
				counter = counter + 1
				current_altitude = vehicle.location.global_relative_frame.alt
				print "Altitude:", current_altitude
				if current_altitude >= aTargetAltitude*0.95: # Trigger just below target alt.
					print("Reached target altitude")
					break
				elif current_altitude >= aTargetAltitude*0.6:
					thrust = SMOOTH_TAKEOFF_THRUST
				set_attitude(thrust = thrust)
				time.sleep(0.2)
			print ('Take Off Success')
			server.send_message_to_all("altitude_success")
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
			vehicle.flush()
			counter = 20
			while not vehicle.commands.next > 0 and counter > 0:
				counter = counter - 1
				print "Counter: ", counter
				time.sleep(1)
			server.send_message_to_all("rtl_success")
			print ('RTL Success')
		if "landDrone" in message:
			print ('Now Landing Drone')
			vehicle.mode = VehicleMode("LAND")
			vehicle.flush()
			counter = 10
			while not vehicle.commands.next > 0 and counter > 0:
				counter = counter - 1
				print "Counter: ", counter
				time.sleep(1)
			server.send_message_to_all("landed_success")
			print ('Landing Success')
		if "disconnectDrone" in message:
			print ('Disarming')
			vehicle.armed = False
			vehicle.flush()
			while vehicle.armed:
				time.sleep(1)
			server.send_message_to_all("disconnected_success")
			print ('Disarming Success')
		if "operationDrone" in message:
			pos = message.find("operationDrone")
			newVehicleMode = int(message[pos+15:])
			print 'New Operation Mode=', newVehicleMode
			vehicle.mode = VehicleMode(newVehicleMode)
			vehicle.flush()
			server.send_message_to_all("opdrone_success")
			print ('Unconfirmed changed vehicle mode')

def set_attitude(roll_angle = 0.0, pitch_angle = 0.0,
                 yaw_angle = None, yaw_rate = 0.0, use_yaw_rate = False,
                 thrust = 0.5, duration = 0):
    """
    Note that from AC3.3 the message should be re-sent more often than every
    second, as an ATTITUDE_TARGET order has a timeout of 1s.
    In AC3.2.1 and earlier the specified attitude persists until it is canceled.
    The code below should work on either version.
    Sending the message multiple times is the recommended way.
    """
    send_attitude_target(roll_angle, pitch_angle,
                         yaw_angle, yaw_rate, False,
                         thrust)
    start = time.time()
    while time.time() - start < duration:
        send_attitude_target(roll_angle, pitch_angle,
                             yaw_angle, yaw_rate, False,
                             thrust)
        time.sleep(0.1)
    # Reset attitude, or it will persist for 1s more due to the timeout
    send_attitude_target(0, 0,
                         0, 0, True,
                         thrust)    
						 
def send_attitude_target(roll_angle = 0.0, pitch_angle = 0.0,
                         yaw_angle = None, yaw_rate = 0.0, use_yaw_rate = False,
                         thrust = 0.5):
    """
    use_yaw_rate: the yaw can be controlled using yaw_angle OR yaw_rate.
                  When one is used, the other is ignored by Ardupilot.
    thrust: 0 <= thrust <= 1, as a fraction of maximum vertical thrust.
            Note that as of Copter 3.5, thrust = 0.5 triggers a special case in
            the code for maintaining current altitude.
    """
    if yaw_angle is None:
        # this value may be unused by the vehicle, depending on use_yaw_rate
        yaw_angle = vehicle.attitude.yaw
    # Thrust >  0.5: Ascend
    # Thrust == 0.5: Hold the altitude
    # Thrust <  0.5: Descend
    msg = vehicle.message_factory.set_attitude_target_encode(
        0, # time_boot_ms
        1, # Target system
        1, # Target component
        0b00000000 if use_yaw_rate else 0b00000100,
        to_quaternion(roll_angle, pitch_angle, yaw_angle), # Quaternion
        0, # Body roll rate in radian
        0, # Body pitch rate in radian
        math.radians(yaw_rate), # Body yaw rate in radian/second
        thrust  # Thrust
    )
    vehicle.send_mavlink(msg)
    vehicle.flush()
						 
def to_quaternion(roll = 0.0, pitch = 0.0, yaw = 0.0):
    """
    Convert degrees to quaternions
    """
    t0 = math.cos(math.radians(yaw * 0.5))
    t1 = math.sin(math.radians(yaw * 0.5))
    t2 = math.cos(math.radians(roll * 0.5))
    t3 = math.sin(math.radians(roll * 0.5))
    t4 = math.cos(math.radians(pitch * 0.5))
    t5 = math.sin(math.radians(pitch * 0.5))

    w = t0 * t2 * t4 + t1 * t3 * t5
    x = t0 * t3 * t4 - t1 * t2 * t5
    y = t0 * t2 * t5 + t1 * t3 * t4
    z = t1 * t2 * t4 - t0 * t3 * t5

    return [w, x, y, z]
	
server = WebsocketServer(8080, '0.0.0.0')
server.set_fn_new_client(new_client)
server.set_fn_message_received(message_received)
server.run_forever()