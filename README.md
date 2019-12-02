RUN SIMULATOR
-------------
cd ardupilot/ArduCopter/
sim_vehicle.py -L 3DRBerkeley --map --console --out 192.168.0.23:14550

DRONE CONTROLLER
----------------

The video tutorial is in the next link:
	
https://youtu.be/1ySxn_7KEVE	

Thanks to this asset you will be able to use the magic of Unity to control any Ardupilot based drone.

In this tutorial we will show the steps to do in order to control any drone.

BUILDING THE APK
----------------

First, import the package DroneController into a new project.

Then, open the building settings and add the scene to the scenes to build.

Next, switch to Android platform.

Finally, you can proceed to make a build and you will get your APK file.

EMULATOR
--------

In order to test the behavior we are going to use an emulator that will allow you to perform action on a virtual drone in the same way that you will do it with a physical one.

Go to ardupilot.org and then follow the instructions that are described there to install the simulator. 

http://ardupilot.org/dev/docs/sitl-native-on-windows.html

In my case I have choosen a Windows machine since I had problems when I tried to run Ubuntu with a Virtual Machine. The Windows tutorial makes it easy for you to set up the emulator in no time.

Once you set up your environment, you can run the simulator and you should define the IP address of the android device you are going to use.

Then, three windows will be opened: A command console, a informative console, and a map showing
the position of the drone.

Before running the app remember to disable temporarily the firewall to allow the app to access the simulator.

Now you can run the app in your Android device.

First, we will press the button to connect.

Second, we will press the button to arm the drone.

Third, we are going to take off, by default the height is about 10 meters. You can check the information about the height and other parameters in the information console.

Once we reach the desired height we can now move the drone around just by pressing
in the direction we want it to go.

The drone will receive an instruction that will ask to change its velocity vector.

CONCLUSIONS
-----------

The cool thing about this project is that libraries which communicate with the drone are developed with Android Dronekit. And the community behind them is supportive and quite active.

Another great thing to stress out is that it's not limited to flying drones, but you can use it for rovers, and even mini-submarines.

In the next tutorial we will put several pieces together: 

	* VR technology, 
	* 360 life streaming
	* Network communications 
		
to be able to control the drone remotely using a VR headset and only 1 button. 

THIRD PARTY LICENCES:
-----------------------
This asset uses [Android-Dronekit] and [SimpleJSON] under [The MIT License (MIT)]; see Third-Party Notices.txt file in package for details.

