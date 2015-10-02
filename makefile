JFLAGS = -g
jc = javac
JVM = java
BIN = bin
SRC = src
LIB = lib

.SUFFIXES: .java .class

JAR = commons-codec-1.10.jar

.java.class:
		$(JC) -cp $(LIB)/$(JAR) $(JFLAGS) $*.java

CLASSES = SimpleSearchEngine.java

MAIN = SimpleSearchEngine

default: $(BIN)

classes: $(CLASSES:.java=.class)

run: $(MAIN).class
		$(JVM) -cp $(LIB)/$(JAR):$(BIN) $(MAIN)

clean:
		$(RM) $(BIN)/*.class
