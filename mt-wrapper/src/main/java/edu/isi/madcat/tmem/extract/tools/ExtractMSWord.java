package edu.isi.madcat.tmem.extract.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xslf.XSLFSlideShow;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlException;

import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.utils.ParameterMap;
import edu.isi.madcat.tmem.utils.Utils;

public class ExtractMSWord {
  private String inputFile;
  private String outputFile;

  public ExtractMSWord(ParameterMap params) {
    inputFile = params.getStringRequired("input_file");
    outputFile = params.getStringRequired("output_file");
  }

  public void process() {
    String extension = getExtension(inputFile);
    try {
      String text = "";
      if (extension.equals("pptx")) {
        XSLFSlideShow document = new XSLFSlideShow(inputFile);
        XSLFPowerPointExtractor extractor = new XSLFPowerPointExtractor(document);
        text = extractor.getText();
      } else if (extension.equals("docx")) {
        FileInputStream fis = new FileInputStream(inputFile);
        XWPFDocument document = new XWPFDocument(fis);
        XWPFWordExtractor extractor = new XWPFWordExtractor(document);
        text = extractor.getText();
        fis.close();
      }
      Writer outFile = Utils.createWriter(outputFile);
      outFile.write(text);
      IOUtils.closeQuietly(outFile);
    } catch (FileNotFoundException e) {
      ExceptionHandler.handle(e);
    } catch (InvalidFormatException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      ExceptionHandler.handle(e);
    } catch (OpenXML4JException e) {
      ExceptionHandler.handle(e);
    } catch (XmlException e) {
      ExceptionHandler.handle(e);
    }
  }

  private String getExtension(String inputFile) {
    Pattern p = Pattern.compile("^.*\\.(.*)$");
    String extension = "";
    Matcher m = p.matcher(inputFile);
    if (m.find()) {
      extension = m.group(1);
    }
    return extension;
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new RuntimeException("Usage: [app] parameter_file");
    }

    String parameterFile = args[0];
    ParameterMap params = new ParameterMap(parameterFile);
    ExtractMSWord processor = new ExtractMSWord(params);
    processor.process();
  }

}
