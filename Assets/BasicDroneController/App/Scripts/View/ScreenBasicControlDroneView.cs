﻿using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.EventSystems;
using UnityEngine.UI;
using YourCommonTools;

namespace BasicDroneController
{
    /******************************************
	 * 
	 * ScreenBasicControlDroneView
	 * 
	 * Basic control view
	 * 
	 * @author Esteban Gallardo
	 */
    public class ScreenBasicControlDroneView : ScreenBaseView, IBasicView
	{
		public const string SCREEN_NAME = "SCREEN_DRONE_CONTROL";

        // ----------------------------------------------
        // EVENTS
        // ----------------------------------------------
        public const string EVENT_BASICCONTROLDRONE_DISPLAY_DIRECTION_SIGNAL = "EVENT_BASICCONTROLDRONE_DISPLAY_DIRECTION_SIGNAL";

        public const string SUB_EVENT_BASICCONTROLDRONE_CONFIRMATION_EXIT_APP = "SUB_EVENT_BASICCONTROLDRONE_CONFIRMATION_EXIT_APP";

        // ----------------------------------------------
        // CONSTANTS
        // ----------------------------------------------
        public static readonly string[] OPERATION_MODES = { "Stabilize", "Acro", "Alt Hold", "Auto", "Guided", "Loiter", "RTL", "Circle", "Land", "Drift", "Sport", "Flip", "Autotune", "PosHold", "Brake" };
        public static readonly int[] INDEXES_MODES = { 0, 1, 2, 3, 4, 5, 6, 7, 9, 11, 13, 14, 15, 16, 17 };

        enum Operations {
            CONNECT = 0, ARM = 1, TAKEOFF = 2, LAND = 3
        }

        // ----------------------------------------------
        // PUBLIC MEMBERS
        // ----------------------------------------------
        public GameObject DotDirection;
        public GameObject DotNext;

        // ----------------------------------------------
        // PRIVATE MEMBERS
        // ----------------------------------------------	
        private GameObject m_root;
		private Transform m_container;

        private Text m_textDescription;

        private InputField m_speedInput;
        private GameObject m_applySpeed;

        private InputField m_timeInput;
        private GameObject m_applyTime;

        private InputField m_heightInput;
        private GameObject m_applyHeight;

        private InputField m_portNumberInput;
        private GameObject m_applyPortNumber;

        private InputField m_ipAddressInput;
        private GameObject m_applyIPAddress;

        private Dropdown m_modesVehicle;
        private GameObject m_applyVehicleMode;

        private GameObject m_buttonOperation;
        private Operations m_operation;

        private GameObject m_buttonRTL;
        private GameObject m_buttonLand;
        private GameObject m_buttonDisarm;

        private Vector2 m_vectorVelocity = Vector2.zero;

        // RADAR AREA
        private GameObject m_radarArea;
        private RectTransform m_rectRadarArea;
        private Vector2 m_centerPosition;
        private Vector2 m_sizeArea;
        private bool m_pressedInArea = false;

        private bool m_ignoreUpdate = false;

        private GameObject m_currentDotVelocity;
        private GameObject m_nextDotVelocity;

        // -------------------------------------------
        /* 
		 * Constructor
		 */
        public override void Initialize(params object[] _list)
		{
			m_root = this.gameObject;
			m_container = m_root.transform.Find("Content");

            m_container.Find("Title").GetComponent<Text>().text = LanguageController.Instance.GetText("message.game.title");

            m_textDescription = m_container.Find("Text").GetComponent<Text>();
            m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.1.connect");

            m_container.Find("Speed/Label").GetComponent<Text>().text = LanguageController.Instance.GetText("message.speed");
            m_speedInput = m_container.Find("Speed/Value").GetComponent<InputField>();
            m_speedInput.text = BasicDroneController.Instance.Speed.ToString();
            m_speedInput.onEndEdit.AddListener(OnChangeSpeed);
            m_applySpeed = m_container.Find("Speed/Apply").gameObject;
            m_applySpeed.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.apply");
            m_applySpeed.GetComponent<Button>().onClick.AddListener(ApplySpeed);
            m_applySpeed.SetActive(false);

            m_container.Find("Time/Label").GetComponent<Text>().text = LanguageController.Instance.GetText("message.time");
            m_timeInput = m_container.Find("Time/Value").GetComponent<InputField>();
            m_timeInput.text = BasicDroneController.Instance.Time.ToString();
            m_timeInput.onEndEdit.AddListener(OnChangeTime);
            m_applyTime = m_container.Find("Time/Apply").gameObject;
            m_applyTime.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.apply");
            m_applyTime.GetComponent<Button>().onClick.AddListener(ApplyTime);
            m_applyTime.SetActive(false);

            m_container.Find("Height/Label").GetComponent<Text>().text = LanguageController.Instance.GetText("message.height");
            m_heightInput = m_container.Find("Height/Value").GetComponent<InputField>();
            m_heightInput.text = BasicDroneController.Instance.Height.ToString();
            m_heightInput.onEndEdit.AddListener(OnChangeHeight);
            m_applyHeight = m_container.Find("Height/Apply").gameObject;
            m_applyHeight.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.apply");
            m_applyHeight.GetComponent<Button>().onClick.AddListener(ApplyHeight);
            m_applyHeight.SetActive(false);

            m_container.Find("PortNumber/Label").GetComponent<Text>().text = LanguageController.Instance.GetText("message.port.number");
            m_portNumberInput = m_container.Find("PortNumber/Value").GetComponent<InputField>();
            m_portNumberInput.text = BasicDroneController.Instance.Port.ToString();
            m_portNumberInput.onEndEdit.AddListener(OnChangePortNumber);
            m_applyPortNumber = m_container.Find("PortNumber/Apply").gameObject;
            m_applyPortNumber.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.apply");
            m_applyPortNumber.GetComponent<Button>().onClick.AddListener(ApplyPortNumber);
            m_applyPortNumber.SetActive(false);

            m_container.Find("IPDrone/Label").GetComponent<Text>().text = LanguageController.Instance.GetText("message.ip.address");
            m_ipAddressInput = m_container.Find("IPDrone/Value").GetComponent<InputField>();
            m_ipAddressInput.text = BasicDroneController.Instance.IPDrone;
            m_ipAddressInput.onEndEdit.AddListener(OnChangeIPAddress);
            m_applyIPAddress = m_container.Find("IPDrone/Apply").gameObject;
            m_applyIPAddress.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.apply");
            m_applyIPAddress.GetComponent<Button>().onClick.AddListener(ApplyIPAddress);
            m_applyIPAddress.SetActive(false);

            m_buttonRTL = m_container.Find("RTL").gameObject;
            m_buttonRTL.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.rtl");
            m_buttonRTL.GetComponent<Button>().onClick.AddListener(ApplyActionRTL);
            m_buttonRTL.SetActive(true);

            m_buttonLand = m_container.Find("LAND").gameObject;
            m_buttonLand.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.land");
            m_buttonLand.GetComponent<Button>().onClick.AddListener(ApplyLand);
            m_buttonLand.SetActive(true);
            
            m_buttonDisarm = m_container.Find("DISARM").gameObject;
            m_buttonDisarm.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.disarm");
            m_buttonDisarm.GetComponent<Button>().onClick.AddListener(ApplyDisarm);
            m_buttonDisarm.SetActive(true);

            // VEHICLES MODES
            m_container.Find("Mode").gameObject.SetActive(false);
            m_modesVehicle = m_container.Find("Mode/ModeVehicle").GetComponent<Dropdown>();
            m_modesVehicle.onValueChanged.AddListener(OnChangedModeVehicle);            
            m_modesVehicle.options = new List<Dropdown.OptionData>();
            for (int i = 0; i < OPERATION_MODES.Length; i++)
            {
                m_modesVehicle.options.Add(new Dropdown.OptionData(OPERATION_MODES[i]));
            }

            m_applyVehicleMode = m_container.Find("Mode/Apply").gameObject;
            m_applyVehicleMode.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.apply");
            m_applyVehicleMode.GetComponent<Button>().onClick.AddListener(ApplyVehicleMode);
            m_applyVehicleMode.SetActive(false);

            m_modesVehicle.value = 1;
            m_modesVehicle.value = 0;

            // RADAR AREA
            m_radarArea = m_container.Find("RadarArea").gameObject;
            m_rectRadarArea = m_radarArea.transform.GetComponent<RectTransform>();
            m_radarArea.GetComponent<Button>().onClick.AddListener(OnPressedInArea);
            m_centerPosition = new Vector2(m_rectRadarArea.sizeDelta.x / 2, m_rectRadarArea.sizeDelta.y / 2);

            // BUTTON BASIC OPERATION
            m_buttonOperation = m_container.Find("Button_Operation").gameObject;
            m_operation = Operations.CONNECT;
            m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.connect.drone");
            m_buttonOperation.GetComponent<Button>().onClick.AddListener(BasicOperation);

            // CLOSE CONNECTION
            m_container.Find("Button_Close").GetComponent<Button>().onClick.AddListener(CloseConnection);

            UIEventController.Instance.UIEvent += new UIEventHandler(OnUIEvent);
            BasicSystemEventController.Instance.BasicSystemEvent += new BasicSystemEventHandler(OnBasicSystemEvent);
        }

        // -------------------------------------------
        /* 
		 * BasicOperation
		 */
        private void BasicOperation()
        {
            switch (m_operation)
            {
                case Operations.CONNECT:
                    m_buttonOperation.SetActive(false);
                    m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.1.connecting.wait");
#if ENABLE_DRONEANDROIDCONTROLLER
                    DroneKitAndroidController.Instance.Initialitzation(false, BasicDroneController.Instance.IPDrone, BasicDroneController.Instance.Port, BasicDroneController.Instance.Height);
#elif ENABLE_WEBSOCKET_DRONEKIT
                    WebSocketDroneKitController.Instance.Connect(BasicDroneController.Instance.IPDrone + ":8080" );
#endif
                    break;

                case Operations.ARM:
                    m_buttonOperation.SetActive(false);
#if ENABLE_DRONEANDROIDCONTROLLER
                    DroneKitAndroidController.Instance.ArmDrone();
#elif ENABLE_WEBSOCKET_DRONEKIT
                    WebSocketDroneKitController.Instance.ArmDrone();
#endif
                    break;

                case Operations.TAKEOFF:
                    m_buttonOperation.SetActive(false);
#if ENABLE_DRONEANDROIDCONTROLLER
                    DroneKitAndroidController.Instance.TakeOffDrone();
#elif ENABLE_WEBSOCKET_DRONEKIT
                    WebSocketDroneKitController.Instance.TakeOffDrone((int)BasicDroneController.Instance.Height);
#endif
                    break;

                case Operations.LAND:
                    m_buttonOperation.SetActive(false);
#if ENABLE_DRONEANDROIDCONTROLLER
                    DroneKitAndroidController.Instance.LandDrone();
#elif ENABLE_WEBSOCKET_DRONEKIT
                    WebSocketDroneKitController.Instance.LandDrone();
#endif
                    break;
            }
        }

        // -------------------------------------------
        /* 
		 * OnChangeHeight
		 */
        private void OnChangeHeight(string _value)
        {
            m_applyHeight.SetActive(true);
        }

        // -------------------------------------------
        /* 
		 * ApplyHeight
		 */
        private void ApplyHeight()
        {
            BasicDroneController.Instance.Height = float.Parse(m_heightInput.text);
            m_applyHeight.SetActive(false);
#if ENABLE_DRONEANDROIDCONTROLLER
            DroneKitAndroidController.Instance.ChangeAltitude(BasicDroneController.Instance.Height);
#elif ENABLE_WEBSOCKET_DRONEKIT
            WebSocketDroneKitController.Instance.ChangeAltitude(BasicDroneController.Instance.Height);
#endif
        }

        // -------------------------------------------
        /* 
		 * OnChangeSpeed
		 */
        private void OnChangeSpeed(string arg0)
        {
            m_applySpeed.SetActive(true);
        }

        // -------------------------------------------
        /* 
		 * ApplySpeed
		 */
        private void ApplySpeed()
        {
            BasicDroneController.Instance.Speed = float.Parse(m_speedInput.text);
            m_applySpeed.SetActive(false);
        }

        // -------------------------------------------
        /* 
		 * ApplyTime
		 */
        private void ApplyTime()
        {
            BasicDroneController.Instance.Time = float.Parse(m_timeInput.text);
            m_applyTime.SetActive(false);
        }

        // -------------------------------------------
        /* 
		 * OnChangeTime
		 */
        private void OnChangeTime(string _value)
        {
            m_applyTime.SetActive(true);
        }

        // -------------------------------------------
        /* 
		 * OnChangedModeVehicle
		 */
        private void OnChangedModeVehicle(int _value)
        {
            m_ignoreUpdate = true;
            m_applyVehicleMode.SetActive(true);
        }

        // -------------------------------------------
        /* 
		 * OnChangedModeVehicle
		 */
        private void ApplyVehicleMode()
        {
            int typeMode = INDEXES_MODES[m_modesVehicle.value];
#if ENABLE_DRONEANDROIDCONTROLLER
            DroneKitAndroidController.Instance.SetModeOperation(typeMode);
#elif ENABLE_WEBSOCKET_DRONEKIT
            WebSocketDroneKitController.Instance.SetModeOperation(INDEXES_MODES[m_modesVehicle.value]);
#endif
            m_applyVehicleMode.SetActive(false);
            m_ignoreUpdate = false;
        }

        // -------------------------------------------
        /* 
		 * OnChangePortNumber
		 */
        private void OnChangePortNumber(string arg0)
        {
            m_applyPortNumber.SetActive(true);
        }

        // -------------------------------------------
        /* 
		 * ApplyPortNumber
		 */
        private void ApplyPortNumber()
        {
            BasicDroneController.Instance.Port = int.Parse(m_portNumberInput.text);
            m_applyPortNumber.SetActive(false);
        }

        // -------------------------------------------
        /* 
		 * OnChangeIPAddress
		 */
        private void OnChangeIPAddress(string arg0)
        {
            m_applyIPAddress.SetActive(true);
        }

        // -------------------------------------------
        /* 
		 * ApplyIPAddress
		 */
        private void ApplyIPAddress()
        {
            BasicDroneController.Instance.IPDrone = m_ipAddressInput.text;
            m_applyIPAddress.SetActive(false);
        }


        // -------------------------------------------
        /* 
		 * ApplyActionRTL
		 */
        private void ApplyActionRTL()
        {
            m_modesVehicle.value = 6;
            int typeMode = INDEXES_MODES[m_modesVehicle.value];
#if ENABLE_DRONEANDROIDCONTROLLER
            DroneKitAndroidController.Instance.SetModeOperation(typeMode);
#elif ENABLE_WEBSOCKET_DRONEKIT
            WebSocketDroneKitController.Instance.SetModeOperation(INDEXES_MODES[m_modesVehicle.value]);
#endif
        }

        // -------------------------------------------
        /* 
		 * ApplyLand
		 */
        private void ApplyLand()
        {
#if ENABLE_DRONEANDROIDCONTROLLER
            // DroneKitAndroidController.Instance.SetModeOperation(typeMode);
#elif ENABLE_WEBSOCKET_DRONEKIT
            WebSocketDroneKitController.Instance.LandDrone();
#endif
        }

        // -------------------------------------------
        /* 
		 * ApplyDisarm
		 */
        private void ApplyDisarm()
        {
#if ENABLE_DRONEANDROIDCONTROLLER
            // DroneKitAndroidController.Instance.SetModeOperation(typeMode);
#elif ENABLE_WEBSOCKET_DRONEKIT
            WebSocketDroneKitController.Instance.DisarmDrone();
#endif

            BasicSystemEventController.Instance.DispatchBasicSystemEvent(DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_CONNECTED);
        }
        

        // -------------------------------------------
        /* 
		 * ApplyActionLand
		 */
        private void ApplyActionLand()
        {
            m_modesVehicle.value = 8;
            int typeMode = INDEXES_MODES[m_modesVehicle.value];
#if ENABLE_DRONEANDROIDCONTROLLER
            DroneKitAndroidController.Instance.SetModeOperation(typeMode);
#elif ENABLE_WEBSOCKET_DRONEKIT
            WebSocketDroneKitController.Instance.SetModeOperation(INDEXES_MODES[m_modesVehicle.value]);
#endif
        }

        // -------------------------------------------
        /* 
		 * UpdateModeVehicle
		 */
        private void UpdateModeVehicle()
        {
            if (m_ignoreUpdate)
            {
                return;
            }

            int typeMode = INDEXES_MODES[m_modesVehicle.value];
            int vehicleMode = DroneKitAndroidController.Instance.GetVehicleMode();
            if (vehicleMode != typeMode)
            {
                for (int i = 0; i < INDEXES_MODES.Length; i++)
                {
                    if (INDEXES_MODES[i] == vehicleMode)
                    {
                        m_modesVehicle.value = i;
                        return;
                    }
                }
            }
        }

        // -------------------------------------------
        /* 
		 * CloseConnection
		 */
        private void CloseConnection()
        {
            BasicDroneController.Instance.CreateNewInformationScreen(ScreenInformationView.SCREEN_CONFIRMATION, UIScreenTypePreviousAction.KEEP_CURRENT_SCREEN, LanguageController.Instance.GetText("message.info"), LanguageController.Instance.GetText("message.do.you.want.exit"), null, SUB_EVENT_BASICCONTROLDRONE_CONFIRMATION_EXIT_APP);
        }

        // -------------------------------------------
        /* 
		 * Destroy
		 */
        public override bool Destroy()
		{
			if (base.Destroy()) return true;

			UIEventController.Instance.UIEvent -= OnUIEvent;

            GameObject.Destroy(this.gameObject);
			return false;
		}


        // -------------------------------------------
        /* 
		 * CreateDotDirection
		 */
        private GameObject CreateDotDirection(GameObject _dot, Vector2 _forward, float _time)
        {
            GameObject dotVelocity = Utilities.AddChild(m_radarArea.transform, _dot);
            GameObject.Destroy(dotVelocity, _time);

            dotVelocity.transform.localPosition = Vector2.zero + _forward.normalized * (m_centerPosition.x * 1f);
            dotVelocity.transform.localScale = Vector3.one;

            return dotVelocity;
        }

        // -------------------------------------------
        /* 
		 * DestroyDotDirection
		 */
        private void DestroyDotDirection(GameObject _dotDirection)
        {
            if (_dotDirection != null)
            {
                GameObject.Destroy(_dotDirection);
                _dotDirection = null;
            }
        }

        // -------------------------------------------
        /* 
		 * OnPressedInArea
		 */
        public void OnPressedInArea()
        {
            m_pressedInArea = true;
        }

        // -------------------------------------------
        /* 
		 * OnUIEvent
		 */
        private void OnUIEvent(string _nameEvent, params object[] _list)
		{
			if (_nameEvent == ScreenController.EVENT_CONFIRMATION_POPUP)
			{
				GameObject screen = (GameObject)_list[0];
				bool accepted = (bool)_list[1];
				string subnameEvent = (string)_list[2];
                if (_nameEvent == ScreenController.EVENT_CONFIRMATION_POPUP)
                {
                    string subEvent = (string)_list[2];
                    if (subEvent == SUB_EVENT_BASICCONTROLDRONE_CONFIRMATION_EXIT_APP)
                    {
                        if ((bool)_list[1])
                        {
                            Application.Quit();
                        }
                    }
                }
            }
            if (_nameEvent == EVENT_BASICCONTROLDRONE_DISPLAY_DIRECTION_SIGNAL)
            {
                Vector2 forwardSignal = (Vector2)_list[0];
                Vector2 forward = new Vector2(forwardSignal.x, forwardSignal.y);
                m_vectorVelocity = forward * BasicDroneController.Instance.Speed;

#if ENABLE_DRONEANDROIDCONTROLLER
                if (DroneKitAndroidController.Instance.RunVelocity(m_vectorVelocity.x, 0, m_vectorVelocity.y, false, BasicDroneController.Instance.Time))
#elif ENABLE_WEBSOCKET_DRONEKIT
                if (WebSocketDroneKitController.Instance.RunVelocity(m_vectorVelocity.x, 0, m_vectorVelocity.y, false, BasicDroneController.Instance.Time))
#endif
                {
                    DestroyDotDirection(m_nextDotVelocity);
                    BasicSystemEventController.Instance.DispatchBasicSystemEvent(DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_START_FLYING, BasicDroneController.Instance.Time, new Vector3(m_vectorVelocity.x, 0, m_vectorVelocity.y));
                }
                else
                {
                    DestroyDotDirection(m_nextDotVelocity);
                    m_nextDotVelocity = CreateDotDirection(DotNext, forward, 1000);
                }
            }
        }

        // -------------------------------------------
        /* 
		 * OnBasicSystemEvent
		 */
        private void OnBasicSystemEvent(string _nameEvent, object[] _list)
        {
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_DISCONNECTED)
            {
                m_operation = Operations.CONNECT;
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.connect.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.1.connect");
                m_buttonOperation.SetActive(true);
            }
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_CONNECTED)
            {
                if (_list.Length > 0)
                {
                    BasicDroneController.Instance.CreateNewInformationScreen(ScreenInformationView.SCREEN_INFORMATION, UIScreenTypePreviousAction.KEEP_CURRENT_SCREEN, LanguageController.Instance.GetText("message.error"), LanguageController.Instance.GetText("message.there.has.been.problem.to.arm"), null, "");
                }
                m_operation = Operations.ARM;
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.arm.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.2.arm");
                m_buttonOperation.SetActive(true);
            }
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_ARMED)
            {
                m_operation = Operations.TAKEOFF;
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.takeoff.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.3.takeoff");
                m_buttonOperation.SetActive(true);
            }
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_TAKEN_OFF)
            {
                m_operation = Operations.LAND;
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.land.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.3.now.takingoff");
                m_buttonOperation.SetActive(true);
                m_container.Find("Mode").gameObject.SetActive(true);
            }
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_READY)
            {
                m_operation = Operations.LAND;
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.land.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.4.velocity");
                m_buttonOperation.SetActive(true);
                m_container.Find("Mode").gameObject.SetActive(true);
            }
            if ((_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_START_FLYING)
                || (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_FLYING))
            {
                m_operation = Operations.LAND;
                int timeRemaining = (int)((float)_list[0]);
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.land.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.5.flying", timeRemaining, m_vectorVelocity.ToString());
                if (_list.Length > 1)
                {
                    Vector3 forward = (Vector3)_list[1];
                    Vector3 forward2D = new Vector2(forward.x, forward.z);
                    DestroyDotDirection(m_nextDotVelocity);
                    DestroyDotDirection(m_currentDotVelocity);
                    m_currentDotVelocity = CreateDotDirection(DotDirection, forward2D.normalized, 1000);
                }
                if (m_currentDotVelocity != null)
                {
                    m_currentDotVelocity.transform.Find("Text").GetComponent<Text>().text = timeRemaining.ToString();
                }
                if (timeRemaining == 0)
                {
                    DestroyDotDirection(m_currentDotVelocity);
                }
                m_buttonOperation.SetActive(true);
            }
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_LANDING)
            {
                DestroyDotDirection(m_nextDotVelocity);
                DestroyDotDirection(m_currentDotVelocity);
                m_buttonOperation.SetActive(false);
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.6.now.landing");
                m_container.Find("Mode").gameObject.SetActive(false);
            }
            if (_nameEvent == DroneKitAndroidController.EVENT_DRONEKITCONTROLLER_LANDED)
            {
                m_operation = Operations.ARM;
                m_buttonOperation.transform.Find("Text").GetComponent<Text>().text = LanguageController.Instance.GetText("message.arm.drone");
                m_textDescription.text = LanguageController.Instance.GetText("message.basic.instructions.2.arm");
                m_buttonOperation.SetActive(true);
                m_container.Find("Mode").gameObject.SetActive(false);
            }
        }

        // -------------------------------------------
        /* 
         * Update
         */
        void Update()
        {
            UpdateModeVehicle();

#if UNITY_EDITOR
            if (true)
#elif ENABLE_DRONEANDROIDCONTROLLER
            if (DroneKitAndroidController.Instance.TakeoffAltitudeReached)
#elif ENABLE_WEBSOCKET_DRONEKIT
            if (WebSocketDroneKitController.Instance.TakeoffAltitudeReached)
#endif
            {
                if (m_pressedInArea)
                {
                    m_pressedInArea = false;
                    if (GameObject.Find("Dropdown List") == null)
                    {
                        Vector3 centerReal = m_radarArea.transform.position;
                        
                        Vector2 relPos = new Vector2(Input.mousePosition.x, Input.mousePosition.y) - new Vector2(centerReal.x, centerReal.y);
                        UIEventController.Instance.DispatchUIEvent(EVENT_BASICCONTROLDRONE_DISPLAY_DIRECTION_SIGNAL, relPos.normalized);
                    }
                }
            }
        }
    }
}