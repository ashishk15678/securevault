package storage;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom filesystem storage implementation
 * Handles file operations, directory management, and metadata storage
 */
public class Filesystem {

    private final String basePath;
    private final Map<String, FileMetadata> fileMetadata;
    private final Map<String, Set<String>> directoryContents;

    public Filesystem(String basePath) {
        this.basePath = basePath;
        this.fileMetadata = new ConcurrentHashMap<>();
        this.directoryContents = new ConcurrentHashMap<>();

        createDirectoryIfNotExists(basePath);
        loadMetadata();
    }

    public boolean storeFile(String path, byte[] data, Map<String, String> metadata) {
        try {
            String fullPath = getFullPath(path);
            File file = new File(fullPath);

            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Write file data
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            }

            // Store metadata
            FileMetadata fileMeta = new FileMetadata(
                    path,
                    data.length,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    metadata
            );
            fileMetadata.put(path, fileMeta);

            // Update directory contents
            updateDirectoryContents(path);

            // Save metadata to disk
            saveMetadata();

            return true;
        } catch (IOException e) {
            System.err.println("Error storing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieve a file from the filesystem
     */
    public byte[] retrieveFile(String path) {
        try {
            String fullPath = getFullPath(path);
            File file = new File(fullPath);

            if (!file.exists()) {
                return null;
            }

            byte[] data = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(data);
            }

            // Update access time
            FileMetadata meta = fileMetadata.get(path);
            if (meta != null) {
                meta.setLastAccessed(System.currentTimeMillis());
                saveMetadata();
            }

            return data;
        } catch (IOException e) {
            System.err.println("Error retrieving file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete a file from the filesystem
     */
    public boolean deleteFile(String path) {
        try {
            String fullPath = getFullPath(path);
            File file = new File(fullPath);

            if (!file.exists()) {
                return false;
            }

            boolean deleted = file.delete();
            if (deleted) {
                fileMetadata.remove(path);
                updateDirectoryContents(path);
                saveMetadata();
            }

            return deleted;
        } catch (Exception e) {
            System.err.println("Error deleting file: " + e.getMessage());
            return false;
        }
    }

    /**
     * List files in a directory
     */
    public List<String> listFiles(String directoryPath) {
        String normalizedPath = normalizePath(directoryPath);
        Set<String> contents = directoryContents.get(normalizedPath);

        if (contents == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(contents);
    }

    /**
     * Get file metadata
     */
    public FileMetadata getFileMetadata(String path) {
        return fileMetadata.get(path);
    }

    /**
     * Check if a file exists
     */
    public boolean fileExists(String path) {
        String fullPath = getFullPath(path);
        return new File(fullPath).exists();
    }

    /**
     * Get file size
     */
    public long getFileSize(String path) {
        FileMetadata meta = fileMetadata.get(path);
        return meta != null ? meta.getSize() : -1;
    }

    /**
     * Create a directory
     */
    public boolean createDirectory(String path) {
        try {
            String fullPath = getFullPath(path);
            File dir = new File(fullPath);

            boolean created = dir.mkdirs();
            if (created) {
                updateDirectoryContents(path);
                saveMetadata();
            }

            return created;
        } catch (Exception e) {
            System.err.println("Error creating directory: " + e.getMessage());
            return false;
        }
    }

    /**
     * Delete a directory
     */
    public boolean deleteDirectory(String path) {
        try {
            String fullPath = getFullPath(path);
            File dir = new File(fullPath);

            if (!dir.exists() || !dir.isDirectory()) {
                return false;
            }

            // Remove all files in directory
            List<String> files = listFiles(path);
            for (String file : files) {
                deleteFile(file);
            }

            boolean deleted = dir.delete();
            if (deleted) {
                directoryContents.remove(normalizePath(path));
                saveMetadata();
            }

            return deleted;
        } catch (Exception e) {
            System.err.println("Error deleting directory: " + e.getMessage());
            return false;
        }
    }

    /**
     * Search for files by name pattern
     */
    public List<String> searchFiles(String pattern) {
        List<String> results = new ArrayList<>();

        for (String path : fileMetadata.keySet()) {
            if (path.contains(pattern)) {
                results.add(path);
            }
        }

        return results;
    }

    /**
     * Get storage statistics
     */
    public StorageStats getStorageStats() {
        long totalSize = 0;
        int fileCount = 0;
        int directoryCount = directoryContents.size();

        for (FileMetadata meta : fileMetadata.values()) {
            totalSize += meta.getSize();
            fileCount++;
        }

        return new StorageStats(totalSize, fileCount, directoryCount);
    }

    private String getFullPath(String path) {
        return basePath + File.separator + path.replace("/", File.separator);
    }

    private String normalizePath(String path) {
        return path.replace("\\", "/").replaceAll("/+", "/");
    }

    private void createDirectoryIfNotExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void updateDirectoryContents(String filePath) {
        String normalizedPath = normalizePath(filePath);
        String parentDir = getParentDirectory(normalizedPath);

        if (parentDir != null) {
            directoryContents.computeIfAbsent(parentDir, k -> new HashSet<>()).add(normalizedPath);
        }
    }

    private String getParentDirectory(String path) {
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        }
        return null;
    }

    private void loadMetadata() {
        try {
            String metadataPath = basePath + File.separator + ".metadata";
            File metadataFile = new File(metadataPath);

            if (!metadataFile.exists()) {
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(metadataFile))) {
                @SuppressWarnings("unchecked")
                Map<String, FileMetadata> loadedMetadata = (Map<String, FileMetadata>) ois.readObject();
                fileMetadata.putAll(loadedMetadata);

                @SuppressWarnings("unchecked")
                Map<String, Set<String>> loadedContents = (Map<String, Set<String>>) ois.readObject();
                directoryContents.putAll(loadedContents);
            }
        } catch (Exception e) {
            System.err.println("Error loading metadata: " + e.getMessage());
        }
    }

    private void saveMetadata() {
        try {
            String metadataPath = basePath + File.separator + ".metadata";
            File metadataFile = new File(metadataPath);

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(metadataFile))) {
                oos.writeObject(fileMetadata);
                oos.writeObject(directoryContents);
            }
        } catch (Exception e) {
            System.err.println("Error saving metadata: " + e.getMessage());
        }
    }

    /**
     * File metadata class
     */
    public static class FileMetadata implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String path;
        private final long size;
        private final long created;
        private long lastAccessed;
        private final Map<String, String> metadata;

        public FileMetadata(String path, long size, long created, long lastAccessed, Map<String, String> metadata) {
            this.path = path;
            this.size = size;
            this.created = created;
            this.lastAccessed = lastAccessed;
            this.metadata = new HashMap<>(metadata);
        }

        public String getPath() { return path; }
        public long getSize() { return size; }
        public long getCreated() { return created; }
        public long getLastAccessed() { return lastAccessed; }
        public Map<String, String> getMetadata() { return new HashMap<>(metadata); }

        public void setLastAccessed(long lastAccessed) {
            this.lastAccessed = lastAccessed;
        }
    }

    /**
     * Storage statistics class
     */
    public static class StorageStats {
        private final long totalSize;
        private final int fileCount;
        private final int directoryCount;

        public StorageStats(long totalSize, int fileCount, int directoryCount) {
            this.totalSize = totalSize;
            this.fileCount = fileCount;
            this.directoryCount = directoryCount;
        }

        public long getTotalSize() { return totalSize; }
        public int getFileCount() { return fileCount; }
        public int getDirectoryCount() { return directoryCount; }

        @Override
        public String toString() {
            return String.format("StorageStats{totalSize=%d bytes, fileCount=%d, directoryCount=%d}",
                    totalSize, fileCount, directoryCount);
        }
    }
}
