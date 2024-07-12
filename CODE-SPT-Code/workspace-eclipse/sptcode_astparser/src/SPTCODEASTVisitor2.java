import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jdt.core.dom.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SPTCODEASTVisitor2 {

    // Regular expression pattern to match string literals
    private static final Pattern STRING_MATCHING_PATTERN = Pattern.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");
    
    public static void main(String[] args) {
        String filePath = "dataset/finetune_raw/java-small-json/java-small.train.json";
        String outputFilePath = "dataset/finetune_raw/java-small-json/finetune_methods_train_final.txt";

        int lineCount = 0;
        JSONParser parser = new JSONParser();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath));
             BufferedWriter bw = new BufferedWriter(new FileWriter(outputFilePath))) {

            String line;
            while ((line = br.readLine()) != null) {
                lineCount++;
                try {
                    JSONObject jsonObject = (JSONObject) parser.parse(line);
                    String leftContext = replaceStringLiteral((String) jsonObject.get("left_context"));
                    String rightContext = replaceStringLiteral((String) jsonObject.get("right_context"));
                    String targetSeq = replaceStringLiteral((String) jsonObject.get("target_seq"));
                    String formattedString = leftContext + " " + targetSeq + " " + rightContext;

                    // Process with ASTVisitor
                    String astOutput = processWithASTVisitor(formattedString);
                    bw.write(astOutput);
                    bw.newLine();
                } catch (ParseException e) {
                    System.out.println("Error decoding JSON on input line " + lineCount);
                    bw.write("\"Error in JSON format on line " + lineCount + "\"");
                    bw.newLine();
                }
            }

            System.out.println("Total input lines read: " + lineCount);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Data has been processed and saved to " + outputFilePath + ".");
    }

    private static String replaceStringLiteral(String source) {
        Matcher matcher = STRING_MATCHING_PATTERN.matcher(source);
        return matcher.replaceAll("___STR");
    }

    private static String wrapWithClass(String codeSnippet) {
        return "public class DummyClass {\n" + codeSnippet.replace("@Override", "\n@Override") + "\n}";
    }

    private static String processWithASTVisitor(String codeSnippet) {
        String formattedCode = wrapWithClass(codeSnippet);
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(formattedCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        final StringBuilder astOutput = new StringBuilder();
        cu.accept(new ASTVisitor() {
           
        	public boolean visit(MethodDeclaration node) {
                astOutput.append("MethodDeclaration");//.append(node.getName()).append(" ")
                return true; 
            }

            public boolean visit(VariableDeclarationFragment node) {
                astOutput.append(" VariableDeclarationFragment");//.append(node.getName()).append(" ");
                return true;
            }
            
            @Override
              public boolean visit(MethodInvocation node) {
            	astOutput.append(" MethodInvocation");// + node.getName());
              	return true;
              }
              
              @Override
              public boolean visit(IfStatement node) {
            	astOutput.append(" IfStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(ReturnStatement node) {
            	astOutput.append(" ReturnStatement");
              	return true;
              }
              
              @Override
              public boolean visit(CastExpression node) {
            	astOutput.append(" CastExpression");
              	return true;
              }
              
              @Override
              public boolean visit(ParenthesizedExpression node) {
            	astOutput.append(" ParenthesizedExpression");
              	return true; 
              }
              
              @Override
              public boolean visit(InstanceofExpression node) {
            	astOutput.append(" InstanceofExpression");
              	return true;
              }
              
              @Override
              public boolean visit(VariableDeclarationStatement node) {
            	astOutput.append(" VariableDeclarationStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(ForStatement node) {
            	  astOutput.append(" ForStatement");
                  return super.visit(node);
              }
              
              @Override
              public boolean visit(EnhancedForStatement node) {
            	  astOutput.append(" EnhancedForStatement");
                  return super.visit(node); 
              }
              
              @Override
              public boolean visit(SwitchStatement node) {
            	astOutput.append(" SwitchStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(TryStatement node) {
            	astOutput.append(" TryStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(CatchClause node) {
              	astOutput.append(" CatchClause");
                  return true; 
              }
              
              @Override
              public boolean visit(InfixExpression node) {
              	astOutput.append(" InfixExpression");
              	return true;
              }
             
              @Override
              public boolean visit(ConditionalExpression node) {
              	astOutput.append(" ConditionalExpression");
              	return true; 
              }
              
              @Override
              public boolean visit(DoStatement node) {
              	astOutput.append(" DoStatement");
              	return true;
              }
              
              @Override
              public boolean visit(LambdaExpression node) {
              	astOutput.append(" LambdaExpression");
              	return true; 
              }
              
              @Override
              public boolean visit(AnonymousClassDeclaration node) {
              	astOutput.append(" AnonymousClassDeclaration");
              	return true; 
              }
              
              @Override
              public boolean visit(ArrayCreation node) {
              	astOutput.append(" ArrayCreation");
              	return true; 
              }
              
              @Override
              public boolean visit(AssertStatement node) {
              	astOutput.append(" AssertStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(Assignment node) {
              	astOutput.append(" Assignment");
              	return true; 
              }
              
              @Override
              public boolean visit(BreakStatement node) {
              	astOutput.append(" BreakStatement");
              	return true;
              }
              
              @Override
              public boolean visit(ContinueStatement node) {
              	astOutput.append(" ContinueStatement");
              	return true;
              }
              
              @Override
              public boolean visit(PostfixExpression node) {
              	astOutput.append(" PostfixExpression");
              	return true;
              }
              
              @Override
              public boolean visit(PrefixExpression node) {
              	astOutput.append(" PrefixExpression");
              	return true; 
              }
              
              @Override
              public boolean visit(EmptyStatement node) {
              	astOutput.append(" EmptyStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(FieldDeclaration node) {
              	astOutput.append(" FieldDeclaration");
              	return true; 
              }
              
              @Override
              public boolean visit(ExpressionStatement node) {
              	astOutput.append(" ExpressionStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(LabeledStatement node) {
              	astOutput.append(" LabeledStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(ClassInstanceCreation node) {
              	astOutput.append(" ClassInstanceCreation");
              	return true; 
              }
              
              @Override
              public boolean visit(SwitchExpression node) {
              	astOutput.append(" SwitchExpression");
              	return true;
              }
              
              @Override
              public boolean visit(SynchronizedStatement node) {
              	astOutput.append(" SynchronizedStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(ThrowStatement node) {
              	astOutput.append(" ThrowStatement");
              	return true; 
              }
              
              @Override
              public boolean visit(SingleVariableDeclaration node) {
              	astOutput.append(" SingleVariableDeclaration");
              	return true; 
              }
              
              @Override
              public boolean visit(WhileStatement node) {
              	astOutput.append(" WhileStatement");
              	return true; 
              }
          
              @Override
              public boolean visit(YieldStatement node) {
            	astOutput.append(" YieldStatement");
              	return true;
              }

            
        });
        
        return astOutput.toString().trim();
    }
}



//import java.io.BufferedReader;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import org.eclipse.jdt.core.dom.*;
//
//public class SPTCODEASTVisitor2 {
//
//   // private static final boolean TRUE = false;
//
//	private static String formatCode(String codeSnippet) {
//        // Removing the leading and trailing quote
////        if (codeSnippet.startsWith("\"") && codeSnippet.endsWith("\"")) {
////            codeSnippet = codeSnippet.substring(1, codeSnippet.length() - 1);
////        }
////        // Unescaping newline, tab, quote, and return characters
////        codeSnippet = codeSnippet.replace("\\n", "\n")
////                                 .replace("\\t", "\t")
////                                 .replace("\\r", "\r")
////                                 .replace("\\\"", "\"");
//
//        // Wrapping in a dummy class structure
//        return "public class DummyClass {\n" + codeSnippet + "\n}";
//    }
//	
//	private static String wrapWithClass(String method) {
//	    return "public class DummyWrapper {\n" +
//	           method.replace("string", "String")
//	                 .replace("file", "File")
//	                 .replace("ioException", "IOException")
//	                 .replace("[MSK]", "0xFF") + // Assume 0xFF as a mask value
//	           "\n}";
//	}
//
//    public static void main(String[] args) {
//        String javaFileName = "input/input1-pre.txt";
//        String outputFileName = "input/output1-pre.txt";
//
//        try (BufferedReader reader = new BufferedReader(new FileReader(javaFileName));
//             FileWriter writer = new FileWriter(outputFileName)) {
//
//            //StringBuilder snippetBuilder = new StringBuilder();
//            String line;
////            boolean isCodeSnippet = TRUE ;
//
//            while ((line = reader.readLine()) != null) {
//            	//snippetBuilder.append(line).append("\n"); // Ensure newlines are preserved
//            
//                // Check if the line starts or ends a code snippet
////                if (line.trim().startsWith("\"")) {
////                    isCodeSnippet = true;
////                    snippetBuilder.append(line);
////                    if (line.trim().endsWith("\"")) {
////                        isCodeSnippet = false;
////                    }
////                } else if (line.trim().endsWith("\"")) {
////                    snippetBuilder.append("\n").append(line);
////                    isCodeSnippet = false;
////                } else if (isCodeSnippet) {
////                    snippetBuilder.append("\n").append(line);
////                }
//
//                // If the code snippet is complete, process it
////                if (!isCodeSnippet && snippetBuilder.length() > 0) {
//                    String formattedCode = wrapWithClass(line.toString());
//                    ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
//                    parser.setSource(formattedCode.toCharArray());
//                    parser.setKind(ASTParser.K_COMPILATION_UNIT);
//
//                    CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//                    final StringBuilder astOutput = new StringBuilder();
//                    cu.accept(new ASTVisitor() {
//                        public boolean visit(MethodDeclaration node) {
//                            astOutput.append("MethodDeclaration");//.append(node.getName()).append(" ")
//                            return true; 
//                        }
//
//                        public boolean visit(VariableDeclarationFragment node) {
//                            astOutput.append(" VariableDeclarationFragment");//.append(node.getName()).append(" ");
//                            return true;
//                        }
//                        
//                        @Override
//	                      public boolean visit(MethodInvocation node) {
//                        	astOutput.append(" MethodInvocation");// + node.getName());
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(IfStatement node) {
//	                    	astOutput.append(" IfStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ReturnStatement node) {
//	                    	astOutput.append(" ReturnStatement");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(CastExpression node) {
//	                    	astOutput.append(" CastExpression");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ParenthesizedExpression node) {
//	                    	astOutput.append(" ParenthesizedExpression");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(InstanceofExpression node) {
//	                    	astOutput.append(" InstanceofExpression");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(VariableDeclarationStatement node) {
//	                    	astOutput.append(" VariableDeclarationStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ForStatement node) {
//	                    	  astOutput.append(" ForStatement");
//	                          return super.visit(node);
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(EnhancedForStatement node) {
//	                    	  astOutput.append(" EnhancedForStatement");
//	                          return super.visit(node); 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(SwitchStatement node) {
//	                    	astOutput.append(" SwitchStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(TryStatement node) {
//	                    	astOutput.append(" TryStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(CatchClause node) {
//	                      	astOutput.append(" CatchClause");
//	                          return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(InfixExpression node) {
//	                      	astOutput.append(" InfixExpression");
//	                      	return true;
//	                      }
//	                     
//	                      @Override
//	                      public boolean visit(ConditionalExpression node) {
//	                      	astOutput.append(" ConditionalExpression");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(DoStatement node) {
//	                      	astOutput.append(" DoStatement");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(LambdaExpression node) {
//	                      	astOutput.append(" LambdaExpression");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(AnonymousClassDeclaration node) {
//	                      	astOutput.append(" AnonymousClassDeclaration");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ArrayCreation node) {
//	                      	astOutput.append(" ArrayCreation");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(AssertStatement node) {
//	                      	astOutput.append(" AssertStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(Assignment node) {
//	                      	astOutput.append(" Assignment");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(BreakStatement node) {
//	                      	astOutput.append(" BreakStatement");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ContinueStatement node) {
//	                      	astOutput.append(" ContinueStatement");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(PostfixExpression node) {
//	                      	astOutput.append(" PostfixExpression");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(PrefixExpression node) {
//	                      	astOutput.append(" PrefixExpression");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(EmptyStatement node) {
//	                      	astOutput.append(" EmptyStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(FieldDeclaration node) {
//	                      	astOutput.append(" FieldDeclaration");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ExpressionStatement node) {
//	                      	astOutput.append(" ExpressionStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(LabeledStatement node) {
//	                      	astOutput.append(" LabeledStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ClassInstanceCreation node) {
//	                      	astOutput.append(" ClassInstanceCreation");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(SwitchExpression node) {
//	                      	astOutput.append(" SwitchExpression");
//	                      	return true;
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(SynchronizedStatement node) {
//	                      	astOutput.append(" SynchronizedStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(ThrowStatement node) {
//	                      	astOutput.append(" ThrowStatement");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(SingleVariableDeclaration node) {
//	                      	astOutput.append(" SingleVariableDeclaration");
//	                      	return true; 
//	                      }
//	                      
//	                      @Override
//	                      public boolean visit(WhileStatement node) {
//	                      	astOutput.append(" WhileStatement");
//	                      	return true; 
//	                      }
//                      
//	                      @Override
//	                      public boolean visit(YieldStatement node) {
//	                    	astOutput.append(" YieldStatement");
//	                      	return true;
//	                      }
//
//                        // Add more node types if needed
//                    });
//
//                    // Write the collected AST output to file and append a newline to separate entries
//                    writer.write(astOutput.toString().trim() + "\n");
//                    // Clear the snippetBuilder for the next snippet
//                    //snippetBuilder.setLength(0);
//                }
//            //}
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
//
////
////public class SPTCODEASTVisitor extends ASTVisitor {
////
////    private void print(String message) {
////        System.out.println(message);
////    }
////
////    public boolean visit(MethodDeclaration node) {
////        print("method_declaration");
////        return true; 
////    }
////
//////    public boolean visit(IfStatement node) {
//////        print("if_statement__");
//////        return true; 
//////    }
//////
//////    public void endVisit(IfStatement node) {
//////        print("__if_statement");
//////    }
//////    
//////    public boolean visit(ReturnStatement node) {
//////        print("return_statement");
//////        return true; 
//////    }
//////
//////    public boolean visit(CastExpression node) {
//////        print("cast_expression");
//////        return true; 
//////    }
//////
//////    public boolean visit(ParenthesizedExpression node) {
//////        print("parenthesized_expression__");
//////        return true; 
//////    }
//////
//////    public void endVisit(ParenthesizedExpression node) {
//////        print("__parenthesized_expression");
//////    }
//////
//////    public boolean visit(MethodInvocation node) {
//////        print("method_invocation");
//////        return true; 
//////    }
//////
//////    public boolean visit(InstanceofExpression node) {
//////        print("instanceof_expression");
//////        return true; 
//////    }
//////
//////    public boolean visit(VariableDeclarationStatement node) {
//////        print("variable_declaration_statement");
//////        return true; 
//////    }
//////
//////    public boolean visit(ForStatement node) {
//////        print("for_statement");
//////        return true; 
//////    }
//////
//////    public boolean visit(EnhancedForStatement node) {
//////        print("enhanced_for_statement");
//////        return true; 
//////    }
//////
//////    public boolean visit(SwitchStatement node) {
//////        print("switch_statement");
//////        return true; 
//////    }
//////
//////    public boolean visit(TryStatement node) {
//////        print("try_statement");
//////        return true; 
//////    }
//////
//////    public boolean visit(CatchClause node) {
//////        print("catch_clause");
//////        return true; 
//////    }
//////    
//////    public boolean visit(InfixExpression node) {
//////        print("binary_expression");
//////        return true; 
//////    }
//////    
//////    public boolean visit(ConditionalExpression node) {
//////        print("conditional_expression");
//////        return true; 
//////    }
//////    
//////    public boolean visit(DoStatement node) {
//////        print("do_statement");
//////        return true; 
//////    }
////    
////    public boolean visit(SimpleName node) {
////    	System.out.println("Token: " + node.getIdentifier());  // Print the token
////        return true;  // Continue visiting other nodes
////        
////    }
////
////    @Override
////    public boolean visit(MethodInvocation node) {
////    	System.out.println("Visiting MethodInvocation: " + node.getName());
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(IfStatement node) {
////    	System.out.println("Visiting IfStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(ReturnStatement node) {
////    	System.out.println("Visiting ReturnStatement");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(CastExpression node) {
////    	System.out.println("Visiting CastExpression");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(ParenthesizedExpression node) {
////    	System.out.println("Visiting ParenthesizedExpression");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(InstanceofExpression node) {
////    	System.out.println("Visiting InstanceofExpression");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(VariableDeclarationStatement node) {
////    	System.out.println("Visiting VariableDeclarationStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(ForStatement node) {
////    	System.out.println("Visiting ForStatement");
////        return super.visit(node);
////    }
////    
////    @Override
////    public boolean visit(EnhancedForStatement node) {
////    	System.out.println("Visiting EnhancedForStatement");
////        return super.visit(node); 
////    }
////    
////    @Override
////    public boolean visit(SwitchStatement node) {
////    	System.out.println("Visiting SwitchStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(TryStatement node) {
////    	System.out.println("Visiting TryStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(CatchClause node) {
////    	System.out.println("Visiting CatchClause");
////        return true; 
////    }
////    
////    @Override
////    public boolean visit(InfixExpression node) {
////    	System.out.println("Visiting InfixExpression");
////    	return true;
////    }
////   
////    @Override
////    public boolean visit(ConditionalExpression node) {
////    	System.out.println("Visiting ConditionalExpression");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(DoStatement node) {
////    	System.out.println("Visiting DoStatement");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(LambdaExpression node) {
////    	System.out.println("Visiting LambdaExpression");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(AnonymousClassDeclaration node) {
////    	System.out.println("Visiting AnonymousClassDeclaration");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(ArrayCreation node) {
////    	System.out.println("Visiting ArrayCreation");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(AssertStatement node) {
////    	System.out.println("Visiting AssertStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(Assignment node) {
////    	System.out.println("Visiting Assignment");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(BreakStatement node) {
////    	System.out.println("Visiting BreakStatement");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(ContinueStatement node) {
////    	System.out.println("Visiting ContinueStatement");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(PostfixExpression node) {
////    	System.out.println("Visiting PostfixExpression");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(PrefixExpression node) {
////    	System.out.println("Visiting PrefixExpression");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(EmptyStatement node) {
////    	System.out.println("Visiting EmptyStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(FieldDeclaration node) {
////    	System.out.println("Visiting FieldDeclaration");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(ExpressionStatement node) {
////    	System.out.println("Visiting ExpressionStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(LabeledStatement node) {
////    	System.out.println("Visiting LabeledStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(ClassInstanceCreation node) {
////    	System.out.println("Visiting ClassInstanceCreation");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(SwitchExpression node) {
////    	System.out.println("Visiting SwitchExpression");
////    	return true;
////    }
////    
////    @Override
////    public boolean visit(SynchronizedStatement node) {
////    	System.out.println("Visiting SynchronizedStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(ThrowStatement node) {
////    	System.out.println("Visiting ThrowStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(SingleVariableDeclaration node) {
////    	System.out.println("Visiting SingleVariableDeclaration");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(WhileStatement node) {
////    	System.out.println("Visiting WhileStatement");
////    	return true; 
////    }
////    
////    @Override
////    public boolean visit(YieldStatement node) {
////    	System.out.println("Visiting YieldStatement");
////    	return true;
////    }
////
//////    public static void main(String[] args) {
//////        String source = "public class Test {\n"
//////            + "    private boolean isApplicable(RepositoryResource resource) {\n"
//////            + "        if (resource instanceof ApplicableToProduct) {\n"
//////            + "            if (((ApplicableToProduct) resource).getAppliesTo() == null) {\n"
//////            + "                return true; // No appliesTo -> applicable\n"
//////            + "            }\n"
//////            + "        }\n"
//////            + "        return ((RepositoryResourceImpl) resource).doesResourceMatch(productDefinitions, null);\n"
//////            + "    }\n"
//////            + "}\n";
////    
////      public static void main(String[] args) {
////      String source = "public class Test {\n"
////          + "    private boolean isApplicable(RepositoryResource resource) {\n"
////          + "        if (resource instanceof ApplicableToProduct) {\n"
////          + "            if (((ApplicableToProduct) resource).getAppliesTo() == null) {\n"
////          + "                return true; // No appliesTo -> applicable\n"
////          + "            }\n"
////          + "        }\n"
////          + "        return ((RepositoryResourceImpl) resource).doesResourceMatch(productDefinitions, null);\n"
////          + "    }\n"
////          + "}\n";
////
////        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
////        parser.setSource(source.toCharArray());
////        parser.setKind(ASTParser.K_COMPILATION_UNIT);
////
////        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
////        cu.accept(new SPTCODEASTVisitor());
////    }
////}
////
