/*
 * MainSpot.java
 * 
 * Author: James Howard
 * 01/16/2013
 * 
 * On Mote program to detect the direction of walking over open spaces.
 */

package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.ISleepManager;
import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.IRadioPolicyManager;
import com.sun.spot.peripheral.radio.RadioFactory;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.IIOPin;
import com.sun.spot.resources.transducers.IInputPinListener;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.InputPinEvent;
import com.sun.spot.sensorboard.EDemoBoard;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;


class PinListener implements IInputPinListener {
    
    Queue q;
    int pin;
    
    public PinListener(Queue q, int pin) {
        this.q = q;
        this.pin = pin;
    }
    
    public PinListener(Queue q) {
        this.q = q;
        this.pin = 10;
    }
    
    public void pinSetHigh(InputPinEvent evt) {
        System.out.println("Pin " + evt.getInputPin() + "  to high.");
    }

    public void pinSetLow(InputPinEvent evt) {
        System.out.println("Test.");
        
        if(this.q.isEmpty()) {
            System.out.println("Pin " + evt.getInputPin() + "  to low.");
        }
        q.put(new Integer(pin));
    }
}

class MidSensorReader implements Runnable {
    
    RecordHandler records = null;
    IIOPin io = null;
    LedController ledc = null;
    int pf;
    
    public MidSensorReader(RecordHandler records, 
                               IIOPin io, LedController ledc) {
        this.records = records;
        this.io = io;
        this.ledc = ledc;
    }
    
    public void run() {
        
        int sleepSum = 0;
        int counts = 0;
        
        while(true) {
            if(io.getState() == false) {
                counts += 1;
            }
            sleepSum += Const.MID_SENSOR_POLL_FREQUENCY;
            
            if(sleepSum >= Const.MID_SENSOR_COUNT_TIME) {
                records.addRecord(Const.ROOM_COUNT, counts);
                counts = 0;
                sleepSum = 0;
            }
            
            Utils.sleep(Const.MID_SENSOR_POLL_FREQUENCY);
        }
    }
}



public class MainSPOT extends MIDlet {
        
    private IIOPin[]        ioPins; 
    private LedController   ledc;
    private Thread          ledThread;
    private ISleepManager   sleepManager;
    private RecordHandler   records;
    //private PinListener     pinListenerFront;
    //private PinListener     pinListenerBack;
    private PinListener     pinListener;
    private Thread          midSensorThread;
    private MidSensorReader msr;
    private Queue           triggers;
    private int             state;
    private IIOPin          frontSensor, backSensor;
    private ITriColorLEDArray leds;
    
    protected void startApp() throws MIDletStateChangeException {
    
        long ourAddr = RadioFactory.getRadioPolicyManager().getIEEEAddress();
        System.out.println("Our radio address = " + IEEEAddress.toDottedHex(ourAddr));
        setup();
        
        handleNetworkCommands();
        
        int timeoutCounter = 0;
        boolean fs, bs;
        
        try {
            while(true) {
                System.out.println("Waiting for trigger.");
               int trigger = ((Integer)triggers.get()).intValue();

               if(trigger == Const.BACK_SENSOR) {
                   state = Const.BACK_TRIGGER_STATE;
               } else if(trigger == Const.FRONT_SENSOR) {
                   state = Const.FRONT_TRIGGER_STATE;
               }

               while(state != Const.WAITING_STATE) {

                   fs = frontSensor.getState();
                   bs = backSensor.getState();               

                   if ((timeoutCounter >= Const.DOWN_SENSORS_RESET_TIMEOUT) && 
                           (state != Const.COOLDOWN_STATE)) {
                       state = Const.COOLDOWN_STATE;
                       System.out.println("Movement timeout");
                       //TODO blink error code
                   }

                   if((state == Const.COOLDOWN_STATE) && fs && bs) {
                       state = Const.WAITING_STATE;
                   }

                   if((state == Const.BACK_TRIGGER_STATE) && (fs == false)) {
                       state = Const.COOLDOWN_STATE;
                       records.addRecord(Const.EXIT, 0);
                       ledc.addCommand(Const.LED_EXIT);
                       System.out.println("Exit");
                       //TODO add blink
                   }

                   if((state == Const.FRONT_TRIGGER_STATE) && (bs == false)) {
                       state = Const.COOLDOWN_STATE;
                       records.addRecord(Const.ENTER, 0);
                       ledc.addCommand(Const.LED_ENTER);
                       System.out.println("Enter");
                       //TODO add blink
                   }

                   Utils.sleep(Const.DOWN_SENSORS_POLL_FREQUENCY);
                   timeoutCounter += Const.DOWN_SENSORS_POLL_FREQUENCY;
               }

               timeoutCounter = 0;
               triggers.empty();
               ledc.addCommand(Const.LED_RESET);
               System.out.println("Reset");
            }
        } catch(Exception e) {
            //Crash state.  Mote needs to be reset.
            while(true) {
                ledc.addCommand(Const.LED_CRASH);
                Utils.sleep(10000);
            }
        }
        
        
        
        //notifyDestroyed();                      // cause the MIDlet to exit
    }
    
    protected void setup() {
        try {
            ioPins = EDemoBoard.getInstance().getIOPins();
            ioPins[Const.RED_LED].setAsOutput(true);
            ioPins[Const.BLUE_LED].setAsOutput(true);
            ioPins[Const.RED_LED].setHigh();
            ioPins[Const.BLUE_LED].setHigh();
            frontSensor = ioPins[Const.FRONT_SENSOR];
            backSensor = ioPins[Const.BACK_SENSOR];

            sleepManager = Spot.getInstance().getSleepManager();
            sleepManager.enableDeepSleep();
            
            leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
            //ledc = new LedController(ioPins);
            ledc = new LedController(leds);
            ledThread = new Thread(ledc);
            ledThread.start();

            records = new RecordHandler(Const.RECORD_NAME);
            
            triggers = new Queue();
            state = Const.WAITING_STATE;
            
            pinListener = new PinListener(triggers);//, Const.FRONT_SENSOR);
            //pinListenerBack = new PinListener(triggers, Const.BACK_SENSOR);
            frontSensor.addIInputPinListener(pinListener);
            backSensor.addIInputPinListener(pinListener);
            
            msr = new MidSensorReader(records, ioPins[Const.MID_SENSOR], ledc);
            midSensorThread = new Thread(msr);
                        
            System.out.println("Saved mote records " + records.getNumRecords());
        } catch (Exception e) {
            System.out.println("Setup error.");
            System.out.println(e);
            ledc.addCommand(Const.LED_SETUP_ERROR);
        }
    }
    
    public void handleNetworkCommands() {     
        
        RadiogramConnection inConn = null;
        Datagram dg;
        
        broadcast();
        
        //Now wait for a response and perform the specified action
        try {
            System.out.println("Waiting for response.");
            inConn = (RadiogramConnection) Connector.open("radiogram://:" + Const.LOCAL_PORT);
            dg = inConn.newDatagram(inConn.getMaximumLength());
            inConn.setTimeout(2000);
            inConn.receive(dg);
            RecordData tmpData = RecordData.deserialize(dg);
            
            //dump data
            if (tmpData.type == Const.DATA_DUMP) {
                if (transmitData(inConn, dg)) {
                    if (recieveDeleteData(inConn, dg)) {
                        System.out.println("Deleting records.");
                        records.deleteRecords(Const.RECORD_NAME);
                    }
                } else {
                    System.out.println("Data did not transmit fully.");
                }
            }
        } catch (Exception e) {
            System.out.println("Timeout.");
            System.out.println(e);
        }

        
        //Now close and turn off radio
        try {
            System.out.println("Closing connections.");
            inConn.close();
            IRadioPolicyManager rpm = Spot.getInstance().getRadioPolicyManager();
            rpm.setRxOn(false);
        } catch(Exception e) {
            System.out.println("Closing radio error.");
        }
    }
    
    
    public boolean recieveDeleteData(RadiogramConnection inConn, Datagram dg) {
        try {
            //Wait for delete
            dg.reset();
            inConn.setTimeout(20000);
            System.out.println("Waiting for response to delete records.");
            inConn.receive(dg);
            RecordData rd = RecordData.deserialize(dg);
            
            if (rd.type == Const.DELETE_RECORDS) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("Must have been a timeout.");
        }
        return false;
    }
    
    
    public boolean transmitData(RadiogramConnection inConn, Datagram dg) {
        int counter = 0;
        records.rebuildEnumerator();
        byte[] tmp = records.getNextRecord();
        
        try {
            while(tmp != null) {
                ledc.addCommand(Const.LED_DATA);
                dg.reset();
                dg.write(tmp);
                inConn.send(dg);
                tmp = records.getNextRecord();
                counter += 1;
            }
            System.out.println("Sent " + counter + " records");
            
            dg.reset();
            RecordData tmpRecord = new RecordData(Const.EOF, (short)0);
            dg.write(tmpRecord.serialize());
            inConn.send(dg);
            System.out.println("EOF sent");
            return true;
        } catch(Exception e) {
            System.out.println("Error while transmitting");
        }
        return false;
    }
    
    public void broadcast() {
        RadiogramConnection outConn = null;
        Datagram dg;
        
        //First try to broadcast out our address.
        try {    
            System.out.println("Broadcasting");
            outConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + Const.BROADCAST_PORT);
            dg = outConn.newDatagram(outConn.getMaximumLength());
            RecordData broadCommand = new RecordData(Const.BROADCAST, (short)0);
            dg.write(broadCommand.serialize());
            
            //Send three times due to unreliable broadcasts
            //outConn.send(dg);
            //outConn.send(dg);
            outConn.send(dg);
            outConn.close();
            //TODO Call led for broadcasting here.
        } catch (Exception e) {
            //TODO Call exception here
            System.out.println("Error in broadcasting");
        } 
    }
    
    
    protected void pauseApp() {
    }
    
    /**
     * Called if the MIDlet is terminated by the system.
     * I.e. if startApp throws any exception other than MIDletStateChangeException,
     * if the isolate running the MIDlet is killed with Isolate.exit(), or
     * if VM.stopVM() is called.
     * 
     * It is not called if MIDlet.notifyDestroyed() was called.
     *
     * @param unconditional If true when this method is called, the MIDlet must
     *    cleanup and release all resources. If false the MIDlet may throw
     *    MIDletStateChangeException  to indicate it does not want to be destroyed
     *    at this time.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }
}