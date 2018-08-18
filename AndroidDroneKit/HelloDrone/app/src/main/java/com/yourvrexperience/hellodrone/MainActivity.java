package com.yourvrexperience.hellodrone;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.MAVLink.common.msg_local_position_ned;
import com.MAVLink.common.msg_global_position_int;
import com.MAVLink.common.msg_message_interval;
import com.MAVLink.common.msg_rc_channels_override;
import com.MAVLink.common.msg_set_attitude_target;
import com.MAVLink.common.msg_set_position_target_local_ned;
import com.MAVLink.enums.MAV_FRAME;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.MavlinkObserver;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.mavlink.MavlinkMessageWrapper;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

import org.droidplanner.services.android.impl.core.drone.autopilot.MavLinkDrone;
import org.droidplanner.services.android.impl.core.drone.manager.MavLinkDroneManager;
import org.json.JSONObject;

import java.util.List;

public class MainActivity extends AppCompatActivity implements DroneListener, TowerListener {

    private ControlTower controlTower;

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();
    Spinner modeSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the service manager
        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        this.modeSelector = (Spinner)findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);

    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }
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

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateAltitude();
                updateSpeed();
                break;

            case AttributeEvent.HOME_UPDATED:
                updateDistanceFromHome();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
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
    // CONNECTION
    //---------------------------------------------------------
    //---------------------------------------------------------

    public void onBtnConnectTap(View view) {
        if(this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(14550, null);
            this.drone.connect(connectionParams);
        }
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button)findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    //---------------------------------------------------------
    //---------------------------------------------------------
    // INFORMATION
    //---------------------------------------------------------
    //---------------------------------------------------------

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes =  VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter)this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView)findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView)findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateDistanceFromHome() {
        TextView distanceTextView = (TextView)findViewById(R.id.distanceValueTextView);
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

        distanceTextView.setText(String.format("%3.1f", distanceFromHome) + "m");
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

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button)findViewById(R.id.btnArmTakeOff);
        Button velocityButton = (Button)findViewById(R.id.btnChangeVelocity);
        Button gotoButton = (Button)findViewById(R.id.btnGoTo);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
            velocityButton.setVisibility(View.INVISIBLE);
            gotoButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            alertUser("vehicleState IS FLYING");
            // Land
            armButton.setText("LAND");
            velocityButton.setVisibility(View.VISIBLE);
            gotoButton.setVisibility(View.VISIBLE);
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
            velocityButton.setVisibility(View.INVISIBLE);
            gotoButton.setVisibility(View.INVISIBLE);
            alertUser("vehicleState IS ARMED");
        } else if (vehicleState.isConnected()){
            // Connected but not Armed
            armButton.setText("ARM");
            alertUser("vehicleState IS CONNECTED");
            velocityButton.setVisibility(View.INVISIBLE);
            gotoButton.setVisibility(View.INVISIBLE);
        }
    }

    //---------------------------------------------------------
    //---------------------------------------------------------
    // ACTIONS
    //---------------------------------------------------------
    //---------------------------------------------------------

    public void onArmButtonTap(View view) {
        Button thisButton = (Button)view;
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND);
        } else if (vehicleState.isArmed()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
            });

        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else if (vehicleState.isConnected() && !vehicleState.isArmed()){
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true);
        }
    }

    public void onChangeVelocityButtonTap(View view) {
        Button thisButton = (Button) view;
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying())
        {
            EditText vxText = (EditText) findViewById(R.id.vx);
            EditText vyText = (EditText) findViewById(R.id.vy);
            EditText vzText = (EditText) findViewById(R.id.vz);
            float vx = Float.parseFloat(vxText.getText().toString());
            float vy = Float.parseFloat(vyText.getText().toString());
            float vz = Float.parseFloat(vzText.getText().toString());

            msg_set_position_target_local_ned msgMessageInterval = new msg_set_position_target_local_ned();
            float velocity = 10;
            // msgMessageInterval.vx = vx * velocity;
            // msgMessageInterval.vy = vz * velocity;
            // msgMessageInterval.vz = vy * velocity;
            msgMessageInterval.time_boot_ms = 0;
            msgMessageInterval.target_system = 0;
            msgMessageInterval.target_component = 0;
            msgMessageInterval.coordinate_frame = MAV_FRAME.MAV_FRAME_BODY_NED;
            msgMessageInterval.type_mask = 0b0000111111000111;
            msgMessageInterval.x = 0;
            msgMessageInterval.y = 0;
            msgMessageInterval.z = 0;
            msgMessageInterval.vx = 10;
            msgMessageInterval.vy = 0;
            msgMessageInterval.vz = 0;
            msgMessageInterval.afx = 0;
            msgMessageInterval.afy = 0;
            msgMessageInterval.afz = 0;
            msgMessageInterval.yaw = 0;
            msgMessageInterval.yaw_rate = 0;

            alertUser("FLY VELOCITY TYPE_MASK=" + msgMessageInterval.type_mask);

            MavlinkMessageWrapper mavlinkMessageWrapper = new MavlinkMessageWrapper(msgMessageInterval);

            ExperimentalApi.getApi(drone).sendMavlinkMessage(mavlinkMessageWrapper);

            drone.addMavlinkObserver(new MavlinkObserver() {
                @Override
                public void onMavlinkMessageReceived(MavlinkMessageWrapper mavlinkMessageWrapper) {
                    System.out.println("MESSAGE RECEIVED="+mavlinkMessageWrapper.getMavLinkMessage().toString());
                }
            });
        }
    }

    public void onGoToButtonTap(View view) {
        Button thisButton = (Button) view;
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        String gpsData = "" + this.drone.getAttribute(AttributeType.GPS);
        int indexLat = gpsData.indexOf("latitude=") + 9;
        int indexLon = gpsData.indexOf("longitude=");
        int indexVa = gpsData.indexOf("vehicleArmed=");
        float latitude = Float.parseFloat(gpsData.substring(indexLat, (indexLon - 2)));
        float longitude = Float.parseFloat(gpsData.substring(indexLon + 10, (indexVa - 3)));
        // alertUser("LAT["+latitude+"],LONG["+longitude+"]");
        if (vehicleState.isFlying()) {
            LatLong newPosition = new LatLong(latitude + 2, longitude);
            ControlApi.getApi(this.drone).goTo(newPosition, true, new AbstractCommandListener() {
                @Override
                public void onSuccess() {
                    alertUser("Goto Success!!!");
                }

                @Override
                public void onError(int executionError) {
                    alertUser("Goto Error!!!");
                }

                @Override
                public void onTimeout() {
                    alertUser("Goto Timeout!!!");
                }
            });
        }
    }


}