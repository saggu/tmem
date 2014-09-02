package edu.isi.madcat.tmem.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.InputStreamResource;

import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.processors.SubstringOutput;
import edu.isi.madcat.tmem.processors.SubstringOutput.Replacement;
import edu.isi.madcat.tmem.processors.SubstringProcessor;
import edu.isi.madcat.tmem.utils.CateInitializer;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.Utils;

public class RunTransformer {
  SubstringProcessor substringProcessor;
  String inputFile;
  String outputFile;

  public RunTransformer(String[] args) throws CateProcessException {
    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    String configFile = params.getStringRequired("config_file");
    inputFile = params.getStringRequired("input_file");
    outputFile = params.getStringRequired("output_file");

    DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
    XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(factory);
    reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_NONE);
    try {
      reader.loadBeanDefinitions(new InputStreamResource(new FileInputStream(configFile)));
    } catch (BeanDefinitionStoreException e) {
      ExceptionHandler.handle(e);
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    }

    substringProcessor = (SubstringProcessor) Utils.getRequiredBean(factory, "substringProcessor");
    substringProcessor.setId("sp");
  }

  void process() {
    List<String> inputLines = Utils.readLinesFromFile(inputFile);
    for (String inputText : inputLines) {
      SubstringOutput substringOutput = substringProcessor.processString(inputText);
      String markedSourceText = substringOutput.getMarkedText();
      List<String> markedInputTokens = Arrays.asList(StringUtils.split(markedSourceText, " "));
      List<Replacement> replacementList = substringOutput.getReplacementList();
      List<String> replTokenizedTrans =
          SubstringOutput.replaceTokens(markedInputTokens, replacementList);
      String outputString = StringUtils.join(replTokenizedTrans, " ");
      System.out.println(outputString);
    }

  }

  public static void main(String[] args) throws CateProcessException {
    RunTransformer transformer = new RunTransformer(args);
    transformer.process();
  }
}
