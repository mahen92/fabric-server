package mctraceaws1;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.ChaincodeID;
import org.hyperledger.fabric.sdk.Channel;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;


public class ApiServer {

	public static void main(String[] args)
	{
		try {
		String caUrl = "https://3.133.126.76:9059"; // ensure that port is of CA
		String caName = "rca-mclane";
		String pemStr = "-----BEGIN CERTIFICATE-----\r\n"
				+ "MIICGzCCAcKgAwIBAgIUKH2tOz4ySPmvkDdI6bZV8BFODb0wCgYIKoZIzj0EAwIw\r\n"
				+ "YjELMAkGA1UEBhMCVVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRQwEgYDVQQK\r\n"
				+ "EwtIeXBlcmxlZGdlcjEPMA0GA1UECxMGRmFicmljMRMwEQYDVQQDEwpyY2EtbWNs\r\n"
				+ "YW5lMB4XDTIxMDUwMjE1MjIwMFoXDTM2MDQyODE1MjIwMFowYjELMAkGA1UEBhMC\r\n"
				+ "VVMxFzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMRQwEgYDVQQKEwtIeXBlcmxlZGdl\r\n"
				+ "cjEPMA0GA1UECxMGRmFicmljMRMwEQYDVQQDEwpyY2EtbWNsYW5lMFkwEwYHKoZI\r\n"
				+ "zj0CAQYIKoZIzj0DAQcDQgAEzHI5SWc3r3DeeA1xFr4vj64v0wYzzVanxTZSAMdi\r\n"
				+ "ahOFJe/lMwYakdtS7JrDsu4AgUTJQxO6QiEDpI0Fxg0ylqNWMFQwDgYDVR0PAQH/\r\n"
				+ "BAQDAgEGMBIGA1UdEwEB/wQIMAYBAf8CAQEwHQYDVR0OBBYEFAg99sc195RhWGTi\r\n"
				+ "ZDD6UX0PXqFtMA8GA1UdEQQIMAaHBAAAAAAwCgYIKoZIzj0EAwIDRwAwRAIgI/a8\r\n"
				+ "f8CWniylb+erZ+p+1FTpyk/vxgZJqxCFRrmc2IUCICjwJFHMMkwPKyb5WPQgsN0A\r\n"
				+ "90el1s9kYvDAx/lv8lpG\r\n"
				+ "-----END CERTIFICATE-----\r\n"
				+ "\r\n";

		Properties properties = new Properties();
		properties.put("pemBytes", pemStr.getBytes());

		
		HFCAClient hfcaClient = HFCAClient.createNewInstance(caName, caUrl, properties);
		CryptoSuite cryptoSuite = CryptoSuite.Factory.getCryptoSuite();
		
		hfcaClient.setCryptoSuite(cryptoSuite);
	    
		UserContext adminUserContext = new UserContext();
		adminUserContext.setName("admin"); // admin username
		adminUserContext.setAffiliation("org1"); // affiliation
		adminUserContext.setMspId("org1"); // org1 mspid
		
		Enrollment adminEnrollment = hfcaClient.enroll("rca-mclane-admin", "rca-mclane-adminpw"); //pass admin username and password
		adminUserContext.setEnrollment(adminEnrollment);
		
		
		Util.writeUserContext(adminUserContext); // save admin context to local file system
		
		UserContext userContext = new UserContext();
		userContext.setName("");
		userContext.setAffiliation("org1");
		userContext.setMspId("org1");

		RegistrationRequest rr = new RegistrationRequest("user1", "org1");
		String enrollmentSecret = hfcaClient.register(rr, adminUserContext);
        
		
		Enrollment enrollment = hfcaClient.enroll(userContext.getName(), enrollmentSecret);
		userContext.setEnrollment(enrollment);
		Util.writeUserContext(userContext);
		
		UserContext adminUserContext1 = Util.readUserContext("org1", "admin");
		CryptoSuite cryptoSuite1 = CryptoSuite.Factory.getCryptoSuite();
		HFClient hfClient = HFClient.createNewInstance();
		hfClient.setCryptoSuite(cryptoSuite1);
		hfClient.setUserContext(adminUserContext1);
		
		
		 String peer_name = "org1-peer1";
		 String peer_url = "grpcs://xxxxx-org1-peer1.aus01.blockchain.ibm.com:xxxxx"; // Ensure that port is of peer1
		 String pemStr1 = "-----BEGIN CERTIFICATE-----\r\nxxxxxx\r\n";

		 Properties peer_properties = new Properties();
		 peer_properties.put("pemBytes", pemStr1.getBytes());
		 peer_properties.setProperty("sslProvider", "openSSL");
		 peer_properties.setProperty("negotiationType", "TLS");
         Peer peer = hfClient.newPeer(peer_name, peer_url, peer_properties);

		 String event_url = "grpcs://xxxxxx-org1-peer1.xxxx.blockchain.ibm.com:31003"; // ensure that port is of event hub
		 EventHub eventHub = hfClient.newEventHub(peer_name, event_url, peer_properties);

		 String orderer_name = "orderer";
		 String orderer_url = "grpcs://xxxxx-orderer.xxxx.blockchain.ibm.com:31001"; // ensure that port is of orderer
		 String pemStr2 = "-----BEGIN CERTIFICATE-----\r\nxxxxx\r\n-----END CERTIFICATE-----\r\n";

		 Properties orderer_properties = new Properties();
		 orderer_properties.put("pemBytes", pemStr2.getBytes());
		 orderer_properties.setProperty("sslProvider", "openSSL");
		 orderer_properties.setProperty("negotiationType", "TLS");
		 Orderer orderer = hfClient.newOrderer(orderer_name, orderer_url, orderer_properties);
		 
		 
		 Channel channel = hfClient.newChannel("");

		 channel.addPeer(peer);
		 channel.addEventHub(eventHub);
		 channel.addOrderer(orderer);
		 channel.initialize();
		 
		 
		 TransactionProposalRequest request = hfClient.newTransactionProposalRequest();
		 String cc = "carauction"; // Chaincode name
		 ChaincodeID ccid = ChaincodeID.newBuilder().setName(cc).build();

		 request.setChaincodeID(ccid);
		 request.setFcn("initLedger"); // Chaincode invoke funtion name
		 String[] arguments = { "N4", "Provider", "28-01-2019", "Food", "1000", "04-02-2019" }; // Arguments that Chaincode function takes
		 request.setArgs(arguments);
		 request.setProposalWaitTime(3000);
		 Collection<ProposalResponse> responses = channel.sendTransactionProposal(request);
		 for (ProposalResponse res : responses) {
		   // Process response from transaction proposal
		 }
		 
		 
		 CompletableFuture<TransactionEvent> cf = channel.sendTransaction(responses);
		 
		 
		 QueryByChaincodeRequest queryRequest = hfClient.newQueryProposalRequest();
		 queryRequest.setChaincodeID(ccid); // ChaincodeId object as created in Invoke block
		 queryRequest.setFcn("query"); // Chaincode function name for querying the blocks

		 String[] arguments1 = { "all"}; // Arguments that the above functions take
		 if (arguments != null)
		  queryRequest.setArgs(arguments1);

		 // Query the chaincode  
		 Collection<ProposalResponse> queryResponse = channel.queryByChaincode(queryRequest);

		 for (ProposalResponse pres : queryResponse) {
		  // process the response here
		 }
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CryptoException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (org.hyperledger.fabric.sdk.exception.InvalidArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EnrollmentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
