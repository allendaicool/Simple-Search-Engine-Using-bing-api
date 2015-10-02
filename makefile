FLAGS = -g
JC = javac
JVM = java
SRC = src
LIB = lib

.SUFFIXES: .java .class

JAR = commons-codec-1.10.jar

MAIN = SimpleSearchEngine

default: classes

classes:
	$(JC) -cp $(LIB)/$(JAR) $(JFLAGS) $(SRC)/*.java -d .

run: $(MAIN).class
	$(JVM) -cp $(LIB)/$(JAR):. $(MAIN)

clean:
	$(RM) *.class
