package org.example.nonprofit;

import java.util.LinkedHashMap;
import java.util.Map;

final class Json {
    private Json() {}

    static String object(Map<String, String> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (!first) json.append(',');
            first = false;
            json.append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
        }
        return json.append('}').toString();
    }

    static Map<String, Object> parseObject(String input) {
        Object value = new Parser(input).readValue();
        if (!(value instanceof Map<?, ?> map)) throw new IllegalArgumentException("Expected JSON object");
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) map;
        return result;
    }

    static String quote(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.append('\"').toString();
    }

    private static final class Parser {
        private final String text;
        private int at;

        Parser(String text) { this.text = text; }

        Object readValue() {
            spaces();
            if (at >= text.length()) throw error("Missing value");
            char c = text.charAt(at);
            if (c == '{') return readObject();
            if (c == '[') return readArray();
            if (c == '\"') return readString();
            if (text.startsWith("true", at)) { at += 4; return Boolean.TRUE; }
            if (text.startsWith("false", at)) { at += 5; return Boolean.FALSE; }
            if (text.startsWith("null", at)) { at += 4; return null; }
            return readNumber();
        }

        private Map<String, Object> readObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            at++;
            spaces();
            if (take('}')) return result;
            do {
                spaces();
                String key = readString();
                spaces();
                expect(':');
                result.put(key, readValue());
                spaces();
            } while (take(','));
            expect('}');
            return result;
        }

        private Object readArray() {
            java.util.List<Object> values = new java.util.ArrayList<>();
            at++;
            spaces();
            if (take(']')) return values;
            do {
                values.add(readValue());
                spaces();
            } while (take(','));
            expect(']');
            return values;
        }

        private String readString() {
            expect('\"');
            StringBuilder out = new StringBuilder();
            while (at < text.length()) {
                char c = text.charAt(at++);
                if (c == '\"') return out.toString();
                if (c != '\\') { out.append(c); continue; }
                if (at >= text.length()) throw error("Bad escape");
                char escaped = text.charAt(at++);
                switch (escaped) {
                    case '\"', '\\', '/' -> out.append(escaped);
                    case 'b' -> out.append('\b');
                    case 'f' -> out.append('\f');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (at + 4 > text.length()) throw error("Bad unicode escape");
                        out.append((char) Integer.parseInt(text.substring(at, at + 4), 16));
                        at += 4;
                    }
                    default -> throw error("Bad escape");
                }
            }
            throw error("Unclosed string");
        }

        private Number readNumber() {
            int start = at;
            while (at < text.length() && "-+0123456789.eE".indexOf(text.charAt(at)) >= 0) at++;
            String number = text.substring(start, at);
            try { return number.contains(".") ? Double.parseDouble(number) : Long.parseLong(number); }
            catch (NumberFormatException e) { throw error("Bad number"); }
        }

        private void spaces() { while (at < text.length() && Character.isWhitespace(text.charAt(at))) at++; }
        private boolean take(char expected) {
            if (at < text.length() && text.charAt(at) == expected) { at++; return true; }
            return false;
        }
        private void expect(char expected) { if (!take(expected)) throw error("Expected " + expected); }
        private IllegalArgumentException error(String message) { return new IllegalArgumentException(message + " at " + at); }
    }
}
