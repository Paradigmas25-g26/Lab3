JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
JAVAC=$(JAVA_HOME)/bin/javac
JAVA=$(JAVA_HOME)/bin/java

SRC_DIR=src
OUT_DIR=out
MAIN_CLASS=FeedReaderMain
LIB_DIR=lib
SPARK_HOME=../spark-3.5.5-bin-hadoop3
CLASSPATH=$(OUT_DIR):$(LIB_DIR)/json-20240303.jar:$(SPARK_HOME)/jars/*

SOURCES=$(shell find $(SRC_DIR) -name "*.java")

all: build

build:
	mkdir -p $(OUT_DIR)
	$(JAVAC) -cp "$(CLASSPATH)" -d $(OUT_DIR) $(SOURCES)

run: build
	$(JAVA) -cp "$(CLASSPATH)" $(MAIN_CLASS)

clean:
	rm -rf $(OUT_DIR)