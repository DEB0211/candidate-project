package com.bis.qa.sftp;

import com.bis.qa.config.TestConfig;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import io.qameta.allure.Step;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Vector;

/**
 * Thin JSch wrapper for uploading bond CSV files to the SFTP ingestion server
 * (PRODUCT.md section 5, docker-compose {@code sftp} service on port 2222).
 *
 * <p>The modern {@code com.github.mwiede:jsch} fork is used so that the current
 * key-exchange and cipher suites offered by {@code atmoz/sftp} negotiate cleanly.
 */
public class SftpClient implements AutoCloseable {

    private final Session session;
    private final ChannelSftp channel;

    public SftpClient() {
        try {
            JSch jsch = new JSch();
            this.session = jsch.getSession(
                    TestConfig.sftpUsername(),
                    TestConfig.sftpHost(),
                    TestConfig.sftpPort());
            session.setPassword(TestConfig.sftpPassword());

            Properties config = new Properties();
            // Test SFTP server: skip host-key verification.
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);

            session.connect(15000);

            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect(15000);
            this.channel = sftp;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open SFTP session to "
                    + TestConfig.sftpHost() + ":" + TestConfig.sftpPort(), e);
        }
    }

    /**
     * Uploads CSV content as {@code <remoteDir>/<fileName>}.
     *
     * @return the full remote path written.
     */
    @Step("SFTP upload {0}")
    public String upload(String fileName, String csvContent) {
        String remotePath = TestConfig.sftpRemoteDir() + "/" + fileName;
        try (ByteArrayInputStream in =
                     new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))) {
            channel.put(in, remotePath);
            return remotePath;
        } catch (Exception e) {
            throw new IllegalStateException("SFTP upload failed for " + remotePath, e);
        }
    }

    /** Lists file names currently present in the remote upload directory. */
    @Step("SFTP list {0}")
    @SuppressWarnings("unchecked")
    public java.util.List<String> list() {
        try {
            Vector<ChannelSftp.LsEntry> entries =
                    (Vector<ChannelSftp.LsEntry>) channel.ls(TestConfig.sftpRemoteDir());
            return entries.stream()
                    .map(ChannelSftp.LsEntry::getFilename)
                    .filter(name -> !name.equals(".") && !name.equals(".."))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("SFTP list failed for " + TestConfig.sftpRemoteDir(), e);
        }
    }

    @Override
    public void close() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
