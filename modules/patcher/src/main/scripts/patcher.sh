#!/bin/sh

java() {
	../runtime/jre1.8.0_172/bin/java $*
}
java -classpath ./patcher.jar:./javassist.jar org.gotti.wurmunlimited.patcher.PatchClientJar
