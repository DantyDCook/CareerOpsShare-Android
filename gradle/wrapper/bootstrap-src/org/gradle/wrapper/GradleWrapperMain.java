package org.gradle.wrapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Small, dependency-free compatibility bootstrap used only to make this
 * source bundle self-contained. After the first successful Gradle run,
 * execute `./gradlew wrapper --gradle-version 9.5.0` to replace this JAR
 * with Gradle's official generated wrapper files.
 */
public final class GradleWrapperMain {
    private static final int MAX_REDIRECTS = 10;

    private GradleWrapperMain() {}

    public static void main(String[] args) throws Exception {
        Path projectRoot = locateProjectRoot();
        Path propertiesPath = projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties");
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            props.load(in);
        }

        String distributionUrl = require(props, "distributionUrl");
        String expectedSha = props.getProperty("distributionSha256Sum", "").trim().toLowerCase();
        int timeoutMs = parseInt(props.getProperty("networkTimeout"), 10_000);

        String gradleUserHome = System.getenv("GRADLE_USER_HOME");
        if (gradleUserHome == null || gradleUserHome.isBlank()) {
            gradleUserHome = Paths.get(System.getProperty("user.home"), ".gradle").toString();
        }

        URI uri = URI.create(distributionUrl.replace("\\:", ":"));
        String fileName = Path.of(uri.getPath()).getFileName().toString();
        String distName = fileName.endsWith(".zip") ? fileName.substring(0, fileName.length() - 4) : fileName;
        String cacheKey = expectedSha.isBlank() ? Integer.toHexString(distributionUrl.hashCode()) : expectedSha.substring(0, 16);
        Path distBase = Paths.get(gradleUserHome, "wrapper", "dists", distName, cacheKey);
        Path zipFile = distBase.resolve(fileName);
        Path readyMarker = distBase.resolve(".careerops-wrapper-ready");

        Files.createDirectories(distBase);
        Path gradleHome = findGradleHome(distBase);

        if (gradleHome == null || !Files.exists(readyMarker)) {
            if (!Files.exists(zipFile)) {
                System.out.println("Downloading " + distributionUrl);
                download(uri, zipFile, timeoutMs);
            }

            if (!expectedSha.isBlank()) {
                String actual = sha256(zipFile);
                if (!actual.equals(expectedSha)) {
                    Files.deleteIfExists(zipFile);
                    throw new IOException("Gradle distribution SHA-256 mismatch. Expected " + expectedSha + " but got " + actual);
                }
            }

            Path tempDir = distBase.resolve(".extracting");
            deleteRecursively(tempDir);
            Files.createDirectories(tempDir);
            unzip(zipFile, tempDir);

            Path extracted = findGradleHome(tempDir);
            if (extracted == null) {
                throw new IOException("Downloaded archive did not contain a Gradle distribution directory");
            }

            Path finalHome = distBase.resolve(extracted.getFileName().toString());
            deleteRecursively(finalHome);
            try {
                Files.move(extracted, finalHome, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveFailure) {
                Files.move(extracted, finalHome, StandardCopyOption.REPLACE_EXISTING);
            }
            deleteRecursively(tempDir);
            Files.writeString(readyMarker, "ready\n");
            gradleHome = finalHome;
        }

        int exit = launchGradle(gradleHome, args);
        System.exit(exit);
    }

    private static Path locateProjectRoot() throws Exception {
        Path jar = Paths.get(
            GradleWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()
        ).toAbsolutePath().normalize();
        Path wrapperDir = Files.isDirectory(jar) ? jar : jar.getParent();
        if (wrapperDir == null || wrapperDir.getParent() == null || wrapperDir.getParent().getParent() == null) {
            throw new IOException("Unable to locate project root from wrapper JAR");
        }
        return wrapperDir.getParent().getParent();
    }

    private static String require(Properties props, String key) {
        String value = props.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Missing " + key + " in gradle-wrapper.properties");
        return value;
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static void download(URI uri, Path destination, int timeoutMs) throws Exception {
        Path partial = destination.resolveSibling(destination.getFileName() + ".part");
        Files.deleteIfExists(partial);

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            Files.copy(Paths.get(uri), partial, StandardCopyOption.REPLACE_EXISTING);
        } else {
            URL current = uri.toURL();
            HttpURLConnection connection = null;
            for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
                connection = (HttpURLConnection) current.openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(timeoutMs);
                connection.setReadTimeout(timeoutMs);
                connection.setRequestProperty("User-Agent", "CareerOpsShare-GradleBootstrap/0.1.1");
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    connection.disconnect();
                    if (location == null) throw new IOException("Redirect without Location while downloading Gradle");
                    current = new URL(current, location);
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new IOException("HTTP " + status + " while downloading Gradle from " + current);
                }
                try (InputStream in = new BufferedInputStream(connection.getInputStream());
                     OutputStream out = Files.newOutputStream(partial)) {
                    in.transferTo(out);
                } finally {
                    connection.disconnect();
                }
                connection = null;
                break;
            }
            if (connection != null) {
                connection.disconnect();
                throw new IOException("Too many redirects while downloading Gradle");
            }
        }

        try {
            Files.move(partial, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicMoveFailure) {
            Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static void unzip(Path zip, Path destination) throws IOException {
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zip)))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) {
                    throw new IOException("Blocked unsafe ZIP entry: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zin, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zin.closeEntry();
            }
        }
    }

    private static Path findGradleHome(Path base) throws IOException {
        if (!Files.exists(base)) return null;
        try (var stream = Files.list(base)) {
            return stream
                .filter(Files::isDirectory)
                .filter(p -> Files.exists(p.resolve("bin/gradle")) || Files.exists(p.resolve("bin/gradle.bat")))
                .findFirst()
                .orElse(null);
        }
    }

    private static int launchGradle(Path gradleHome, String[] args) throws Exception {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        List<String> command = new ArrayList<>();
        if (windows) {
            command.add("cmd.exe");
            command.add("/d");
            command.add("/c");
            command.add(gradleHome.resolve("bin/gradle.bat").toString());
        } else {
            command.add("sh");
            command.add(gradleHome.resolve("bin/gradle").toString());
        }
        for (String arg : args) command.add(arg);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new java.io.File(System.getProperty("user.dir")));
        pb.inheritIO();
        return pb.start().waitFor();
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path)) return;
        try (var walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); }
                catch (IOException e) { throw new RuntimeException(e); }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException io) throw io;
            throw e;
        }
    }
}
