package edu.isi.madcat.tmem.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import edu.isi.madcat.tmem.backend.messages.SegmentorRequest;
import edu.isi.madcat.tmem.backend.messages.SegmentorResponse;
import edu.isi.madcat.tmem.backend.messages.TokenizerRequest;
import edu.isi.madcat.tmem.backend.messages.TokenizerResponse;
import edu.isi.madcat.tmem.backend.messages.TranslationRequest;
import edu.isi.madcat.tmem.backend.messages.TranslationResponse;
import edu.isi.madcat.tmem.exceptions.CateProcessException;
import edu.isi.madcat.tmem.logging.ExceptionHandler;
import edu.isi.madcat.tmem.serialization.DecodeException;

public class BbnMtHttpBackendRequestor extends BackendRequestor {
  protected String serverUrl;

  public BbnMtHttpBackendRequestor() {

  }

  public String getServerUrl() {
    return serverUrl;
  }

  @Override
  public SegmentorResponse segment(SegmentorRequest request) throws CateProcessException {
    String requestString = request.toXml();
    String responseString = sendRequest(requestString);
    SegmentorResponse response = new SegmentorResponse();
    try {
      response.fromXml(responseString);
    } catch (DecodeException e) {
      throw new CateProcessException("Unable to parse response.");
    }
    return response;
  }

  public void setServerUrl(String serverUrl) {
    this.serverUrl = serverUrl;
  }

  @Override
  public TokenizerResponse tokenize(TokenizerRequest request) throws CateProcessException {
    String requestString = request.toXml();
    String responseString = sendRequest(requestString);
    TokenizerResponse response = new TokenizerResponse();
    try {
      response.fromXml(responseString);
    } catch (DecodeException e) {
      throw new CateProcessException("Unable to parse response", e);
    }
    return response;
  }

  @Override
  public TranslationResponse translate(TranslationRequest request) throws CateProcessException {
    String requestString = request.toXml();
    String responseString = sendRequest(requestString);
    TranslationResponse response = new TranslationResponse();
    try {
      response.fromXml(responseString);
    } catch (DecodeException e) {
      throw new CateProcessException("Unable to parse response", e);
    }
    return response;
  }

  String sendRequest(String input) throws CateProcessException {
    String output = null;
    try {
      URL url = new URL(serverUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      String charset = "utf-8";
      connection.setDoInput(true);
      connection.setDoOutput(true);
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Accept-Charset", charset);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset="
          + charset);
      OutputStream os = null;
      os = connection.getOutputStream();
      os.write(input.getBytes(charset));
      os.close();
      int responseCode = connection.getResponseCode();
      if (responseCode == 200) {
        InputStream response = connection.getInputStream();
        output = IOUtils.toString(response, charset);
        response.close();
      } else {
        InputStream response = connection.getErrorStream();
        if (response != null) {
          output = IOUtils.toString(response, charset);
          response.close();
        }
      }
      if (responseCode != 200) {
        throw new CateProcessException("HTTP server returned bad response code: " + responseCode
            + ", Message = " + output);
      }
    } catch (MalformedURLException e) {
      ExceptionHandler.handle(e);
    } catch (UnsupportedEncodingException e) {
      ExceptionHandler.handle(e);
    } catch (IOException e) {
      throw new CateProcessException("Unable to connect to url: " + serverUrl, e);
    }
    return output;
  }
}
