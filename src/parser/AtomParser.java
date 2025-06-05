package parser;

/* Esta clase implementa el parser de feed de tipo rss (xml)
 * https://www.tutorialspoint.com/java_xml/java_dom_parse_document.htm 
 * */


import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.IOException;
import feed.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.text.ParseException;
import org.xml.sax.SAXException;
import java.io.ByteArrayInputStream;


public class AtomParser extends RssParser {

	@Override
	public feed.Feed parser(String archXML) {
		//archXML mal formado desde httpRequest
		if(archXML == null || archXML.equals("")){return null;}
		try {
			ByteArrayInputStream file = new ByteArrayInputStream( archXML.getBytes("UTF-8"));

			// Crear el parser DOM
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);

			// Normalizar el documento
			doc.getDocumentElement().normalize();

			// Obtener todos los elementos para el feed 
			NodeList listItems = doc.getElementsByTagName("entry");
			return parsing_elements(listItems, TITLE, "subtitle", LINK, "updated", "Atom file", false);
		}
		catch (ParserConfigurationException | SAXException e) {
			//el xml que devuelve parser no está bien formado
			System.out.println("El archivo no está bien configurado " + e);
			return null;
		}
		catch (IOException e) {
			System.out.println("El archivo no existe o no se puede acceder" + e);
			return null;
		}
		catch (NullPointerException e) {
			//getElementsByTagName, si no está esa tag, hacer item(0) nos lleva aca.
			System.out.println("El archivo no contiene info que podemos proveer. " + e);
			return null;
		}
		catch (Exception e) {
			System.out.println("Problema desconocido. Vuelva a intentarlo. " + e);
			return null;
		}
	}
}	
