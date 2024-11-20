/**
 * @(#) UTFile.java
 */
package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

// import extract.json.ser.JavaSmallJsonExtract;

/**
 * @since J2SE-1.8
 */
public class UtilFile {
   public static String readEntireFile(String filename) throws IOException {
      FileReader in = new FileReader(filename);
      StringBuilder contents = new StringBuilder();
      char[] buffer = new char[4096];
      int read = 0;
      do {
         contents.append(buffer, 0, read);
         read = in.read(buffer);
      }
      while (read >= 0);
      in.close();
      return contents.toString();
   }

   public static String[] readFileArray(String filePath) {
      String[] lines = null;

      try {
         int lineCount = countLines(filePath);
         lines = new String[lineCount];
         readLines(filePath, lines);
      } catch (IOException e) {
         e.printStackTrace();
      }
      return lines;
   }

   public static int countLines(String filePath) throws IOException {
      int lines = 0;
      try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
         while (reader.readLine() != null) {
            lines++;
         }
      }
      return lines;
   }

   public static void readLines(String filePath, String[] lines) throws IOException {
      int index = 0;
      try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
         String line;
         while ((line = reader.readLine()) != null) {
            lines[index++] = line;
         }
      }
   }

   public static String getShortFileName(String fileName) {
      String S = System.getProperty("file.separator");
      int idx = fileName.lastIndexOf(S);
      if (idx == -1) {
         idx = fileName.lastIndexOf("/");
         if (idx == -1) {
            idx = fileName.lastIndexOf("\\");
         }
      }
      return fileName.substring(idx + 1);
   }

   // @SuppressWarnings("unchecked")
   // public static List<JavaSmallJsonExtract> loadFileSer(String filePath) {
   //    List<JavaSmallJsonExtract> listJavaSmallJson = null;
   //    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath))) {
   //       listJavaSmallJson = (List<JavaSmallJsonExtract>) ois.readObject();
   //    } catch (IOException | ClassNotFoundException e) {
   //       e.printStackTrace();
   //    }
   //    return listJavaSmallJson;
   // }

   public static boolean checkFile(String f) {
      boolean exists = new File(f).exists();
      if (exists == false) {
         System.out.println("[ERR] File Not Found: " + f);
         System.exit(0);
      }
      System.out.println("[DBG] Checking to read: " + f);
      return true;
   }

   // public static void saveFileSer(List<JavaSmallJsonExtract> listJavaSmallJson, String filePath) {
   //    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
   //       oos.writeObject(listJavaSmallJson);
   //    } catch (IOException e) {
   //       e.printStackTrace();
   //    }
   // }

   public static void saveFile(String filePath, List<String> contents) throws IOException {
      FileWriter fileWriter = new FileWriter(filePath);
      PrintWriter printWriter = new PrintWriter(fileWriter);
      for (String str : contents) {
         printWriter.print(str + System.lineSeparator());
      }
      printWriter.close();
   }

   public static void checkCreateDir(String dirName) {
      Path dirPath = Paths.get(dirName);

      if (Files.notExists(dirPath)) {
         try {
            Files.createDirectory(dirPath);
            System.out.println("[DBG] Directory '" + dirName + "' created.");
         } catch (IOException e) {
            System.err.println("Failed to create directory '" + dirName + "': " + e.getMessage());
         }
      }
   }

   public static int getLineNumberAtOffset(String file, int offset) throws IOException {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      int lineNumber = 1;
      int currentOffset = 0;
      int charRead;
      
      while ((charRead = reader.read()) != -1) {
         if (currentOffset >= offset) {
            break;
         }
         if (charRead == '\n') {
            lineNumber++;
         }
         currentOffset++;
      }
      reader.close();
      return lineNumber;
   }
}

//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
