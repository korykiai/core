package ai.koryki.model;

import ai.koryki.model.schema.Schema;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class JsonUtil {

    public static Schema readSchemaFromResource(String ressource) throws IOException {

        InputStream i = JsonUtil.class.getResourceAsStream(ressource);
        return readDatabaseJson(i);
    }

    public static Schema readDatabaseJson(InputStream in) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return mapper.readValue(r, Schema.class);
        }
    }

    public static <K, V> HashMap<K, V> readHashSetFromJson(InputStream in, TypeReference<HashMap<K, V>> ref) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {

            return  mapper.readValue(r, ref);
        }
    }

    public static HashMap<String, List<String>> readHashSetFromResource(String resource) throws IOException {
        return readHashSetFromJson(
                JsonUtil.class.getResourceAsStream(resource),
                new TypeReference<HashMap<String, List<String>>>() { });
    }

    public static HashMap<String, List<String>> readHashSetFromJson(InputStream in) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return  mapper.readValue(r, new TypeReference<HashMap<String, List<String>>>() {});
        }
    }

}
