package util;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

public class UtilASTJavaParser {
   public static CheckParsing checkParsable(String contents, String givenClassName) {
      SymbolResolver symbolResolver = new JavaSymbolSolver(new CombinedTypeSolver());
      ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver(symbolResolver);
      JavaParser javaParser = new JavaParser(parserConfiguration);

      // Check Parse Failure
      ParseResult<CompilationUnit> parseResults = javaParser.parse(contents);
      if (!parseResults.isSuccessful() && !parseResults.getResult().isPresent()) {
         return CheckParsing.PARSE_FAILURE;
      }

      CompilationUnit cu = parseResults.getResult().get();

      // Check Unparsed
      NodeList<TypeDeclaration<?>> types = cu.getTypes();
      if (types.size() == 0) {
         return CheckParsing.UNPARSED;
      }

      // Check Bad Class
      TypeDeclaration<?> typeDecl = types.get(0);
      String className = typeDecl.getName().toString();
      if (className.equals(givenClassName) == false) {

         if (UtilAST.checkClassName(contents, givenClassName) == false) {
            return CheckParsing.BAD_CLASS;
         }
      }

      // Check Bad Methods
      int numMethods = typeDecl.getMethods().size();
      if (numMethods != 1) {
         return CheckParsing.BAD_METHODS;
      }

      return CheckParsing.Pass;
   }

   public static String getMethod(String contents) {
      SymbolResolver symbolResolver = new JavaSymbolSolver(new CombinedTypeSolver());
      ParserConfiguration parserConfiguration = new ParserConfiguration().setSymbolResolver(symbolResolver);
      JavaParser javaParser = new JavaParser(parserConfiguration);

      // Check Parse Failure
      ParseResult<CompilationUnit> parseResults = javaParser.parse(contents);
      if (!parseResults.isSuccessful() && !parseResults.getResult().isPresent()) {
         return null;
      }

      CompilationUnit cu = parseResults.getResult().get();
      // Check Unparsed
      NodeList<TypeDeclaration<?>> types = cu.getTypes();
      if (types.size() == 0) {
         return null;
      }

      TypeDeclaration<?> typeDecl = types.get(0);

      int numMethods = typeDecl.getMethods().size();
      if (numMethods != 1) {
         return null;
      }

      String methodStr = typeDecl.getMethods().get(0).toString();
      return methodStr;
   }
}
