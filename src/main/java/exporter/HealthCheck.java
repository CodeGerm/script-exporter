package main.java.exporter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.naming.spi.DirStateFactory.Result;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HealthCheck {
	final private String name;
	final private String script;
	final private long updateInterval;
	final private long timeoutLimit;
	private long lastCheckTime;
	private int healthCheckResult;
	public static final Logger logger =  LogManager.getLogger();
	
	public HealthCheck(String n, String s, String i, String t) {
		this.name = n;
		this.script = s;
		this.updateInterval = Integer.parseInt(i);
		this.timeoutLimit = Integer.parseInt(t);
		this.lastCheckTime = 0l;
		this.healthCheckResult = 1;
	}
	
	public String getName() {
		return this.name;
	}
	
	public synchronized int getResult() {
		//result out of date need update
		logger.debug("current: " + System.currentTimeMillis());
		logger.debug("lastCheckTime " + lastCheckTime);
		logger.debug("diff: " + (System.currentTimeMillis() - lastCheckTime));
		logger.debug("updateInterval " + updateInterval);
		if(System.currentTimeMillis() - lastCheckTime > updateInterval*1000) {
			logger.info("Updating result of " + this.name);
			updateResult();
		}
		logger.info(this.name + "'s result is:" + healthCheckResult);
		return this.healthCheckResult;
	}
	
	
	public synchronized void updateResult() {
    	File dir = null;
		try {
			dir = createDirectory(this.name);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
    	Runtime currRuntime = Runtime.getRuntime();
    	int exitStatus = 1;
    	if (dir != null) {
    		Process p;
    		try {
    			p = currRuntime.exec(this.script,null, dir);
    			p.waitFor(this.timeoutLimit, TimeUnit.SECONDS);
    			exitStatus = p.exitValue();
			} catch (InterruptedException e) {

				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				deleteDirectory(dir);
			}
		}
    	this.lastCheckTime = System.currentTimeMillis();
        this.healthCheckResult = exitStatus;
    }
	
	 private static File createDirectory(String context) throws InterruptedException {
	    	File directory = new File("/tmp/"+ context + "/" +Long.toString(System.nanoTime()));
	        if(directory.exists()){
	           Thread.sleep(2);
	           createDirectory(context);
	        }
	        logger.info("try to creating:" + directory.getAbsolutePath());
	        boolean successful = directory.mkdirs();
	        if (successful)
	        {
	          return directory;
	        }
	        else
	        {
	          logger.error("Failed trying to create the tmp directory");
	          return(null);
	        }
	    }

	    private static boolean deleteDirectory(File directory) {
	        if(directory.exists()){
	            File[] files = directory.listFiles();
	            if(null!=files){
	                for(int i=0; i<files.length; i++) {
	                    if(files[i].isDirectory()) {
	                        deleteDirectory(files[i]);
	                    }
	                    else {
	                        files[i].delete();
	                    }
	                }
	            }
	        }
	        return(directory.delete());
	    }
}
