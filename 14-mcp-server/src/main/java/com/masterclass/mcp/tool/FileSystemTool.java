package com.masterclass.mcp.tool;

import com.masterclass.mcp.config.McpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

/**
 * MCP Tool: sandboxed file system operations.
 *
 * All paths are resolved relative to app.mcp.filesystem.allowed-base-path.
 * Path traversal attacks (../../../etc/passwd) are blocked by resolving
 * the canonical path and checking it is still inside the sandbox.
 */
@Component
public class FileSystemTool {

    private static final Logger log = LoggerFactory.getLogger(FileSystemTool.class);
    private static final long MAX_FILE_SIZE_BYTES = 1024 * 1024; // 1 MB

    private final Path sandboxRoot;

    public FileSystemTool(McpProperties props) throws IOException {
        this.sandboxRoot = Path.of(props.filesystem().allowedBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(sandboxRoot);
        log.info("MCP FileSystemTool sandbox: {}", sandboxRoot);
    }

    @Tool(description = """
            List files and directories at the given path inside the MCP sandbox.
            The path is relative to the sandbox root — do not use absolute paths.
            Returns a newline-separated list of file names with type indicators:
              [DIR]  subdirectory
              [FILE] regular file (with size in bytes)
            """)
    public String listFiles(
            @ToolParam(description = "Relative path inside the sandbox, e.g. '.' or 'reports/2024'") String relativePath) {
        try {
            Path target = resolve(relativePath);
            if (!Files.isDirectory(target)) {
                return "Error: path is not a directory.";
            }
            return Files.list(target)
                    .map(p -> {
                        if (Files.isDirectory(p)) return "[DIR]  " + p.getFileName();
                        try { return "[FILE] " + p.getFileName() + " (" + Files.size(p) + " bytes)"; }
                        catch (IOException e) { return "[FILE] " + p.getFileName(); }
                    })
                    .collect(Collectors.joining("\n"));
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            log.warn("listFiles failed: {}", e.getMessage());
            return "Error reading directory: " + e.getMessage();
        }
    }

    @Tool(description = """
            Read the contents of a text file inside the MCP sandbox.
            The path is relative to the sandbox root.
            Returns the file contents as a string (UTF-8).
            Maximum file size: 1 MB. Binary files are not supported.
            """)
    public String readFile(
            @ToolParam(description = "Relative path to the file, e.g. 'reports/summary.txt'") String relativePath) {
        try {
            Path target = resolve(relativePath);
            if (Files.size(target) > MAX_FILE_SIZE_BYTES) {
                return "Error: file exceeds 1 MB limit.";
            }
            return Files.readString(target);
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            log.warn("readFile failed: {}", e.getMessage());
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = """
            Write text content to a file inside the MCP sandbox.
            The path is relative to the sandbox root.
            Creates the file and any missing parent directories.
            Overwrites the file if it already exists.
            Returns 'OK' on success or an error message.
            """)
    public String writeFile(
            @ToolParam(description = "Relative path to write, e.g. 'output/result.txt'") String relativePath,
            @ToolParam(description = "The text content to write to the file") String content) {
        try {
            Path target = resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("MCP wrote file: {}", target);
            return "OK — wrote " + content.length() + " characters to " + relativePath;
        } catch (SecurityException e) {
            return "Error: " + e.getMessage();
        } catch (IOException e) {
            log.warn("writeFile failed: {}", e.getMessage());
            return "Error writing file: " + e.getMessage();
        }
    }

    /**
     * Resolves a user-supplied relative path against the sandbox root.
     * Throws SecurityException if the resolved path escapes the sandbox
     * (prevents path traversal attacks like ../../etc/passwd).
     */
    private Path resolve(String relativePath) {
        Path resolved = sandboxRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(sandboxRoot)) {
            throw new SecurityException("Path escapes sandbox: " + relativePath);
        }
        return resolved;
    }
}
