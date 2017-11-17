package com.ibm.ia.monitoring;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

public class DSIMonitoring {
        @Parameter(description = "server jmx urls")
        private List<String> hostnames;
        
        @Parameter(names = { "-h", "--help" }, help = true)
        private boolean help;

        @Parameter(names = { "-i", "--interval" }, help = true)
        private int interval;
        
        @Parameter(names = { "-a", "--all" }, help = true)
        private boolean all = false;
        
        @Parameter(names = { "-u", "--username" }, help = true)
        private String userName = "tester";
        
        @Parameter(names = { "-p", "--password" }, help = true)
        private String password = "tester";
        
        @Parameter(names = { "-s", "--shutdown" }, help = true)
        private boolean shutdown = false;

        @Parameter(names = { "-d", "--directory" }, help = true)
        private String directory;
        
        private static final String CSV_SEPARATOR = ",";
        
        public void run(String[] args) throws InterruptedException, KeyManagementException, NoSuchAlgorithmException, MalformedObjectNameException, IOException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException {
                JCommander jc;

                disableCertif();
                
                jc = new JCommander(this, args);

                if (help) {
                        jc.usage();
                        System.exit(0);
                }

                if (args == null || args.length == 0) {
                        jc.usage();
                        System.exit(1);
                }
                
                if (shutdown) {
                        for (String h: hostnames)
                                shutdown(h);
                } else {
	            		System.out.println("Start DSI monitoring (interval=" + interval + "s)");
	                    if (directory != null) {
	                    	initOutputDir();
	                    }
                        if (interval <= 0)                 
                                for (String h: hostnames)
                                        try {
                                                run(h);
                                        } catch (IOException 
                                                 | MBeanException
                                                 | InstanceNotFoundException
                                                 | IntrospectionException
                                                 | MalformedObjectNameException
                                                 | AttributeNotFoundException
                                                 | ReflectionException e) {
                                                e.printStackTrace();
                                        }                                        
                        else
                                while (true) {
                                        for (String h: hostnames)
                                                try {
                                                        run(h);
                                                } catch (IOException 
                                                         | MBeanException
                                                         | InstanceNotFoundException
                                                         | IntrospectionException
                                                         | MalformedObjectNameException
                                                         | AttributeNotFoundException
                                                         | ReflectionException e) {
                                                        System.err.println(h + " " + e.getMessage());
                                                }

                                        Thread.sleep(interval * 1000);
                                }
                }
        }

		private void shutdown(String h) throws IOException, MalformedObjectNameException, InstanceNotFoundException, IntrospectionException, ReflectionException, MBeanException {
                MBeanServerConnection conn;
                
                System.out.println("Shutdown: " + h);
                
                conn = getConnection(h);
                
                conn.invoke(new ObjectName("com.ibm.ia:type=ServerAdmin"), "shutdown", null, null);
        }
        
        private MBeanServerConnection getConnection(String h) throws IOException {
                String strURL;
                JMXConnector jmxc;
                JMXServiceURL url;
                HashMap<String, Object> env;
                
                strURL = "service:jmx:rest://" + h + "/IBMJMXConnectorREST";
                
                url = new JMXServiceURL(strURL);
                
                env = new HashMap<String, Object>();
                env.put(JMXConnector.CREDENTIALS, new String[] { userName, password });
                env.put("com.ibm.ws.jmx.connector.client.disableURLHostnameVerification", Boolean.TRUE);
                
                jmxc = JMXConnectorFactory.connect(url, env);
                
                return jmxc.getMBeanServerConnection();
        }
        
        private void run(String h) throws IOException, InstanceNotFoundException, IntrospectionException, MalformedObjectNameException, ReflectionException, AttributeNotFoundException, MBeanException {
                MBeanServerConnection mbsc;
                
                mbsc = getConnection(h);
                                
                if (all) {
                        display(h, mbsc);
                } else {
                        display(h, mbsc, new ObjectName("java.nio:type=BufferPool,name=direct"), "Count");
                        display(h, mbsc, new ObjectName("java.nio:type=BufferPool,name=direct"), "TotalCapacity");
                        display(h, mbsc, new ObjectName("java.nio:type=BufferPool,name=direct"), "MemoryUsed");
                
                        display(h, mbsc, new ObjectName("java.lang:type=Memory"), "HeapMemoryUsage");
                        display(h, mbsc, new ObjectName("java.lang:type=Memory"), "NonHeapMemoryUsage");
                }
        }
        
        private static void disableCertif() throws NoSuchAlgorithmException, KeyManagementException {
                TrustManager[] mgrs;
                SSLContext sc;
                
                mgrs = new TrustManager[]{
                                new X509TrustManager() {
                                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                                return null;
                                        }

                                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                                        }

                                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                                        }
                                }
                };

                sc = SSLContext.getInstance("SSL");
                sc.init(null, mgrs, new java.security.SecureRandom());
                SSLContext.setDefault(sc);
        }
        
        private void display(String host, MBeanServerConnection mbsc) throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException, AttributeNotFoundException, MBeanException {
              Set<ObjectName> names;
              ObjectInstance o;
              MBeanInfo info;
              MBeanAttributeInfo[] attInfos;
                        
              names = mbsc.queryNames(null, null);
              
              for(ObjectName n: names) {
                      o = mbsc.getObjectInstance(n);
                      info = mbsc.getMBeanInfo(n);
                      attInfos = info.getAttributes();
                      for (MBeanAttributeInfo att: attInfos)
                              try {
                            	  if( att.isReadable())	
                                  	display(host, mbsc, o.getObjectName(), att.getName());
                            	  else 
                                   	System.err.println("Cannot read " + o.getObjectName() + "#" + att.getName() + " isReadable=false");                    		  
                              } catch(RuntimeMBeanException | IOException e) {
                            	  System.err.println("Cannot retrieve " + o.getObjectName() + "#" + att.getName() + " " + e.getMessage());
                              }
              }
                
        }
        
        private void display(String host,
                             MBeanServerConnection mbsc,
                             ObjectName name,
                             String att) throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, IOException {
                Object o;
                MemoryUsage mu;
                CompositeData cd;
                                
                o = mbsc.getAttribute(name, att);
                
                if (o instanceof CompositeData) {
                        cd = (CompositeDataSupport)o;
                        if (cd.getCompositeType().getTypeName().equals(MemoryUsage.class.getName())) {
                                mu = MemoryUsage.from(cd);
                                o = "Initial" + CSV_SEPARATOR + mu.getInit() + CSV_SEPARATOR + "Max" + CSV_SEPARATOR + mu.getMax() +
                                		CSV_SEPARATOR + "Used" + CSV_SEPARATOR + mu.getUsed() + CSV_SEPARATOR + "Committed" + CSV_SEPARATOR + mu.getCommitted();
                        }
                }
                
                
                if (o==null) 
                	o = new String("null");
                
                writeOutput(host, name.getCanonicalName(), att, o.toString());
        }
        
        private void writeOutput(String host, String canonicalName, String att, String value) throws IOException {        	
            String dataType = CSV_SEPARATOR + canonicalName.replaceAll("\\,",  "#") + "#" + att;
            dataType = dataType.substring(dataType.indexOf("type=")+"type=".length());
            host = host.replaceAll("\\:",  "-");

                if (directory == null)
                        System.out.println(LocalDateTime.now() + CSV_SEPARATOR + host + CSV_SEPARATOR + dataType
                                        + CSV_SEPARATOR + value);
                else
                        writeToFile(host, dataType, value);
        }

        private void writeToFile(String host, String dataType, String value) throws IOException {

                String csvFileName = dataType + "-" + host.replaceAll("\\.", "_").replaceAll(":", "-") + ".csv";
                String csvFilePath = directory + File.separator + csvFileName;

                try (FileWriter fw = new FileWriter(csvFilePath, true)) {
                        fw.write(LocalDateTime.now() + CSV_SEPARATOR + value + "\n");
                }
        }
      
        private void initOutputDir() {
                File outputDir = new File(directory);
                if (outputDir.exists()) {
                        System.out.println("Delete directory " + outputDir.getAbsolutePath());
                        if (!deleteDirectory(outputDir))
                                throw new RuntimeException("Cannot delete directory " + directory);
                }

                System.out.println("Create directory " + outputDir.getAbsolutePath()
                                + " for storing DSIMonitoring csv data");
                if (!outputDir.mkdir())
                        throw new RuntimeException("Cannot create directory " + directory);
        }
		
        private boolean deleteDirectory(File dir) {
                File[] allFiles = dir.listFiles();
                if (allFiles == null)
                        return true;
                for (int i = 0; i < allFiles.length; i++) {
                        if (allFiles[i].isDirectory()) {
                                deleteDirectory(allFiles[i]);
                        } else {
                                if (!allFiles[i].delete())
                                        throw new RuntimeException(
                                                        "Cannot delete file " + allFiles[i].getAbsolutePath());
                        }
                }
                return (dir.delete());
        }
	       
        public static void main(String[] args) throws Exception {
                new DSIMonitoring().run(args);
        }
}