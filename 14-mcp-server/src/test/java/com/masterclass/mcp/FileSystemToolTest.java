package com.masterclass.mcp;

import com.masterclass.mcp.config.McpProperties;
import com.masterclass.mcp.tool.FileSystemTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemToolTest {

    @TempDir
    Path tempDir;

    private FileSystemTool tool() throws IOException {
        var props = new McpProperties(
                new McpProperties.Filesystem(tempDir.toString()),
                new McpProperties.Api(10)
        );
        return new FileSystemTool(props);
    }

    @Test
    void writeAndReadFile() throws IOException {
        var fs = tool();
        String writeResult = fs.writeFile("test.txt", "hello world");
        assertThat(writeResult).startsWith("OK");

        String readResult = fs.readFile("test.txt");
        assertThat(readResult).isEqualTo("hello world");
    }

    @Test
    void listFilesShowsWrittenFile() throws IOException {
        var fs = tool();
        fs.writeFile("report.txt", "data");
        String listing = fs.listFiles(".");
        assertThat(listing).contains("report.txt");
    }

    @Test
    void pathTraversalIsBlocked() throws IOException {
        var fs = tool();
        String result = fs.readFile("../../etc/passwd");
        assertThat(result).contains("Error");
    }

    @Test
    void readNonExistentFileReturnsError() throws IOException {
        var fs = tool();
        String result = fs.readFile("nonexistent.txt");
        assertThat(result).contains("Error");
    }
}
