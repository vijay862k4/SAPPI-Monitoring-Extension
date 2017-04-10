package com.appdynamics.monitors.sappi.StatsCollector;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import com.appdynamics.monitors.sappi.config.Configuration;
import com.google.common.net.UrlEscapers;

public class PiStatsCollector {
private static final Logger logger = Logger.getLogger(PiStatsCollector.class);
private static HashMap<String,String> hMap=new HashMap<String, String>();
private static int snctr = 0;
private static int sictr = 0;

/*Makes a http request to the PI host and passes the response object to DOM parsing logic*/
public Map<String, String> collect(Configuration config) throws UnsupportedOperationException, SAXException 
	{	
		HashMap<String,String> piStats=new HashMap<String, String>();
		HashMap<String, Integer>cMap=new HashMap<String, Integer>();
		StringBuilder result = null;
		//String configFilename="C://Vijay//eclipse//sappi-monitoring-extension//src//main//resources//config.yml";
		//Configuration config = YmlReader.readFromFile(configFilename, Configuration.class);
		Document formattedDocument;
		NodeList RowList = null;
	    BufferedReader rd;   
		HttpResponse response=querystats(config);
        if (response.getStatusLine().getStatusCode()<200||response.getStatusLine().getStatusCode()>=400)
		{
        	logger.error("Recieved a bad response Code : "+ response.getStatusLine().getStatusCode()+"Please check connection settings");
		}
        else 
        	logger.info("Response Code"+"\t"+response.getStatusLine().getStatusCode()+"\t"+"Received");
			try {
				rd = new BufferedReader(
				new InputStreamReader(response.getEntity().getContent()));
                result = new StringBuilder();
		        String line = "";
				while ((line = rd.readLine()) != null) {
				result.append(line);
				} 
			}catch (IOException e) {
				logger.error("IO Exception:"+e);
			}
			 
		if (logger.isDebugEnabled()) {
		            logger.debug("Complete Response content"+"\n"+result);
				}   
    formattedDocument = format(result);
    cMap=buildMetricIndex(formattedDocument);
	Set<Entry<String, Integer>> keys=cMap.entrySet();
	Iterator<Entry<String, Integer>> iterator=keys.iterator();
	if (logger.isDebugEnabled()){
	logger.debug("----------Metric Reference Index--------\n");
	while(iterator.hasNext())
	{
		Entry<String, Integer> mentry=iterator.next();
		logger.debug("Key is"+mentry.getKey()+"and index value is "+mentry.getValue());
    }
	}
	if (responseValidation(formattedDocument))
	{
	RowList= formattedDocument.getElementsByTagName("Row");
	logger.info("Total Row Lists to Parse:"+"\t"+RowList.getLength());
	for (int temp=0;temp<RowList.getLength();temp++)
	{
		Node nNode=RowList.item(temp);
		if (nNode.getNodeType()==Node.ELEMENT_NODE)
		{
			Element eElement=(Element)nNode;
			NodeList cList=eElement.getElementsByTagName("Entry");
			piStats=process(cList,cMap);
			if (logger.isDebugEnabled()){
				printMap(piStats);
				}
	     }
		
	  }
	}
	logger.info("Total Metrics Retrieved:"+piStats.size());
	return piStats;		
}
private static void printMap(HashMap<String, String> testMap) {
	
	Set<Entry<String, String>> keys=testMap.entrySet();
	Iterator<Entry<String, String>> iterator=keys.iterator();
 	while(iterator.hasNext())
	{
		Entry<String, String> entry=iterator.next();
		logger.debug("Metric is"+entry.getKey()+"and value "+entry.getValue());
	}		
}
/* Response Document Validation*/  
private static boolean responseValidation(Document formattedDocument) {
	String piResponseCode = formattedDocument.getElementsByTagName("Code").item(0).getTextContent();
	if (piResponseCode.equals("OK"))
		{logger.debug("Response Validation Successfull");
		return true;}
	else 
		logger.debug(" Response Content :"+piResponseCode+" Please validate the parameters in the config.yml");
		return false;
	}
/* Logic to metric reference Index for querying key values*/
private static HashMap<String, Integer> buildMetricIndex(Document formattedDocument) {
	    
		HashMap<String, Integer>indexMap=new HashMap<String, Integer>();
	    NodeList ColumnList,columnNames;
		ColumnList=formattedDocument.getElementsByTagName("ColumnNames");
		//Get index locations of metric prefix to be sent 
	    for (int i=0 ;i<ColumnList.getLength();i++ )
		{
			Node cNode=ColumnList.item(i);
			if (logger.isDebugEnabled()) {
	            logger.debug("\n Current ColumnName Element:"+cNode.getNodeName()+"."+i);
		}   
	        Element cElement=(Element)cNode;
	        columnNames=cElement.getElementsByTagName("Column");
	        for (int j=0;j<columnNames.getLength();j++)
	        {
	            String SenderDetails =cElement.getElementsByTagName("Column").item(j).getTextContent();
	            if (SenderDetails.equalsIgnoreCase("Sender Component") || SenderDetails.equalsIgnoreCase("Sender Interface")|| SenderDetails.equalsIgnoreCase("Error")|| SenderDetails.equalsIgnoreCase("Scheduled")|| SenderDetails.equalsIgnoreCase("Successful")||SenderDetails.equalsIgnoreCase("Terminated With Error"))
	            indexMap.put(SenderDetails, j);
	        }
	     }
		return indexMap;
	}
/* Query PI host for metrics*/
private static HttpResponse querystats(Configuration config)
	{
		HttpResponse response = null;
	    SimpleDateFormat date=new SimpleDateFormat("yyyy-MM-dd HH:00:00.0");
        Date now=new Date();
		String current=date.format(now);
		String PrevHour=date.format(new Date(System.currentTimeMillis() - 3600*1000));
		try{
		String url = UrlEscapers.urlFragmentEscaper().escape(config.getProtocol()+"://"+config.getHost()+":"+config.getPort()+"/mdt/messageoverviewqueryservlet?component="+config.getComponent()+"&view="+config.getView()+"&begin="+PrevHour+"&end="+current+"&j_username="+config.getUsername()+"&j_password="+config.getPassword());
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(url);
		logger.info("Complete Request URI:"+request.getURI());
		if (logger.isDebugEnabled()) {
            logger.debug("Complete Request URI"+request.getURI());
		}
		request.addHeader("Authorization","Basic");	
		request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
	    logger.info("Querying :"+config.getHost()+"for metrics");
		response = client.execute(new HttpGet (url));
	}catch(ClientProtocolException e)
	{
		logger.error("Connection Exception , check host/port connection details"+e);
	}
    catch(IOException e)
	{
    	logger.error("IO Exception"+e);
	}
		return response;
 }
  /* Parses the nodeList containing Row entries to grab the key value pair for metrics to be sent*/
  private static HashMap<String, String> process(NodeList cList, HashMap<String, Integer> cMap) {
		Iterator<Entry<String, Integer>> entries=cMap.entrySet().iterator();
        String sn,si,err,sch,ss,te;
        sn=si=err=sch=ss=te=null;
		while(entries.hasNext())
		{ 
			Entry<String, Integer> entry=entries.next();
			String currentColumnElement=(String) entry.getKey();
			switch(currentColumnElement){
			case "Sender Component":
				 sn=retrieveKeyValue(cList,entry.getValue());
				 snctr++;
				break;
			case "Sender Interface":
				 si=retrieveKeyValue(cList,entry.getValue());
				 sictr++;
				break;
			case "Error":
				 err=retrieveKeyValue(cList,entry.getValue());
				break;
			case "Scheduled":
				 sch=retrieveKeyValue(cList,entry.getValue());
				break;
			case "Successful":
				 ss=retrieveKeyValue(cList,entry.getValue());
				break;
			case "Terminated with error":
				 te=retrieveKeyValue(cList,entry.getValue());
				break;
			default:break;
			}					
		}  String MetricPrefix=sn+"|"+si;
		hMap.put(MetricPrefix+"|"+"Error", err);
		hMap.put(MetricPrefix+"|"+"Success", ss);
		hMap.put(MetricPrefix+"|"+"Scheduled", sch);
		hMap.put(MetricPrefix+"|"+"Terminated", te);      	
		hMap=ValueConvert(hMap);
		return hMap;
	}
	/* The PI passes null strings as - which is casted to zero before being sent to the metric system*/
       private static HashMap<String, String> ValueConvert(HashMap<String, String> hMap) {
		Set<Entry<String, String>> hkeys=hMap.entrySet();
		Iterator<Entry<String, String>> iterator=hkeys.iterator();
		while(iterator.hasNext())
		{
			Entry<String, String> entry=iterator.next();
			if (entry.getValue().equals("-"))
				entry.setValue("0");
		}	
		return hMap;
	}
	private static String retrieveKeyValue(NodeList cList, Object value) {
		
		String str =cList.item((int) value).getTextContent();
		return str;
	}

	/* Convert http response object to DOM object*/
	private static Document format(StringBuilder result) throws UnsupportedOperationException {
	DocumentBuilderFactory fac=DocumentBuilderFactory.newInstance();
	DocumentBuilder builder;
	Document doc=null;
	try{
		if (logger.isDebugEnabled()) {
            logger.debug("XMl Response"+result);}
		builder=fac.newDocumentBuilder();
		ByteArrayInputStream input=new ByteArrayInputStream(result.toString().getBytes("UTF-8"));
		 doc =builder.parse(input);
	}catch(ParserConfigurationException e)
	{
		logger.error("Parser Error"+e);
	}catch (SAXException e){
		logger.error("SAXException "+e);
	}catch (IOException e)
	{
		logger.error("I/O Exception"+e);
	}
	return doc;
 }
}	


