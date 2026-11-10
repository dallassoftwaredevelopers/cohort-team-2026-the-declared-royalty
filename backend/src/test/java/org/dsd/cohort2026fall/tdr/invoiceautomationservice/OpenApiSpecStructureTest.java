package org.dsd.cohort2026fall.tdr.invoiceautomationservice;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Guards the multi-file OpenAPI layout: a small root file and fragments whose $refs all resolve. */
class OpenApiSpecStructureTest {

    private static final Path SPEC_DIR = Path.of("src/main/resources/openapi");
    private static final Path ROOT = SPEC_DIR.resolve("openapi.yaml");
    private static final Pattern FILE_REF = Pattern.compile("\\$ref:\\s*['\"]?([^'\"#\\s][^'\"\\s]*\\.yaml)['\"]?");

    @Test
    void rootSpecOnlyReferencesFragments() throws IOException {
        assertThat(ROOT).exists();
        List<String> lines = Files.readAllLines(ROOT);
        assertThat(lines.size()).isLessThan(150);

        boolean inPaths = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("paths:")) {
                inPaths = true;
                continue;
            }
            if (inPaths && !line.isBlank() && !line.startsWith(" ")) {
                inPaths = false;
            }
            if (inPaths && line.matches("^ {2}\\S.*:\\s*$")) {
                assertThat(lines.get(i + 1).trim()).startsWith("$ref:");
            }
        }
        assertThat(resolveAllRefs(ROOT)).isEmpty();
    }

    @Test
    void everyFragmentRefResolves() throws IOException {
        List<String> unresolved = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SPEC_DIR)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".yaml")).toList()) {
                unresolved.addAll(resolveAllRefs(file));
            }
        }
        assertThat(unresolved).isEmpty();
    }

    /** Returns descriptions of file $refs in the given file whose target does not exist. */
    private static List<String> resolveAllRefs(Path file) throws IOException {
        List<String> missing = new ArrayList<>();
        Matcher matcher = FILE_REF.matcher(Files.readString(file));
        while (matcher.find()) {
            Path target = file.getParent().resolve(matcher.group(1)).normalize();
            if (!Files.exists(target)) {
                missing.add(file + " -> " + matcher.group(1));
            }
        }
        return missing;
    }
}
