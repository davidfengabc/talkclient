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


  public static final char BASE64EN[] = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'};
  public static final int mod[] = {0,1,1};



  private static void collectXMLUntil(BufferedReader in, String match) {
    String string = "";
    System.out.println("Reading\n");
    char ch;
    try {
      while(true) {
	ch = (char) in.read();
	System.out.print(ch);
	string = string + ch;
	if (ch == '>' && string.contains(match))
	  break;
      }
    } catch (IOException e) {
      System.err.println("in.read() failed: " + e.getMessage());
    }
    //return string;
  }

  
  private static void startDoc(XMLStreamReader reader) throws Exception {
    if(reader.getEventType() == XMLStreamReader.START_DOCUMENT) {
      System.out.println("start doc found yay");
    } else {
      System.err.println("start doc not foudn.....");
      System.exit(1);
    }
  }

  private static void initStream(XMLStreamReader reader) throws Exception {
    if(reader.next() != XMLStreamReader.START_ELEMENT){
      throw new XMLStreamException("must be <stream:stream>");
    }
    if(reader.getLocalName() != "stream") {
      throw new XMLStreamException("must be <stream:stream>");
    }
    while((reader.next() != XMLStreamReader.END_ELEMENT) ||
      reader.getLocalName() != "features")
    {
      //debugXML(reader);
    }
  }
  
  private static void saslAuth(XMLStreamWriter writer, XMLStreamReader reader, 
	char [] encodedPW) throws Exception {
    writer.writeStartElement("auth");
    writer.writeDefaultNamespace("urn:ietf:params:xml:ns:xmpp-sasl");
    writer.writeAttribute("mechanism","PLAIN");
    writer.writeCharacters(encodedPW,0,encodedPW.length);
    writer.writeEndElement();
    writer.flush();
    
    Arrays.fill(encodedPW,' ');

    if((reader.next() != XMLStreamReader.START_ELEMENT) 
      || (reader.getLocalName() != "success")) {
      debugXML(reader);
      throw new XMLStreamException("success not found");
    }

    if((reader.next() != XMLStreamReader.END_ELEMENT) 
      || (reader.getLocalName() != "success")) {
      throw new XMLStreamException("success end not found");
    }
  }


  private static void debugXML(XMLStreamReader reader) throws Exception{
    //if(reader.getEventType() == XMLStreamReader.START_DOCUMENT) {
      //System.out.println("start doc found yay");
    //} else {
      //System.err.println("start doc not foudn.....");
      //System.exit(1);
    //}
    //while (reader.hasNext()) {
      switch(reader.getEventType()) {
	case XMLStreamReader.ATTRIBUTE:
	   System.out.println("attribute");
	   for(int i=0; i<reader.getAttributeCount(); i++) {
	     System.out.println("localname=" + reader.getAttributeName(i) + " getAttributeNamespace=" + reader.getAttributeNamespace(i) + " getAttributePrefix=" + reader.getAttributePrefix(i) + " getAttributeType" + reader.getAttributeType(i) + " getAttributeValue" + reader.getAttributeValue(i));
	   }
	   break;
	case XMLStreamReader.CDATA:
	   System.out.println("cdata");
	   break;
	case XMLStreamReader.CHARACTERS:
	   System.out.println("characters");
	   break;
	case XMLStreamReader.COMMENT:
	   System.out.println("comment");
	   break;
	case XMLStreamReader.DTD:
	   System.out.println("dtd");
	   break;
	case XMLStreamReader.END_DOCUMENT:
	   System.out.println("end document");
	   break;
	case XMLStreamReader.END_ELEMENT:
	   System.out.println("end element - " + reader.getPrefix() + ":" + reader.getLocalName());
	   break;
	case XMLStreamReader.ENTITY_DECLARATION:
	   System.out.println("entity declaration");
	   break;
	case XMLStreamReader.ENTITY_REFERENCE:
	   System.out.println("entity reference");
	   break;
	case XMLStreamReader.NAMESPACE:
	   System.out.println("namespace");
	   break;
	case XMLStreamReader.NOTATION_DECLARATION:
	   System.out.println("notation declaration");
	   break;
	case XMLStreamReader.PROCESSING_INSTRUCTION:
	   System.out.println("processing instruciton");
	   break;
	case XMLStreamReader.SPACE:
	   System.out.println("space");
	   break;
	case XMLStreamReader.START_DOCUMENT:
	   System.out.println("start doc");
	   break;
	case XMLStreamReader.START_ELEMENT:
	   System.out.println("start element - " + reader.getPrefix() + ":" + reader.getLocalName());
	   for(int i=0; i<reader.getAttributeCount(); i++) {
	     System.out.println("localname=" + reader.getAttributeName(i) + " getAttributeNamespace=" + reader.getAttributeNamespace(i) + " getAttributePrefix=" + reader.getAttributePrefix(i) + " getAttributeType" + reader.getAttributeType(i) + " getAttributeValue" + reader.getAttributeValue(i));
	   }
	   break;
	default:
	   System.out.println("error:default");
	   break;
	   
      }
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
      //in = new BufferedReader(new InputStreamReader(
//	    soc.getInputStream()));
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

    XMLStreamWriter writer = openXMLOut(soc);

    System.out.println("Sending init xml\n");
    initStream(writer);
    XMLStreamReader reader = openXMLIn(soc);
    startDoc(reader);
    initStream(reader);


    System.out.println("Sending starttls xml\n");
    startTLS(writer, reader);

    //reinitialize stream


    SSLContext sslCtxt = SSLContext.getDefault();
    SSLSocketFactory sslSF = sslCtxt.getSocketFactory();
    SSLSocket sslSoc = (SSLSocket) sslSF.createSocket(soc,server,soc.getPort(),true);
    sslSoc.setUseClientMode(true);

    PrintWriter sslout = null;
    BufferedReader sslin = null;
    sslout = new PrintWriter(sslSoc.getOutputStream(), true);
    sslin = new BufferedReader(new InputStreamReader(
	  sslSoc.getInputStream()));
    XMLStreamWriter sslWriter = openXMLOut(sslSoc);

    //Initiate new stream after TLS proceed
    System.out.println("Sending init xml tls\n");
    initStream(sslWriter);
    XMLStreamReader sslReader = openXMLIn(sslSoc);
    initStream(sslReader);
    

    //SASL auth XMPP 6.4
    System.out.println("Sending sasl auth\n");
    saslAuth(sslWriter, sslReader, encodedPW);
    Arrays.fill(encodedPW,' ');

    //Initiate new stream after SASL success XMPP 6.4.6
    initStream(sslWriter);
    initStream(sslReader);

    //Resource binding XMPP 7
    String resource = bindResource(sslWriter, sslReader);

    sslout.println("<message from='" + resource +"' id='ktx72v49' to='david.feng7@gmail.com' type='chat' xml:lang='en'><body>Art thou not Romeo, and a Montague?</body></message>");
    sslout.flush();


    sslWriter.writeEndElement();

    sslin.close();
    sslout.close();
    stdIn.close();
    sslSoc.close();
    soc.close();
  }

  private static XMLStreamWriter openXMLOut(Socket soc) {
    XMLStreamWriter writer = null;
    XMLOutputFactory output = null;
    try{
      output = XMLOutputFactory.newInstance();
      writer = output.createXMLStreamWriter(soc.getOutputStream());
      //writer = output.createXMLStreamWriter(System.out);
      writer.writeStartDocument();
    } catch (Exception e) {
      System.out.println("Error openXMLOut");
      System.exit(1);
    }
    return writer;
  }

  private static XMLStreamReader openXMLIn(Socket soc) {
    XMLStreamReader reader = null;
    XMLInputFactory input = null;
    try{
      System.out.println("new fact");
      input = XMLInputFactory.newInstance();
      System.out.println("getInputStream");
      reader = input.createXMLStreamReader(soc.getInputStream());
    } catch (Exception e) {
      System.out.println("Error openXMLIn");
      System.exit(1);
    }
    return reader;
  }

  private static void initStream(XMLStreamWriter writer) throws Exception {
    writer.writeStartElement("stream", "stream", "http://etherx.jabber.org/streams");
    writer.writeDefaultNamespace("jabber:client");
    writer.writeNamespace("stream","http://etherx.jabber.org/streams");
    writer.writeAttribute("to","gmail.com");
    writer.writeAttribute("version","1.0");
    writer.writeCharacters("");
    writer.flush();
  }

  private static void startTLS(XMLStreamWriter writer, XMLStreamReader reader) throws Exception {
    writer.writeEmptyElement("starttls");
    writer.writeDefaultNamespace("urn:ietf:params:xml:ns:xmpp-tls");
    writer.writeCharacters("");
    writer.flush();

    if((reader.next() != XMLStreamReader.START_ELEMENT) 
      || (reader.getLocalName() != "proceed")) {
      debugXML(reader);
      throw new XMLStreamException("proceed not found");
    }

    if((reader.next() != XMLStreamReader.END_ELEMENT) 
      || (reader.getLocalName() != "proceed")) {
      throw new XMLStreamException("proceed end not found");
    }

    reader.close();
  }
  

  private static String bindResource(XMLStreamWriter writer, XMLStreamReader reader) throws Exception {
    String result;
    String id,jid;

    writer.writeStartElement("iq");
    writer.writeAttribute("id","1");
    writer.writeAttribute("type","set");
    
    writer.writeEmptyElement("bind");
    writer.writeDefaultNamespace("urn:ietf:params:xml:ns:xmpp-bind");
    writer.writeCharacters("");

    writer.writeEndElement();

    writer.flush();

    if((reader.next() != XMLStreamReader.START_ELEMENT) 
      || (reader.getLocalName() != "iq")) {
      throw new XMLStreamException("iq not found");
    }
    if((result = reader.getAttributeValue(null, "type")) == null) {
      throw new XMLStreamException("type not found");
    } 
    if (!result.matches("result")) {
      throw new XMLStreamException("type = " + result);
    }

    if((id = reader.getAttributeValue(null, "id")) == null) {
      throw new XMLStreamException("id not found");
    }
    System.out.println("id is " + id);

    while((reader.next() != XMLStreamReader.START_ELEMENT)
                            || (reader.getLocalName() != "jid")) {
      //debugXML(reader);
    }

    if(reader.next() != XMLStreamReader.CHARACTERS) {
      debugXML(reader);
      throw new XMLStreamException("jid not found");
    }
    jid = reader.getText();
    System.out.println("jid is " + jid);

    while((reader.next() != XMLStreamReader.END_ELEMENT)
                            || (reader.getLocalName() != "iq")) {
      //debugXML(reader);
    }
    //debugXML(reader);
    return jid;

  }
}
