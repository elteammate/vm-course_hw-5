package space.elteammate.lama;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LamaTest {

    private static final String LAMA_LANGUAGE_ID = "lama";

    record TestData(Path source, String input, String result) {
    }

    private static List<TestData> gatherTests() throws IOException {
        List<TestData> tests = new ArrayList<>();
        tests.addAll(gatherPlainTests(Paths.get("tests")));
        tests.addAll(gatherCramTests(Paths.get("Lama/regression")));
        // tests.addAll(gatherCramTests(Paths.get("Lama/regression_long/expressions")));
        // tests.addAll(gatherCramTests(Paths.get("Lama/regression_long/deep-expressions")));
        // tests.addAll(gatherPlainTests(Paths.get("Lama/performance")));
        return tests;
    }

    private static List<TestData> gatherPlainTests(Path path) throws IOException {
        List<TestData> tests = new ArrayList<>();
        if (Files.exists(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream.filter(p -> p.toString().endsWith(".lama"))
                        .forEach(p -> tests.add(new TestData(p, null, null)));
            }
        }
        tests.sort(Comparator.comparing(t -> t.source.toString()));
        return tests;
    }

    private static List<TestData> gatherCramTests(Path path) throws IOException {
        List<TestData> tests = new ArrayList<>();
        if (Files.exists(path)) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream.filter(p -> p.toString().endsWith(".t"))
                        .forEach(p -> tests.addAll(parseCramFile(p)));
            }
        }
        tests.sort(Comparator.comparing(t -> t.source.toString()));
        return tests;
    }

    private static List<TestData> parseCramFile(Path path) {
        List<TestData> tests = new ArrayList<>();
        try {
            String content = new String(Files.readAllBytes(path));
            Pattern cmdPattern = Pattern.compile("^  \\$ (.*)");
            Pattern outputPattern = Pattern.compile("^  (.*)$");

            Path currentSource = null;
            Path currentInput = null;
            List<String> currentOutput = new ArrayList<>();

            for (String line : content.split("\\R")) {
                Matcher cmdMatcher = cmdPattern.matcher(line);
                if (cmdMatcher.matches()) {
                    if (currentSource != null) {
                        String result = currentOutput.isEmpty() ? null : String.join("\n", currentOutput).strip();
                        tests.add(new TestData(currentSource, readOptionalFile(currentInput), result));
                        currentOutput.clear();
                    }

                    String cmd = cmdMatcher.group(1);
                    Matcher lamaMatcher = Pattern.compile("(\\S+\\.lama)").matcher(cmd);
                    Matcher inputMatcher = Pattern.compile("<\\s*(\\S+\\.input)").matcher(cmd);

                    currentSource = lamaMatcher.find() ? path.getParent().resolve(lamaMatcher.group(1)) : null;
                    currentInput = inputMatcher.find() ? path.getParent().resolve(inputMatcher.group(1)) : null;

                } else if (currentSource != null) {
                    Matcher outMatcher = outputPattern.matcher(line);
                    if (outMatcher.matches()) {
                        currentOutput.add(outMatcher.group(1));
                    }
                }
            }
            if (currentSource != null) {
                String result = currentOutput.isEmpty() ? null : String.join("\n", currentOutput).strip();
                tests.add(new TestData(currentSource, readOptionalFile(currentInput), result));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tests;
    }

    private static String readOptionalFile(Path path) throws IOException {
        if (path != null && Files.exists(path)) {
            return new String(Files.readAllBytes(path));
        }
        return "";
    }

    public static Stream<TestData> lamaTestProvider() throws IOException {
        List<String> excludedTests = List.of(
                "Lama/regression/test054.lama",
                "Lama/regression/test110.lama",
                "Lama/regression/test111.lama",
                "Lama/regression/test803.lama"
        );
        return gatherTests().stream().filter(t -> !excludedTests.contains(t.source.toString().replace(File.separatorChar, '/')));
    }

    @DisplayName("Lama Test")
    @ParameterizedTest(name = "{index} => {0}")
    @MethodSource("lamaTestProvider")
    void testLama(TestData testData) throws IOException {
        byte[] inBuf = testData.input != null ? testData.input.getBytes() : new byte[0];
        var in = new ByteArrayInputStream(inBuf);
        var out = new ByteArrayOutputStream();

        var source = Source
                .newBuilder(LAMA_LANGUAGE_ID, testData.source.toFile())
                .build();

        int code = PolyglotLamaMain.executeSource(
                source,
                in,
                new PrintStream(out),
                false
        );
        if (testData.result != null) {
            String actualOutput = out.toString().strip().replaceAll("\\r\\n", "\n");
            String expectedOutput = testData.result.strip().replaceAll("\\r\\n", "\n");
            System.out.println(">>>> Expected >>>>");
            System.out.println(expectedOutput);
            System.out.println("<<<<<<<<<");
            System.out.println(">>>> Actual >>>>");
            System.out.println(actualOutput);
            System.out.println("<<<<<<<<<");
            assertEquals(expectedOutput, actualOutput);
        }
        assertEquals(0, code);
    }
}
