package com.walmart.otto.utils;

import java.io.File;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtils {

  static String deviceName;

  public static void updateXMLFilesWithDeviceName(File[] files) {
    for (File file : files) {
      if (file.isDirectory()) {
        deviceName = file.getName();
        updateXMLFilesWithDeviceName(file.listFiles());
      } else {
        XMLUtils.updateXML(file.getAbsolutePath(), deviceName, "testcase", "name");
      }
    }
  }

  public static void updateXML(
      String filename, String deviceName, String elementTag, String element) {

    try {
      Document doc = getXMLFile(filename);

      if (doc == null) {
        return;
      }

      //update attribute value
      updateAttributeValue(doc, deviceName, elementTag, element);

      //write the updated document to file or console
      doc.getDocumentElement().normalize();
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);
      StreamResult result = new StreamResult(new File(filename));
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.transform(source, result);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static Document getXMLFile(String filename) {
    File xmlFile = new File(filename);
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder;
    try {
      dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(xmlFile);
      doc.getDocumentElement().normalize();
      return doc;
    } catch (Exception ex) {
      throw new RuntimeException("Something went wrong reading " + filename, ex);
    }
  }

  public static StringBuilder getText(Document doc) {
    Element item = doc.getDocumentElement();
    NodeList itemChilds = item.getChildNodes();
    StringBuilder stringBuilder = new StringBuilder();

    for (int i = 0; i != itemChilds.getLength(); ++i) {
      Node itemChildNode = itemChilds.item(i);
      if (!(itemChildNode instanceof Element)) {
        continue;
      }
      Element itemChild = (Element) itemChildNode;
      String itemChildName = itemChild.getNodeName();

      if (itemChildName.equals("testcase")) {
        stringBuilder.append(itemChild.getAttribute("name"));
        stringBuilder.append(itemChild.getAttribute("className"));
        if (itemChild.getChildNodes().getLength() > 0) {
          stringBuilder.append(itemChild.getChildNodes().item(1).getNodeName());
        }
      }
    }
    return stringBuilder;
  }

  private static void updateAttributeValue(
      Document doc, String deviceName, String elementTag, String element) {
    NodeList testcase = doc.getElementsByTagName(elementTag); //testcase
    Element name;

    for (int i = 0; i < testcase.getLength(); i++) {
      name = (Element) testcase.item(i);
      name.setAttribute(element, name.getAttribute(element) + " [" + deviceName + "]");
    }
  }
}
