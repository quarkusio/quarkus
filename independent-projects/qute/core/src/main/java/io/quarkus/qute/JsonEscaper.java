package io.quarkus.qute;

import java.util.Optional;

import io.quarkus.qute.TemplateNode.Origin;

public class JsonEscaper implements ResultMapper {

    @Override
    public boolean appliesTo(Origin origin, Object result) {
        if (result instanceof RawString) {
            return false;
        }
        Optional<Variant> variant = origin.getVariant();
        if (variant.isPresent()) {
            return variant.get().getContentType().startsWith("application/json");
        }
        return false;
    }

    @Override
    public String map(Object result, Expression expression) {
        return escapeJson(result.toString());
    }

    String escapeJson(String value) {
        if (value == null)
          return "";
    
        StringBuilder b = new StringBuilder();
        for (char c : value.toCharArray()) {
          if (c == '\r')
            b.append("\\r");
          else if (c == '\n')
            b.append("\\n");
          else if (c == '\t')
            b.append("\\t");
          else if (c == '"')
            b.append("\\\"");
          else if (c == '\\')
            b.append("\\\\");
          else if (c == ' ')
            b.append(" ");
          else if (Character.isWhitespace(c)) {
            b.append("\\u"+String.format("%4s", Integer.toHexString(c)).replace(' ','0'));
          } else if (((int) c) < 32)
            b.append("\\u"+String.format("%4s", Integer.toHexString(c)).replace(' ','0'));
          else
            b.append(c);
        }
        return b.toString();
      }
}