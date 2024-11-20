# #!/bin/bash

# echo "###########################################"
# if [ -z "$JAVA_HOME" ]; then
#   echo "JAVA_HOME is not set"
# else
#   echo "JAVA_HOME is set to: $JAVA_HOME"
# fi

# echo "###########################################"
# echo $(java -version)

# BASE_PATH="/home/user1-selab3/Documents/research-spt-code-data/workspace-mvn-vscode-eclipse/jdtparser-sptcode-data/"

# if [ ! -d "$BASE_PATH" ]; then
#   echo "Error: Base path '$BASE_PATH' does not exist."
#   exit 1
# fi
# echo "###########################################"
# echo "Base path '$BASE_PATH' exists."

# JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
# if [[ "$JAVA_VERSION" != 17* ]]; then
#   echo "Error: JDK 17 is required. Current version is $JAVA_VERSION."
#   exit 1
# fi
# echo "###########################################"
# echo "Java version '$JAVA_VERSION' is compatible."

# CLASSPATH="$BASE_PATH/lib/JARS/*:$BASE_PATH/lib/*:$BASE_PATH/target/jdtparser-sptcode-data-jar-with-dependencies.jar"
# echo "###########################################"
# # java -cp "$CLASSPATH" extract.json.ser.MainExtractJSON 
# java -cp "$CLASSPATH" rawmethod.gentraindata.MainParserJavaSmallJson.java
# echo "###########################################"

#!/bin/bash

echo "###########################################"
if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME is not set"
else
  echo "JAVA_HOME is set to: $JAVA_HOME"
fi

echo "###########################################"
java -version

#BASE_PATH="/home/user1-selab3/Documents/research-spt-code-data/workspace-mvn-vscode-eclipse/jdtparser-sptcode-data/"
#BASE_PATH="/home/user1-selab3/work-dream/research-dream/CODE/research-dream-spt-code-data/workspace-mvn-vscode-eclipse/jdtparser-sptcode-data"
BASE_PATH="/home/user1-system11/Documents/research-shradha/CODE-SPT-Code/workspace-mvn-vscode-eclipse/jdtparser"
if [ ! -d "$BASE_PATH" ]; then
  echo "Error: Base path '$BASE_PATH' does not exist."
  exit 1
fi
echo "###########################################"
echo "Base path '$BASE_PATH' exists."

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
if [[ "$JAVA_VERSION" != 17* ]]; then
  echo "Error: JDK 17 is required. Current version is $JAVA_VERSION."
  exit 1
fi
echo "###########################################"
echo "Java version '$JAVA_VERSION' is compatible."

# JAR_FILE="$BASE_PATH/target/jdtparser-sptcode-data-jar-with-dependencies.jar"
JAR_FILE="$BASE_PATH/target/jdtparser-jar-with-dependencies.jar"

# Ensure the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "Error: JAR file not found at $JAR_FILE"
  exit 1
fi

CLASSPATH="$BASE_PATH/lib/JARS/*:$BASE_PATH/lib/*:$JAR_FILE"
echo "###########################################"
echo "Using CLASSPATH: $CLASSPATH"

# Run the Java program and print the parameters
echo "Running MainParserJavaSmallJson with parameters: $@"
#java -cp "$CLASSPATH" rawmethod.gentraindata.MainParserJavaSmallJson "$@"
#java -cp "$CLASSPATH" rawmethod.genast.ASTExprGenerator "$@"
# java -cp "$CLASSPATH" rawmethod.genast.TokenCounter "$@" -recent

# java -cp "$CLASSPATH" dataflow.MainDataFlowAnalysis "$@"
# java -cp "$CLASSPATH" dataflow.SimpleExampleDefUseImp2 "$@"
# java -cp "$CLASSPATH" DefUseSimpleNameMain_correct "$@"
#java -cp "$CLASSPATH" DefUseSimpleNameMain_correctImp2 "$@"
# java -cp "$CLASSPATH" IfElseMatcher "$@"
java -cp "$CLASSPATH" ParsingValidation "$@"

#java -cp "$CLASSPATH" rawmethod.genast.ExtractNL "$@"
#java -cp "$CLASSPATH" rawmethod.gentraindata.MainParserJavaSmallJson "$@"
#java -cp "$CLASSPATH" extract.json.ser.MainExtractJSON "$@"
echo "###########################################"


