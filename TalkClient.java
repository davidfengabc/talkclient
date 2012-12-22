import java.io.*;
import java.util.*;
import java.net.*;
import javax.net.ssl.*;
import tools.base64encode;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

public class TalkClient {
  public static final String server = "talk.google.com";
  public static final int port = 5222;
  public static final int READ_LENGTH = 512;
  public static final String jid = "deaglemetimbers";

  public static final String xml_init = "<stream:stream from='deaglemetimbers@gmail.com' to='gmail.com' version='1.0' xml:lang='en' xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams'>";
  public static final String xml_starttls = "<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>";
  public static final String xml_saslauth = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='PLAIN'>";
  public static final String xml_endstream = "</stream:stream>";

  public static final char BASE64EN[] = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'};
  public static final int mod[] = {0,1,1};



  private static String collectXMLUntil(BufferedReader in, String match) {
    String string = "";
    System.out.println("Reading\n");
    char ch;
    try {
      while(true) {
	ch = (char) in.read();
	string = string + ch;
	if (ch == '>' && string.contains(match))
	  break;
      }
    } catch (IOException e) {
      System.err.println("in.read() failed: " + e.getMessage());
    }
    return string;
  }

  public static void main(String[] args) throws Exception {

    char[] nextRead = new char[READ_LENGTH];
    int charsRead = 0;
    Socket soc = null;
    PrintWriter out = null;
    BufferedReader in = null;

    Console console = System.console();
    if (console == null) {
      System.err.println("No console.");
      System.exit(1);
    }

    char [] password = console.readPassword("Enter your password: ");
    char [] jidtemp = jid.toCharArray();
    char [] temp = new char[2+jidtemp.length+password.length];
    temp[0] = 0;
    for(int i=0; i<jidtemp.length; i++) {
      temp[i+1] = jidtemp[i];
    }
    temp[jidtemp.length+1] = 0;
    for(int i=0; i<password.length; i++) {
      temp[i+2+jidtemp.length] = password[i];
    }
    Arrays.fill(password, ' ');
    char [] encodedPW = base64encode.base64(temp);


    try {
      soc = new Socket(server, port);
      in = new BufferedReader(new InputStreamReader(
	    soc.getInputStream()));
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host: " + server);
      System.exit(1);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for "
	  + "the connection to: " + server);
      System.exit(1);
    }

    BufferedReader stdIn = new BufferedReader(
	new InputStreamReader(System.in));

    XMLEventWriter writer = openXMLOut(soc);
    XMLEventFactory eventFac = XMLEventFactory.newInstance();

    initStream(writer, eventFac, "gmail.com");
    //System.out.println("Sending init xml\n");
    //out.print(xml_init);
    //out.flush();
    //System.out.println(collectXMLUntil(in, "</stream:features>"));

    System.out.println("Sending starttls xml\n");
    out.print(xml_starttls);
    out.flush();
    System.out.println(collectXMLUntil(in, "/>"));


    SSLContext sslCtxt = SSLContext.getDefault();
    SSLSocketFactory sslSF = sslCtxt.getSocketFactory();
    SSLSocket sslSoc = (SSLSocket) sslSF.createSocket(soc,server,soc.getPort(),true);
    sslSoc.setUseClientMode(true);

    PrintWriter sslout = null;
    BufferedReader sslin = null;
    sslout = new PrintWriter(sslSoc.getOutputStream(), true);
    sslin = new BufferedReader(new InputStreamReader(
	  sslSoc.getInputStream()));

    System.out.println("Sending init xml tls\n");
    sslout.print(xml_init);
    sslout.flush();
    System.out.println(collectXMLUntil(sslin, "</stream:features>"));

    System.out.println("Sending sasl auth\n");
    sslout.print(xml_saslauth);
    for(int i=0; i<encodedPW.length; i++) {
      sslout.print(encodedPW[i]);
    }
    Arrays.fill(encodedPW,' ');
    sslout.print("</auth>");
    sslout.flush();
    System.out.println(collectXMLUntil(sslin, "/>"));

    sslout.println(xml_endstream);
    sslout.flush();

    sslin.close();
    sslout.close();
    stdIn.close();
    sslSoc.close();
    soc.close();
  }

  private static XMLEventWriter openXMLOut(Socket soc) {
    XMLEventWriter writer = null;
    XMLOutputFactory output = null;
    try{
      output = XMLOutputFactory.newInstance();
      //XMLEventWriter writer = output.createXMLEventWriter(soc.getOutputStream());
      writer = output.createXMLEventWriter(System.out);
    } catch (Exception e) {
      System.out.println("Error openXMLOut");
      System.exit(1);
    }
    return writer;
  }

  private static void initStream(XMLEventWriter writer, XMLEventFactory evtFac, String to) throws Exception {
    ArrayList<Attribute> attr = new ArrayList<Attribute>();
    ArrayList<Namespace> ns = new ArrayList<Namespace>();;
    Namespace defaultNs = evtFac.createNamespace("jabber:client");
    Namespace streamNs = evtFac.createNamespace("stream","http://etherx.jabber.org/streams");
    attr.add(evtFac.createAttribute(new QName("to"),"gmail.com"));
    attr.add(evtFac.createAttribute(new QName("version"),"1.0"));
    writer.add(evtFac.createStartElement(defaultNs.getPrefix(), defaultNs.getNamespaceURI(),"stream", attr.iterator(), ns.iterator()));
    writer.flush();

    
  }

//  private static char[] base64(char[] PW) {
//    int sixth1;
//    int sixth2;
//    int sixth3;
//    int sixth4;
//    int thirds = PW.length/3;
//    int lastthird = PW.length%3;
//    char[] encodedPW = new char[(thirds+mod[lastthird])*4];
//    int i=0;
//
//    for(i=0; i < thirds; i++) {
//      sixth1 = (PW[i*3] & (0xfc)) >> 2;
//      sixth2 = ((PW[i*3] & (0x3)) << 4) + ((PW[i*3+1] & (0xf0)) >> 4);
//      sixth3 = ((PW[i*3+1] & (0xf)) << 2) + ((PW[i*3+2] & (0xc0)) >> 6);
//      sixth4 = PW[i*3+2] & (0x3f);
//
//      encodedPW[i*4] = BASE64EN[sixth1];
//      encodedPW[i*4+1] = BASE64EN[sixth2];
//      encodedPW[i*4+2] = BASE64EN[sixth3];
//      encodedPW[i*4+3] = BASE64EN[sixth4];
//    }
//
//    if(lastthird == 1) {
//      sixth1 = (PW[i*3] & (0xfc)) >> 2;
//      sixth2 = (PW[i*3] & (0x3)) << 4;
//
//      encodedPW[i*4] = BASE64EN[sixth1];
//      encodedPW[i*4+1] = BASE64EN[sixth2];
//      encodedPW[i*4+2] = '=';
//      encodedPW[i*4+3] = '=';
//    } else if (lastthird == 2) {
//      sixth1 = (PW[i*3] & (0xfc)) >> 2;
//      sixth2 = ((PW[i*3] & (0x3)) << 4) + ((PW[i*3+1] & (0xf0)) >> 4);
//      sixth3 = (PW[i*3+1] & (0xf)) << 2;
//
//      encodedPW[i*4] = BASE64EN[sixth1];
//      encodedPW[i*4+1] = BASE64EN[sixth2];
//      encodedPW[i*4+2] = BASE64EN[sixth3];
//      encodedPW[i*4+3] = '=';
//    }
//
//    return encodedPW;
//  }

}
