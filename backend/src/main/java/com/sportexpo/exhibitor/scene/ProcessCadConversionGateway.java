package com.sportexpo.exhibitor.scene;

import com.sportexpo.exhibitor.file.LocalFileStorageService;
import com.sportexpo.exhibitor.file.StoredFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessCadConversionGateway implements CadConversionGateway {
    private final StoredFileRepository storedFileRepository;
    private final LocalFileStorageService fileStorageService;
    private final SceneCadConversionService conversionService;
    private final String workDir;
    private final String externalCommand;
    private final long timeoutSeconds;

    public ProcessCadConversionGateway(StoredFileRepository storedFileRepository,
                                       LocalFileStorageService fileStorageService,
                                       @Lazy SceneCadConversionService conversionService,
                                       @Value("${app.scene.cad.work-dir}") String workDir,
                                       @Value("${app.scene.cad.external-command}") String externalCommand,
                                       @Value("${app.scene.cad.timeout-seconds:300}") long timeoutSeconds) {
        this.storedFileRepository = storedFileRepository;
        this.fileStorageService = fileStorageService;
        this.conversionService = conversionService;
        this.workDir = workDir;
        this.externalCommand = externalCommand;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public void requestConversion(SceneSourceFile sourceFile, SceneConversionJob job) {
        if (externalCommand == null || externalCommand.isBlank()) {
            // Conversion is intentionally asynchronous. A worker may claim this PENDING job later;
            // the web request must not turn a valid upload into a failure merely because no local
            // converter process is configured.
            return;
        }
        Path input = null;
        Path output = null;
        Path log = null;
        try {
            var storedFile = storedFileRepository.findById(sourceFile.getStoredFileId())
                    .orElseThrow(() -> new IllegalStateException("CAD source file does not exist"));
            input = Path.of(workDir).resolve("source-" + job.getId() + "." + sourceFile.getSourceFormat().toLowerCase());
            output = Path.of(workDir).resolve("result-" + job.getId() + ".svg");
            log = Path.of(workDir).resolve("log-" + job.getId() + ".txt");
            Files.createDirectories(input.getParent());
            Files.write(input, fileStorageService.readContent(storedFile.getId()));
            var command = externalCommand.replace("{input}", quote(input.toAbsolutePath().toString()))
                    .replace("{output}", quote(output.toAbsolutePath().toString()));
            var builder = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? new ProcessBuilder("cmd", "/c", command)
                    : new ProcessBuilder("sh", "-c", command);
            builder.redirectErrorStream(true);
            builder.redirectOutput(log.toFile());
            var process = builder.start();
            var finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
                conversionService.markFailedForGateway(job, "CONVERTER_TIMEOUT", "CAD conversion exceeded " + timeoutSeconds + " seconds");
                return;
            }
            var outputLog = Files.isRegularFile(log)
                    ? Files.readString(log, StandardCharsets.UTF_8)
                    : "";
            var exitCode = process.exitValue();
            if (exitCode != 0 || !Files.isRegularFile(output)) {
                conversionService.markFailedForGateway(job, "CONVERTER_FAILED", truncate(outputLog));
                return;
            }
            var result = fileStorageService.storeBytes(Files.readAllBytes(output), output.getFileName().toString(), "image/svg+xml", sourceFile.getCreatedBy(), job.getId());
            conversionService.markSucceededForGateway(job, result.fileId());
        } catch (Exception exception) {
            conversionService.markFailedForGateway(job, "CONVERTER_ERROR", truncate(exception.getMessage()));
        } finally {
            deleteIfPresent(input);
            deleteIfPresent(output);
            deleteIfPresent(log);
        }
    }

    private String quote(String value) {
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private String truncate(String value) {
        if (value == null || value.isBlank()) return "CAD conversion failed";
        return value.length() > 500 ? value.substring(0, 500) : value;
    }

    private void deleteIfPresent(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); } catch (Exception ignored) { }
    }
}
