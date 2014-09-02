package edu.isi.madcat.tmem.serialization;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.utils.Utils;

/**
 * @author jdevlin SimpleSerializable objects are those which
 */
public class SimpleSerializable {
  public static Set<String> basicTypeClasses;

  static {
    basicTypeClasses = new HashSet<String>();
    basicTypeClasses.add("java.lang.String");
    basicTypeClasses.add("java.lang.Long");
    basicTypeClasses.add("java.lang.Double");
    basicTypeClasses.add("java.lang.Boolean");
  }

  public static String getBasicTypeValue(Object o) {
    String typeName = o.getClass().getName();
    if (!basicTypeClasses.contains(typeName)) {
      return null;
    }
    String stringValue = null;
    try {
      Method method = o.getClass().getMethod("toString");
      stringValue = (String) method.invoke(o);
    } catch (NoSuchMethodException e) {
      ExceptionHandler.handle(e);
    } catch (SecurityException e) {
      ExceptionHandler.handle(e);
    } catch (IllegalAccessException e) {
      ExceptionHandler.handle(e);
    } catch (IllegalArgumentException e) {
      ExceptionHandler.handle(e);
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(e);
    }
    return stringValue;
  }

  public static String javaNameToElementName(String javaName) {
    String baseName = javaName;
    Pattern p = Pattern.compile(".*\\.(.*?)$");
    Matcher m = p.matcher(javaName);
    if (m.find()) {
      baseName = m.group(1);
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < baseName.length(); i++) {
      char c = baseName.charAt(i);
      char lc = Character.toLowerCase(c);
      if (c != lc) {
        if (i > 0) {
          sb.append("_");
        }
      }
      sb.append(lc);
    }
    return sb.toString();
  }

  public void fromXml(String str) throws DecodeException {
    DocumentBuilderFactory docBuilderFactory = null;
    DocumentBuilder docBuilder = null;
    Document doc = null;

    try {
      docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilder = docBuilderFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      ExceptionHandler.handle(e);
    }

    try {
      InputSource inputSource = new InputSource(new StringReader(str));
      doc = docBuilder.parse(inputSource);
    } catch (SAXException e) {
      throw new DecodeException("Input is not valid xml.");
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    }

    NodeList childNodes = doc.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node node = childNodes.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        fromXmlElement((Element) node);
      }
    }
  }

  public void fromXmlElement(Element element) throws DecodeException {
    Class<?> c = this.getClass();
    Field[] fields = c.getDeclaredFields();
    for (Field f : fields) {
      f.setAccessible(true);
      String name = f.getName();
      String elementName = javaNameToElementName(name);
      Class<?> childClass = f.getType();
      String typeName = childClass.getName();
      try {
        Object basicValueObject = getBasicValue(childClass, elementName, element);
        if (basicValueObject != null) {
          f.set(this, basicValueObject);
        } else {
          Element fieldElement = null;
          NodeList childNodes = element.getChildNodes();
          for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals(elementName)) {
              fieldElement = (Element) node;
            }
          }
          Class<?> effectiveClass = childClass;
          if (typeName.equals("java.util.List")) {
            try {
              effectiveClass = Class.forName("java.util.ArrayList");
            } catch (ClassNotFoundException e) {
              ExceptionHandler.handle(e);
            }
          }
          Object obj = callEmptyConstructor(effectiveClass);
          f.set(this, obj);
          if (typeName.equals("java.util.List")) {
            Class<?> itemClass = null;
            Type genericFieldType = f.getGenericType();
            if (genericFieldType instanceof ParameterizedType) {
              ParameterizedType aType = (ParameterizedType) genericFieldType;
              Type[] fieldArgTypes = aType.getActualTypeArguments();
              if (fieldArgTypes.length != 1) {
                throw new DecodeException();
              }
              itemClass = (Class<?>) fieldArgTypes[0];
            } else {
              throw new DecodeException();
            }

            Method[] childMethods = childClass.getMethods();
            Method addMethod = null;
            for (Method method : childMethods) {
              if (method.getName().equals("add") && method.getParameterTypes().length == 1) {
                addMethod = method;
              }
            }
            if (addMethod == null) {
              throw new DecodeException();
            }

            NodeList nodes = fieldElement.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
              Node node = nodes.item(i);
              if (node.getNodeType() == Node.ELEMENT_NODE) {
                Object itemObj = getBasicValue(itemClass, "value", (Element) node);
                if (itemObj == null) {
                  itemObj = callEmptyConstructor(itemClass);
                  Method method = getMethodByName(itemObj, "fromXmlElement");
                  method.invoke(itemObj, (Element) node);
                }
                addMethod.invoke(obj, itemObj);
              }
            }
          } else {
            Method[] childMethods = childClass.getMethods();
            Method extractorMethod = null;
            for (Method method : childMethods) {
              if (method.getName().equals("fromXmlElement")) {
                extractorMethod = method;
              }
            }
            if (extractorMethod == null) {
              throw new DecodeException();
            }
            extractorMethod.invoke(obj, fieldElement);
          }
        }
      } catch (IllegalArgumentException e) {
        throw new DecodeException(Utils.getStackTrace(e));
      } catch (IllegalAccessException e) {
        throw new DecodeException(e.getMessage());
      } catch (InvocationTargetException e) {
        throw new DecodeException(e.getMessage());
      }
    }
  }

  public Method getMethodByName(Object obj, String name) {
    Class<?> c = obj.getClass();
    Method[] allMethods = c.getMethods();
    Method selectedMethod = null;
    for (Method method : allMethods) {
      if (method.getName().equals(name)) {
        selectedMethod = method;
      }
    }
    return selectedMethod;
  }

  public String toXml() {
    DocumentBuilderFactory docBuilderFactory = null;
    DocumentBuilder docBuilder = null;
    Document document = null;
    try {
      docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilder = docBuilderFactory.newDocumentBuilder();
      document = docBuilder.newDocument();
    } catch (ParserConfigurationException e) {
      ExceptionHandler.handle(e);
    }
    String output = null;
    try {
      Class<?> c = this.getClass();
      Element root = document.createElement(javaNameToElementName(c.getName()));
      toXmlElement(root, document);
      document.appendChild(root);
      output = xmlToString(document);
    } catch (EncodeException e) {
      ExceptionHandler.handle(e);
    }
    return output;
  }

  public String toString() {
    return toXml();
  }
  
  public void toXmlElement(Element element, Document document) throws EncodeException {
    Class<?> c = this.getClass();
    Field[] fields = c.getDeclaredFields();
    for (Field f : fields) {
      f.setAccessible(true);
      String name = f.getName();
      Class<?> childClass = f.getType();
      String typeName = childClass.getName();
      try {
        Object obj = f.get(this);
        if (obj == null) {
          throw new EncodeException("Object cannot contain any null elements: "
              + this.getClass().getName() + " " + f.getName());
        }
        String stringValue = getBasicTypeValue(obj);
        if (stringValue != null) {
          element.setAttribute(javaNameToElementName(name), stringValue);
        }
        // Not basic type
        else {
          if (typeName.equals("java.util.List")) {
            Element listElement = document.createElement(javaNameToElementName(name));
            List<?> list = (List<?>) f.get(this);
            for (Object o : list) {
              String itemValue = getBasicTypeValue(o);
              Element itemElement = document.createElement("item");
              if (itemValue != null) {
                itemElement.setAttribute("value", itemValue);
              } else {
                Method method = getMethodByName(o, "toXmlElement");
                System.out.println(method.getName());
                method.invoke(o, itemElement, document);
              }
              listElement.appendChild(itemElement);
            }
            element.appendChild(listElement);
          } else {
            Object child = f.get(this);
            Element childElement = document.createElement(javaNameToElementName(name));
            if (child != null) {
              Method method = null;
              Method[] methods = childClass.getMethods();
              for (Method m : methods) {
                if (m.getName().equals("toXmlElement")) {
                  method = m;
                }
              }
              if (method == null) {
                throw new EncodeException("Child class is not SimpleSerializable: "
                    + childClass.getName());
              }
              method.invoke(child, childElement, document);
            }
            element.appendChild(childElement);
          }
        }
      } catch (IllegalArgumentException e) {
        throw new EncodeException(e.getMessage());
      } catch (IllegalAccessException e) {
        throw new EncodeException(e.getMessage());
      } catch (InvocationTargetException e) {
        e.getTargetException().printStackTrace();
        throw new EncodeException(e.getTargetException().getMessage());
      }
    }
  }

  protected Object callEmptyConstructor(Class<?> c) throws DecodeException {
    Constructor<?> emptyConstructor = null;
    Constructor<?>[] constructors = c.getConstructors();
    for (Constructor<?> constructor : constructors) {
      Class<?>[] constructorParams = constructor.getParameterTypes();
      if (constructorParams.length == 0) {
        emptyConstructor = constructor;
      }
    }
    if (emptyConstructor == null) {
      throw new DecodeException("No empty constructor specified for: " + c.getName());
    }
    Object obj = null;
    try {
      obj = emptyConstructor.newInstance();
    } catch (InstantiationException e) {
      ExceptionHandler.handle(e);
    } catch (IllegalAccessException e) {
      ExceptionHandler.handle(e);
    } catch (IllegalArgumentException e) {
      ExceptionHandler.handle(e);
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(e);
    }
    return obj;
  }

  protected Object getBasicValue(Class<?> c, String name, Element element) {
    if (!basicTypeClasses.contains(c.getName())) {
      return null;
    }
    String value = element.getAttribute(name);
    Constructor<?> stringConstructor = null;
    Constructor<?>[] constructors = c.getConstructors();
    for (Constructor<?> constructor : constructors) {
      Class<?>[] constructorParams = constructor.getParameterTypes();
      if (constructorParams.length == 1
          && constructorParams[0].getName().equals("java.lang.String")) {
        stringConstructor = constructor;
      }
    }
    Object obj = null;
    try {
      obj = stringConstructor.newInstance(value);
    } catch (InstantiationException e) {
      ExceptionHandler.handle(e);
    } catch (IllegalAccessException e) {
      ExceptionHandler.handle(e);
    } catch (IllegalArgumentException e) {
      ExceptionHandler.handle(e);
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(e);
    }
    return obj;
  }

  protected String xmlToString(Document document) {
    String output = null;
    try {
      TransformerFactory tfactory = TransformerFactory.newInstance();
      Transformer tx = tfactory.newTransformer();
      DOMSource source = new DOMSource(document);
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      BufferedOutputStream os = new BufferedOutputStream(bos);
      StreamResult result = new StreamResult(os);
      tx.transform(source, result);
      output = new String(bos.toByteArray(), "utf-8");
    } catch (TransformerConfigurationException e) {
      ExceptionHandler.handle(e);
    } catch (TransformerFactoryConfigurationError e) {
      ExceptionHandler.handle(e);
    } catch (TransformerException e) {
      ExceptionHandler.handle(e);
    } catch (UnsupportedEncodingException e) {
      ExceptionHandler.handle(e);
    }
    return output;
  }
}
