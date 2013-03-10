package org.sunspotworld.demo;


public class Const {
    //activities
    public static short         ENTER                       = 1; // back to f
    public static short         EXIT                        = 2; // front to b
    public static short         ROOM_COUNT                  = 3;
    public static short         DATA_DUMP                   = 4;
    public static short         DELETE_RECORDS              = 5;
    public static short         EOF                         = 16;
    public static short         BROADCAST                   = 6;
    
    //sensors
    public static int           BACK_SENSOR                 = 0;
    public static int           FRONT_SENSOR                = 1;
    public static int           MID_SENSOR                  = 2;
    
    //network stuff
    public static int           LOCAL_PORT                  = 100;
    public static int           BROADCAST_PORT              = 200;  
    
    
    //mote stuff
    public static int           DOWN_SENSORS_RESET_TIMEOUT  = 1000;
    public static int           DOWN_SENSORS_POLL_FREQUENCY = 50;
    public static int           MID_SENSOR_POLL_FREQUENCY   = 5000;   //5 sec
    public static int           MID_SENSOR_COUNT_TIME       = 300000; //5 mins
    
    //states
    public static int           WAITING_STATE               = 0;
    public static int           BACK_TRIGGER_STATE          = 1;
    public static int           FRONT_TRIGGER_STATE         = 2;
    public static int           COOLDOWN_STATE              = 3;
    
    //leds
    public static int           RED_LED                     = 3;
    public static int           BLUE_LED                    = 4;
    
    public static String        RECORD_NAME                 = "data";
    public static String        DATA_DIRECTORY              = "/Users/jahoward/Documents/Dropbox/data/people/new_brown_hall/";
    
    //LED Commands
    public static int[]         LED_ERROR                   = {3, 3, 150, 0};
    public static int[]         LED_DATA                    = {1, 1, 20, 0};
    public static int[]         LED_CRASH                   = {3, 10, 150, 0};
    public static int[]         LED_SETUP_ERROR             = {2, 10, 200, 0};
    public static int[]         LED_EXIT                    = {1, 1, 200, 0};
    public static int[]         LED_ENTER                   = {2, 1, 200, 0};
    public static int[]         LED_RESET                   = {3, 1, 200, 0};
}