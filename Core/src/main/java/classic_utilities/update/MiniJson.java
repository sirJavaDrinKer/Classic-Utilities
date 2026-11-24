package classic_utilities.update;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


final class MiniJson {
    private static final int MAX_DEPTH = 64;

    private final String s;
    private int i;

    private MiniJson(String s) {
        this.s = s;
    }

    static Object parse(String text) throws IOException {
        if(text == null) throw new IOException("no JSON");
        MiniJson p = new MiniJson(text);
        p.ws();
        Object v = p.value(0);
        p.ws();
        if(p.i != p.s.length()) throw p.err("trailing data");
        return v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> obj(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    static String str(Object o) {
        return o instanceof String ? (String) o : null;
    }

    private Object value(int depth) throws IOException {
        if(depth > MAX_DEPTH) throw err("nesting too deep");
        if(i >= s.length()) throw err("unexpected end");
        char c = s.charAt(i);
        if(c == '{') return object(depth);
        if(c == '[') return array(depth);
        if(c == '"') return string();
        if(c == 't') {
            word("true");
            return Boolean.TRUE;
        }
        if(c == 'f') {
            word("false");
            return Boolean.FALSE;
        }
        if(c == 'n') {
            word("null");
            return null;
        }
        return number();
    }

    private Map<String, Object> object(int depth) throws IOException {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        i++;
        ws();
        if(peek() == '}') {
            i++;
            return out;
        }
        while(true) {
            ws();
            if(peek() != '"') throw err("expected a key");
            String key = string();
            ws();
            if(peek() != ':') throw err("expected ':'");
            i++;
            ws();
            out.put(key, value(depth + 1));
            ws();
            char c = peek();
            i++;
            if(c == '}') return out;
            if(c != ',') throw err("expected ',' or '}'");
        }
    }

    private List<Object> array(int depth) throws IOException {
        List<Object> out = new ArrayList<Object>();
        i++;
        ws();
        if(peek() == ']') {
            i++;
            return out;
        }
        while(true) {
            ws();
            out.add(value(depth + 1));
            ws();
            char c = peek();
            i++;
            if(c == ']') return out;
            if(c != ',') throw err("expected ',' or ']'");
        }
    }

    private String string() throws IOException {
        i++;
        StringBuilder sb = new StringBuilder();
        while(true) {
            if(i >= s.length()) throw err("unterminated string");
            char c = s.charAt(i++);
            if(c == '"') return sb.toString();
            if(c < 0x20) throw err("control character in string");
            if(c != '\\') {
                sb.append(c);
                continue;
            }
            if(i >= s.length()) throw err("unterminated escape");
            char e = s.charAt(i++);
            switch(e) {
                case '"': sb.append('"'); break;
                case '\\': sb.append('\\'); break;
                case '/': sb.append('/'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'u':
                    if(i + 4 > s.length()) throw err("bad \\u escape");
                    try {
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    } catch(NumberFormatException ex) {
                        throw err("bad \\u escape");
                    }
                    i += 4;
                    break;
                default:
                    throw err("bad escape");
            }
        }
    }

    private Object number() throws IOException {
        int start = i;
        while(i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if(start == i) throw err("unexpected character");
        String t = s.substring(start, i);
        try {
            boolean integral = t.indexOf('.') < 0 && t.indexOf('e') < 0 && t.indexOf('E') < 0;
            return integral ? (Object) Long.valueOf(t) : (Object) Double.valueOf(t);
        } catch(NumberFormatException e) {
            i = start;
            throw err("bad number");
        }
    }

    private void word(String w) throws IOException {
        if(!s.startsWith(w, i)) throw err("unexpected character");
        i += w.length();
    }

    private char peek() throws IOException {
        if(i >= s.length()) throw err("unexpected end");
        return s.charAt(i);
    }

    private void ws() {
        while(i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private IOException err(String msg) {
        return new IOException("bad JSON at offset " + i + ": " + msg);
    }
}
