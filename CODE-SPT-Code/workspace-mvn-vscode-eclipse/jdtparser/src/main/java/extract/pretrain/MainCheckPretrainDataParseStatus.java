package extract.pretrain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import base.MainBaseClass;
import util.CheckParsing;
import util.UtilAST;
import util.UtilFile;

public class MainCheckPretrainDataParseStatus extends MainBaseClass implements GlobalInfo {
   // ==================================================================================
   //
   // ### Input :
   //
   // ### Output :
   //
   // ==================================================================================
   static private String dataName = "unixcoder";
   private int counterParseFailed = 0;
   private int counterUnparsed = 0;
   private int counterBadClass = 0;
   private int counterPass = 0;
   private int counterBadMethod = 0;

   List<String> resultParseStatus = new ArrayList<String>();
   List<String> resultParseStatusParsed = new ArrayList<String>();
   List<String> resultTrainOrgStr = new ArrayList<String>();
   List<String> resultTrainOrgStrSL = new ArrayList<String>();

   static String inputPath, outputPath;

   public MainCheckPretrainDataParseStatus() {
   }

   public MainCheckPretrainDataParseStatus(String dataName) {
      String dirPath = dataName;
      File dir = new File(outputPath + "/" + dirPath);

      if (!dir.exists()) {
         dir.mkdirs();
      }
   }

   public static void main(String[] args) {
      if (args.length != 2) {
         System.out.println("Usage: java MainProgram <input_path> <output_path>");
         System.out.println("Please provide both input and output dir paths.");
         System.exit(1);
      }

      inputPath = args[0];
      outputPath = args[1];

      System.out.println("Input Path: " + inputPath);
      System.out.println("Output Path: " + outputPath);

      try {
         MainCheckPretrainDataParseStatus main = new MainCheckPretrainDataParseStatus(dataName);
         main.beginProc();
         main.closingTime();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void beginProc() throws Exception {
      extract(inputPath + "/" + INPUT_TRAIN_JSON);
      UtilFile.saveFile(outputPath + "/" + dataName + "/" + OUTPUT_PARSE_STATUS, resultParseStatus);
      UtilFile.saveFile(outputPath + "/" + dataName + "/" + OUTPUT_PARSE_STATUS_PARSED, resultParseStatusParsed);
      UtilFile.saveFile(outputPath + "/" + dataName + "/" + OUTPUT_TRAIN_ORG_STR, resultTrainOrgStr);
      UtilFile.saveFile(outputPath + "/" + dataName + "/" + OUTPUT_TRAIN_ORG_STR_SINGLELINE, resultTrainOrgStrSL);
   }

   void extract(String json) throws Exception {
      int lineCount = 0;
      BufferedReader reader = new BufferedReader(new FileReader(json));
      JSONParser parser = new JSONParser();
      String line;

      while ((line = reader.readLine()) != null) {
         lineCount++;
         // if (lineCount == 5034)
         // System.out.print("");

         JSONObject jsonObject = (JSONObject) parser.parse(line);
         String orgStr = (String) jsonObject.get("original_string");

         String unitName = "ParsedAndToString";
         String source = "class " + unitName + " {\n" + orgStr + "\n}";

         CheckParsing checkParsable = UtilAST.checkParsable(source, unitName);
         if (checkParsable.equals(CheckParsing.Pass) == false) {
            resultParseStatus.add("// [DBG] " + lineCount);
            resultParseStatus.add("// [DBG] " + checkParsable);
            resultParseStatus.add(orgStr);
            resultParseStatus.add("// [DBG] ------------------------------------------------------");

            resultParseStatusParsed.add("// [DBG] " + lineCount);
            resultParseStatusParsed.add("// [DBG] " + checkParsable);
            resultParseStatusParsed.add(parseAndToStr(source, unitName));
            resultParseStatusParsed.add("// [DBG] ------------------------------------------------------");
         }
         else {
            String parseAndToStr = parseAndToStr(source, unitName);
            resultTrainOrgStr.add("// [DBG] " + lineCount);
            resultTrainOrgStr.add(parseAndToStr);

            // Step 1. make single line
            String singleLine = parseAndToStr.replaceAll("\\r?\\n", " ");
            
            // Step 2. ensure no problems by comparing # of tokens between org and update.
            int count1 = countNonSpaceCharacters(parseAndToStr);
            int count2 = countNonSpaceCharacters(singleLine);

            if (count1 != count2) {
               System.out.println("[DBG] line: " + lineCount + ", diff chars after single line:" + count1 + ", " + count2);
               System.out.println("[DBG] " + parseAndToStr + "\n\n" + singleLine);
            }

            // Step 3. remove contiguous spaces (i.e., actual spaces, not other types of whitespace like tabs or newlines.
            parseAndToStr = parseAndToStr.replaceAll(" {2,}", " ");
            singleLine = singleLine.replaceAll(" {2,}", " ");
            int count3 = countNonSpaceCharacters(parseAndToStr);
            int count4 = countNonSpaceCharacters(singleLine);
            if (count3 != count4) {
               System.out.println("[DBG] line: " + lineCount + ", diff chars after single line:" + count3 + ", " + count4);
               System.out.println("[DBG] " + parseAndToStr + "\n\n" + singleLine);
            }

            resultTrainOrgStrSL.add("// [DBG] " + lineCount);
            resultTrainOrgStrSL.add(singleLine);
         }

         switch (checkParsable) {
         case PARSE_FAILURE:
            counterParseFailed++;
            break;

         case UNPARSED:
            counterUnparsed++;
            String parseAndToStr = parseAndToStr(source, unitName);
            CheckParsing recheckParsable = UtilAST.checkParsable(parseAndToStr, unitName);
            if (recheckParsable.equals(CheckParsing.Pass) == true) {
               counterUnparsed--;
               resultTrainOrgStr.add("// [DBG] " + lineCount);
               resultTrainOrgStr.add(parseAndToStr);
            }
            else {
               System.out.print("");
            }
            break;

         case BAD_CLASS:
            counterBadClass++;
            break;

         case BAD_METHODS:
            counterBadMethod++;
            break;

         case Pass:
            counterPass++;
            break;
         default:
         }
      }
      reader.close();
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + OUTPUT_LOG, true))) {
         String[] logs = { //
               "[DBG] Input lines read (json): " + lineCount, //
               "[DBG] Num Pass of Parse: " + counterPass, //
               "[DBG] ------------------------------------------------------", //
               "[DBG] Num Parse Failed Code: " + counterParseFailed, //
               "[DBG] Num Unparsed Code: " + counterUnparsed, //
               "[DBG] Num Bad Classs: " + counterBadClass, //
               "[DBG] Num Bad Methods: " + counterBadMethod, //
               "[DBG] ------------------------------------------------------" //
         };

         for (String log : logs) {
            System.out.println(log);
            writer.write(log);
            writer.newLine();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   String parseAndToStr(String source, String unitName) {
      ASTParser astParser = UtilAST.parseSrcCode(source, unitName);
      CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
      String parsedContents = cu.toString().trim();
      return parsedContents;
   }

   int countNonSpaceCharacters(String input) {
      return input.replace(" ", "").replace("\n", "") // Remove new line characters
            .replace("\r", "") // Remove carriage return (if needed)
            .length();
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
