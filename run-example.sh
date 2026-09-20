#!/usr/bin/env sh
set -eu

classes="build/classes"
mkdir -p "$classes"
javac -d "$classes" $(find src/main/java -name '*.java')
java -cp "$classes" org.example.nonprofit.NonprofitReportApplication
