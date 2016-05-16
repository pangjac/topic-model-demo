package cc.mallet.topics;

import cc.mallet.types.*;
import cc.mallet.util.MalletLogger;
import cc.mallet.util.Randoms;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.joda.time.DateTime;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import java.util.regex.*;

import java.util.Date;
import java.lang.*;
import java.text.SimpleDateFormat;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.FileWriter;
import java.io.IOException;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.api.services.gmail.Gmail;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.Properties;

public class GmailQuickstartV2 {
    /** Application name. */
    	private static final String APPLICATION_NAME =
        "Gmail API Java Quickstart";

    /** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(
        	System.getProperty("user.home"), ".credentials/gmail-java-quickstart.json");

    /** Global instance of the {@link FileDataStoreFactory}. */
    	private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    	private static final JsonFactory JSON_FACTORY =
        	JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    	private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/gmail-java-quickstart.json
     */
    	private static final List<String> SCOPES =
        	Arrays.asList(GmailScopes.GMAIL_READONLY);

    	static {
        	try {
            		HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            		DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        	} catch (Throwable t) {
            	t.printStackTrace();
            	System.exit(1);
        	}
    	}

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    	public static Credential authorize() throws IOException {
        // Load client secrets.
        	InputStream in =
            		GmailQuickstart.class.getResourceAsStream("/client_secret.json");
        	GoogleClientSecrets clientSecrets =
            		GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        	GoogleAuthorizationCodeFlow flow =
                	new GoogleAuthorizationCodeFlow.Builder(
                        	HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                	.setDataStoreFactory(DATA_STORE_FACTORY)
                	.setAccessType("offline")
                	.build();
		System.out.println(flow);
        	Credential credential = new AuthorizationCodeInstalledApp(
            		flow, new LocalServerReceiver()).authorize("user");
        	System.out.println(
                	"Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        	//Credentials saved to /Users/pangjac/.credentials/gmail-java-quickstart.json
        	return credential;
    	}

    /**
     * Build and return an authorized Gmail client service.
     * @return an authorized Gmail client service
     * @throws IOException
     */
    	public static Gmail getGmailService() throws IOException {
        	Credential credential = authorize();
        	return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                	.setApplicationName(APPLICATION_NAME)
                	.build();
    	}

    	// pangjac added : a method to merge two maps
        protected static Map mergeMaps(Map original, Map newMap){
        for (Object key: newMap.keySet()){
            //System.out.println(key);
            boolean bool = original.containsKey(key);
            //System.out.println(bool);
            if (bool){
                int sum = Integer.sum((Integer)original.get(key),(Integer)newMap.get(key));         
                original.put(key, sum);
            }else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }


    	public static void main(String[] args) throws IOException {
	
        // Build a new authorized API client service.
        	Gmail service = getGmailService();

        // Print the labels in the user's account.
        	String user = "me";
        	ListMessagesResponse response =
            		service.users().messages().list(user).setQ("is:read").execute();
       
        	List<Message> messages = new ArrayList<Message>();
        	while (response.getMessages() != null) {
        		    messages.addAll(response.getMessages());
            		if (response.getNextPageToken() != null) {
				try {
            				String pageToken = response.getNextPageToken();
        				response = service.users().messages().list(user).setQ("is:read").setPageToken(pageToken).execute();
				}catch(Exception e){System.out.println(e);}
            		} else {
            			break;
            		}
        	}
		
		System.out.println("### Start downloading emails");

		//Runtime rt = Runtime.getRuntime();

		try{
			   	System.out.println("Creating 'mkdir data' Process...");
   				Process pr1 = Runtime.getRuntime().exec("mkdir data");
   				// cause this process to stop until process p is terminated
   				pr1.waitFor();
   				// when you manually close notepad.exe program will continue here
   				System.out.println("Waiting over.");
		} catch (Exception e){
			e.printStackTrace();
		}

		//Process pr1 = rt.exec("mkdir data");


        	for (Message message : messages) {
			try{ 
       				Message rawmessage = service.users().messages().get(user, message.getId()).execute();
				String m = rawmessage.toString(); 
				int pos = m.indexOf("internalDate");
				String internalDate = m.substring(pos + 15, pos + 28);
				List<MessagePart> parts = rawmessage.getPayload().getParts();
				if (parts != null) {
					for (MessagePart part : parts) {
						if (part.getMimeType().equals("text/plain")) {
							byte[] decoded = Base64.decodeBase64(part.getBody().getData());
							String data = new String(decoded, "UTF-8");
							int pos1 = data.indexOf("wrote:");
							if (pos1 != -1) {
								data = data.substring(0, pos1);
							}
							int pos2 = data.lastIndexOf("\n");
							if (pos2 != -1) {
								data = data.substring(0, pos2);
							}
							int pos3 = data.indexOf("—-");
							if (pos3 != -1) {
								data = data.substring(0, pos3);
							}
							data = data.replaceAll("&#x(.*);", "");
							data = data.replaceAll("<br>(.*)<br>", "");
							data = data.replaceAll("[(.*)]", "");
							data = data.replaceAll("<(.*)>", "");
							data = data.replaceAll("http(.*) ", "");
							data = data.replaceAll("http(.*)\n", "");
							if (data.length() > 10 && data.indexOf("http") == -1) {
								File writename = new File("data/" +internalDate + ".txt"); 		
								writename.createNewFile(); 
								BufferedWriter out = new BufferedWriter(new FileWriter(writename));
								out.write(data + "\n");
								out.close();
							}
						}
						break;
					}
				}
				else { 
					if (rawmessage.getPayload().getMimeType().equals("text/plain")){
            					byte[] decoded = Base64.decodeBase64(rawmessage.getPayload().getBody().getData());
            					String data = new String(decoded, "UTF-8");
						int pos1 = data.indexOf("wrote:");
						if (pos1 != -1) {
							data = data.substring(0, pos1);
						}
						int pos2 = data.lastIndexOf("\n");
						if (pos2 != -1) {
							data = data.substring(0, pos2);
						}
						int pos3 = data.indexOf("--\n\n");
						if (pos3 != -1) {
							data = data.substring(0, pos3);
						}
						data = data.replaceAll("&#x(.*);", "");
						data = data.replaceAll("<br>(.*)<br>", "");
						data = data.replaceAll("[(.*)]", "");
						data = data.replaceAll("<(.*)>", "");
						data = data.replaceAll("http(.*) ", "");
						data = data.replaceAll("http(.*)\n", "");
						if (data.length() > 10 && data.indexOf("http") == -1) {
							File writename = new File("data/" + internalDate + ".txt"); 
							writename.createNewFile(); 
							BufferedWriter out = new BufferedWriter(new FileWriter(writename));
							out.write(data + "\n");
							out.close();
						}
					}
				}
			}catch(Exception e){System.out.println(e);}	
        	}
		System.out.println("### End downloading emails ###");

		System.out.println("### Start import emails ###");

		System.out.println("Waiting for topic-input.mallet created ...");
		//Process pr2 = rt.exec("bin/mallet import-dir --input ./data  --output topic-input.mallet --keep-sequence --remove-stopwords --extra-stopwords stopwords.txt");
       	
		try{
			   	System.out.println("Creating 'topic-input.mallet' Process...");
   				Process pr2 = Runtime.getRuntime().exec("bin/mallet import-dir --input ./data  --output topic-input.mallet --keep-sequence --remove-stopwords --extra-stopwords stopwords.txt");
   				// cause this process to stop until process p is terminated
   				pr2.waitFor();
   				// when you manually close notepad.exe program will continue here
   				System.out.println("Waiting over.");
		} catch (Exception e){
			e.printStackTrace();
		}



       	System.out.println("topic-input.mallet done.");

		System.out.println("### End import emails ###");
		
		//Process pr3 = rt.exec("java -cp './class:./lib/*' cc.mallet.topics.SimpleTimeOverTopic /Users/liudanxiao/Desktop/WorkspaceJava/mallet/topic-input.mallet");
		//Process pr3 = rt.exec("java -cp './class:./lib/*' cc.mallet.topics.GmailQuickstart /Users/pangjac/Downloads/cm/topic-input.mallet")
		System.out.println("Number of emails " + messages.size());


		InstanceList training = InstanceList.load (new File(args[0]));

		long[] times = new long[training.size()];
		int i = 0;
		for (Instance instance : training) {
			
			String name = instance.getName().toString();
			times[i] = Long.valueOf(name.substring(name.length() - 17, name.length() - 4));
			i++;
			instance.setProperty("timestamp", Long.valueOf(name.substring(name.length() - 17, name.length() - 4)));
		}		

        int numTopics = args.length > 1 ? Integer.parseInt(args[1]) : 50;

        SimpleTimeOverTopic tot = new SimpleTimeOverTopic (numTopics, 50.0, 0.01, 0.5);

        tot.addInstances(training);
        tot.sample(50);

		System.out.println("Number of instances " + training.size());

        //global variant for every possible timestamp
        i = 0;
        
        HashMap<String, Map<String,Integer>> datesTopics = new HashMap<String, Map<String, Integer>>();

        for (TopicAssignment aData : tot.data) {

                LabelSequence topicSequence =
                        (LabelSequence) aData.topicSequence;
                long time = times[i];
                i ++;
                System.out.println(time); // print this email timestamp, i.e email txt name
                

                // convert timestamp to time
                Date d = new Date(time);
                SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
        		String date = DATE_FORMAT.format(d);

        		// for this timestamp, get dictionary of {topic: topic count}
        		Map<String,Integer> dateTopics = new HashMap<String,Integer>(); // for this "date", {topic# : topicCount}
                int[] topics = topicSequence.getFeatures();

                for(int topic: topics){
                	System.out.println(topic);
                	String key = Integer.toString(topic);
                	if (dateTopics.containsKey(key)){ // this topic has been existed
                		Integer value = dateTopics.get(key);
                		value ++;  
                		dateTopics.put(key,value);             		
                	} else{ // this word's topic is newly found
                		dateTopics.put(key,1);
                	}
                }

                //for a larger picture
                if (datesTopics.containsKey(date)){ // this "date" has been in datesTopics
                	// fetch this day's result
                	Map<String,Integer> existdateTopics = datesTopics.get(date);
                    //merge existdateTopics and dateTopics
                    Map<String, Integer> dateTopicsUpdated = mergeMaps(existdateTopics,dateTopics);
                    //update larger picture dictionary
                    datesTopics.put(date, dateTopicsUpdated);
                } else{ // this date has never been in larger picture datesTopics before
                    datesTopics.put(date, dateTopics);
                }

         }

         //convert the hashmap to json object
         JSONObject json = new JSONObject();
         json.putAll(datesTopics);
         System.out.printf( "JSON: %s", json.toString());
        
        // save json to local file
            try (FileWriter file = new FileWriter("/Users/pangjac/Downloads/cm/result.json")) {
            file.write(json.toJSONString());
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + json);
        }catch (Exception e){
            e.printStackTrace();
        }

		System.out.println("### End training model ###");
		String result = tot.topWords (10);
		File writename = new File("result.txt"); 
		writename.createNewFile(); 
		BufferedWriter out = new BufferedWriter(new FileWriter(writename));
		out.write(result);
		out.close();
		// Process pr5 = rt.exec("rm -rf ~/.credentials/gmail-java-quickstart.json");
		// Process pr = rt.exec("rm -rf data");
		try{
			   	System.out.println("Remove credentials Process...");
   				Process pr5 = Runtime.getRuntime().exec("rm -rf /Users/pangjac/.credentials/gmail-java-quickstart.json");
   				// cause this process to stop until process p is terminated
   				pr5.waitFor();
   				// when you manually close notepad.exe program will continue here
   				System.out.println("Waiting over.");

   				System.out.println("Remove data folder Process...");
   				Process pr = Runtime.getRuntime().exec("rm -rf data");
   				// cause this process to stop until process p is terminated
   				pr.waitFor();
   				// when you manually close notepad.exe program will continue here
   				System.out.println("Waiting over.");


		} catch (Exception e){
			e.printStackTrace();
		}

    	}

}
