package au.edu.usyd.eng.remotelabs.bartemperaturerig.primitive;

import au.edu.uts.eng.remotelabs.rigclient.rig.IRigControl.PrimitiveRequest;
import au.edu.uts.eng.remotelabs.rigclient.rig.IRigControl.PrimitiveResponse;
import au.edu.uts.eng.remotelabs.rigclient.rig.primitive.IPrimitiveController;
import au.edu.uts.eng.remotelabs.rigclient.util.ConfigFactory;
import au.edu.uts.eng.remotelabs.rigclient.util.IConfig;
import au.edu.uts.eng.remotelabs.rigclient.util.ILogger;
import au.edu.uts.eng.remotelabs.rigclient.util.LoggerFactory;

import gnu.io.CommPortIdentifier;

import java.io.*;
import java.util.Enumeration;


public class BarTemperatureRigController implements IPrimitiveController {

	   /** Logger. **/
    private ILogger logger;


       /** HW simulator or interface **/
    BarTemperatureHW btHW;

    
	@Override
    public boolean initController()
    {
        this.logger = LoggerFactory.getLoggerInstance();
        this.logger.info("Primitive Controller created");
        
        btHW = new BarTemperatureHW("HWThread-1");

		btHW.start();
		return true;
    }
    
    public PrimitiveResponse getFreshnessAction(PrimitiveRequest request) throws IOException
    {
        PrimitiveResponse response = new PrimitiveResponse();
        response.setSuccessful(true);
        response.addResult("Freshness",   		String.valueOf(btHW.getFreshness()));
        return response;
    }
    	
    	public PrimitiveResponse getValsAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);
            
         
            response.addResult("HeaterPV",   		String.valueOf(btHW.getHeaterPV()));
            response.addResult("HeaterSP",   		String.valueOf(btHW.getHeaterSP()));
            response.addResult("HeaterOP",   		String.valueOf(btHW.getHeaterOP()));
            response.addResult("HeaterMode",   		String.valueOf(btHW.getHeaterMode()));
            response.addResult("HeaterKP",   		String.valueOf(btHW.getHeaterKP()));
            response.addResult("HeaterKI",   		String.valueOf(btHW.getHeaterKI()));
            response.addResult("HeaterKD",   		String.valueOf(btHW.getHeaterKD()));
            response.addResult("CoolerPV",   		String.valueOf(btHW.getCoolerPV()));
            response.addResult("CoolerSP",   		String.valueOf(btHW.getCoolerSP()));
            response.addResult("CoolerOP",   		String.valueOf(btHW.getCoolerOP()));
            response.addResult("CoolerMode",   		String.valueOf(btHW.getCoolerMode()));
            response.addResult("CoolerKP",   		String.valueOf(btHW.getCoolerKP()));
            response.addResult("CoolerKI",   		String.valueOf(btHW.getCoolerKI()));
            response.addResult("CoolerKD",   		String.valueOf(btHW.getCoolerKD()));
            response.addResult("Freshness",   		String.valueOf(btHW.getFreshness()));
            response.addResult("Mode",   			String.valueOf(btHW.getProcMode()));
            response.addResult("RigInfo",			btHW.getRigInfo());
       
			for (int i=1;i<=11;i++){
				if (btHW.getTempHealth(i)==1){
					response.addResult("CurTemp"+String.valueOf(i),String.valueOf(btHW.getCurTemp(i)));
				}else{
					response.addResult("CurTemp"+String.valueOf(i),"Bad");
				}
				response.addResult("SensorHealth"+String.valueOf(i),String.valueOf(btHW.getTempHealth(i)));
			}
			if (btHW.getErrorCount()<=10) response.addResult("linksts", String.valueOf(1));
			else if (btHW.getErrorCount()== 999) response.addResult("linksts", String.valueOf(0));
			else response.addResult("linksts", String.valueOf(2));
			
            
            return response;
    }
	
	public PrimitiveResponse setHeaterSPAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("heaterSPtarget"));
            btHW.setHeaterSPBuffer(val);
            return response;
    }
	
	public PrimitiveResponse setHeaterOPAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("heaterOPtarget"));
            btHW.setHeaterOPBuffer(val);
            return response;
    }
	public PrimitiveResponse setHeaterKPAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("heaterKPtarget"));
            btHW.setHeaterKPBuffer(val);
            return response;
    }
	public PrimitiveResponse setHeaterKIAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("heaterKItarget"));
            btHW.setHeaterKIBuffer(val);
            return response;
    }
	public PrimitiveResponse setHeaterKDAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("heaterKDtarget"));
            btHW.setHeaterKDBuffer(val);
            return response;
    }
	public PrimitiveResponse setCoolerSPAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("coolerSPtarget"));
            btHW.setCoolerSPBuffer(val);
            return response;
    }
	
	public PrimitiveResponse setCoolerOPAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("coolerOPtarget"));
            btHW.setCoolerOPBuffer(val);
            return response;
    }
	public PrimitiveResponse setCoolerKPAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("coolerKPtarget"));
            btHW.setCoolerKPBuffer(val);
            return response;
    }
	public PrimitiveResponse setCoolerKIAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("coolerKItarget"));
            btHW.setCoolerKIBuffer(val);
            return response;
    }
	public PrimitiveResponse setCoolerKDAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            float val = Float.parseFloat(request.getParameters().get("coolerKDtarget"));
            btHW.setCoolerKDBuffer(val);
            return response;
    }
	
	public PrimitiveResponse setHeaterModeAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            int val = Integer.parseInt(request.getParameters().get("heaterModeTarget"));
            btHW.setHeaterModeBuffer(val);
            return response;
    }
	public PrimitiveResponse setCoolerModeAction(PrimitiveRequest request) throws IOException
    {
            PrimitiveResponse response = new PrimitiveResponse();
            response.setSuccessful(true);

            int val = Integer.parseInt(request.getParameters().get("coolerModeTarget"));
            btHW.setCoolerModeBuffer(val);
            return response;
    }
	
    


    @Override
    public boolean preRoute()
    {
    	return true;
    }
    
    @Override
    public boolean postRoute()
    {
    	
        return true;
    }

    @Override
    public void cleanup()
    {
    	// Notify Arduino to enter cleanup mode
        this.logger.info("Primitive Controller cleanup");
        btHW.setProcModeBuffer(5);
        
        // Wait until the command is properly accepted by Arduino 
        while (btHW.getProcMode()<5){
        	try{
        		this.logger.info("Primitive Controller cleanup-waiting for feedback");
            	Thread.sleep(500);
        	}catch(Exception e){
        		this.logger.info("Primitive Controller cleanup - Error " + e.toString());
        	}
        }
        
        // destroy HW
        this.logger.info("Primitive Controller cleanup-command out, terminate");
        btHW.stopRunning();
    }
}
