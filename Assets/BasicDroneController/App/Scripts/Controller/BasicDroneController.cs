using System;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.SceneManagement;
using YourCommonTools;

namespace BasicDroneController
{
    /******************************************
	 * 
	 * BasicDroneController
	 * 
	 * ScreenManager controller that handles basic drone operations
	 * 
	 * @author Esteban Gallardo
	 */
    public class BasicDroneController : ScreenController
	{
        // ----------------------------------------------
        // EVENTS
        // ----------------------------------------------	

        // ----------------------------------------------
        // SINGLETON
        // ----------------------------------------------	
        private static BasicDroneController _instance;

		public static BasicDroneController Instance
		{
			get
			{
				if (!_instance)
				{
					_instance = GameObject.FindObjectOfType(typeof(BasicDroneController)) as BasicDroneController;
				}
				return _instance;
			}
		}

        // ----------------------------------------------
        // PUBLIC MEMBERS
        // ----------------------------------------------	

        // ----------------------------------------------
        // PRIVATE MEMBERS
        // ----------------------------------------------	
        private float m_speed = 5f;
        private float m_time = 5f;
        private float m_height = 1f;
        private int m_port = 14550;
        private string m_ipDrone = "192.168.0.21";

        // ----------------------------------------------
        // GETTERS
        // ----------------------------------------------	
        public float Speed
        {
            get { return m_speed; }
            set { m_speed = value; }
        }
        public float Time
        {
            get { return m_time; }
            set { m_time = value; }
        }        
        public float Height
        {
            get { return m_height; }
            set { m_height = value; }
        }
        public int Port
        {
            get { return m_port; }
            set { m_port = value; }
        }
        public string IPDrone
        {
            get { return m_ipDrone; }
            set { m_ipDrone = value; }
        }
        

        // -------------------------------------------
        /* 
		 * Force orientation of the device to landscape
		 */
        public override void Awake()
        {
#if !ENABLE_OCULUS
            Screen.orientation = ScreenOrientation.Portrait;
            Screen.autorotateToLandscapeLeft = false;
            Screen.autorotateToLandscapeRight = false;
#endif
        }

        // -------------------------------------------
        /* 
		 * Initialitzation listener
		 */
        public override void Start()
		{
			base.Start();

            UIEventController.Instance.UIEvent += new UIEventHandler(OnUIEvent);

            LanguageController.Instance.Initialize();

            CreateNewScreen(ScreenBasicControlDroneView.SCREEN_NAME, UIScreenTypePreviousAction.DESTROY_ALL_SCREENS, true);
		}

		// -------------------------------------------
		/* 
		 * Release resources
		 */
		public override void Destroy()
		{
			base.Destroy();

			if (_instance != null)
			{
                UIEventController.Instance.UIEvent -= OnUIEvent;
				Destroy(_instance);
				_instance = null;
			}
		}

        // -------------------------------------------
        /* 
		 * Update
		 */
        public override void Update()
        {
            base.Update();
        }
    }
}