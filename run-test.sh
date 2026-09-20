#!/usr/bin/env sh
set -eu

classes="build/test-classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java src/test/java -name '*.java')
java -cp "$classes" org.example.nonprofit.ReportEmailServiceTest
