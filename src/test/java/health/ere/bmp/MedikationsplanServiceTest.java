package health.ere.bmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.gematik.ws.conn.amts.amtsservice.v1.AMTSService;
import de.gematik.ws.conn.amts.amtsservice.v1.AMTSServicePortType;
import de.gematik.ws.conn.cardservicecommon.v2.CardTypeType;
import de.gematik.ws.conn.connectorcommon.v5.Status;
import de.gematik.ws.conn.connectorcontext.v2.ContextType;
import de.gematik.ws.conn.eventservice.v7.GetCards;
import de.gematik.ws.conn.eventservice.v7.GetCardsResponse;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventService;
import de.gematik.ws.conn.eventservice.wsdl.v7.EventServicePortType;
import de.gematik.ws.conn.eventservice.wsdl.v7.FaultMessage;
import ere.health.emp.bmp.Einwilligung;
import ere.health.emp.bmp.MedikationsPlan;

public class MedikationsplanServiceTest {

   @BeforeEach
   void init() {
       System.setProperty("com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
       System.setProperty("com.sun.xml.internal.ws.transport.http.client.HttpTransportPipe.dump", "true");
       System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dump", "true");
       System.setProperty("com.sun.xml.internal.ws.transport.http.HttpAdapter.dump", "true");
       System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dumpTreshold", "999999");
   }

   @Test
   public void test() throws JAXBException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException,
         FileNotFoundException, KeyStoreException, IOException, KeyManagementException, de.gematik.ws.conn.amts.amtsservice.v1.FaultMessage, FaultMessage, DatatypeConfigurationException {
      JAXBContext jaxbContext = JAXBContext.newInstance(MedikationsPlan.class);
      MedikationsPlan mp = (MedikationsPlan) jaxbContext.createUnmarshaller()
            .unmarshal(getClass().getResourceAsStream("/Medikationsplan_1.xml"));

      // CardServicePortType cardService = new CardService(getClass().getResource("/CardService.wsdl"))
      //      .getCardServicePort();

      // BindingProvider bp = (BindingProvider) cardService;
      // bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
      //      "https://10.0.0.98:443/ws/CardService");

      // configureBindingProvider(bp);

      EventServicePortType eventService = new EventService(getClass().getResource("/EventService.wsdl"))
            .getEventServicePort();

      BindingProvider bp = (BindingProvider) eventService;
      bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "https://10.0.0.98:443/ws/EventService");

      configureBindingProvider(bp);

      String ehcHandle = getFirstCardOfType(eventService, CardTypeType.EGK);
      String hpcHandle = getFirstCardOfType(eventService, CardTypeType.SMC_B);

      AMTSServicePortType aMTSService = new AMTSService(getClass().getResource("/AMTSService.wsdl")).getAMTSServicePort();

      BindingProvider bp2 = (BindingProvider) aMTSService;
      bp2.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
            "https://10.0.0.98:443/ws/AMTSService");

      configureBindingProvider(bp2);
      
      
      JAXBContext einwilligungJaxbContext = JAXBContext.newInstance(Einwilligung.class);

      try {
    	  Holder<Status> status3 = new Holder<>();
    	  Holder<Boolean> egkValid3 = new Holder<>();
    	  Holder<byte[]> consentData = new Holder<>();
    	  
    	  aMTSService.readConsent(ehcHandle, hpcHandle, getContext(), status3, consentData, egkValid3);
    	  
    	  Einwilligung consentDataFromCard = (Einwilligung) einwilligungJaxbContext.createUnmarshaller().unmarshal(new ByteArrayInputStream(consentData.value));
    	  
    	  System.out.println("Einwillung vom: "+consentDataFromCard.getEinwilligungsdatum()+" "+consentDataFromCard.getVorname()+" "+consentDataFromCard.getNachname());
    	  
      } catch(Exception ex) {
    	  System.out.println("Error during reading Consent. Writing Consent");
    	  ex.printStackTrace();
    	  
	      Holder<Status> status = new Holder<>();
	      Holder<Boolean> egkValid = new Holder<>();
	
	      // Write Einwilligung
	
	      Einwilligung einwilligung = new Einwilligung();
	      einwilligung.setEinwilligungsdatum(DatatypeFactory.newInstance().newXMLGregorianCalendar((GregorianCalendar)Calendar.getInstance()));
	      einwilligung.setVersion("1.0.1");
	      einwilligung.setVorname("Manuel");
	      einwilligung.setNachname("Blechschmidt");
	      einwilligung.setStrasse("Droysenstr.");
	      einwilligung.setHausnummer("7");
	      einwilligung.setPostleitzahl("10629");
	      einwilligung.setOrt("Berlin");
	
	
	      ByteArrayOutputStream os2 = new ByteArrayOutputStream();
	      Marshaller marshaller = einwilligungJaxbContext.createMarshaller();
	      
	      marshaller.setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-15"); 
	      marshaller.marshal(einwilligung, os2);
	      aMTSService.writeConsent(ehcHandle, hpcHandle, getContext(), os2.toByteArray(), status, egkValid);
      }

      ByteArrayOutputStream os = new ByteArrayOutputStream();
      Marshaller mPmarshaller = jaxbContext.createMarshaller();
      
      mPmarshaller.setProperty(Marshaller.JAXB_ENCODING, "ISO-8859-15"); 
      mPmarshaller.marshal(mp, os);

      Holder<Status> status2 = new Holder<>();
      Holder<Boolean> egkValid2 = new Holder<>();
      // Write Medikationplan
      aMTSService.writeMP(ehcHandle, hpcHandle, getContext(), os.toByteArray(), "AMTS-PIN", status2, egkValid2);
   }

   private String getFirstCardOfType(EventServicePortType eventService, CardTypeType type) throws FaultMessage {
      GetCards parameter = new GetCards();
      parameter.setContext(getContext());
      parameter.setCardType(type);
      GetCardsResponse getCardsResponse = eventService.getCards(parameter);

      String ehcHandle = getCardsResponse.getCards().getCard().get(0).getCardHandle();
      return ehcHandle;
   }

   private ContextType getContext() {
      ContextType contextType = new ContextType();
      contextType.setMandantId("Incentergy");
      contextType.setWorkplaceId("1786_A1");
      contextType.setClientSystemId("Incentergy");
      return contextType;
   }

   private void configureBindingProvider(BindingProvider bindingProvider)
         throws NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException,
         UnrecoverableKeyException, KeyStoreException, KeyManagementException {
      SSLContext sslContext = SSLContext.getInstance("TLS");

      KeyStore ks = KeyStore.getInstance("pkcs12");
      String connectorTlsCertAuthStorePwd = "U9pRlw8SBfMExkycgNDs";
      try (InputStream is = new FileInputStream(
            "/home/manuel/Desktop/RU-Connector-Cert/incentergy_U9pRlw8SBfMExkycgNDs.p12")) {
         ks.load(is, connectorTlsCertAuthStorePwd.toCharArray());
         KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         kmf.init(ks, connectorTlsCertAuthStorePwd.toCharArray());

         sslContext.init(kmf.getKeyManagers(), new TrustManager[] { new FakeX509TrustManager() },
               null);
      }
      if (sslContext != null) {
         bindingProvider.getRequestContext().put("com.sun.xml.ws.transport.https.client.SSLSocketFactory",
               sslContext.getSocketFactory());
      }
      bindingProvider.getRequestContext().put("com.sun.xml.ws.transport.https.client.hostname.verifier",
            new FakeHostnameVerifier());
   }

   /**
    * This class allow any X509 certificates to be used to authenticate the
    * remote side of a secure socket, including self-signed certificates. This
    * class uses the old deprecated API from the com.sun.ssl
    * package.
    *
    * @author Francis Labrie
    *
    * @deprecated see {@link FakeX509TrustManager}.
    */
   public static class _FakeX509TrustManager
         implements X509TrustManager {

      /**
       * Empty array of certificate authority certificates.
       */
      private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};

      /**
       * Always return true, trusting for client SSL
       * chain peer certificate chain.
       *
       * @param chain the peer certificate chain.
       * @return the true boolean value
       *         indicating the chain is trusted.
       */
      public boolean isClientTrusted(X509Certificate[] chain) {
         return (true);
      } // checkClientTrusted

      /**
       * Always return true, trusting for server SSL
       * chain peer certificate chain.
       *
       * @param chain the peer certificate chain.
       * @return the true boolean value
       *         indicating the chain is trusted.
       */
      public boolean isServerTrusted(X509Certificate[] chain) {
         return (true);
      } // checkServerTrusted

      /**
       * Return an empty array of certificate authority certificates which
       * are trusted for authenticating peers.
       *
       * @return a empty array of issuer certificates.
       */
      public X509Certificate[] getAcceptedIssuers() {
         return (_AcceptedIssuers);
      } // getAcceptedIssuers

      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
         throw new UnsupportedOperationException("Not supported yet.");
      }
   } // _FakeX509TrustManager

   /**
    * This class implements a fake hostname verificator, trusting any host
    * name.
    *
    * @author Francis Labrie
    */
   public static class FakeHostnameVerifier implements HostnameVerifier {

      /**
       * Always return true, indicating that the host name is
       * an acceptable match with the server's authentication scheme.
       *
       * @param hostname the host name.
       * @param session  the SSL session used on the connection to
       *                 host.
       * @return the true boolean value
       *         indicating the host name is trusted.
       */
      public boolean verify(String hostname,
            SSLSession session) {
         return (true);
      } // verify
   } // FakeHostnameVerifier

   /**
    * This class allow any X509 certificates to be used to authenticate the
    * remote side of a secure socket, including self-signed certificates.
    *
    * @author Francis Labrie
    */
   public static class FakeX509TrustManager implements X509TrustManager {

      /**
       * Empty array of certificate authority certificates.
       */
      private static final X509Certificate[] _AcceptedIssuers = new X509Certificate[] {};

      /**
       * Always trust for client SSL chain peer certificate
       * chain with any authType authentication types.
       *
       * @param chain    the peer certificate chain.
       * @param authType the authentication type based on the client
       *                 certificate.
       */
      public void checkClientTrusted(X509Certificate[] chain,
            String authType) {
      } // checkClientTrusted

      /**
       * Always trust for server SSL chain peer certificate
       * chain with any authType exchange algorithm types.
       *
       * @param chain    the peer certificate chain.
       * @param authType the key exchange algorithm used.
       */
      public void checkServerTrusted(X509Certificate[] chain,
            String authType) {
      } // checkServerTrusted

      /**
       * Return an empty array of certificate authority certificates which
       * are trusted for authenticating peers.
       *
       * @return a empty array of issuer certificates.
       */
      public X509Certificate[] getAcceptedIssuers() {
         return (_AcceptedIssuers);
      } // getAcceptedIssuers
   } // FakeX509TrustManager
}
