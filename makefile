FLAGS = -g
JC = javac
JVM = java
SRC = src
LIB = lib

.SUFFIXES: .java .class

JAR = json-simple-1.1.1.jar
JAR2 = commons-codec-1.10.jar
MAIN = SimpleSearchEngine

default: classes

classes:
	$(JC) -cp $(LIB)/$(JAR):$(LIB)/$(JAR2)  $(JFLAGS) $(SRC)/*.java -d .

run: $(MAIN).class
	$(JVM) -cp $(LIB)/$(JAR):$(LIB)/$(JAR2):.  $(MAIN) hOVysMk4Ynb2GSI7COBxmjJf+GXpgKMP0xcy3RpYVY4  0.9 musk

clean:
	$(RM) *.class
