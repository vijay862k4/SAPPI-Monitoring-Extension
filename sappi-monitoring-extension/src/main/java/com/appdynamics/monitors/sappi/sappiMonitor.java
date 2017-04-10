package com.appdynamics.monitors.sappi;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import com.google.common.base.Strings;
import com.appdynamics.monitors.sappi.config.Configuration;
//import com.appdynamics.extensions.PathResolver;
import com.appdynamics.monitors.sappi.StatsCollector.PiStatsCollector;

public class sappiMonitor extends AManagedMonitor{
	private static final Logger logger = Logger.getLogger(sappiMonitor.class);
    public static final String METRIC_SEPARATOR = "|";
    private static final String FILE_NAME = "monitors/sappiMonitor/config.yml";
    //private static final String FILE_NAME= "C:\\Vijay\\eclipse\\sappi-monitoring-extension\\src\\main\\resources\\config.yml";
    public sappiMonitor() {
        printVersion(true);
    }

    private void printVersion(boolean toConsole) {
        String details = sappiMonitor.class.getPackage().getImplementationTitle();
        String msg = "Using Monitor Version [" + details + "]";
        logger.info(msg);
        if (toConsole) {
            System.out.println(msg);
        }
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {

        printVersion(false);
        if (taskArgs != null) {
            logger.info("Starting the PI Monitoring task.");
            Configuration config = null;
			try {
            	Yaml yaml=new Yaml();
            	File configvalue=new File(taskArgs.get("config-file"));
            	InputStream in=null;
            	try{ in=new FileInputStream(configvalue);
            	config=yaml.loadAs(in, Configuration.class);
            	}catch(Exception e){logger.error("Check if config.yml exists"+e);}
               // Configuration config = (Configuration) YmlReader.readFromFile(configFilename, Configuration.class);
                Map<String, String> metrics = populateStats(config);
                //metric overrides;
                printStats(config,metrics);
                logger.info("Completed the SAP PI Monitoring Task successfully");
                return new TaskOutput("SAP PI Monitor executed successfully");
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("SAP Monitor completed with failures");
    }


	private void printStats(Configuration config,Map<String, String> metrics) {
		Set<Map.Entry<String, String>> set =metrics.entrySet();
		MetricWriter metricWriter;
		try{
		for(Map.Entry<String, String> entry:set){
		metricWriter=getMetricWriter(config.getMetricPathPrefix()+METRIC_SEPARATOR+entry.getKey(),MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
        metricWriter.printMetric(entry.getValue());
        }
		}catch(Exception e){logger.error("Error sending metrics to controller"+e);
		}
	}

	private Map<String, String> populateStats(Configuration config) throws TaskExecutionException {
    	 Map<String, String> stats = new HashMap<String, String>();
         PiStatsCollector piStatsCollector = new PiStatsCollector();
         Map<String, String> piServiceStats = null;
		try {
			
			piServiceStats = piStatsCollector.collect(config);
			stats.putAll(piServiceStats);
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		return stats;
	}
	private static String getConfigFilename(String filename) throws UnsupportedEncodingException {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        //File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String path=AManagedMonitor.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath=URLDecoder.decode(path,"UTF-8");
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = decodedPath + File.separator + filename;
            logger.info("Config Path"+decodedPath);
        }
        return configFileName;
    }
	
	public static void main(String args[]) throws TaskExecutionException, UnsupportedEncodingException
	{
		sappiMonitor pi=new sappiMonitor();
        Map<String, String> argsMap = new HashMap<String, String>();
        String filename=getConfigFilename(FILE_NAME);
        argsMap.put("config-file", filename);
        pi.execute(argsMap, null);
		
   }	
}


