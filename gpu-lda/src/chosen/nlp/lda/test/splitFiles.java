package chosen.nlp.lda.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import chosen.nlp.lda.conf.PathConfig;

public class splitFiles {
  /*
  public static void main(String[] args) {
    try {
      BufferedReader bufferedReader = new BufferedReader(new FileReader(
          new File(PathConfig.dataBlockPath)));
      StringBuffer fileData = new StringBuffer();
      int bufferSize = 1024 * 1024;
      char[] buffer = new char[bufferSize];
      int numRead = 0;
      int count = 0;
      int transferTimes = 0;
      String lastLine = "";
      try {
        while ((numRead = bufferedReader.read(buffer)) != -1) {
          String readData = String.valueOf(buffer, 0, numRead);
          fileData.append(readData);
          if(++count > 10) {
            break;
          }
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
      try {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(PathConfig.dataTPathString));
        bufferedWriter.write(fileData.toString());
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  */
}
