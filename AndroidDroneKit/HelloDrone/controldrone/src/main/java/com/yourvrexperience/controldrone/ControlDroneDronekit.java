
package com.yourvrexperience.controldrone;

import com.MAVLink.common.msg_local_position_ned;
import com.MAVLink.common.msg_set_position_target_local_ned;
import com.MAVLink.enums.MAV_FRAME;
import com.googlecode.mp4parser.boxes.mp4.objectdescriptors.AudioSpecificConfig;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.MavlinkObserver;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.unity3d.player.UnityPlayer;
import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompatSideChannelService;

import org.droidplanner.services.android.impl.core.drone.autopilot.apm.ArduCopter;



public class ControlDroneDronekit  extends Activity implements DroneListener, TowerListener {

    public static final int DRONE_EVENT_STATE_CONNECTED = 0;
    public static final int DRONE_EVENT_STATE_DISCONNECTED = 1;
    public static final int DRONE_EVENT_STATE_VEHICLE_MODE = 2;
    public static final int DRONE_EVENT_TYPE_UPDATED = 3;
    public static final int DRONE_EVENT_SPEED_UPDATED = 4;
    public static final int DRONE_EVENT_HOME_UPDATED = 5;
    public static final int DRONE_EVENT_STATE_UPDATED = 6;
    public static final int DRONE_EVENT_STATE_ARMING = 7;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTED    = 1;
    public static final int STATE_ARMED        = 2;
    public static final int STATE_TAKEOFF      = 3;
    public static final int STATE_IDLE         = 4;
    public static final int STATE_FLYING       = 5;
    public static final int STATE_LANDING      = 6;
    public static final int STATE_CRASH        = 7;

    public static final int DRONE_STATE_ERROR = 0;
    public static final int DRONE_STATE_TIMEOUT = -1;

    public static final int COMMAND_STATE_SUCCESS = 1;
    public static final int COMMAND_STATE_ERROR = 0;
    public static final int COMMAND_STATE_TIMEOUT = -1;

    private ControlTower controlTower;

    private int portNumber = 14550;
    private float heightDroneTakeOff = 1.5f;
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();

    private int takeOffState = -2;
    private int gotoState = -2;
    private int pauseState = -2;
    private int landingState = -2;
    private int guidedState = -2;

    private int eventDrone = -2;

    public void initControlDrone(int _portNumber, float _heightDroneTakeOff) {

        portNumber = _portNumber;
        heightDroneTakeOff = _heightDroneTakeOff;

        // Initialize the service manager
        final Context context = UnityPlayer.currentActivity.getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.controlTower.connect(this);
    }

    public void disconnectControlDrone()
    {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // 3DR Services Listener
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {

    }

    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                eventDrone = DRONE_EVENT_STATE_CONNECTED;
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                eventDrone = DRONE_EVENT_STATE_DISCONNECTED;
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                eventDrone = DRONE_EVENT_STATE_VEHICLE_MODE;
                break;

            case AttributeEvent.TYPE_UPDATED:
                eventDrone = DRONE_EVENT_TYPE_UPDATED;
                break;

            case AttributeEvent.SPEED_UPDATED:
                eventDrone = DRONE_EVENT_SPEED_UPDATED;
                break;

            case AttributeEvent.HOME_UPDATED:
                eventDrone = DRONE_EVENT_HOME_UPDATED;
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                eventDrone = DRONE_EVENT_STATE_UPDATED;
                eventDrone = DRONE_EVENT_STATE_ARMING;
                break;

            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    //---------------------------------------------------------
    //---------------------------------------------------------
    // INFORMATION
    //---------------------------------------------------------
    //---------------------------------------------------------

    public int getStateDrone() {
        if (!getConnectedDrone())
        {
            return STATE_DISCONNECTED;
        }
        else
        {
            if (getFlyingDrone())
            {
                if (getLandingDrone())
                {
                    return STATE_LANDING;
                }
                else
                {
                    return STATE_FLYING;
                }
            }
            else
            {
                if (!getArmedDrone())
                {
                    return STATE_CONNECTED;
                }
                else
                {

                    return STATE_ARMED;
                }
            }
        }
    }

    public int getTakeOffState()
    {
        return takeOffState;
    }

    public int getGotoState()
    {
        return gotoState;
    }

    public int getPauseState()
    {
        return pauseState;
    }

    public int getGuidedState()
    {
        return guidedState;
    }

    public int getLandingState()
    {
        return landingState;
    }

    public int getEventDrone() {
        return eventDrone;
    }

    public boolean getConnectedDrone() {
        return this.drone.isConnected();
    }

    public boolean getArmedDrone() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        return vehicleState.isArmed();
    }

    public boolean getFlyingDrone() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        return vehicleState.isFlying();
    }

    public boolean getLandingDrone() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        return vehicleState.getVehicleMode() == VehicleMode.COPTER_LAND;
    }

    public boolean getGuidedDrone() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        return vehicleState.getVehicleMode() == VehicleMode.COPTER_GUIDED;
    }

    public int getVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        return vehicleMode.getMode();
    }

    public double getAltitude() {
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        return droneAltitude.getAltitude();
    }

    public double getLatitude() {
        Gps gpsData = this.drone.getAttribute(AttributeType.GPS);
        return (float)gpsData.getPosition().getLatitude();
    }

    public double getLongitude() {
        Gps gpsData = this.drone.getAttribute(AttributeType.GPS);
        return (float)gpsData.getPosition().getLongitude();
    }

    public float getSpeed() {
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        return (float)droneSpeed.getAirSpeed();
    }

    public float getDistanceFromHome() {
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double vehicleAltitude = droneAltitude.getAltitude();
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        double distanceFromHome =  0;

        if (droneGps.isValid()) {
            LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
            Home droneHome = this.drone.getAttribute(AttributeType.HOME);
            distanceFromHome = distanceBetweenPoints(droneHome.getCoordinate(), vehicle3DPosition);
        } else {
            distanceFromHome = 0;
        }

        // METERS
        return (float)distanceFromHome;
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy  = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    //---------------------------------------------------------
    //---------------------------------------------------------
    // ACTIONS
    //---------------------------------------------------------
    //---------------------------------------------------------

    public boolean connectDrone() {
        if (!getConnectedDrone()) {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(portNumber, null);
            this.drone.connect(connectionParams);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean disconnectDrone() {
        if (getConnectedDrone()) {
            this.drone.disconnect();
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean armDrone() {
        if (getConnectedDrone() && !getArmedDrone()){
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true);
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean takeOffDrone() {
        takeOffState = -2;
        if (getArmedDrone() && !getFlyingDrone()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(heightDroneTakeOff, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    takeOffState = COMMAND_STATE_SUCCESS;
                }

                @Override
                public void onError(int i)
                {
                    takeOffState = COMMAND_STATE_ERROR;
                }

                @Override
                public void onTimeout()
                {
                    takeOffState = COMMAND_STATE_TIMEOUT;
                }
            });
            return true;
        }
        else
        {
            return false;
        }
    }

    public void landModeDrone() {
        // Land
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                landingState = COMMAND_STATE_SUCCESS;
            }

            @Override
            public void onError(int executionError) {
                landingState = COMMAND_STATE_ERROR;
            }

            @Override
            public void onTimeout() {
                landingState = COMMAND_STATE_TIMEOUT;
            }
        });
    }

    public void guidedModeDrone()
    {
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                guidedState = COMMAND_STATE_SUCCESS;
            }

            @Override
            public void onError(int executionError) {
                guidedState = COMMAND_STATE_ERROR;
            }

            @Override
            public void onTimeout() {
                guidedState = COMMAND_STATE_TIMEOUT;
            }
        });
    }

    public void setModeDrone(int _modeOperation)
    {
        VehicleMode selectedMode = VehicleMode.COPTER_STABILIZE;
        switch (_modeOperation)
        {
            case 0:
                selectedMode = VehicleMode.COPTER_STABILIZE;
                break;
            case 1:
                selectedMode = VehicleMode.COPTER_ACRO;
                break;
            case 2:
                selectedMode = VehicleMode.COPTER_ALT_HOLD;
                break;
            case 3:
                selectedMode = VehicleMode.COPTER_AUTO;
                break;
            case 4:
                selectedMode = VehicleMode.COPTER_GUIDED;
                break;
            case 5:
                selectedMode = VehicleMode.COPTER_STABILIZE;
                break;
            case 6:
                selectedMode = VehicleMode.COPTER_RTL;
                break;
            case 7:
                selectedMode = VehicleMode.COPTER_CIRCLE;
                break;
            case 9:
                selectedMode = VehicleMode.COPTER_LAND;
                break;
            case 11:
                selectedMode = VehicleMode.COPTER_DRIFT;
                break;
            case 13:
                selectedMode = VehicleMode.COPTER_SPORT;
                break;
            case 14:
                selectedMode = VehicleMode.COPTER_FLIP;
                break;
            case 15:
                selectedMode = VehicleMode.COPTER_AUTOTUNE;
                break;
            case 16:
                selectedMode = VehicleMode.COPTER_POSHOLD;
                break;
            case 17:
                selectedMode = VehicleMode.COPTER_BRAKE;
                break;
        }

        VehicleApi.getApi(this.drone).setVehicleMode(selectedMode);
    }

    public boolean setVelocityDrone(float _vx, float _vy, float _vz, float _speed) {

        if (getFlyingDrone())
        {
            msg_set_position_target_local_ned msgMessageVelocity = new msg_set_position_target_local_ned();
            msgMessageVelocity.time_boot_ms = 0;
            msgMessageVelocity.target_system = 0;
            msgMessageVelocity.target_component = 0;
            msgMessageVelocity.coordinate_frame = MAV_FRAME.MAV_FRAME_BODY_NED;
            msgMessageVelocity.type_mask = 0b0000111111000111;
            msgMessageVelocity.x = 0;
            msgMessageVelocity.y = 0;
            msgMessageVelocity.z = 0;
            msgMessageVelocity.vx = _vx * _speed;
            msgMessageVelocity.vy = _vz * _speed;
            msgMessageVelocity.vz = _vy * _speed;
            msgMessageVelocity.afx = 0;
            msgMessageVelocity.afy = 0;
            msgMessageVelocity.afz = 0;
            msgMessageVelocity.yaw = 0;
            msgMessageVelocity.yaw_rate = 0;

            MavlinkMessageWrapper mavlinkMessageWrapper = new MavlinkMessageWrapper(msgMessageVelocity);

            ExperimentalApi.getApi(drone).sendMavlinkMessage(mavlinkMessageWrapper);

            drone.addMavlinkObserver(new MavlinkObserver() {
                @Override
                public void onMavlinkMessageReceived(MavlinkMessageWrapper mavlinkMessageWrapper) {
                    System.out.println("MESSAGE RECEIVED="+mavlinkMessageWrapper.getMavLinkMessage().toString());
                }
            });
            return true;
        }
        else
        {
            return false;
        }
    }

    private Point3D getForwardVector(float pitch, float yaw) {
        Point3D forward = new Point3D();
        forward.x = Math.cos(pitch)*Math.cos(yaw);
        forward.y = Math.cos(pitch)*Math.sin(yaw);
        forward.z = Math.sin(pitch);

        return forward;
    }

    public boolean setGoToDrone(float _latitude, float _longitude, float _speed) {
        if (getFlyingDrone()) {

            // SET SPEED M/S
            Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
            droneSpeed.setAirSpeed(_speed);

            // GET CURRENT LATITUDE,LONGITUDE
            Gps gpsData = this.drone.getAttribute(AttributeType.GPS);
            float latitude = (float)gpsData.getPosition().getLatitude();
            float longitude = (float)gpsData.getPosition().getLongitude();

            LatLong newPosition = new LatLong(_latitude, _longitude);
            ControlApi.getApi(this.drone).goTo(newPosition, true, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    gotoState = COMMAND_STATE_SUCCESS;
                }

                @Override
                public void onError(int executionError)
                {
                    gotoState = COMMAND_STATE_ERROR;
                }

                @Override
                public void onTimeout() {
                    gotoState = COMMAND_STATE_TIMEOUT;
                }
            });
            return true;
        }
        else
        {
            return false;
        }
    }

    public void pauseDrone() {
        if (getFlyingDrone()) {
            ControlApi.getApi(this.drone).pauseAtCurrentLocation(new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    pauseState = COMMAND_STATE_SUCCESS;
                }

                @Override
                public void onError(int executionError) {
                    pauseState = COMMAND_STATE_ERROR;
                }

                @Override
                public void onTimeout() {
                    pauseState = COMMAND_STATE_TIMEOUT;
                }
            });
        }
    }

    public void changeAltitudeDrone(float _newAltitude) {
        if (getFlyingDrone()) {
            ControlApi.getApi(this.drone).climbTo(_newAltitude);
        }
    }
}
