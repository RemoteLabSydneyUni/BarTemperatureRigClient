package au.edu.usyd.eng.remotelabs.bartemperaturerig.primitive;

import java.io.*;

import gnu.io.CommPortIdentifier; 
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent; 
import gnu.io.SerialPortEventListener; 
import java.util.Enumeration;

import au.edu.uts.eng.remotelabs.rigclient.util.ConfigFactory;
import au.edu.uts.eng.remotelabs.rigclient.util.IConfig;
import au.edu.uts.eng.remotelabs.rigclient.util.ILogger;
import au.edu.uts.eng.remotelabs.rigclient.util.LoggerFactory;
import java.util.*;

import java.net.URL;
import java.net.URLDecoder;


public class BarTemperatureHW extends Thread implements SerialPortEventListener {

	private boolean running = true;
	private String threadName;
    private int sleepmSec = 50;
	
	//Status variable
	private volatile int intErrCount = 999;
	private volatile long freshnessTimer;
	private volatile long resendTimer;

	
	//local variables to store live data from Arduino
	
	private volatile int intProcessMode = 0;

	private volatile float[] flCurTemp = new float[11];
	private volatile int[] intTempHealth = new int[11];
	
	private volatile float flHeaterPidPV = 0.0f;
	private volatile float flHeaterPidSP = 0.0f;
	private volatile float flHeaterPidOP = 0.0f;
	private volatile float flCoolerPidPV = 0.0f;
	private volatile float flCoolerPidSP = 0.0f;
	private volatile float flCoolerPidOP = 0.0f;

	private volatile float flHeaterPidKP = 0.0f;
	private volatile float flHeaterPidKI = 0.0f;
	private volatile float flHeaterPidKD = 0.0f;
	private volatile float flCoolerPidKP = 0.0f;
	private volatile float flCoolerPidKI = 0.0f;
	private volatile float flCoolerPidKD = 0.0f;
	
	private volatile int intHeaterPidMode = 0;
	private volatile int intCoolerPidMode = 0;
	
	//output data buffer
	private volatile int procModeBuffer;
	private volatile int heaterModeBuffer;
	private volatile int coolerModeBuffer;
	
	private volatile float heaterSPBuffer;
	private volatile float heaterOPBuffer;
	private volatile float coolerSPBuffer;
	private volatile float coolerOPBuffer;
	private volatile float heaterKPBuffer;
	private volatile float heaterKIBuffer;
	private volatile float heaterKDBuffer;
	private volatile float coolerKPBuffer;
	private volatile float coolerKIBuffer;
	private volatile float coolerKDBuffer;

	
	// action flags
	private volatile boolean heaterModeFlag = false;
	private volatile boolean coolerModeFlag = false;
	private volatile boolean heaterSPFlag = false;
	private volatile boolean heaterOPFlag = false;
	private volatile boolean coolerSPFlag = false;
	private volatile boolean coolerOPFlag = false;
	private volatile boolean heaterKPFlag = false;
	private volatile boolean heaterKIFlag = false;
	private volatile boolean heaterKDFlag = false;
	private volatile boolean coolerKPFlag = false;
	private volatile boolean coolerKIFlag = false;
	private volatile boolean coolerKDFlag = false;
	private volatile boolean procModeFlag = false;
	private volatile String rigInfo = "";
	
	// Local data storage for one-wire sensor addresses
	private volatile String[] sensorAddr = new String[11];

	
	// hardware Specs
	private static final float MAX_TEMP= 100.0f;
	private static final float MIN_TEMP= 0.0f;
	
    ILogger logger;

    /* Arduino interfacing */
	SerialPort serialPort;
	/** The port we're normally going to use. */
	private static final String PORT_NAMES[] = {
//		"/dev/tty.usbserial-A9007UX1", // Mac OS X
//        "/dev/ttyACM0", // Raspberry Pi
//        "/dev/ttyUSB0", // Linux
		"COM9", // Windows
	};
    private BufferedReader input;
    private static OutputStream output;
    
    private final IConfig config;
    String rigname = "dontknowyet";
    String configpath = "empty";
    String portNameFromConfig = "empty"; 
    
    private static final int TIME_OUT = 2000;
    private static final int DATA_RATE = 115200;
    
    String inputLine;
    boolean inputLineComplete = false;

    
    //------------------------------------------------------------------
    // HW module constructor
	BarTemperatureHW( String name){
		freshnessTimer = -1;

		threadName = name;
        this.config = ConfigFactory.getInstance();
        
        rigname = config.getProperty("Rig_Name");
        configpath = config.getProperty("Sensor_Address_File_Path");
        portNameFromConfig = config.getProperty("COM_Port");
        

		for (int i = 0; i<11;i++){
			flCurTemp[i] = 0.0f;
			intTempHealth[i] = 0;
		}
		flHeaterPidPV = 0.0f;
		flHeaterPidSP = 0.0f;
		flHeaterPidOP = 0.0f;
		flCoolerPidPV = 0.0f;
		flCoolerPidSP = 0.0f;
		flCoolerPidOP = 0.0f;

		flHeaterPidKP = 0.0f;
		flHeaterPidKI = 0.0f;
		flHeaterPidKD = 0.0f;
		flCoolerPidKP = 0.0f;
		flCoolerPidKI = 0.0f;
		flCoolerPidKD = 0.0f;
		
		intHeaterPidMode = 0;
		intCoolerPidMode = 0;
		
		heaterModeBuffer = 0;
		coolerModeBuffer = 0;
		heaterSPBuffer = 0.0f;
		heaterOPBuffer = 0.0f;
		coolerSPBuffer = 0.0f;
		coolerOPBuffer = 0.0f;
		heaterKPBuffer = 0.0f;
		heaterKIBuffer = 0.0f;
		heaterKDBuffer = 0.0f;
		coolerKPBuffer = 0.0f;
		coolerKIBuffer = 0.0f;
		coolerKDBuffer = 0.0f;
		
		procModeBuffer = 0;
		intProcessMode = 0;
		
	}
	
    //------------------------------------------------------------------
    // HW module main operations
	
	public void run() {

		String response ="";
		
	    logger = LoggerFactory.getLoggerInstance();
        logger.info("Primitive Controller HW Interface created");
        
        // Construct file path of the sensor address configuration file
        String pathRaw = BarTemperatureHW.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String pathFolder = pathRaw.substring(0,pathRaw.lastIndexOf("/"));
        String pathJar = "";
        try{
        	pathJar = URLDecoder.decode(pathFolder, "UTF-8");
        } catch (UnsupportedEncodingException e){
        	pathJar = "NAN";
        }
        String pathFile = pathJar +"/" + configpath;
        logger.info("Primitive Controller HW Interface - Config File Path = "+pathFile);
        
        // Communication port initialization
		serialInitialise();
		
		try {
			Thread.sleep(2000); // Give serial port time to start
		 } catch (Exception e) {
             System.err.println(e.toString());
		 }
		
		logger.info("Primitive Controller HW Interface - Initialized");
		
		// Read sensor addresses from the configuration file
		try {
			BufferedReader inp = new BufferedReader(new InputStreamReader(new FileInputStream(pathFile)));
			logger.info("Primitive Controller HW Interface - Config File Identified");
            // Get file info - line by line
            while ( inp.ready() )
            {
               String nextline = inp.readLine();
               if (nextline == null) continue;

               // Break the line down
               StringTokenizer tokens = new StringTokenizer (nextline);
               int numargs = tokens.countTokens();
               if ( numargs == 0 ) continue;

               String attribute = tokens.nextToken();
               if (attribute.equals("#")) continue;

               // Check the attribute
               if (attribute.equals(rigname)){
            	   int sensornum    = Integer.valueOf(tokens.nextToken()).intValue();
            	   setSensorAddr(sensornum, tokens.nextToken());
               }

            }
            logger.info("Primitive Controller HW Interface - Config Extracted");
            inp.close();
		} catch (IOException e) {
			logger.error("Primitive Controller HW Interface - Cannot get Config file");
         }
		setRigInfo(rigname);

		// Main Process loop
		
		try {
			while (getRunning()) {
				
				// Read data from Arduino
				response = updateValue();
				Thread.sleep(50);
				
				// Mode 1: Arduino in IDLE state, waiting for RigHW to issue start command
				if (getProcMode()==1){
					if (getProcModeBuffer() != 2){
						setProcModeBuffer(2);
						setResendTimer();
					}else{
						if (getResendTimer()>1000){
							//Resend command
							setProcModeFlag();
							setResendTimer();
						}
					}
				}
				
				// Mode 2: Arduino receiving addresses from RigHW
				if (getProcMode()==2){
					for (int i = 0;i<11;i++){
						pushSensorAddr(i);
						Thread.sleep(50);
					}
				}
				
				// Mode 3: Arudino confirm all addresses received, waiting for RigHW to command to proceed
				if (getProcMode()==3){
					if (getProcModeBuffer() != 4){
						setProcModeBuffer(4);
						setResendTimer();
					}else{
						if (getResendTimer()>1000){
							//Resend command
							setProcModeFlag();
							setResendTimer();
						}
					}
				}
				
				// Mode 4: Main Experiment process
				if (getProcMode()==4){
					if (getHeaterModeFlag()){
						response = pushHeaterMode();
						offHeaterModeFlag();
					}
					Thread.sleep(50);
					if (getCoolerModeFlag()){
						response = pushCoolerMode();
						offCoolerModeFlag();
					}
					Thread.sleep(50);
					if (getHeaterSPFlag()){
						response = pushHeaterSP();
						offHeaterSPFlag();
					}
					Thread.sleep(50);
					if (getHeaterOPFlag()){
						response = pushHeaterOP();
						offHeaterOPFlag();
					}
					Thread.sleep(50);
					if (getHeaterKPFlag()){
						response = pushHeaterKP();
						offHeaterKPFlag();
					}
					Thread.sleep(50);
					if (getHeaterKIFlag()){
						response = pushHeaterKI();
						offHeaterKIFlag();
					}
					Thread.sleep(50);
					if (getHeaterKDFlag()){
						response = pushHeaterKD();
						offHeaterKDFlag();
					}
					Thread.sleep(50);
					if (getCoolerSPFlag()){
						response = pushCoolerSP();
						offCoolerSPFlag();
					}
					Thread.sleep(50);
					if (getCoolerOPFlag()){
						response = pushCoolerOP();
						offCoolerOPFlag();
					}
					Thread.sleep(50);
					if (getCoolerKPFlag()){
						response = pushCoolerKP();
						offCoolerKPFlag();
					}
					Thread.sleep(50);
					if (getCoolerKIFlag()){
						response = pushCoolerKI();
						offCoolerKIFlag();
					}
					Thread.sleep(50);
					if (getCoolerKDFlag()){
						response = pushCoolerKD();
						offCoolerKDFlag();
					}
				}
				
				// Mode 5 to 7: Arduino process cleanup. RigHW not involved (IDLE)
				
				// Update mode from request
				if (getProcModeFlag()){
					response = pushProcMode();
					offProcModeFlag();
				}
				Thread.sleep(50);
			}
			logger.info("Primitive Controller HW Interface - Time to end=");
			serialClose();
			logger.info("Primitive Controller HW Interface - Serial port closed=");

		} catch (InterruptedException e) {
			logger.error("Primitive Controller HW Interface - Running exception");
		}
        logger.info("Primitive Controller HW Interface - Thread " +  threadName + " exiting.");
	}
	
	//------------------------------------------------------------------
    // Operation support functions (private access)
	//------------------------------------------------------------------
	
	// update local variable with live data from Arduino
	// Multiple variables in Arduino are transmitted as combined string
	// This function read the entire string, tokenize into correct segments
	// and update corresponding local variables with the data value
	
	private String updateValue(){
		String msg = "";
		String msg1 ="";
		String msg2 ="";
		String msg3 ="";
		String feedback = "";
		String[] response = new String[23];
		String[] pidResponse = new String[7];
		try{
				msg = sendCmd("rlab://REQV?addr=01");
				logger.info("Primitive Controller HW Interface - recvmsg" + msg);
				if (msg.startsWith("Cpl")){
					response = msg.substring(4).trim().split(";",23);
					for (int i=0;i<11;i++){
						setCurTemp(Float.parseFloat(response[2*i]),i);
						setTempHealth(Integer.parseInt(response[2*i+1]),i);
					}
					setProcMode(Integer.parseInt(response[22]));
					// And update the freshness flag
					setFreshness(System.nanoTime());

					feedback = "Cpl";
				}else{
					feedback = "Err";
				}
				Thread.sleep(sleepmSec);
				
				msg1 = sendCmd("rlab://REQV?addr=02");
				logger.info("Primitive Controller HW Interface - recvmsg" + msg1);
				if (msg1.startsWith("Cpl")){
					pidResponse = msg1.substring(4).trim().split(";",7);
					setHeaterPV(Float.parseFloat(pidResponse[0]));
					setHeaterSP(Float.parseFloat(pidResponse[1]));
					setHeaterOP(Float.parseFloat(pidResponse[2]));
					setHeaterMode(Integer.parseInt(pidResponse[3]));
					setHeaterKP(Float.parseFloat(pidResponse[4]));
					setHeaterKI(Float.parseFloat(pidResponse[5]));
					setHeaterKD(Float.parseFloat(pidResponse[6]));
					
					feedback = "Cpl";
				}else{
					feedback = "Err";
				}
				Thread.sleep(sleepmSec);
				
				msg2 = sendCmd("rlab://REQV?addr=03");
				logger.info("Primitive Controller HW Interface - recvmsg" + msg2);
				if (msg2.startsWith("Cpl")){
					pidResponse = msg2.substring(4).trim().split(";",7);
					setCoolerPV(Float.parseFloat(pidResponse[0]));
					setCoolerSP(Float.parseFloat(pidResponse[1]));
					setCoolerOP(Float.parseFloat(pidResponse[2]));
					setCoolerMode(Integer.parseInt(pidResponse[3]));
					setCoolerKP(Float.parseFloat(pidResponse[4]));
					setCoolerKI(Float.parseFloat(pidResponse[5]));
					setCoolerKD(Float.parseFloat(pidResponse[6]));
					
					feedback = "Cpl";
				}else{
					feedback = "Err";
				}
				Thread.sleep(sleepmSec);
				
				msg3 = sendCmd("rlab://REQV?addr=04");
				logger.info("Primitive Controller HW Interface - recvmsg" + msg3);
				Thread.sleep(sleepmSec);
        }
        catch (Exception e) {
            System.err.println(e.toString());
            logger.info("Primitive Controller HW Interface - parsing data error");
            logger.info("Primitive Controller HW Interface - " + e.toString());
        }
		return feedback;
	}
	
	private synchronized void increaseErrCount(){
		// cap the max error count logged as 500
		// to differentiate from the initialization state (999)
		if (intErrCount<500){
			intErrCount++;
		}
	}
	
	// flags handling
	private synchronized boolean getHeaterModeFlag(){ return heaterModeFlag;}
	private synchronized boolean getCoolerModeFlag(){ return coolerModeFlag;}
	private synchronized boolean getHeaterSPFlag(){ return heaterSPFlag;}
	private synchronized boolean getHeaterOPFlag(){ return heaterOPFlag;}
	private synchronized boolean getCoolerSPFlag(){ return coolerSPFlag;}
	private synchronized boolean getCoolerOPFlag(){ return coolerOPFlag;}
	private synchronized boolean getHeaterKPFlag(){ return heaterKPFlag;}
	private synchronized boolean getHeaterKIFlag(){ return heaterKIFlag;}
	private synchronized boolean getHeaterKDFlag(){ return heaterKDFlag;}
	private synchronized boolean getCoolerKPFlag(){ return coolerKPFlag;}
	private synchronized boolean getCoolerKIFlag(){ return coolerKIFlag;}
	private synchronized boolean getCoolerKDFlag(){ return coolerKDFlag;}
	private synchronized boolean getProcModeFlag(){return procModeFlag;}
	
	public synchronized float getHeaterSPBuffer() { return heaterSPBuffer;}
	public synchronized float getHeaterOPBuffer() { return heaterOPBuffer;}
	public synchronized float getCoolerSPBuffer() { return coolerSPBuffer;}
	public synchronized float getCoolerOPBuffer() { return coolerOPBuffer;}
	public synchronized float getHeaterKPBuffer() { return heaterKPBuffer;}
	public synchronized float getHeaterKIBuffer() { return heaterKIBuffer;}
	public synchronized float getHeaterKDBuffer() { return heaterKDBuffer;}
	public synchronized float getCoolerKPBuffer() { return coolerKPBuffer;}
	public synchronized float getCoolerKIBuffer() { return coolerKIBuffer;}
	public synchronized float getCoolerKDBuffer() { return coolerKDBuffer;}
	public synchronized int getHeaterModeBuffer() { return heaterModeBuffer;}
	public synchronized int getCoolerModeBuffer() { return coolerModeBuffer;}
	public synchronized int getProcModeBuffer() { return procModeBuffer;}
	
	private synchronized void offHeaterModeFlag(){  heaterModeFlag = false;}
	private synchronized void offCoolerModeFlag(){  coolerModeFlag = false;}
	private synchronized void offHeaterSPFlag(){  heaterSPFlag = false;}
	private synchronized void offHeaterOPFlag(){  heaterOPFlag = false;}
	private synchronized void offCoolerSPFlag(){  coolerSPFlag = false;}
	private synchronized void offCoolerOPFlag(){  coolerOPFlag = false;}
	private synchronized void offHeaterKPFlag(){  heaterKPFlag = false;}
	private synchronized void offHeaterKIFlag(){  heaterKIFlag = false;}
	private synchronized void offHeaterKDFlag(){  heaterKDFlag = false;}
	private synchronized void offCoolerKPFlag(){  coolerKPFlag = false;}
	private synchronized void offCoolerKIFlag(){  coolerKIFlag = false;}
	private synchronized void offCoolerKDFlag(){  coolerKDFlag = false;}
	private synchronized void offProcModeFlag(){  procModeFlag = false;}
	
	private synchronized void setProcMode(int val){
		intProcessMode = val;
	}
	private synchronized void setProcModeFlag(){	procModeFlag = true;}
	
	private synchronized void setResendTimer(){	resendTimer = System.nanoTime();}
	private synchronized long getResendTimer(){
		return (System.nanoTime()-resendTimer)/1000000; //milli-second
	}
	
	private synchronized void setSensorAddr(int sensornum, String addr){
		sensorAddr[sensornum-1] = addr;
	}
	private synchronized String getSensorAddr(int index){
		return sensorAddr[index];
	}
	
	private synchronized void setRigInfo(String info){
		rigInfo = info;
	}
	
	// Push new values to arduino
	private String pushHeaterSP(){
		String response = "";
		try{
			String msg = String.format("%.2f",getHeaterSPBuffer());
            response = sendCmd("rlab://SETV?addr=12&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	
	private String pushHeaterOP(){
		String response = "";
		try{
			String msg = String.format("%.2f",getHeaterOPBuffer());
            response = sendCmd("rlab://SETV?addr=13&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushCoolerSP(){
		String response = "";
		try{
			String msg = String.format("%.2f",getCoolerSPBuffer());
            response = sendCmd("rlab://SETV?addr=22&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	
	private String pushCoolerOP(){
		String response = "";
		try{
			String msg = String.format("%.2f",getCoolerOPBuffer());
            response = sendCmd("rlab://SETV?addr=23&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushHeaterKP(){
		String response = "";
		try{
			String msg = String.format("%.4f",getHeaterKPBuffer());
            response = sendCmd("rlab://SETV?addr=14&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushHeaterKI(){
		String response = "";
		try{
			String msg = String.format("%.4f",getHeaterKIBuffer());
            response = sendCmd("rlab://SETV?addr=15&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushHeaterKD(){
		String response = "";
		try{
			String msg = String.format("%.4f",getHeaterKDBuffer());
            response = sendCmd("rlab://SETV?addr=16&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushCoolerKP(){
		String response = "";
		try{
			String msg = String.format("%.4f",getCoolerKPBuffer());
            response = sendCmd("rlab://SETV?addr=24&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushCoolerKI(){
		String response = "";
		try{
			String msg = String.format("%.4f",getCoolerKIBuffer());
            response = sendCmd("rlab://SETV?addr=25&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushCoolerKD(){
		String response = "";
		try{
			String msg = String.format("%.4f",getCoolerKDBuffer());
            response = sendCmd("rlab://SETV?addr=26&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushHeaterMode(){
		String response = "";
		try{
			String msg = Integer.toString(getHeaterModeBuffer());
            response = sendCmd("rlab://SETV?addr=11&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushCoolerMode(){
		String response = "";
		try{
			String msg = Integer.toString(getCoolerModeBuffer());
            response = sendCmd("rlab://SETV?addr=21&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	private String pushProcMode(){
		String response = "";
		try{
			String msg = Integer.toString(getProcModeBuffer());
            response = sendCmd("rlab://SETV?addr=90&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	
	private String pushSensorAddr(int index){
		String response = "";
		try{
			String msg = getSensorAddr(index);
            response = sendCmd("rlab://SETA?addr=" + String.format("%02d",index+1)+ "&val=" + msg);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
		
		return response;
	}
	
	//------------------------------------------------------------------
    // Serial port management functions
	//------------------------------------------------------------------
	
    public void serialInitialise() {
    	logger.info("Primitive Controller HW Interface - beginning serial initialisation");
		CommPortIdentifier portId = null;
    	logger.info("Primitive Controller HW Interface - looking for ports");
    	try{
    		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
    		logger.info("Primitive Controller HW Interface - finished looking for ports");
    		//First, Find an instance of serial port as set in PORT_NAMES.
			while (portEnum.hasMoreElements()) {
				CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
				if (currPortId.getName().equals(portNameFromConfig)){
					portId = currPortId;
				}
//				for (String portName : PORT_NAMES) {
//					if (currPortId.getName().equals(portName)) {
//						portId = currPortId;
//						break;
//					}
//				}
			}
		}catch (Exception e) {
			logger.info("Primitive Controller HW Interface - Exception - Could not find port.");
		}
    	
    	logger.info("Primitive Controller HW Interface - finished finding port instance");
		if (portId == null) {
			logger.info("Primitive Controller HW Interface - Could not find COM port.");
			return;
		}
		logger.info("Primitive Controller HW Interface - Serial port ID found");

		try {
			// open serial port, and use class name for the appName.
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIME_OUT);

			// set port parameters
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);

			// open the streams
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();

            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);

            logger.info("Primitive Controller HW Interface - Serial ports opened ");
		} catch (Exception e) {
			System.err.println(e.toString());
		}
	}

    public synchronized void serialEvent(SerialPortEvent oEvent) {
        if (oEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
            	/*
                inputLine=input.readLine();
                setDataReady(true);
                logger.info("Primitive Controller HW Interface - data received ");*/
            } catch (Exception e) {
                System.err.println(e.toString());
                logger.info("Primitive Controller HW Interface - data exception ");
            }
        }
    }

    public synchronized void serialClose() {
		if (serialPort != null) {
			serialPort.removeEventListener();
			serialPort.close();
		}
	}

    private void send(int b){
        try{
            output.write(b);
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
    }
    
    private int read(){
        int b = 0;

        try{
            b = (int)input.read();
        }
        catch (Exception e) {
            System.err.println(e.toString());
        }
        return b;
    }

    private synchronized boolean getDataReady() {
    	return inputLineComplete;
    }
    
    private synchronized void setDataReady(boolean val) {
    	inputLineComplete = val;
    }
    
    private String sendCmd(String cmd) {
		String response="";
	
		try {
			// ignore any input that has already been read in
			setDataReady(false); 
			
			// then send the current command
			while (input.ready()) input.read();  // clear input buffer

			logger.info("Primitive Controller HW Interface - Arduino Cmd=" + cmd);
			output.write(cmd.getBytes());
			output.write('\n');
			output.flush();

			Thread.sleep(50);
			
			int currentChar;
			int attempts=0;
			while (true) {
				currentChar = read();

				if (currentChar==-1 ){
					Thread.sleep(50);
					attempts++;
				}
				else if (currentChar == '\n') break;
				else if (currentChar != 0)response += (char) currentChar;
				if (attempts>60) {
					response = "Err:Timeout";
					break;
				}
			}
			
						
			logger.info("Primitive Controller HW Interface - Arduino Rsp=" + response);
			if (response.startsWith("Err")){
				increaseErrCount();
			}else if (response.startsWith("Cpl")){
				resetErrCount();
			}
			
		 } catch (Exception e) {
			 logger.error("Primitive Controller HW Interface - could not write to port");
             System.err.println(e.toString());
		 }
		return response;
	}
	
	//------------------------------------------------------------------
    // Public access functions
	//------------------------------------------------------------------
	// Status Check
	public synchronized boolean getRunning(){ return this.running; }
	public synchronized int getErrorCount(){ return intErrCount;}
	
	// Command to hardware - status
	public synchronized void stopRunning() { this.running = false ;	}
	public synchronized void resetErrCount(){intErrCount=0;}
	
	// Present Value Retrieve (from local buffer)
    // Get the current temperature freshness
	public synchronized void setFreshness(long newFresh) {
		freshnessTimer = newFresh;
		}
	public synchronized long getFreshness(){
		if (freshnessTimer == -1) return -1;
		else return (System.nanoTime() - freshnessTimer)/1000000;
		}
	
	// PID information for Heating end
	public synchronized float getHeaterPV(){return flHeaterPidPV;}
	public synchronized void setHeaterPV(float val){
		flHeaterPidPV = val;
	}
	public synchronized float getHeaterSP(){return flHeaterPidSP;}
	public synchronized void setHeaterSP(float val){
		flHeaterPidSP = val;
	}
	public synchronized float getHeaterOP(){return flHeaterPidOP;}
	public synchronized void setHeaterOP(float val){
		flHeaterPidOP = val;
	}
	public synchronized int getHeaterMode(){return intHeaterPidMode;}
	public synchronized void setHeaterMode(int val){
		intHeaterPidMode = val;
	}
	// PID tuning for Heating end
	public synchronized float getHeaterKP(){return flHeaterPidKP;}
	public synchronized void setHeaterKP(float val){
		flHeaterPidKP = val;
	}
	public synchronized float getHeaterKI(){return flHeaterPidKI;}
	public synchronized void setHeaterKI(float val){
		flHeaterPidKI = val;
	}
	public synchronized float getHeaterKD(){return flHeaterPidKD;}
	public synchronized void setHeaterKD(float val){
		flHeaterPidKD = val;
	}
	
	// PID information for Cooling end
	public synchronized float getCoolerPV(){return flCoolerPidPV;}
	public synchronized void setCoolerPV(float val){
		flCoolerPidPV = val;
	}
	public synchronized float getCoolerSP(){return flCoolerPidSP;}
	public synchronized void setCoolerSP(float val){
		flCoolerPidSP = val;
	}
	public synchronized float getCoolerOP(){return flCoolerPidOP;}
	public synchronized void setCoolerOP(float val){
		flCoolerPidOP = val;
	}
	public synchronized int getCoolerMode(){return intCoolerPidMode;}
	public synchronized void setCoolerMode(int val){
		intCoolerPidMode = val;
	}
	// PID tuning for Cooling end
	public synchronized float getCoolerKP(){return flCoolerPidKP;}
	public synchronized void setCoolerKP(float val){
		flCoolerPidKP = val;
	}
	public synchronized float getCoolerKI(){return flCoolerPidKI;}
	public synchronized void setCoolerKI(float val){
		flCoolerPidKI = val;
	}
	public synchronized float getCoolerKD(){return flCoolerPidKD;}
	public synchronized void setCoolerKD(float val){
		flCoolerPidKD = val;
	}
		
	public synchronized float getCurTemp(int index){return flCurTemp [index-1];}
	public synchronized void setCurTemp(float val, int index){
		flCurTemp [index] = val;
	}
	public synchronized int getTempHealth(int index){return intTempHealth [index-1];}
	public synchronized void setTempHealth(int val, int index){
		intTempHealth [index] = val;
	}
		
	// Command to hardware - data
	public synchronized void setHeaterSPBuffer(float val) {
		heaterSPBuffer = Math.min(MAX_TEMP, Math.max(MIN_TEMP,val));
		heaterSPFlag = true;
	}
	public synchronized void setHeaterOPBuffer(float val) {
		heaterOPBuffer = Math.min(100.0f, Math.max(0.0f,val));
		heaterOPFlag = true;
	}
	public synchronized void setCoolerSPBuffer(float val) {
		coolerSPBuffer = Math.min(MAX_TEMP, Math.max(MIN_TEMP,val));
		coolerSPFlag = true;
	}
	public synchronized void setCoolerOPBuffer(float val) {
		coolerOPBuffer = Math.min(100.0f, Math.max(0.0f,val));
		coolerOPFlag = true;
	}
	public synchronized void setHeaterKPBuffer(float val) {
		heaterKPBuffer = val;
		heaterKPFlag = true;
	}
	public synchronized void setHeaterKIBuffer(float val) {
		heaterKIBuffer = val;
		heaterKIFlag = true;
	}
	public synchronized void setHeaterKDBuffer(float val) {
		heaterKDBuffer = val;
		heaterKDFlag = true;
	}
	public synchronized void setCoolerKPBuffer(float val) {
		coolerKPBuffer = val;
		coolerKPFlag = true;
	}
	public synchronized void setCoolerKIBuffer(float val) {
		coolerKIBuffer = val;
		coolerKIFlag = true;
	}
	public synchronized void setCoolerKDBuffer(float val) {
		coolerKDBuffer = val;
		coolerKDFlag = true;
	}
	public synchronized void setHeaterModeBuffer(int val) {
		heaterModeBuffer = val;
		heaterModeFlag = true;
	}
	public synchronized void setCoolerModeBuffer(int val) {
		coolerModeBuffer = val;
		coolerModeFlag = true;
	}
	public synchronized void setProcModeBuffer(int val){
		procModeBuffer = val;
		procModeFlag = true;
	}
	public synchronized int getProcMode(){return intProcessMode;}
	public synchronized String getRigInfo(){return rigInfo;}
}